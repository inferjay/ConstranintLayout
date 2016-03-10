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

package com.google.tnt.sherpa;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.tnt.solver.LinearSystem;
import com.google.tnt.solver.widgets.ConstraintAnchor;
import com.google.tnt.solver.widgets.ConstraintWidget;
import com.google.tnt.solver.widgets.ConstraintWidgetContainer;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * A Layout where the positions of the children is described as constraints in relation to each
 * other or to the parent.
 *
 * <p>
 * Note that you cannot have a circular dependency in constraints
 * </p>
 * Also see {@link com.google.tnt.sherpa.ConstraintLayout.LayoutParams
 * ConstraintLayout.LayoutParams} for layout attributes
 * </p>
 */
public class ConstraintLayout extends ViewGroup {

    // For now, disallow embedded (single-layer resolution) situations.
    // While it works, the constraints of the layout have the same importance as any other
    // constraint of the overall layout, which can cause issues. Let's revisit this
    // after implementing priorities/hierarchy of constraints.
    static final boolean ALLOWS_EMBEDDED = false;

    private static final boolean DEBUG = false;
    private static final String TAG = "ConstraintLayout";

    protected HashMap<View, ConstraintWidget> mConstrainedViews = new HashMap<View, ConstraintWidget>();
    protected ArrayList<ConstraintWidget> mConstrainedWidgets = new ArrayList<ConstraintWidget>();
    protected HashMap<View, ConstraintWidget> mConstrainedViewTargets = new HashMap<View, ConstraintWidget>();
    protected ArrayList<ConstraintWidget> mConstrainedTargets = new ArrayList<ConstraintWidget>();
    private ArrayList<ConstraintWidget> mSizeDependentsWidgets = new ArrayList<ConstraintWidget>();

    protected ConstraintWidgetContainer mLayoutWidget = null;
    protected LinearSystem mEquationSystem = new LinearSystem();
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

    public ConstraintLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    protected ConstraintWidgetContainer getLayoutWidget() {
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
                ConstraintWidget widget = null;
                if (child instanceof Guideline) {
                    final LayoutParams layoutParams = (LayoutParams) child.getLayoutParams();
                    com.google.tnt.solver.widgets.Guideline guideline = new com.google.tnt.solver.widgets.Guideline();
                    if (layoutParams.orientation == LayoutParams.HORIZONTAL) {
                        guideline.setOrientation(com.google.tnt.solver.widgets.Guideline.VERTICAL);
                    } else {
                        guideline.setOrientation(com.google.tnt.solver.widgets.Guideline.HORIZONTAL);
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

    protected void addConstrainedChild(View child, ConstraintWidget widget) {
        if (child.getVisibility() == GONE) {
            return;
        }
        mConstrainedViews.put(child, widget);
        mConstrainedWidgets.add(widget);
        widget.setParent(mLayoutWidget);
        widget.setCompanionWidget(child);
        if (mLayoutWidget instanceof ConstraintWidgetContainer) {
            ConstraintWidgetContainer container = (ConstraintWidgetContainer) mLayoutWidget;
            container.add(widget);
        }
    }

    protected void setChildrenConstraints() {
        final int count = getChildCount();
        // Now, let's set the constraints from the xml
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() == GONE) {
                continue;
            }
            ConstraintWidget widget = mConstrainedViews.get(child);
            if (widget == null) {
                continue;
            }
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
                if (widget == null) {
                    continue;
                }

                // Process match_Parent converting it to 0dp & constrain left and right to root
                if (layoutParams.width == LayoutParams.MATCH_PARENT) {
                    widget.connect(ConstraintAnchor.Type.LEFT, mLayoutWidget,
                            ConstraintAnchor.Type.LEFT, layoutParams.left_margin);
                    widget.connect(ConstraintAnchor.Type.RIGHT, mLayoutWidget,
                            ConstraintAnchor.Type.RIGHT, layoutParams.right_margin);
                    layoutParams.width = 0;
                    layoutParams.horizontalLock = false;
                }
                // Process match_Parent converting it to 0dp & constrain top and bottom to root
                if (layoutParams.height == LayoutParams.MATCH_PARENT) {
                    widget.connect(ConstraintAnchor.Type.TOP, mLayoutWidget,
                            ConstraintAnchor.Type.TOP, layoutParams.top_margin);
                    widget.connect(ConstraintAnchor.Type.BOTTOM, mLayoutWidget,
                            ConstraintAnchor.Type.BOTTOM, layoutParams.bottom_margin);
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
                                ConstraintAnchor.Type.LEFT, layoutParams.left_margin);
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
                                ConstraintAnchor.Type.RIGHT, layoutParams.left_margin);
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
                                ConstraintAnchor.Type.LEFT, layoutParams.right_margin);
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
                                ConstraintAnchor.Type.RIGHT, layoutParams.right_margin);
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
                                ConstraintAnchor.Type.TOP, layoutParams.top_margin);
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
                                ConstraintAnchor.Type.BOTTOM, layoutParams.top_margin);
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
                                ConstraintAnchor.Type.TOP, layoutParams.bottom_margin);
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
                                ConstraintAnchor.Type.BOTTOM, layoutParams.bottom_margin);
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
                                ConstraintAnchor.Type.LEFT, layoutParams.left_margin);
                        widget.connect(ConstraintAnchor.Type.RIGHT, target,
                                ConstraintAnchor.Type.RIGHT, layoutParams.right_margin);
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
                                ConstraintAnchor.Type.TOP, layoutParams.top_margin);
                        widget.connect(ConstraintAnchor.Type.BOTTOM, target,
                                ConstraintAnchor.Type.BOTTOM, layoutParams.bottom_margin);
                    }
                }

                // Set the strength
                if (layoutParams.left_strength != LayoutParams.UNSET) {
                    if (layoutParams.left_strength == LayoutParams.STRONG) {
                        widget.getAnchor(ConstraintAnchor.Type.LEFT).setStrength(ConstraintAnchor.Strength.STRONG);
                    } else {
                        widget.getAnchor(ConstraintAnchor.Type.LEFT).setStrength(ConstraintAnchor.Strength.WEAK);
                    }
                } else {
                    widget.getAnchor(ConstraintAnchor.Type.LEFT).setStrength(ConstraintAnchor.Strength.STRONG);
                }

                if (layoutParams.top_strength != LayoutParams.UNSET) {
                    if (layoutParams.top_strength == LayoutParams.STRONG) {
                        widget.getAnchor(ConstraintAnchor.Type.TOP).setStrength(ConstraintAnchor.Strength.STRONG);
                    } else {
                        widget.getAnchor(ConstraintAnchor.Type.TOP).setStrength(ConstraintAnchor.Strength.WEAK);
                    }
                } else {
                    widget.getAnchor(ConstraintAnchor.Type.TOP).setStrength(ConstraintAnchor.Strength.STRONG);
                }
                if (layoutParams.right_strength != LayoutParams.UNSET) {
                    if (layoutParams.right_strength == LayoutParams.STRONG) {
                        widget.getAnchor(ConstraintAnchor.Type.RIGHT).setStrength(ConstraintAnchor.Strength.STRONG);
                    } else {
                        widget.getAnchor(ConstraintAnchor.Type.RIGHT).setStrength(ConstraintAnchor.Strength.WEAK);
                    }
                } else {
                    widget.getAnchor(ConstraintAnchor.Type.RIGHT).setStrength(ConstraintAnchor.Strength.STRONG);
                }
                if (layoutParams.bottom_strength != LayoutParams.UNSET) {
                    if (layoutParams.bottom_strength == LayoutParams.STRONG) {
                        widget.getAnchor(ConstraintAnchor.Type.BOTTOM).setStrength(ConstraintAnchor.Strength.STRONG);
                    } else {
                        widget.getAnchor(ConstraintAnchor.Type.BOTTOM).setStrength(ConstraintAnchor.Strength.WEAK);
                    }
                } else {
                    widget.getAnchor(ConstraintAnchor.Type.BOTTOM).setStrength(ConstraintAnchor.Strength.STRONG);
                }

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

    private void showMeasureSpec(String label, int spec) {
        System.out.print(label);
        if (spec == MeasureSpec.AT_MOST) {
            System.out.println("AT_MOST");
        } else if (spec == MeasureSpec.EXACTLY) {
            System.out.println("EXACTLY");
        } else if (spec == MeasureSpec.UNSPECIFIED) {
            System.out.println("UNSPECIFIED");
        }
    }

    protected void internalMeasureChildren(int parentWidthSpec, int parentHeightSpec) {
        int heightPadding = getPaddingTop() + getPaddingBottom();
        int widthPadding = getPaddingLeft() + getPaddingRight();

        final int widgetsCount = mConstrainedWidgets.size();
        for (int i = 0; i < widgetsCount; i++) {
            ConstraintWidget widget = mConstrainedWidgets.get(i);
            if (widget == null) {
                continue;
            }
            final View child = (View) widget.getCompanionWidget();
            if (child == null) {
                continue;
            }
            if (child.getVisibility() == GONE) {
                continue;
            }
            int width = child.getLayoutParams().width;
            int height = child.getLayoutParams().height;
            if (width == 0 || height == 0) {
                int childWidthMeasureSpec = 0;
                int childHeightMeasureSpec = 0;
                if (width == 0) {
                    childWidthMeasureSpec = LayoutParams.WRAP_CONTENT;
                } else {
                    childWidthMeasureSpec = getChildMeasureSpec(parentWidthSpec,
                            widthPadding, width);
                }
                if (height == 0) {
                    childHeightMeasureSpec = LayoutParams.WRAP_CONTENT;
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
                    widthSpec = LayoutParams.WRAP_CONTENT;
                }
                if (lp.height == LayoutParams.WRAP_CONTENT) {
                    heightSpec = LayoutParams.WRAP_CONTENT;
                }
                // we need to re-measure the child...
                child.measure(widthSpec, heightSpec);
                int width = child.getMeasuredWidth();
                int height = child.getMeasuredHeight();
                widget.setWidth(width);
                widget.setHeight(height);
                childState = combineMeasuredStates(childState, child.getMeasuredState());
            }
            solveLinearSystem(); // second pass
        }

        setMeasuredDimension(
                resolveSizeAndState(mLayoutWidget.getWidth(), widthMeasureSpec, childState),
                resolveSizeAndState(mLayoutWidget.getHeight(), heightMeasureSpec,
                        childState << MEASURED_HEIGHT_STATE_SHIFT));
    }

    protected void setSelfDimensionBehaviour(int widthMeasureSpec, int heightMeasureSpec) {
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
        addAllWidgetsToSolver();
        try {
            mEquationSystem.minimize();
        } catch (Exception e) {
            e.printStackTrace();
        }
        updateAllWidgetsFromSolver();
    }

    protected void addAllWidgetsToSolver() {
        mLayoutWidget.addToSolver(mEquationSystem);
        int num = mConstrainedWidgets.size();
        for (int i = 0; i < num; i++) {
            ConstraintWidget widget = mConstrainedWidgets.get(i);
            widget.addToSolver(mEquationSystem);
        }
        num = mConstrainedTargets.size();
        for (int i = 0; i < num; i++) {
            ConstraintWidget target = mConstrainedTargets.get(i);
            target.addToSolver(mEquationSystem);
        }
    }

    protected void updateAllWidgetsFromSolver() {
        mLayoutWidget.updateFromSolver(mEquationSystem);
        int num = mConstrainedWidgets.size();
        for (int i = 0; i < num; i++) {
            ConstraintWidget widget = mConstrainedWidgets.get(i);
            widget.updateFromSolver(mEquationSystem);
        }
        num = mConstrainedTargets.size();
        for (int i = 0; i < num; i++) {
            ConstraintWidget target = mConstrainedTargets.get(i);
            target.updateFromSolver(mEquationSystem);
        }
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
            int l = widget.getLeft();
            int t = widget.getTop();
            int r = widget.getRight();
            int b = widget.getBottom();
            if (ALLOWS_EMBEDDED) {
                if (getParent() instanceof ConstraintLayout) {
                    int dx = 0;
                    int dy = 0;
                    ConstraintWidget item = getLayoutWidget(); // start with ourselves
                    while (item != null) {
                        dx += item.getLeft();
                        dy += item.getTop();
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
            int l = target.getX();
            int t = target.getY();
            int r = l + target.getWidth();;
            int b = t + target.getHeight();
            if (target instanceof com.google.tnt.solver.widgets.Guideline) {
                com.google.tnt.solver.widgets.Guideline guideline = (com.google.tnt.solver.widgets.Guideline) target;
                if (guideline.getOrientation() == com.google.tnt.solver.widgets.Guideline.HORIZONTAL) {
                    t = ((com.google.tnt.solver.widgets.Guideline) target).getParent().getTop();
                    b = ((com.google.tnt.solver.widgets.Guideline) target).getParent().getBottom();
                    r = l;
                } else {
                    l = ((com.google.tnt.solver.widgets.Guideline) target).getParent().getLeft();
                    r = ((com.google.tnt.solver.widgets.Guideline) target).getParent().getRight();
                    b = t;
                }
            }
            child.layout(l, t, r, b);
        }
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        LayoutParams layoutParams = new ConstraintLayout.LayoutParams(getContext(), attrs);
        resolveRtlProperties(layoutParams);
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

    public static class LayoutParams extends android.view.ViewGroup.LayoutParams {

        public static int UNSET = -1;
        public static int LEFT = 0;
        public static int TOP = 1;
        public static int RIGHT = 2;
        public static int BOTTOM = 3;
        public static int BASELINE = 4;

        public static int HORIZONTAL = 0;
        public static int VERTICAL = 1;

        public static int STRONG = 0;
        public static int WEAK = 1;

        public int left_margin = UNSET;
        public int top_margin = UNSET;
        public int right_margin = UNSET;
        public int bottom_margin = UNSET;

        public int left_strength = UNSET;
        public int top_strength = UNSET;
        public int right_strength = UNSET;
        public int bottom_strength = UNSET;

        public int orientation = UNSET;

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

        public int container_skip = UNSET;
        public int start_margin = UNSET;
        public int end_margin = UNSET;
        public int start_to_end = UNSET;
        public int start_to_start = UNSET;
        public int end_to_start = UNSET;
        public int end_to_end = UNSET;

        public int editor_absolute_x = UNSET;
        public int editor_absolute_y = UNSET;

        public boolean horizontalLock = true;
        public boolean verticalLock = true;

        // Used by TableConstraintLayout
        public int numRows = 1;
        public int numColumns = 1;
        public String columnsAlignment = null;
        public int padding = 0;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
            TypedArray a = c.obtainStyledAttributes(attrs, R.styleable.ConstraintLayout_LayoutParams);
            final int N = a.getIndexCount();
            for (int i = 0; i < N; i++) {
                int attr = a.getIndex(i);
                if (attr ==  R.styleable.ConstraintLayout_LayoutParams_layout_constraintLeft_toLeftOf) {
                    left_to_left = a.getResourceId(attr, left_to_left);
                } else if (attr == R.styleable.ConstraintLayout_LayoutParams_layout_constraintLeft_toRightOf) {
                    left_to_right = a.getResourceId(attr, left_to_right);
                } else if (attr == R.styleable.ConstraintLayout_LayoutParams_layout_constraintRight_toLeftOf) {
                    right_to_left = a.getResourceId(attr, right_to_left);
                } else if (attr == R.styleable.ConstraintLayout_LayoutParams_layout_constraintRight_toRightOf) {
                    right_to_right = a.getResourceId(attr, right_to_right);
                } else if (attr == R.styleable.ConstraintLayout_LayoutParams_layout_constraintTop_toTopOf) {
                    top_to_top = a.getResourceId(attr, top_to_top);
                } else if (attr == R.styleable.ConstraintLayout_LayoutParams_layout_constraintTop_toBottomOf) {
                    top_to_bottom = a.getResourceId(attr, top_to_bottom);
                } else if (attr == R.styleable.ConstraintLayout_LayoutParams_layout_constraintBottom_toTopOf) {
                    bottom_to_top = a.getResourceId(attr, bottom_to_top);
                } else if (attr == R.styleable.ConstraintLayout_LayoutParams_layout_constraintBottom_toBottomOf) {
                    bottom_to_bottom = a.getResourceId(attr, bottom_to_bottom);
                } else if (attr == R.styleable.ConstraintLayout_LayoutParams_layout_constraintBaseline_toBaselineOf) {
                    baseline_to_baseline = a.getResourceId(attr, baseline_to_baseline);
                } else if (attr == R.styleable.ConstraintLayout_LayoutParams_layout_constraintCenterX_toCenterX) {
                    centerX_to_centerX = a.getResourceId(attr, centerX_to_centerX);
                } else if (attr == R.styleable.ConstraintLayout_LayoutParams_layout_constraintCenterY_toCenterY) {
                    centerY_to_centerY = a.getResourceId(attr, centerY_to_centerY);
                } else if (attr == R.styleable.ConstraintLayout_LayoutParams_layout_editor_absoluteX) {
                    editor_absolute_x = a.getDimensionPixelOffset(attr, editor_absolute_x);
                } else if (attr == R.styleable.ConstraintLayout_LayoutParams_layout_editor_absoluteY) {
                    editor_absolute_y = a.getDimensionPixelOffset(attr, editor_absolute_y);
                } else if (attr == R.styleable.ConstraintLayout_LayoutParams_layout_constraintLeft_margin) {
                    left_margin = a.getDimensionPixelOffset(attr, left_margin);
                } else if (attr == R.styleable.ConstraintLayout_LayoutParams_layout_constraintTop_margin) {
                    top_margin = a.getDimensionPixelOffset(attr, top_margin);
                } else if (attr == R.styleable.ConstraintLayout_LayoutParams_layout_constraintRight_margin) {
                    right_margin = a.getDimensionPixelOffset(attr, right_margin);
                } else if (attr == R.styleable.ConstraintLayout_LayoutParams_layout_constraintBottom_margin) {
                    bottom_margin = a.getDimensionPixelOffset(attr, bottom_margin);
                } else if (attr == R.styleable.ConstraintLayout_LayoutParams_layout_constraintLeft_strength) {
                    left_strength = a.getInteger(attr, left_strength);
                } else if (attr == R.styleable.ConstraintLayout_LayoutParams_layout_constraintTop_strength) {
                    top_strength = a.getInteger(attr, top_strength);
                } else if (attr == R.styleable.ConstraintLayout_LayoutParams_layout_constraintRight_strength) {
                    right_strength = a.getInteger(attr, right_strength);
                } else if (attr == R.styleable.ConstraintLayout_LayoutParams_layout_constraintBottom_strength) {
                    bottom_strength = a.getInteger(attr, bottom_strength);
                } else if (attr == R.styleable.ConstraintLayout_LayoutParams_relativeBegin) {
                    relativeBegin = a.getDimensionPixelOffset(attr, relativeBegin);
                } else if (attr == R.styleable.ConstraintLayout_LayoutParams_relativeEnd) {
                    relativeEnd = a.getDimensionPixelOffset(attr, relativeEnd);
                } else if (attr == R.styleable.ConstraintLayout_LayoutParams_relativePercent) {
                    relativePercent = a.getInt(attr, relativePercent);
                } else if (attr == R.styleable.ConstraintLayout_LayoutParams_orientation) {
                    orientation = a.getInt(attr, orientation);
                } else if (attr == R.styleable.ConstraintLayout_LayoutParams_table_numRows) {
                    numRows = a.getInteger(attr, numRows);
                } else if (attr == R.styleable.ConstraintLayout_LayoutParams_table_numColumns) {
                    numColumns = a.getInteger(attr, numColumns);
                } else if (attr == R.styleable.ConstraintLayout_LayoutParams_table_columnsAlignment) {
                    columnsAlignment = a.getString(attr);
                } else if (attr == R.styleable.ConstraintLayout_LayoutParams_table_padding) {
                    padding = a.getDimensionPixelOffset(attr, padding);
                } else if (attr == R.styleable.ConstraintLayout_LayoutParams_layout_constraintStart_toEndOf) {
                    start_to_end = a.getResourceId(attr, start_to_end);
                } else if (attr == R.styleable.ConstraintLayout_LayoutParams_layout_constraintStart_toStartOf) {
                    start_to_start = a.getResourceId(attr, start_to_start);
                } else if (attr == R.styleable.ConstraintLayout_LayoutParams_layout_constraintEnd_toStartOf) {
                    end_to_start = a.getResourceId(attr, end_to_start);
                } else if (attr == R.styleable.ConstraintLayout_LayoutParams_layout_constraintEnd_toEndOf) {
                    end_to_end = a.getResourceId(attr, end_to_end);
                } else if (attr == R.styleable.ConstraintLayout_LayoutParams_layout_constraintStart_margin) {
                    start_margin = a.getDimensionPixelOffset(attr, start_margin);
                } else if (attr == R.styleable.ConstraintLayout_LayoutParams_layout_constraintEnd_margin) {
                    end_margin = a.getDimensionPixelOffset(attr, end_margin);
                } else if (attr == R.styleable.ConstraintLayout_LayoutParams_containerItemSkip) {
                    container_skip = a.getInteger(attr, container_skip);
                } else {
                    Log.w(TAG," UNSUPPORTED attr ! = "+attr);
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

    /**
     * Support rtl
     * @param layoutDirection
     */
    @Override
    public void onRtlPropertiesChanged(int layoutDirection) {
        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            resolveRtlProperties((LayoutParams) getChildAt(i).getLayoutParams());
        }
        setChildrenConstraints();
    }

    /**
     * swap start and end to left and right as needed by RTL
     * @param layoutParams
     */
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
            if (layoutParams.start_margin != LayoutParams.UNSET) {
                layoutParams.right_margin = layoutParams.start_margin;
            }
            if (layoutParams.end_margin != LayoutParams.UNSET) {
                layoutParams.left_margin = layoutParams.end_margin;
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
            if (layoutParams.start_margin != LayoutParams.UNSET) {
                layoutParams.left_margin = layoutParams.start_margin;
            }
            if (layoutParams.end_margin != LayoutParams.UNSET) {
                layoutParams.right_margin = layoutParams.end_margin;
            }
        }
    }

    @Override
    public void requestLayout() {
        super.requestLayout();
        mDirtyHierarchy = true;
    }
}