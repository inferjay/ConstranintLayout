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

    final HashMap<View, ConstraintWidget> mConstrainedViews = new HashMap<>();
    private final ArrayList<ConstraintWidget> mConstrainedWidgets = new ArrayList<>();
    private final HashMap<View, ConstraintWidget> mConstrainedViewTargets = new HashMap<>();
    private final ArrayList<ConstraintWidget> mConstrainedTargets = new ArrayList<>();
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
        mLayoutWidget = null;
        mConstrainedViews.clear();
        mConstrainedWidgets.clear();
        mConstrainedViewTargets.clear();
        mConstrainedTargets.clear();
        mSizeDependentsWidgets.clear();

        mLayoutWidget = getLayoutWidget();
        mLayoutWidget.setCompanionWidget(this);
        mConstrainedViews.put(this, mLayoutWidget);
        final int count = getChildCount();

        // First, let's gather all the children
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (!mConstrainedViews.containsKey(child)) {
                ConstraintWidget widget;
                if (child instanceof Guideline) {
                    final LayoutParams layoutParams = (LayoutParams) child.getLayoutParams();
                    android.support.constraint.solver.widgets.Guideline guideline = new android.support.constraint.solver.widgets.Guideline();
                    if (layoutParams.orientation == LayoutParams.HORIZONTAL) {
                        guideline.setOrientation(android.support.constraint.solver.widgets.Guideline.VERTICAL);
                    } else {
                        guideline.setOrientation(android.support.constraint.solver.widgets.Guideline.HORIZONTAL);
                    }
                    if (layoutParams.relativeBegin != -1) {
                        guideline.setRelativeBegin(layoutParams.relativeBegin);
                    }
                    if (layoutParams.relativeEnd != -1) {
                        guideline.setRelativeEnd(layoutParams.relativeEnd);
                    }
                    if (layoutParams.relativePercent != -1) {
                        guideline.setRelativePercent(layoutParams.relativePercent);
                    }
                    guideline.setCompanionWidget(child);
                    guideline.setParent(mLayoutWidget);
                    mLayoutWidget.add(guideline);
                    mConstrainedTargets.add(guideline);
                    mConstrainedViewTargets.put(child, guideline);
                    continue;
                } else if (ALLOWS_EMBEDDED && child instanceof ConstraintLayout) {
                    widget = ((ConstraintLayout) child).getLayoutWidget();
                } else {
                    widget = new ConstraintWidget();
                }
                addConstrainedChild(child, widget);
            }
        }
        setChildrenConstraints();
    }

    private void addConstrainedChild(View child, ConstraintWidget widget) {
        mConstrainedViews.put(child, widget);
        mConstrainedWidgets.add(widget);
        widget.setCompanionWidget(child);
        if (mLayoutWidget != null) {
            ConstraintWidgetContainer container = mLayoutWidget;
            container.add(widget);
        }
    }

    void setChildrenConstraints() {
        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            ConstraintWidget widget = mConstrainedViews.get(child);
            if (widget == null) {
                continue;
            }

            widget.setVisibility(child.getVisibility());
            widget.setParent(mLayoutWidget);

            final LayoutParams layoutParams = (LayoutParams) child.getLayoutParams();
            if ((layoutParams.left_to_left != LayoutParams.UNSET)
                    || (layoutParams.left_to_right != LayoutParams.UNSET)
                    || (layoutParams.right_to_left != LayoutParams.UNSET)
                    || (layoutParams.right_to_right != LayoutParams.UNSET)
                    || (layoutParams.top_to_top != LayoutParams.UNSET)
                    || (layoutParams.top_to_bottom != LayoutParams.UNSET)
                    || (layoutParams.bottom_to_top != LayoutParams.UNSET)
                    || (layoutParams.bottom_to_bottom != LayoutParams.UNSET)
                    || (layoutParams.baseline_to_baseline != LayoutParams.UNSET)
                    || (layoutParams.centerX_to_centerX != LayoutParams.UNSET)
                    || (layoutParams.centerY_to_centerY != LayoutParams.UNSET)
                    || (layoutParams.editor_absolute_x != LayoutParams.UNSET)
                    || (layoutParams.editor_absolute_y != LayoutParams.UNSET)) {

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
                if (layoutParams.left_to_left != LayoutParams.UNSET) {
                    View view = findViewById(layoutParams.left_to_left);
                    ConstraintWidget target = mConstrainedViews.get(view);
                    if (target == null) {
                        target = mConstrainedViewTargets.get(view);
                    }
                    if (target != null) {
                        widget.connect(ConstraintAnchor.Type.LEFT, target,
                                ConstraintAnchor.Type.LEFT, layoutParams.leftMargin);
                    }
                }
                if (layoutParams.left_to_right != LayoutParams.UNSET) {
                    View view = findViewById(layoutParams.left_to_right);
                    ConstraintWidget target = mConstrainedViews.get(view);
                    if (target == null) {
                        target = mConstrainedViewTargets.get(view);
                    }
                    if (target != null) {
                        widget.connect(ConstraintAnchor.Type.LEFT, target,
                                ConstraintAnchor.Type.RIGHT, layoutParams.leftMargin);
                    }
                }

                // Right constraint
                if (layoutParams.right_to_left != LayoutParams.UNSET) {
                    View view = findViewById(layoutParams.right_to_left);
                    ConstraintWidget target = mConstrainedViews.get(view);
                    if (target == null) {
                        target = mConstrainedViewTargets.get(view);
                    }
                    if (target != null) {
                        widget.connect(ConstraintAnchor.Type.RIGHT, target,
                                ConstraintAnchor.Type.LEFT, layoutParams.rightMargin);
                    }
                }
                if (layoutParams.right_to_right != LayoutParams.UNSET) {
                    View view = findViewById(layoutParams.right_to_right);
                    ConstraintWidget target = mConstrainedViews.get(view);
                    if (target == null) {
                        target = mConstrainedViewTargets.get(view);
                    }
                    if (target != null) {
                        widget.connect(ConstraintAnchor.Type.RIGHT, target,
                                ConstraintAnchor.Type.RIGHT, layoutParams.rightMargin);
                    }
                }

                // Top constraint
                if (layoutParams.top_to_top != LayoutParams.UNSET) {
                    View view = findViewById(layoutParams.top_to_top);
                    ConstraintWidget target = mConstrainedViews.get(view);
                    if (target == null) {
                        target = mConstrainedViewTargets.get(view);
                    }
                    if (target != null) {
                        widget.connect(ConstraintAnchor.Type.TOP, target,
                                ConstraintAnchor.Type.TOP, layoutParams.topMargin);
                    }
                }
                if (layoutParams.top_to_bottom != LayoutParams.UNSET) {
                    View view = findViewById(layoutParams.top_to_bottom);
                    ConstraintWidget target = mConstrainedViews.get(view);
                    if (target == null) {
                        target = mConstrainedViewTargets.get(view);
                    }
                    if (target != null) {
                        widget.connect(ConstraintAnchor.Type.TOP, target,
                                ConstraintAnchor.Type.BOTTOM, layoutParams.topMargin);
                    }
                }

                // Bottom constraint
                if (layoutParams.bottom_to_top != LayoutParams.UNSET) {
                    View view = findViewById(layoutParams.bottom_to_top);
                    ConstraintWidget target = mConstrainedViews.get(view);
                    if (target == null) {
                        target = mConstrainedViewTargets.get(view);
                    }
                    if (target != null) {
                        widget.connect(ConstraintAnchor.Type.BOTTOM, target,
                                ConstraintAnchor.Type.TOP, layoutParams.bottomMargin);
                    }
                }
                if (layoutParams.bottom_to_bottom != LayoutParams.UNSET) {
                    View view = findViewById(layoutParams.bottom_to_bottom);
                    ConstraintWidget target = mConstrainedViews.get(view);
                    if (target == null) {
                        target = mConstrainedViewTargets.get(view);
                    }
                    if (target != null) {
                        widget.connect(ConstraintAnchor.Type.BOTTOM, target,
                                ConstraintAnchor.Type.BOTTOM, layoutParams.bottomMargin);
                    }
                }

                // Baseline constraint
                if (layoutParams.baseline_to_baseline != LayoutParams.UNSET) {
                    ConstraintWidget target = mConstrainedViews.get(findViewById(layoutParams.baseline_to_baseline));
                    if (target != null) {
                        widget.connect(ConstraintAnchor.Type.BASELINE, target,
                                ConstraintAnchor.Type.BASELINE);
                    }
                }

                // Horizontal Center constraint
                if (layoutParams.centerX_to_centerX != LayoutParams.UNSET) {
                    View view = findViewById(layoutParams.centerX_to_centerX);
                    ConstraintWidget target = mConstrainedViews.get(view);
                    if (target == null) {
                        target = mConstrainedViewTargets.get(view);
                    }
                    if (target != null) {
                        widget.connect(ConstraintAnchor.Type.LEFT, target,
                                ConstraintAnchor.Type.LEFT, layoutParams.leftMargin);
                        widget.connect(ConstraintAnchor.Type.RIGHT, target,
                                ConstraintAnchor.Type.RIGHT, layoutParams.rightMargin);
                    }
                }

                // Vertical Center constraint
                if (layoutParams.centerY_to_centerY != LayoutParams.UNSET) {
                    View view = findViewById(layoutParams.centerY_to_centerY);
                    ConstraintWidget target = mConstrainedViews.get(view);
                    if (target == null) {
                        target = mConstrainedViewTargets.get(view);
                    }
                    if (target != null) {
                        widget.connect(ConstraintAnchor.Type.TOP, target,
                                ConstraintAnchor.Type.TOP, layoutParams.topMargin);
                        widget.connect(ConstraintAnchor.Type.BOTTOM, target,
                                ConstraintAnchor.Type.BOTTOM, layoutParams.bottomMargin);
                    }
                }
                if (layoutParams.horizontal_bias >= 0 && layoutParams.horizontal_bias != 0.5f) {
                    widget.setHorizontalBiasPercent(layoutParams.horizontal_bias);
                }
                if (layoutParams.vertical_bias >= 0 && layoutParams.vertical_bias != 0.5f) {
                    widget.setVerticalBiasPercent(layoutParams.vertical_bias);
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

                if ((layoutParams.editor_absolute_x != LayoutParams.UNSET)
                        || (layoutParams.editor_absolute_y != LayoutParams.UNSET)) {
                    widget.setOrigin(layoutParams.editor_absolute_x, layoutParams.editor_absolute_y);
                }
                if (hasBaseline(child)) {
                    widget.setBaselineDistance(child.getBaseline());
                }
            }
        }
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

            if (hasBaseline(child)) {
                widget.setBaselineDistance(child.getBaseline());
            }
        }
    }

    private boolean hasBaseline(View view) {
        return (view instanceof EditText) || (view instanceof TextView) || (view instanceof Spinner);
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

        final int targetsCount = mConstrainedTargets.size();
        for (int i = 0; i < targetsCount; i++) {
            ConstraintWidget target = mConstrainedTargets.get(i);
            final View child = (View) target.getCompanionWidget();
            if (child == null) {
                continue;
            }
            int l = target.getDrawX();
            int t = target.getDrawY();
            int r = l + target.getWidth();
            int b = t + target.getHeight();

            if (target instanceof android.support.constraint.solver.widgets.Guideline) {
                android.support.constraint.solver.widgets.Guideline guideline =
                        (android.support.constraint.solver.widgets.Guideline) target;
                if (guideline.getOrientation() == android.support.constraint.solver.widgets.Guideline.HORIZONTAL) {
                    t = target.getParent().getDrawY();
                    b = target.getParent().getBottom();
                    r = l;
                } else {
                    l = target.getParent().getDrawX();
                    r = target.getParent().getRight();
                    b = t;
                }
            }
            child.layout(l, t, r, b);
        }
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        LayoutParams layoutParams = new ConstraintLayout.LayoutParams(getContext(), attrs);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            resolveRtlProperties(layoutParams);
        }
        return layoutParams;
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

        public int left_to_left = UNSET;
        public int left_to_right = UNSET;
        public int right_to_left = UNSET;
        public int right_to_right = UNSET;
        public int top_to_top = UNSET;
        public int top_to_bottom = UNSET;
        public int bottom_to_top = UNSET;
        public int bottom_to_bottom = UNSET;
        public int baseline_to_baseline = UNSET;
        public int centerX_to_centerX = UNSET;
        public int centerY_to_centerY = UNSET;

        public int start_to_end = UNSET;
        public int start_to_start = UNSET;
        public int end_to_start = UNSET;
        public int end_to_end = UNSET;

        public float horizontal_bias = 0.5f;
        public float vertical_bias = 0.5f;

        public int editor_absolute_x = UNSET;
        public int editor_absolute_y = UNSET;

        // TODO: Hide those for now (for table layout)
        int orientation = UNSET;
        int container_skip = UNSET;

        // Internal use only
        boolean horizontalLock = true;
        boolean verticalLock = true;

        // Used by TableConstraintLayout
        // TODO: Inflate these from XML
        int numRows = 1;
        int numColumns = 1;
        String columnsAlignment = null;
        int padding = 0;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
            TypedArray a = c.obtainStyledAttributes(attrs, R.styleable.ConstraintLayout_Layout);
            final int N = a.getIndexCount();
            for (int i = 0; i < N; i++) {
                int attr = a.getIndex(i);
                if (attr == R.styleable.ConstraintLayout_Layout_layout_constraintLeft_toLeftOf) {
                    left_to_left = a.getResourceId(attr, left_to_left);
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_constraintLeft_toRightOf) {
                    left_to_right = a.getResourceId(attr, left_to_right);
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_constraintRight_toLeftOf) {
                    right_to_left = a.getResourceId(attr, right_to_left);
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_constraintRight_toRightOf) {
                    right_to_right = a.getResourceId(attr, right_to_right);
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_constraintTop_toTopOf) {
                    top_to_top = a.getResourceId(attr, top_to_top);
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_constraintTop_toBottomOf) {
                    top_to_bottom = a.getResourceId(attr, top_to_bottom);
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_constraintBottom_toTopOf) {
                    bottom_to_top = a.getResourceId(attr, bottom_to_top);
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_constraintBottom_toBottomOf) {
                    bottom_to_bottom = a.getResourceId(attr, bottom_to_bottom);
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_constraintBaseline_toBaselineOf) {
                    baseline_to_baseline = a.getResourceId(attr, baseline_to_baseline);
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_constraintCenterX_toCenterX) {
                    centerX_to_centerX = a.getResourceId(attr, centerX_to_centerX);
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_constraintCenterY_toCenterY) {
                    centerY_to_centerY = a.getResourceId(attr, centerY_to_centerY);
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_editor_absoluteX) {
                    editor_absolute_x = a.getDimensionPixelOffset(attr, editor_absolute_x);
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_editor_absoluteY) {
                    editor_absolute_y = a.getDimensionPixelOffset(attr, editor_absolute_y);
                } else if (attr == R.styleable.ConstraintLayout_Layout_relativeBegin) {
                    relativeBegin = a.getDimensionPixelOffset(attr, relativeBegin);
                } else if (attr == R.styleable.ConstraintLayout_Layout_relativeEnd) {
                    relativeEnd = a.getDimensionPixelOffset(attr, relativeEnd);
                } else if (attr == R.styleable.ConstraintLayout_Layout_relativePercent) {
                    relativePercent = a.getInt(attr, relativePercent);
                } else if (attr == R.styleable.ConstraintLayout_Layout_orientation) {
                    orientation = a.getInt(attr, orientation);
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_constraintStart_toEndOf) {
                    start_to_end = a.getResourceId(attr, start_to_end);
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_constraintStart_toStartOf) {
                    start_to_start = a.getResourceId(attr, start_to_start);
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_constraintEnd_toStartOf) {
                    end_to_start = a.getResourceId(attr, end_to_start);
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_constraintEnd_toEndOf) {
                    end_to_end = a.getResourceId(attr, end_to_end);
                } else if (attr == R.styleable.ConstraintLayout_Layout_containerItemSkip) {
                    container_skip = a.getInteger(attr, container_skip);
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_constraintHorizontal_Bias) {
                    horizontal_bias = a.getFloat(attr, horizontal_bias);
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_constraintVertical_Bias) {
                    vertical_bias = a.getFloat(attr, vertical_bias);
                } else {
                    Log.w(TAG, " Unknown attribute 0x" + Integer.toHexString(attr));
                }
            }

            if (this.width == 0) {
                horizontalLock = false;
            }
            if (this.height == 0) {
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
    }

    @Override
    public void onRtlPropertiesChanged(int layoutDirection) {
        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            resolveRtlProperties((LayoutParams) getChildAt(i).getLayoutParams());
        }
        setChildrenConstraints();
    }

    /**
     * Swap start and end to left and right as needed by RTL
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private void resolveRtlProperties(LayoutParams layoutParams) {
        boolean isRtl = (View.LAYOUT_DIRECTION_RTL == getLayoutDirection());
        if (isRtl) {
            if (layoutParams.start_to_end != LayoutParams.UNSET) {
                layoutParams.right_to_left = layoutParams.start_to_end;
            }
            if (layoutParams.start_to_start != LayoutParams.UNSET) {
                layoutParams.right_to_right = layoutParams.start_to_start;
            }
            if (layoutParams.end_to_start != LayoutParams.UNSET) {
                layoutParams.left_to_right = layoutParams.end_to_start;
            }
            if (layoutParams.end_to_end != LayoutParams.UNSET) {
                layoutParams.left_to_left = layoutParams.end_to_end;
            }
        } else {
            if (layoutParams.start_to_end != LayoutParams.UNSET) {
                layoutParams.left_to_right = layoutParams.start_to_end;
            }
            if (layoutParams.start_to_start != LayoutParams.UNSET) {
                layoutParams.left_to_left = layoutParams.start_to_start;
            }
            if (layoutParams.end_to_start != LayoutParams.UNSET) {
                layoutParams.right_to_left = layoutParams.end_to_start;
            }
            if (layoutParams.end_to_end != LayoutParams.UNSET) {
                layoutParams.right_to_right = layoutParams.end_to_end;
            }
        }
    }

    @Override
    public void requestLayout() {
        super.requestLayout();
        mDirtyHierarchy = true;
    }
}
