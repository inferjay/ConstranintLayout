/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.support.constraint;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import android.support.constraint.solver.LinearSystem;
import android.support.constraint.solver.widgets.Animator;
import android.support.constraint.solver.widgets.ConstraintAnchor;
import android.support.constraint.solver.widgets.ConstraintWidget;
import android.support.constraint.solver.widgets.Guideline;
import android.support.constraint.solver.widgets.ConstraintWidgetContainer;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * A Layout where the positions of the children is described as constraints in relation to each
 * other or to the parent.
 * <p>
 * <p>
 * Note that you cannot have a circular dependency in constraints
 * </p>
 * Also see {@link ConstraintLayout.LayoutParams
 * ConstraintLayout.LayoutParams} for layout attributes
 * </p>
 */
public class ConstraintLayout extends ViewGroup {
    // For now, disallow embedded (single-layer resolution) situations.
    // While it works, the constraints of the layout have the same importance as any other
    // constraint of the overall layout, which can cause issues. Let's revisit this
    // after implementing priorities/hierarchy of constraints.
    static final boolean ALLOWS_EMBEDDED = false;

    private static final String TAG = "ConstraintLayout";

    private final ArrayList<ConstraintWidget> mConstrainedWidgets = new ArrayList<>();
    private final ArrayList<ConstraintWidget> mSizeDependentsWidgets = new ArrayList<>();

    private final LinearSystem mEquationSystem = new LinearSystem();

    ConstraintWidgetContainer mLayoutWidget = null;

    private boolean mDirtyHierarchy;

    public ConstraintLayout(Context context) {
        super(context);
    }

    public ConstraintLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ConstraintLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    ConstraintWidgetContainer getLayoutWidget() {
        if (mLayoutWidget == null) {
            mLayoutWidget = new ConstraintWidgetContainer();
        }
        return mLayoutWidget;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        updateHierarchy();
    }

    private void updateHierarchy() {
        if (mLayoutWidget != null) {
            mLayoutWidget.reset();
        }
        mConstrainedWidgets.clear();
        mSizeDependentsWidgets.clear();

        mLayoutWidget = getLayoutWidget();
        mLayoutWidget.setCompanionWidget(this);
        final int count = getChildCount();

        // First, let's gather all the children
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            addConstrainedChild(child);
        }
        setChildrenConstraints();
    }

    private void addConstrainedChild(View child) {
        ConstraintWidget widget = getViewWidget(child);
        // TODO: see if we can only partially reset the widget
        widget.reset();
        mConstrainedWidgets.add(widget);
        widget.setCompanionWidget(child);
        mLayoutWidget.add(widget);
    }

    void setChildrenConstraints() {
        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            ConstraintWidget widget = getViewWidget(child);
            if (widget == null) {
                continue;
            }
            if (child instanceof android.support.constraint.Guideline) {
                if (!(widget instanceof Guideline)) {
                    ((LayoutParams) child.getLayoutParams()).widget = new Guideline();
                    widget = getViewWidget(child);
                    if (widget == null) {
                        continue;
                    }
                }
            }

            widget.setVisibility(child.getVisibility());
            widget.setParent(mLayoutWidget);

            final LayoutParams layoutParams = (LayoutParams) child.getLayoutParams();
            if ((widget instanceof Guideline)
                && (layoutParams.relativeBegin != LayoutParams.UNSET)
                || (layoutParams.relativeEnd != LayoutParams.UNSET)
                || (layoutParams.relativePercent != LayoutParams.UNSET)) {
                Guideline guideline = (Guideline) widget;
                if (layoutParams.relativeBegin != -1) {
                    guideline.setRelativeBegin(layoutParams.relativeBegin);
                }
                if (layoutParams.relativeEnd != -1) {
                    guideline.setRelativeEnd(layoutParams.relativeEnd);
                }
                if (layoutParams.relativePercent != -1) {
                    guideline.setRelativePercent(layoutParams.relativePercent);
                }
                if (layoutParams.orientation == LayoutParams.VERTICAL) {
                    guideline.setOrientation(Guideline.VERTICAL);
                } else {
                    guideline.setOrientation(Guideline.HORIZONTAL);
                }
            } else if ((layoutParams.lefToLeft != LayoutParams.UNSET)
                    || (layoutParams.leftToRight != LayoutParams.UNSET)
                    || (layoutParams.rightToLeft != LayoutParams.UNSET)
                    || (layoutParams.rightToRight != LayoutParams.UNSET)
                    || (layoutParams.topToTop != LayoutParams.UNSET)
                    || (layoutParams.topToBottom != LayoutParams.UNSET)
                    || (layoutParams.bottomToTop != LayoutParams.UNSET)
                    || (layoutParams.bottomToBottom != LayoutParams.UNSET)
                    || (layoutParams.baselineToBaseline != LayoutParams.UNSET)
                    || (layoutParams.centerXToCenterX != LayoutParams.UNSET)
                    || (layoutParams.centerYToCenterY != LayoutParams.UNSET)
                    || (layoutParams.editorAbsoluteX != LayoutParams.UNSET)
                    || (layoutParams.editorAbsoluteY != LayoutParams.UNSET)) {

                // Process match_Parent converting it to 0dp & constrain left and right to root
                if (layoutParams.width == LayoutParams.MATCH_PARENT) {
                    widget.connect(ConstraintAnchor.Type.LEFT, mLayoutWidget,
                            ConstraintAnchor.Type.LEFT, layoutParams.leftMargin);
                    widget.connect(ConstraintAnchor.Type.RIGHT, mLayoutWidget,
                            ConstraintAnchor.Type.RIGHT, layoutParams.rightMargin);
                    layoutParams.width = 0;
                    layoutParams.horizontalLock = false;
                }
                // Process match_Parent converting it to 0dp & constrain top and bottom to root
                if (layoutParams.height == LayoutParams.MATCH_PARENT) {
                    widget.connect(ConstraintAnchor.Type.TOP, mLayoutWidget,
                            ConstraintAnchor.Type.TOP, layoutParams.topMargin);
                    widget.connect(ConstraintAnchor.Type.BOTTOM, mLayoutWidget,
                            ConstraintAnchor.Type.BOTTOM, layoutParams.bottomMargin);
                    layoutParams.height = 0;
                    layoutParams.verticalLock = false;
                }

                // Left constraint
                if (layoutParams.lefToLeft != LayoutParams.UNSET) {
                    View view = findViewById(layoutParams.lefToLeft);
                    ConstraintWidget target = getViewWidget(view);
                    if (target != null) {
                        widget.connect(ConstraintAnchor.Type.LEFT, target,
                                ConstraintAnchor.Type.LEFT, layoutParams.leftMargin);
                    }
                }
                if (layoutParams.leftToRight != LayoutParams.UNSET) {
                    View view = findViewById(layoutParams.leftToRight);
                    ConstraintWidget target = getViewWidget(view);
                    if (target != null) {
                        widget.connect(ConstraintAnchor.Type.LEFT, target,
                                ConstraintAnchor.Type.RIGHT, layoutParams.leftMargin);
                    }
                }

                // Right constraint
                if (layoutParams.rightToLeft != LayoutParams.UNSET) {
                    View view = findViewById(layoutParams.rightToLeft);
                    ConstraintWidget target = getViewWidget(view);
                    if (target != null) {
                        widget.connect(ConstraintAnchor.Type.RIGHT, target,
                                ConstraintAnchor.Type.LEFT, layoutParams.rightMargin);
                    }
                }
                if (layoutParams.rightToRight != LayoutParams.UNSET) {
                    View view = findViewById(layoutParams.rightToRight);
                    ConstraintWidget target = getViewWidget(view);
                    if (target != null) {
                        widget.connect(ConstraintAnchor.Type.RIGHT, target,
                                ConstraintAnchor.Type.RIGHT, layoutParams.rightMargin);
                    }
                }

                // Top constraint
                if (layoutParams.topToTop != LayoutParams.UNSET) {
                    View view = findViewById(layoutParams.topToTop);
                    ConstraintWidget target = getViewWidget(view);
                    if (target != null) {
                        widget.connect(ConstraintAnchor.Type.TOP, target,
                                ConstraintAnchor.Type.TOP, layoutParams.topMargin);
                    }
                }
                if (layoutParams.topToBottom != LayoutParams.UNSET) {
                    View view = findViewById(layoutParams.topToBottom);
                    ConstraintWidget target = getViewWidget(view);
                    if (target != null) {
                        widget.connect(ConstraintAnchor.Type.TOP, target,
                                ConstraintAnchor.Type.BOTTOM, layoutParams.topMargin);
                    }
                }

                // Bottom constraint
                if (layoutParams.bottomToTop != LayoutParams.UNSET) {
                    View view = findViewById(layoutParams.bottomToTop);
                    ConstraintWidget target = getViewWidget(view);
                    if (target != null) {
                        widget.connect(ConstraintAnchor.Type.BOTTOM, target,
                                ConstraintAnchor.Type.TOP, layoutParams.bottomMargin);
                    }
                }
                if (layoutParams.bottomToBottom != LayoutParams.UNSET) {
                    View view = findViewById(layoutParams.bottomToBottom);
                    ConstraintWidget target = getViewWidget(view);
                    if (target != null) {
                        widget.connect(ConstraintAnchor.Type.BOTTOM, target,
                                ConstraintAnchor.Type.BOTTOM, layoutParams.bottomMargin);
                    }
                }

                // Baseline constraint
                if (layoutParams.baselineToBaseline != LayoutParams.UNSET) {
                    View view = findViewById(layoutParams.baselineToBaseline);
                    ConstraintWidget target = getViewWidget(view);
                    if (target != null) {
                        widget.connect(ConstraintAnchor.Type.BASELINE, target,
                                ConstraintAnchor.Type.BASELINE);
                    }
                }

                // Horizontal Center constraint
                if (layoutParams.centerXToCenterX != LayoutParams.UNSET) {
                    View view = findViewById(layoutParams.centerXToCenterX);
                    ConstraintWidget target = getViewWidget(view);
                    if (target != null) {
                        widget.connect(ConstraintAnchor.Type.LEFT, target,
                                ConstraintAnchor.Type.LEFT, layoutParams.leftMargin);
                        widget.connect(ConstraintAnchor.Type.RIGHT, target,
                                ConstraintAnchor.Type.RIGHT, layoutParams.rightMargin);
                    }
                }

                // Vertical Center constraint
                if (layoutParams.centerYToCenterY != LayoutParams.UNSET) {
                    View view = findViewById(layoutParams.centerYToCenterY);
                    ConstraintWidget target = getViewWidget(view);
                    if (target != null) {
                        widget.connect(ConstraintAnchor.Type.TOP, target,
                                ConstraintAnchor.Type.TOP, layoutParams.topMargin);
                        widget.connect(ConstraintAnchor.Type.BOTTOM, target,
                                ConstraintAnchor.Type.BOTTOM, layoutParams.bottomMargin);
                    }
                }
                if (layoutParams.horizontalBias >= 0 && layoutParams.horizontalBias != 0.5f) {
                    widget.setHorizontalBiasPercent(layoutParams.horizontalBias);
                }
                if (layoutParams.verticalBias >= 0 && layoutParams.verticalBias != 0.5f) {
                    widget.setVerticalBiasPercent(layoutParams.verticalBias);
                }

                // Set the strength
                widget.getAnchor(ConstraintAnchor.Type.LEFT).setStrength(ConstraintAnchor.Strength.STRONG);
                widget.getAnchor(ConstraintAnchor.Type.TOP).setStrength(ConstraintAnchor.Strength.STRONG);
                widget.getAnchor(ConstraintAnchor.Type.RIGHT).setStrength(ConstraintAnchor.Strength.STRONG);
                widget.getAnchor(ConstraintAnchor.Type.BOTTOM).setStrength(ConstraintAnchor.Strength.STRONG);

                // FIXME: need to agree on the correct magic value for this rather than simply using zero.
                if (!layoutParams.horizontalLock) {
                    widget.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.ANY);
                } else {
                    widget.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
                    widget.setWidth(layoutParams.width);
                }
                if (!layoutParams.verticalLock) {
                    widget.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.ANY);
                } else {
                    widget.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
                    widget.setHeight(layoutParams.height);
                }

                if (isInEditMode() && ((layoutParams.editorAbsoluteX != LayoutParams.UNSET)
                        || (layoutParams.editorAbsoluteY != LayoutParams.UNSET))) {
                    widget.setOrigin(layoutParams.editorAbsoluteX, layoutParams.editorAbsoluteY);
                }

                if (layoutParams.dimensionRatio > 0) {
                    widget.setDimensionRatio(layoutParams.dimensionRatio);
                }

                int baseline = child.getBaseline();
                if (baseline != -1) {
                    widget.setBaselineDistance(baseline);
                }
            }
        }
    }

    ConstraintWidget getViewWidget(View view) {
        if (view instanceof ConstraintLayout) return ((ConstraintLayout) view).getLayoutWidget();
        return view == null ? null : ((LayoutParams) view.getLayoutParams()).widget;
    }

    void internalMeasureChildren(int parentWidthSpec, int parentHeightSpec) {
        int heightPadding = getPaddingTop() + getPaddingBottom();
        int widthPadding = getPaddingLeft() + getPaddingRight();

        final int widgetsCount = mConstrainedWidgets.size();
        for (int i = 0; i < widgetsCount; i++) {
            ConstraintWidget widget = mConstrainedWidgets.get(i);
            if (widget == null) {
                continue;
            }
            if (widget instanceof Guideline) {
                continue;
            }

            final View child = (View) widget.getCompanionWidget();
            if (child == null || child.getVisibility() == GONE) {
                continue;
            }

            int width = child.getLayoutParams().width;
            int height = child.getLayoutParams().height;

            if (width == 0 || height == 0) {
                int childWidthMeasureSpec;
                int childHeightMeasureSpec;
                if (width == 0) {
                    childWidthMeasureSpec = getChildMeasureSpec(parentWidthSpec,
                            widthPadding, LayoutParams.WRAP_CONTENT);
                } else {
                    childWidthMeasureSpec = getChildMeasureSpec(parentWidthSpec,
                            widthPadding, width);
                }
                if (height == 0) {
                    childHeightMeasureSpec = getChildMeasureSpec(parentHeightSpec,
                            heightPadding, LayoutParams.WRAP_CONTENT);
                } else {
                    childHeightMeasureSpec = getChildMeasureSpec(parentHeightSpec,
                            heightPadding, height);
                }
                child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
            } else {
                measureChild(child, parentWidthSpec, parentHeightSpec);
            }

            width = child.getMeasuredWidth();
            height = child.getMeasuredHeight();
            widget.setWidth(width);
            widget.setHeight(height);

            int baseline = child.getBaseline();
            if (baseline != -1) {
                widget.setBaselineDistance(baseline);
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mDirtyHierarchy) {
            mDirtyHierarchy = false;
            updateHierarchy();
        }

        mLayoutWidget.setX(getPaddingLeft());
        mLayoutWidget.setY(getPaddingTop());

        setSelfDimensionBehaviour(widthMeasureSpec, heightMeasureSpec);
        internalMeasureChildren(widthMeasureSpec, heightMeasureSpec);

        //noinspection PointlessBooleanExpression
        if (ALLOWS_EMBEDDED && mLayoutWidget.getParent() != null) {
            setVisibility(INVISIBLE);
            return;
        }

        mSizeDependentsWidgets.clear();

        final int widgetsCount = mConstrainedWidgets.size();
        for (int i = 0; i < widgetsCount; i++) {
            ConstraintWidget widget = mConstrainedWidgets.get(i);
            if (widget instanceof Guideline) {
                continue;
            }
            if (widget.getHorizontalDimensionBehaviour() == ConstraintWidget.DimensionBehaviour.ANY
                    || widget.getVerticalDimensionBehaviour() == ConstraintWidget.DimensionBehaviour.ANY) {
                mSizeDependentsWidgets.add(widget);
            }
        }

        // let's solve the linear system.
        solveLinearSystem(); // first pass
        int childState = 0;

        // let's update the size dependent widgets if any...
        final int sizeDependentWidgetsCount = mSizeDependentsWidgets.size();

        int heightPadding = getPaddingTop() + getPaddingBottom();
        int widthPadding = getPaddingLeft() + getPaddingRight();

        if (sizeDependentWidgetsCount > 0) {
            for (int i = 0; i < sizeDependentWidgetsCount; i++) {
                ConstraintWidget widget = mSizeDependentsWidgets.get(i);
                if (widget instanceof Guideline) {
                    continue;
                }
                View child = (View) widget.getCompanionWidget();
                if (child == null) {
                    continue;
                }

                int widthSpec = MeasureSpec.makeMeasureSpec(widget.getWidth(), MeasureSpec.EXACTLY);
                int heightSpec = MeasureSpec.makeMeasureSpec(widget.getHeight(), MeasureSpec.EXACTLY);

                final ViewGroup.LayoutParams lp = child.getLayoutParams();
                if (lp.width == LayoutParams.WRAP_CONTENT) {
                    widthSpec = getChildMeasureSpec(widthMeasureSpec, widthPadding, lp.width);
                }
                if (lp.height == LayoutParams.WRAP_CONTENT) {
                    heightSpec = getChildMeasureSpec(heightMeasureSpec, heightPadding, lp.height);
                }

                // we need to re-measure the child...
                child.measure(widthSpec, heightSpec);

                int width = child.getMeasuredWidth();
                int height = child.getMeasuredHeight();
                widget.setWidth(width);
                widget.setHeight(height);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    childState = combineMeasuredStates(childState, child.getMeasuredState());
                }
            }
            solveLinearSystem(); // second pass
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            int widthSize = resolveSizeAndState(mLayoutWidget.getWidth(), widthMeasureSpec, childState);
            int heightSize = resolveSizeAndState(mLayoutWidget.getHeight(), heightMeasureSpec,
                    childState << MEASURED_HEIGHT_STATE_SHIFT);
            setMeasuredDimension(widthSize & MEASURED_SIZE_MASK, heightSize & MEASURED_SIZE_MASK);
        } else {
            setMeasuredDimension(mLayoutWidget.getWidth(), mLayoutWidget.getHeight());
        }
    }

    void setSelfDimensionBehaviour(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int heightPadding = getPaddingTop() + getPaddingBottom();
        int widthPadding = getPaddingLeft() + getPaddingRight();

        // TODO: investigate measure too small (check MeasureSpec)
        if (widthMode == MeasureSpec.AT_MOST) {
            mLayoutWidget.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
            mLayoutWidget.setWidth(0);
        } else {
            mLayoutWidget.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
            mLayoutWidget.setWidth(widthSize - widthPadding);
        }

        if (heightMode == MeasureSpec.AT_MOST) {
            mLayoutWidget.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
            mLayoutWidget.setHeight(0);
        } else {
            mLayoutWidget.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
            mLayoutWidget.setHeight(heightSize - heightPadding);
        }
    }

    /**
     * Solve the linear system
     */
    private void solveLinearSystem() {
        mEquationSystem.reset();
        Animator.setAnimationEnabled(false);
        mLayoutWidget.layout();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        final int widgetsCount = mConstrainedWidgets.size();
        for (int i = 0; i < widgetsCount; i++) {
            ConstraintWidget widget = mConstrainedWidgets.get(i);
            final View child = (View) widget.getCompanionWidget();

            if (child == null) {
                continue;
            }

            int l = widget.getDrawX();
            int t = widget.getDrawY();
            int r = l + widget.getWidth();
            int b = t + widget.getHeight();

            if (ALLOWS_EMBEDDED) {
                if (getParent() instanceof ConstraintLayout) {
                    int dx = 0;
                    int dy = 0;
                    ConstraintWidget item = getLayoutWidget(); // start with ourselves
                    while (item != null) {
                        dx += item.getDrawX();
                        dy += item.getDrawY();
                        item = item.getParent();
                    }
                    l -= dx;
                    t -= dy;
                    r -= dx;
                    b -= dy;
                }
            }
            child.layout(l, t, r, b);
        }
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new LayoutParams(p);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }

    public static class LayoutParams extends ViewGroup.MarginLayoutParams {
        public static int UNSET = -1;

        public static int HORIZONTAL = 0;
        public static int VERTICAL = 1;

        public int relativeBegin = -1;
        public int relativeEnd = -1;
        public int relativePercent = -1;

        public int lefToLeft = UNSET;
        public int leftToRight = UNSET;
        public int rightToLeft = UNSET;
        public int rightToRight = UNSET;
        public int topToTop = UNSET;
        public int topToBottom = UNSET;
        public int bottomToTop = UNSET;
        public int bottomToBottom = UNSET;
        public int baselineToBaseline = UNSET;
        public int centerXToCenterX = UNSET;
        public int centerYToCenterY = UNSET;

        public int startToEnd = UNSET;
        public int startToStart = UNSET;
        public int endToStart = UNSET;
        public int endToEnd = UNSET;

        public float horizontalBias = 0.5f;
        public float verticalBias = 0.5f;
        public float dimensionRatio = 0f;

        public int editorAbsoluteX = UNSET;
        public int editorAbsoluteY = UNSET;

        // TODO: Hide those for now (for table layout)
        int orientation = UNSET;
        int containerSkip = UNSET;

        // Internal use only
        boolean horizontalLock = true;
        boolean verticalLock = true;

        // Used by TableConstraintLayout
        // TODO: Inflate these from XML
        int numRows = 1;
        int numColumns = 1;
        String columnsAlignment = null;
        int padding = 0;

        ConstraintWidget widget = new ConstraintWidget();

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);

            TypedArray a = c.obtainStyledAttributes(attrs, R.styleable.ConstraintLayout_Layout);
            final int N = a.getIndexCount();
            for (int i = 0; i < N; i++) {
                int attr = a.getIndex(i);
                if (attr == R.styleable.ConstraintLayout_Layout_layout_constraintLeft_toLeftOf) {
                    lefToLeft = a.getResourceId(attr, lefToLeft);
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_constraintLeft_toRightOf) {
                    leftToRight = a.getResourceId(attr, leftToRight);
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_constraintRight_toLeftOf) {
                    rightToLeft = a.getResourceId(attr, rightToLeft);
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_constraintRight_toRightOf) {
                    rightToRight = a.getResourceId(attr, rightToRight);
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_constraintTop_toTopOf) {
                    topToTop = a.getResourceId(attr, topToTop);
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_constraintTop_toBottomOf) {
                    topToBottom = a.getResourceId(attr, topToBottom);
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_constraintBottom_toTopOf) {
                    bottomToTop = a.getResourceId(attr, bottomToTop);
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_constraintBottom_toBottomOf) {
                    bottomToBottom = a.getResourceId(attr, bottomToBottom);
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_constraintBaseline_toBaselineOf) {
                    baselineToBaseline = a.getResourceId(attr, baselineToBaseline);
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_constraintCenterX_toCenterX) {
                    centerXToCenterX = a.getResourceId(attr, centerXToCenterX);
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_constraintCenterY_toCenterY) {
                    centerYToCenterY = a.getResourceId(attr, centerYToCenterY);
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_editor_absoluteX) {
                    editorAbsoluteX = a.getDimensionPixelOffset(attr, editorAbsoluteX);
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_editor_absoluteY) {
                    editorAbsoluteY = a.getDimensionPixelOffset(attr, editorAbsoluteY);
                } else if (attr == R.styleable.ConstraintLayout_Layout_relativeBegin) {
                    relativeBegin = a.getDimensionPixelOffset(attr, relativeBegin);
                } else if (attr == R.styleable.ConstraintLayout_Layout_relativeEnd) {
                    relativeEnd = a.getDimensionPixelOffset(attr, relativeEnd);
                } else if (attr == R.styleable.ConstraintLayout_Layout_relativePercent) {
                    relativePercent = a.getInt(attr, relativePercent);
                } else if (attr == R.styleable.ConstraintLayout_Layout_orientation) {
                    orientation = a.getInt(attr, orientation);
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_constraintStart_toEndOf) {
                    startToEnd = a.getResourceId(attr, startToEnd);
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_constraintStart_toStartOf) {
                    startToStart = a.getResourceId(attr, startToStart);
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_constraintEnd_toStartOf) {
                    endToStart = a.getResourceId(attr, endToStart);
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_constraintEnd_toEndOf) {
                    endToEnd = a.getResourceId(attr, endToEnd);
                } else if (attr == R.styleable.ConstraintLayout_Layout_containerItemSkip) {
                    containerSkip = a.getInteger(attr, containerSkip);
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_constraintHorizontal_bias) {
                    horizontalBias = a.getFloat(attr, horizontalBias);
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_constraintVertical_bias) {
                    verticalBias = a.getFloat(attr, verticalBias);
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_constraintDimensionRatio) {
                    String ratio = a.getString(attr);
                    if (ratio != null) {
                        int colonIndex = ratio.indexOf(':');
                        if (colonIndex >= 0 && colonIndex < ratio.length() - 1) {
                            String nominator = ratio.substring(0, colonIndex);
                            String denominator = ratio.substring(colonIndex + 1);
                            if (nominator.length() > 0 && denominator.length() > 0) {
                                try {
                                    float nominatorValue = Float.parseFloat(nominator);
                                    float denominatorValue = Float.parseFloat(denominator);
                                    if (nominatorValue > 0 && denominatorValue > 0) {
                                        dimensionRatio = Math.abs(nominatorValue / denominatorValue);
                                    }
                                } catch (NumberFormatException e) {
                                    // Ignore
                                }
                            }
                        }
                    }
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_constraintLeft_creator) {
                    // Skip
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_constraintTop_creator) {
                    // Skip
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_constraintRight_creator) {
                    // Skip
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_constraintBottom_creator) {
                    // Skip
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_constraintBaseline_creator) {
                    // Skip
                } else {
                    Log.w(TAG, "Unknown attribute 0x" + Integer.toHexString(attr));
                }
            }

            if (width == 0) {
                horizontalLock = false;
            }
            if (height == 0) {
                verticalLock = false;
            }
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }

        @Override
        protected void setBaseAttributes(TypedArray a, int widthAttr, int heightAttr) {
            try {
                width = a.getLayoutDimension(widthAttr, "layout_width");
                height = a.getLayoutDimension(heightAttr, "layout_height");
            } catch (RuntimeException e) {
                // we ignore the runtime exception for now if layout_width and layout_height aren't there.
            }
        }

        @Override
        @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
        public void resolveLayoutDirection(int layoutDirection) {
            super.resolveLayoutDirection(layoutDirection);

            boolean isRtl = (View.LAYOUT_DIRECTION_RTL == getLayoutDirection());
            if (isRtl) {
                if (startToEnd != UNSET) {
                    rightToLeft = startToEnd;
                }
                if (startToStart != UNSET) {
                    rightToRight = startToStart;
                }
                if (endToStart != UNSET) {
                    leftToRight = endToStart;
                }
                if (endToEnd != UNSET) {
                    lefToLeft = endToEnd;
                }
            } else {
                if (startToEnd != UNSET) {
                    leftToRight = startToEnd;
                }
                if (startToStart != UNSET) {
                    lefToLeft = startToStart;
                }
                if (endToStart != UNSET) {
                    rightToLeft = endToStart;
                }
                if (endToEnd != UNSET) {
                    rightToRight = endToEnd;
                }
            }
        }
    }

    @Override
    public void requestLayout() {
        super.requestLayout();
        mDirtyHierarchy = true;
    }
}
