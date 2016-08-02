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
import android.support.constraint.solver.widgets.Animator;
import android.support.constraint.solver.widgets.ConstraintAnchor;
import android.support.constraint.solver.widgets.ConstraintWidget;
import android.support.constraint.solver.widgets.ConstraintWidgetContainer;
import android.support.constraint.solver.widgets.Guideline;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

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
    private static final boolean SIMPLE_LAYOUT = true;

    SparseArray<View> mChildrenByIds = new SparseArray<>();
    private final ArrayList<ConstraintWidget> mSizeDependentsWidgets = new ArrayList<>(100);

    ConstraintWidgetContainer mLayoutWidget = new ConstraintWidgetContainer();

    private boolean mDirtyHierarchy = true;

    public ConstraintLayout(Context context) {
        super(context);
        init();
    }

    public ConstraintLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ConstraintLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mLayoutWidget.setCompanionWidget(this);
        mChildrenByIds.put(getId(), this);
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        super.addView(child, index, params);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            onViewAdded(child);
        }
    }

    @Override
    public void removeView(View view) {
        super.removeView(view);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            onViewRemoved(view);
        }
    }

    @Override
    public void onViewAdded(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            super.onViewAdded(view);
        }
        ConstraintWidget widget = getViewWidget(view);
        if (view instanceof android.support.constraint.Guideline) {
            if (!(widget instanceof Guideline)) {
                LayoutParams layoutParams = (LayoutParams) view.getLayoutParams();
                layoutParams.widget = new Guideline();
                layoutParams.isGuideline = true;
                widget = layoutParams.widget;
            }
        }
        ConstraintWidgetContainer container = mLayoutWidget;
        widget.setCompanionWidget(view);
        mChildrenByIds.put(view.getId(), view);
        container.add(widget);
        widget.setParent(container);
        updateHierarchy();
    }

    @Override
    public void onViewRemoved(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            super.onViewRemoved(view);
        }
        mChildrenByIds.remove(view.getId());
        mLayoutWidget.remove(getViewWidget(view));
        updateHierarchy();
    }

    private void updateHierarchy() {
        final int count = getChildCount();

        boolean recompute = false;
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.isLayoutRequested()) {
                recompute = true;
                break;
            }
        }
        if (recompute) {
            mSizeDependentsWidgets.clear();
            setChildrenConstraints();
        }
    }

    void setChildrenConstraints() {
        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            ConstraintWidget widget = getViewWidget(child);
            if (widget == null) {
                continue;
            }

            final LayoutParams layoutParams = (LayoutParams) child.getLayoutParams();

            widget.reset();
            widget.setParent(mLayoutWidget);
            widget.setVisibility(child.getVisibility());
            widget.setCompanionWidget(child);

            if (!layoutParams.verticalLock || !layoutParams.horizontalLock) {
                mSizeDependentsWidgets.add(widget);
            }

            if (layoutParams.isGuideline) {
                Guideline guideline = (Guideline) widget;
                if (layoutParams.guideBegin != -1) {
                    guideline.setGuideBegin(layoutParams.guideBegin);
                }
                if (layoutParams.guideEnd != -1) {
                    guideline.setGuideEnd(layoutParams.guideEnd);
                }
                if (layoutParams.guidePercent != -1) {
                    guideline.setGuidePercent(layoutParams.guidePercent);
                }
                if (layoutParams.orientation == LayoutParams.VERTICAL) {
                    guideline.setOrientation(Guideline.VERTICAL);
                } else {
                    guideline.setOrientation(Guideline.HORIZONTAL);
                }
            } else if ((layoutParams.leftToLeft != LayoutParams.UNSET)
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

                // Process match_Parent converting it to 0dp
                if (layoutParams.width == LayoutParams.MATCH_PARENT) {
                    layoutParams.horizontalLock = false;
                }
                // Process match_Parent converting it to 0dp
                if (layoutParams.height == LayoutParams.MATCH_PARENT) {
                    layoutParams.verticalLock = false;
                }

                // Left constraint
                if (layoutParams.leftToLeft != LayoutParams.UNSET) {
                    ConstraintWidget target = getTargetWidget(layoutParams.leftToLeft);
                    if (target != null) {
                        widget.immediateConnect(ConstraintAnchor.Type.LEFT, target,
                                ConstraintAnchor.Type.LEFT, layoutParams.leftMargin);
                    }
                } else if (layoutParams.leftToRight != LayoutParams.UNSET) {
                    ConstraintWidget target = getTargetWidget(layoutParams.leftToRight);
                    if (target != null) {
                        widget.immediateConnect(ConstraintAnchor.Type.LEFT, target,
                                ConstraintAnchor.Type.RIGHT, layoutParams.leftMargin);
                    }
                }

                // Right constraint
                if (layoutParams.rightToLeft != LayoutParams.UNSET) {
                    ConstraintWidget target = getTargetWidget(layoutParams.rightToLeft);
                    if (target != null) {
                        widget.immediateConnect(ConstraintAnchor.Type.RIGHT, target,
                                ConstraintAnchor.Type.LEFT, layoutParams.rightMargin);
                    }
                } else if (layoutParams.rightToRight != LayoutParams.UNSET) {
                    ConstraintWidget target = getTargetWidget(layoutParams.rightToRight);
                    if (target != null) {
                        widget.immediateConnect(ConstraintAnchor.Type.RIGHT, target,
                                ConstraintAnchor.Type.RIGHT, layoutParams.rightMargin);
                    }
                }

                // Top constraint
                if (layoutParams.topToTop != LayoutParams.UNSET) {
                    ConstraintWidget target = getTargetWidget(layoutParams.topToTop);
                    if (target != null) {
                        widget.immediateConnect(ConstraintAnchor.Type.TOP, target,
                                ConstraintAnchor.Type.TOP, layoutParams.topMargin);
                    }
                } else if (layoutParams.topToBottom != LayoutParams.UNSET) {
                    ConstraintWidget target = getTargetWidget(layoutParams.topToBottom);
                    if (target != null) {
                        widget.immediateConnect(ConstraintAnchor.Type.TOP, target,
                                ConstraintAnchor.Type.BOTTOM, layoutParams.topMargin);
                    }
                }

                // Bottom constraint
                if (layoutParams.bottomToTop != LayoutParams.UNSET) {
                    ConstraintWidget target = getTargetWidget(layoutParams.bottomToTop);
                    if (target != null) {
                        widget.immediateConnect(ConstraintAnchor.Type.BOTTOM, target,
                                ConstraintAnchor.Type.TOP, layoutParams.bottomMargin);
                    }
                } else if (layoutParams.bottomToBottom != LayoutParams.UNSET) {
                    ConstraintWidget target = getTargetWidget(layoutParams.bottomToBottom);
                    if (target != null) {
                        widget.immediateConnect(ConstraintAnchor.Type.BOTTOM, target,
                                ConstraintAnchor.Type.BOTTOM, layoutParams.bottomMargin);
                    }
                }

                // Baseline constraint
                if (layoutParams.baselineToBaseline != LayoutParams.UNSET) {
                    View view = mChildrenByIds.get(layoutParams.baselineToBaseline);
                    ConstraintWidget target = getTargetWidget(layoutParams.baselineToBaseline);
                    if (target != null) {
                        LayoutParams targetParams = (LayoutParams) view.getLayoutParams();
                        layoutParams.needsBaseline = true;
                        targetParams.needsBaseline = true;
                        ConstraintAnchor baseline = widget.getAnchor(ConstraintAnchor.Type.BASELINE);
                        ConstraintAnchor targetBaseline =
                                target.getAnchor(ConstraintAnchor.Type.BASELINE);
                        baseline.connect(targetBaseline, 0, ConstraintAnchor.Strength.STRONG,
                                ConstraintAnchor.USER_CREATOR, true);

                        widget.getAnchor(ConstraintAnchor.Type.TOP).reset();
                        widget.getAnchor(ConstraintAnchor.Type.BOTTOM).reset();
                    }
                }

                // Horizontal Center constraint
                if (layoutParams.centerXToCenterX != LayoutParams.UNSET) {
                    ConstraintWidget target = getTargetWidget(layoutParams.centerXToCenterX);
                    if (target != null) {
                        widget.immediateConnect(ConstraintAnchor.Type.LEFT, target,
                                ConstraintAnchor.Type.LEFT, layoutParams.leftMargin);
                        widget.immediateConnect(ConstraintAnchor.Type.RIGHT, target,
                                ConstraintAnchor.Type.RIGHT, layoutParams.rightMargin);
                    }
                }

                // Vertical Center constraint
                if (layoutParams.centerYToCenterY != LayoutParams.UNSET) {
                    ConstraintWidget target = getTargetWidget(layoutParams.centerYToCenterY);
                    if (target != null) {
                        widget.immediateConnect(ConstraintAnchor.Type.TOP, target,
                                ConstraintAnchor.Type.TOP, layoutParams.topMargin);
                        widget.immediateConnect(ConstraintAnchor.Type.BOTTOM, target,
                                ConstraintAnchor.Type.BOTTOM, layoutParams.bottomMargin);
                    }
                }
                if (layoutParams.horizontalBias >= 0 && layoutParams.horizontalBias != 0.5f) {
                    widget.setHorizontalBiasPercent(layoutParams.horizontalBias);
                }
                if (layoutParams.verticalBias >= 0 && layoutParams.verticalBias != 0.5f) {
                    widget.setVerticalBiasPercent(layoutParams.verticalBias);
                }

                // FIXME: need to agree on the correct magic value for this rather than simply using zero.
                if (!layoutParams.horizontalLock) {
                    widget.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.ANY);
                    widget.setWidth(0);
                    if (layoutParams.width == LayoutParams.MATCH_PARENT) {
                        widget.setWidth(mLayoutWidget.getWidth());
                    }
                } else {
                    widget.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
                    widget.setWidth(layoutParams.width);
                }
                if (!layoutParams.verticalLock) {
                    widget.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.ANY);
                    widget.setHeight(0);
                    if (layoutParams.height == LayoutParams.MATCH_PARENT) {
                        widget.setWidth(mLayoutWidget.getHeight());
                    }
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

            }
        }
    }

    private final ConstraintWidget getTargetWidget(int id) {
        if (id == LayoutParams.PARENT_ID) {
            return mLayoutWidget;
        } else {
            View view = mChildrenByIds.get(id);
            if (view == this) {
                return mLayoutWidget;
            }
            return view == null ? null : ((LayoutParams) view.getLayoutParams()).widget;
        }
    }

    private final ConstraintWidget getViewWidget(View view) {
        if (view == this) {
            return mLayoutWidget;
        }
        return view == null ? null : ((LayoutParams) view.getLayoutParams()).widget;
    }

    void internalMeasureChildren(int parentWidthSpec, int parentHeightSpec) {
        int heightPadding = getPaddingTop() + getPaddingBottom();
        int widthPadding = getPaddingLeft() + getPaddingRight();

        final int widgetsCount = getChildCount();
        for (int i = 0; i < widgetsCount; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() == GONE) {
                continue;
            }
            LayoutParams params = (LayoutParams) child.getLayoutParams();
            ConstraintWidget widget = params.widget;
            if (params.isGuideline) {
                continue;
            }

            int width = params.width;
            int height = params.height;

            final int childWidthMeasureSpec;
            final int childHeightMeasureSpec;

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

            width = child.getMeasuredWidth();
            height = child.getMeasuredHeight();
            widget.setWidth(width);
            widget.setHeight(height);

            if (params.needsBaseline) {
                int baseline = child.getBaseline();
                if (baseline != -1) {
                    widget.setBaselineDistance(baseline);
                }
            }
        }
    }

    int previousPaddingLeft = -1;
    int previousPaddingTop = -1;
    int previousWidthMeasureSpec = -1;
    int previousHeightMeasureSpec = -1;

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mDirtyHierarchy) {
            mDirtyHierarchy = false;
            updateHierarchy();
        }

        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();

        if (previousPaddingLeft == -1
                || previousPaddingTop == -1
                || previousHeightMeasureSpec == -1
                || previousWidthMeasureSpec == -1
                || previousPaddingLeft != paddingLeft
                || previousPaddingTop != paddingTop
                || previousWidthMeasureSpec != widthMeasureSpec
                || previousHeightMeasureSpec != heightMeasureSpec) {
            mLayoutWidget.setX(paddingLeft);
            mLayoutWidget.setY(paddingTop);
            setSelfDimensionBehaviour(widthMeasureSpec, heightMeasureSpec);
        }
        previousPaddingLeft = paddingLeft;
        previousPaddingTop = paddingTop;
        previousWidthMeasureSpec = widthMeasureSpec;
        previousHeightMeasureSpec = heightMeasureSpec;

        internalMeasureChildren(widthMeasureSpec, heightMeasureSpec);

        //noinspection PointlessBooleanExpression
        if (ALLOWS_EMBEDDED && mLayoutWidget.getParent() != null) {
            setVisibility(INVISIBLE);
            return;
        }

        // let's solve the linear system.
        solveLinearSystem(); // first pass
        int childState = 0;

        // let's update the size dependent widgets if any...
        final int sizeDependentWidgetsCount = mSizeDependentsWidgets.size();

        int heightPadding = paddingTop + getPaddingBottom();
        int widthPadding = paddingLeft + getPaddingRight();

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

        final int androidLayoutWidth = mLayoutWidget.getWidth() + widthPadding;
        final int androidLayoutHeight = mLayoutWidget.getHeight() + heightPadding;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            int widthSize = resolveSizeAndState(androidLayoutWidth, widthMeasureSpec, childState);
            int heightSize = resolveSizeAndState(androidLayoutHeight, heightMeasureSpec,
                    childState << MEASURED_HEIGHT_STATE_SHIFT);
            setMeasuredDimension(widthSize & MEASURED_SIZE_MASK, heightSize & MEASURED_SIZE_MASK);
        } else {
            setMeasuredDimension(androidLayoutWidth, androidLayoutHeight);
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
        if (widthMode == MeasureSpec.AT_MOST || widthMode == MeasureSpec.UNSPECIFIED) {
            mLayoutWidget.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
            mLayoutWidget.setWidth(0);
        } else {
            mLayoutWidget.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
            mLayoutWidget.setWidth(widthSize - widthPadding);
        }

        if (heightMode == MeasureSpec.AT_MOST || heightMode == MeasureSpec.UNSPECIFIED) {
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
        Animator.setAnimationEnabled(false);
        if (SIMPLE_LAYOUT) {
            mLayoutWidget.layout();
        } else {
            int groups = mLayoutWidget.layoutFindGroupsSimple();
            mLayoutWidget.layoutWithGroup(groups);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        final int widgetsCount = getChildCount();
        for (int i = 0; i < widgetsCount; i++) {
            final View child = getChildAt(i);
            LayoutParams params = (LayoutParams) child.getLayoutParams();
            ConstraintWidget widget = params.widget;

            int l = widget.getDrawX();
            int t = widget.getDrawY();
            int r = l + widget.getWidth();
            int b = t + widget.getHeight();

            if (ALLOWS_EMBEDDED) {
                if (getParent() instanceof ConstraintLayout) {
                    int dx = 0;
                    int dy = 0;
                    ConstraintWidget item = mLayoutWidget; // start with ourselves
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
        public static final int PARENT_ID = 0;
        public static int UNSET = -1;

        public static int HORIZONTAL = 0;
        public static int VERTICAL = 1;

        public boolean needsBaseline = false;
        public boolean isGuideline = false;
        public int guideBegin = UNSET;
        public int guideEnd = UNSET;
        public float guidePercent = UNSET;

        public int leftToLeft = UNSET;
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

        public int orientation = UNSET;

        // Internal use only
        boolean horizontalLock = true;
        boolean verticalLock = true;

        int originalLeftToLeft = UNSET;
        int originalLeftToRight = UNSET;
        int originalRightToLeft = UNSET;
        int originalRightToRight = UNSET;

        ConstraintWidget widget = new ConstraintWidget();

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);

            TypedArray a = c.obtainStyledAttributes(attrs, R.styleable.ConstraintLayout_Layout);
            final int N = a.getIndexCount();
            for (int i = 0; i < N; i++) {
                int attr = a.getIndex(i);
                if (attr == R.styleable.ConstraintLayout_Layout_layout_constraintLeft_toLeftOf) {
                    leftToLeft = a.getResourceId(attr, leftToLeft);
                    if (leftToLeft == UNSET) {
                        leftToLeft = a.getInt(attr, UNSET);
                    }
                    originalLeftToLeft = leftToLeft;
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_constraintLeft_toRightOf) {
                    leftToRight = a.getResourceId(attr, leftToRight);
                    if (leftToRight == UNSET) {
                        leftToRight = a.getInt(attr, UNSET);
                    }
                    originalLeftToRight = leftToRight;
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_constraintRight_toLeftOf) {
                    rightToLeft = a.getResourceId(attr, rightToLeft);
                    if (rightToLeft == UNSET) {
                        rightToLeft = a.getInt(attr, UNSET);
                    }
                    originalRightToLeft = rightToLeft;
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_constraintRight_toRightOf) {
                    rightToRight = a.getResourceId(attr, rightToRight);
                    if (rightToRight == UNSET) {
                        rightToRight = a.getInt(attr, UNSET);
                    }
                    originalRightToRight = rightToRight;
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_constraintTop_toTopOf) {
                    topToTop = a.getResourceId(attr, topToTop);
                    if (topToTop == UNSET) {
                        topToTop = a.getInt(attr, UNSET);
                    }
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_constraintTop_toBottomOf) {
                    topToBottom = a.getResourceId(attr, topToBottom);
                    if (topToBottom == UNSET) {
                        topToBottom = a.getInt(attr, UNSET);
                    }
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_constraintBottom_toTopOf) {
                    bottomToTop = a.getResourceId(attr, bottomToTop);
                    if (bottomToTop == UNSET) {
                        bottomToTop = a.getInt(attr, UNSET);
                    }
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_constraintBottom_toBottomOf) {
                    bottomToBottom = a.getResourceId(attr, bottomToBottom);
                    if (bottomToBottom == UNSET) {
                        bottomToBottom = a.getInt(attr, UNSET);
                    }
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_constraintBaseline_toBaselineOf) {
                    baselineToBaseline = a.getResourceId(attr, baselineToBaseline);
                    if (baselineToBaseline == UNSET) {
                        baselineToBaseline = a.getInt(attr, UNSET);
                    }
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_constraintCenterX_toCenterX) {
                    centerXToCenterX = a.getResourceId(attr, centerXToCenterX);
                    if (centerXToCenterX == UNSET) {
                        centerXToCenterX = a.getInt(attr, UNSET);
                    }
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_constraintCenterY_toCenterY) {
                    centerYToCenterY = a.getResourceId(attr, centerYToCenterY);
                    if (centerYToCenterY == UNSET) {
                        centerYToCenterY = a.getInt(attr, UNSET);
                    }
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_editor_absoluteX) {
                    editorAbsoluteX = a.getDimensionPixelOffset(attr, editorAbsoluteX);
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_editor_absoluteY) {
                    editorAbsoluteY = a.getDimensionPixelOffset(attr, editorAbsoluteY);
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_constraintGuide_begin) {
                    guideBegin = a.getDimensionPixelOffset(attr, guideBegin);
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_constraintGuide_end) {
                    guideEnd = a.getDimensionPixelOffset(attr, guideEnd);
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_constraintGuide_Percent) {
                    guidePercent = a.getInteger(attr, UNSET);
                    if (guidePercent > UNSET) {
                        guidePercent /= 100f;
                    }
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_constraintGuide_percent) {
                    guidePercent = a.getFloat(attr, guidePercent);
                } else if (attr == R.styleable.ConstraintLayout_Layout_android_orientation) {
                    orientation = a.getInt(attr, orientation);
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_constraintStart_toEndOf) {
                    startToEnd = a.getResourceId(attr, startToEnd);
                    if (startToEnd == UNSET) {
                        startToEnd = a.getInt(attr, UNSET);
                    }
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_constraintStart_toStartOf) {
                    startToStart = a.getResourceId(attr, startToStart);
                    if (startToStart == UNSET) {
                        startToStart = a.getInt(attr, UNSET);
                    }
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_constraintEnd_toStartOf) {
                    endToStart = a.getResourceId(attr, endToStart);
                    if (endToStart == UNSET) {
                        endToStart = a.getInt(attr, UNSET);
                    }
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_constraintEnd_toEndOf) {
                    endToEnd = a.getResourceId(attr, endToEnd);
                    if (endToEnd == UNSET) {
                        endToEnd = a.getInt(attr, UNSET);
                    }
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
            if (guidePercent != UNSET || guideBegin != UNSET || guideEnd != UNSET) {
                isGuideline = true;
                horizontalLock = true;
                verticalLock = true;
                if (!(widget instanceof Guideline)) {
                    widget = new Guideline();
                }
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

            rightToLeft = originalRightToLeft;
            rightToRight = originalRightToRight;
            leftToRight = originalLeftToRight;
            leftToLeft = originalLeftToLeft;

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
                    leftToLeft = endToEnd;
                }
            } else {
                if (startToEnd != UNSET) {
                    leftToRight = startToEnd;
                }
                if (startToStart != UNSET) {
                    leftToLeft = startToStart;
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
