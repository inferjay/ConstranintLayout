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

import static android.support.constraint.ConstraintLayout.LayoutParams.UNSET;

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
            } else if ((layoutParams.resolvedLeftToLeft != UNSET)
                    || (layoutParams.resolvedLeftToRight != UNSET)
                    || (layoutParams.resolvedRightToLeft != UNSET)
                    || (layoutParams.resolvedRightToRight != UNSET)
                    || (layoutParams.topToTop != UNSET)
                    || (layoutParams.topToBottom != UNSET)
                    || (layoutParams.bottomToTop != UNSET)
                    || (layoutParams.bottomToBottom != UNSET)
                    || (layoutParams.baselineToBaseline != UNSET)
                    || (layoutParams.editorAbsoluteX != UNSET)
                    || (layoutParams.editorAbsoluteY != UNSET)) {

                // Process match_Parent converting it to 0dp
                if (layoutParams.width == LayoutParams.MATCH_PARENT) {
                    layoutParams.horizontalLock = false;
                }
                // Process match_Parent converting it to 0dp
                if (layoutParams.height == LayoutParams.MATCH_PARENT) {
                    layoutParams.verticalLock = false;
                }

                // Get the left/right constraints resolved for RTL
                int resolvedLeftToLeft = layoutParams.resolvedLeftToLeft;
                int resolvedLeftToRight = layoutParams.resolvedLeftToRight;
                int resolvedRightToLeft = layoutParams.resolvedRightToLeft;
                int resolvedRightToRight = layoutParams.resolvedRightToRight;
                int resolveGoneLeftMargin = layoutParams.resolveGoneLeftMargin;
                int resolveGoneRightMargin = layoutParams.resolveGoneRightMargin;

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    // Pre JB MR1, left/right should take precedence, unless they are
                    // not defined and somehow a corresponding start/end constraint exists
                    resolvedLeftToLeft = layoutParams.leftToLeft;
                    resolvedLeftToRight = layoutParams.leftToRight;
                    resolvedRightToLeft = layoutParams.rightToLeft;
                    resolvedRightToRight = layoutParams.rightToRight;
                    resolveGoneLeftMargin = layoutParams.goneLeftMargin;
                    resolveGoneRightMargin = layoutParams.goneRightMargin;
                    if (resolvedLeftToLeft == UNSET && resolvedLeftToRight == UNSET) {
                        if (layoutParams.startToStart != UNSET) {
                            resolvedLeftToLeft = layoutParams.startToStart;
                        } else if (layoutParams.startToEnd != UNSET) {
                            resolvedLeftToRight = layoutParams.startToEnd;
                        }
                    }
                    if (resolvedRightToLeft == UNSET && resolvedRightToRight == UNSET) {
                        if (layoutParams.endToStart != UNSET) {
                            resolvedRightToLeft = layoutParams.endToStart;
                        } else if (layoutParams.endToEnd != UNSET) {
                            resolvedRightToRight = layoutParams.endToEnd;
                        }
                    }
                }

                // Left constraint
                if (resolvedLeftToLeft != UNSET) {
                    ConstraintWidget target = getTargetWidget(resolvedLeftToLeft);
                    if (target != null) {
                        widget.immediateConnect(ConstraintAnchor.Type.LEFT, target,
                                ConstraintAnchor.Type.LEFT, layoutParams.leftMargin,
                                resolveGoneLeftMargin);
                    }
                } else if (resolvedLeftToRight != UNSET) {
                    ConstraintWidget target = getTargetWidget(resolvedLeftToRight);
                    if (target != null) {
                        widget.immediateConnect(ConstraintAnchor.Type.LEFT, target,
                                ConstraintAnchor.Type.RIGHT, layoutParams.leftMargin,
                                resolveGoneLeftMargin);
                    }
                }

                // Right constraint
                if (resolvedRightToLeft != UNSET) {
                    ConstraintWidget target = getTargetWidget(resolvedRightToLeft);
                    if (target != null) {
                        widget.immediateConnect(ConstraintAnchor.Type.RIGHT, target,
                                ConstraintAnchor.Type.LEFT, layoutParams.rightMargin,
                                resolveGoneRightMargin);
                    }
                } else if (resolvedRightToRight != UNSET) {
                    ConstraintWidget target = getTargetWidget(resolvedRightToRight);
                    if (target != null) {
                        widget.immediateConnect(ConstraintAnchor.Type.RIGHT, target,
                                ConstraintAnchor.Type.RIGHT, layoutParams.rightMargin,
                                resolveGoneRightMargin);
                    }
                }

                // Top constraint
                if (layoutParams.topToTop != UNSET) {
                    ConstraintWidget target = getTargetWidget(layoutParams.topToTop);
                    if (target != null) {
                        widget.immediateConnect(ConstraintAnchor.Type.TOP, target,
                                ConstraintAnchor.Type.TOP, layoutParams.topMargin,
                                layoutParams.goneTopMargin);
                    }
                } else if (layoutParams.topToBottom != UNSET) {
                    ConstraintWidget target = getTargetWidget(layoutParams.topToBottom);
                    if (target != null) {
                        widget.immediateConnect(ConstraintAnchor.Type.TOP, target,
                                ConstraintAnchor.Type.BOTTOM, layoutParams.topMargin,
                                layoutParams.goneTopMargin);
                    }
                }

                // Bottom constraint
                if (layoutParams.bottomToTop != UNSET) {
                    ConstraintWidget target = getTargetWidget(layoutParams.bottomToTop);
                    if (target != null) {
                        widget.immediateConnect(ConstraintAnchor.Type.BOTTOM, target,
                                ConstraintAnchor.Type.TOP, layoutParams.bottomMargin,
                                layoutParams.goneBottomMargin);
                    }
                } else if (layoutParams.bottomToBottom != UNSET) {
                    ConstraintWidget target = getTargetWidget(layoutParams.bottomToBottom);
                    if (target != null) {
                        widget.immediateConnect(ConstraintAnchor.Type.BOTTOM, target,
                                ConstraintAnchor.Type.BOTTOM, layoutParams.bottomMargin,
                                layoutParams.goneBottomMargin);
                    }
                }

                // Baseline constraint
                if (layoutParams.baselineToBaseline != UNSET) {
                    View view = mChildrenByIds.get(layoutParams.baselineToBaseline);
                    ConstraintWidget target = getTargetWidget(layoutParams.baselineToBaseline);
                    if (target != null && view != null && view.getLayoutParams() instanceof LayoutParams) {
                        LayoutParams targetParams = (LayoutParams) view.getLayoutParams();
                        layoutParams.needsBaseline = true;
                        targetParams.needsBaseline = true;
                        ConstraintAnchor baseline = widget.getAnchor(ConstraintAnchor.Type.BASELINE);
                        ConstraintAnchor targetBaseline =
                                target.getAnchor(ConstraintAnchor.Type.BASELINE);
                        baseline.connect(targetBaseline, 0, -1, ConstraintAnchor.Strength.STRONG,
                                ConstraintAnchor.USER_CREATOR, true);

                        widget.getAnchor(ConstraintAnchor.Type.TOP).reset();
                        widget.getAnchor(ConstraintAnchor.Type.BOTTOM).reset();
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

                if (isInEditMode() && ((layoutParams.editorAbsoluteX != UNSET)
                        || (layoutParams.editorAbsoluteY != UNSET))) {
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

        int androidLayoutWidth = mLayoutWidget.getWidth() + widthPadding;
        int androidLayoutHeight = mLayoutWidget.getHeight() + heightPadding;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            int resolvedWidthSize = resolveSizeAndState(androidLayoutWidth, widthMeasureSpec, childState);
            int resolvedHeightSize = resolveSizeAndState(androidLayoutHeight, heightMeasureSpec,
                    childState << MEASURED_HEIGHT_STATE_SHIFT);
            setMeasuredDimension(resolvedWidthSize & MEASURED_SIZE_MASK, resolvedHeightSize & MEASURED_SIZE_MASK);
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

        ConstraintWidget.DimensionBehaviour widthBehaviour = ConstraintWidget.DimensionBehaviour.FIXED;
        ConstraintWidget.DimensionBehaviour heightBehaviour = ConstraintWidget.DimensionBehaviour.FIXED;
        int desiredWidth = 0;
        int desiredHeight = 0;

        // TODO: investigate measure too small (check MeasureSpec)
        ViewGroup.LayoutParams params = getLayoutParams();
        switch (widthMode) {
            case MeasureSpec.AT_MOST: {
                widthBehaviour = ConstraintWidget.DimensionBehaviour.WRAP_CONTENT;
            }
            break;
            case MeasureSpec.UNSPECIFIED: {
                if (params.width > 0) {
                    desiredWidth = params.width;
                } else {
                    widthBehaviour = ConstraintWidget.DimensionBehaviour.WRAP_CONTENT;
                }
            }
            break;
            case MeasureSpec.EXACTLY: {
                desiredWidth = widthSize - widthPadding;
            }
        }
        switch (heightMode) {
            case MeasureSpec.AT_MOST: {
                heightBehaviour = ConstraintWidget.DimensionBehaviour.WRAP_CONTENT;
            }
            break;
            case MeasureSpec.UNSPECIFIED: {
                if (params.height > 0) {
                    desiredHeight = params.height;
                } else {
                    heightBehaviour = ConstraintWidget.DimensionBehaviour.WRAP_CONTENT;
                }
            }
            break;
            case MeasureSpec.EXACTLY: {
                desiredHeight = heightSize - heightPadding;
            }
        }

        mLayoutWidget.setHorizontalDimensionBehaviour(widthBehaviour);
        mLayoutWidget.setWidth(desiredWidth);
        mLayoutWidget.setVerticalDimensionBehaviour(heightBehaviour);
        mLayoutWidget.setHeight(desiredHeight);
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
            if (child.getVisibility() == GONE) {
                continue;
            }
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
        public static final int UNSET = -1;

        public static final int HORIZONTAL = 0;
        public static final int VERTICAL = 1;

        public static final int LEFT = 1;
        public static final int RIGHT = 2;
        public static final int TOP = 3;
        public static final int BOTTOM = 4;
        public static final int BASELINE = 5;
        public static final int START = 6;
        public static final int END =  7;

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

        public int startToEnd = UNSET;
        public int startToStart = UNSET;
        public int endToStart = UNSET;
        public int endToEnd = UNSET;

        public int goneLeftMargin = UNSET;
        public int goneTopMargin = UNSET;
        public int goneRightMargin = UNSET;
        public int goneBottomMargin = UNSET;
        public int goneStartMargin = UNSET;
        public int goneEndMargin = UNSET;

        public float horizontalBias = 0.5f;
        public float verticalBias = 0.5f;
        public float dimensionRatio = 0f;

        public int editorAbsoluteX = UNSET;
        public int editorAbsoluteY = UNSET;

        public int orientation = UNSET;

        // Internal use only
        boolean horizontalLock = true;
        boolean verticalLock = true;

        int resolvedLeftToLeft = UNSET;
        int resolvedLeftToRight = UNSET;
        int resolvedRightToLeft = UNSET;
        int resolvedRightToRight = UNSET;
        int resolveGoneLeftMargin = UNSET;
        int resolveGoneRightMargin = UNSET;

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
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_constraintLeft_toRightOf) {
                    leftToRight = a.getResourceId(attr, leftToRight);
                    if (leftToRight == UNSET) {
                        leftToRight = a.getInt(attr, UNSET);
                    }
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_constraintRight_toLeftOf) {
                    rightToLeft = a.getResourceId(attr, rightToLeft);
                    if (rightToLeft == UNSET) {
                        rightToLeft = a.getInt(attr, UNSET);
                    }
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_constraintRight_toRightOf) {
                    rightToRight = a.getResourceId(attr, rightToRight);
                    if (rightToRight == UNSET) {
                        rightToRight = a.getInt(attr, UNSET);
                    }
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
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_editor_absoluteX) {
                    editorAbsoluteX = a.getDimensionPixelOffset(attr, editorAbsoluteX);
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_editor_absoluteY) {
                    editorAbsoluteY = a.getDimensionPixelOffset(attr, editorAbsoluteY);
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_constraintGuide_begin) {
                    guideBegin = a.getDimensionPixelOffset(attr, guideBegin);
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_constraintGuide_end) {
                    guideEnd = a.getDimensionPixelOffset(attr, guideEnd);
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
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_goneMarginLeft) {
                    goneLeftMargin = a.getDimensionPixelSize(attr, goneLeftMargin);
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_goneMarginTop) {
                    goneTopMargin = a.getDimensionPixelSize(attr, goneTopMargin);
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_goneMarginRight) {
                    goneRightMargin = a.getDimensionPixelSize(attr, goneRightMargin);
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_goneMarginBottom) {
                    goneBottomMargin = a.getDimensionPixelSize(attr, goneBottomMargin);
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_goneMarginStart) {
                    goneStartMargin = a.getDimensionPixelSize(attr, goneStartMargin);
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_goneMarginEnd) {
                    goneEndMargin = a.getDimensionPixelSize(attr, goneEndMargin);
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
            validate();
        }

        public void validate() {
            isGuideline = false;
            horizontalLock = true;
            verticalLock = true;
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

            resolvedRightToLeft = UNSET;
            resolvedRightToRight = UNSET;
            resolvedLeftToLeft = UNSET;
            resolvedLeftToRight = UNSET;

            resolveGoneLeftMargin = UNSET;
            resolveGoneRightMargin = UNSET;
            resolveGoneLeftMargin = goneLeftMargin;
            resolveGoneRightMargin = goneRightMargin;

            boolean isRtl = (View.LAYOUT_DIRECTION_RTL == getLayoutDirection());
            // Post JB MR1, if start/end are defined, they take precedence over left/right
            if (isRtl) {
                if (startToEnd != UNSET) {
                    resolvedRightToLeft = startToEnd;
                } else if (startToStart != UNSET) {
                    resolvedRightToRight = startToStart;
                }
                if (endToStart != UNSET) {
                    resolvedLeftToRight = endToStart;
                }
                if (endToEnd != UNSET) {
                    resolvedLeftToLeft = endToEnd;
                }
                if (goneStartMargin != UNSET) {
                    resolveGoneRightMargin = goneStartMargin;
                }
                if (goneEndMargin != UNSET) {
                    resolveGoneLeftMargin = goneEndMargin;
                }
            } else {
                if (startToEnd != UNSET) {
                    resolvedLeftToRight = startToEnd;
                }
                if (startToStart != UNSET) {
                    resolvedLeftToLeft = startToStart;
                }
                if (endToStart != UNSET) {
                    resolvedRightToLeft = endToStart;
                }
                if (endToEnd != UNSET) {
                    resolvedRightToRight = endToEnd;
                }
                if (goneStartMargin != UNSET) {
                    resolveGoneLeftMargin = goneStartMargin;
                }
                if (goneEndMargin != UNSET) {
                    resolveGoneRightMargin = goneEndMargin;
                }
            }
            // if no constraint is defined via RTL attributes, use left/right if present
            if (endToStart == UNSET && endToEnd == UNSET) {
                if (rightToLeft != UNSET) {
                    resolvedRightToLeft = rightToLeft;
                } else if (rightToRight != UNSET) {
                    resolvedRightToRight = rightToRight;
                }
            }
            if (startToStart == UNSET && startToEnd == UNSET) {
                if (leftToLeft != UNSET) {
                    resolvedLeftToLeft = leftToLeft;
                } else if (leftToRight != UNSET) {
                    resolvedLeftToRight = leftToRight;
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
