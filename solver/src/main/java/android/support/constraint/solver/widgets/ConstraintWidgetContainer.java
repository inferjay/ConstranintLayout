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

package android.support.constraint.solver.widgets;

import android.support.constraint.solver.ArrayRow;
import android.support.constraint.solver.LinearSystem;
import android.support.constraint.solver.SolverVariable;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * A container of ConstraintWidget that can layout its children
 */
public class ConstraintWidgetContainer extends WidgetContainer {

    private static final boolean USE_THREAD = false;
    private static final boolean USE_SNAPSHOT = true;
    private static final int MAX_ITERATIONS = 8;

    private static final boolean DEBUG = false;
    private static final boolean DEBUG_OPTIMIZE = false;
    private static final boolean DEBUG_LAYOUT = false;

    protected LinearSystem mSystem = new LinearSystem();
    protected LinearSystem mBackgroundSystem = null;

    private Snapshot mSnapshot;

    static boolean ALLOW_ROOT_GROUP = true;

    int mWrapWidth;
    int mWrapHeight;

    int mPaddingLeft;
    int mPaddingTop;
    int mPaddingRight;
    int mPaddingBottom;

    private int mHorizontalChainsSize = 0;
    private int mVerticalChainsSize = 0;
    private ConstraintWidget[] mMatchConstraintsChainedWidgets = new ConstraintWidget[4];
    private ConstraintWidget[] mVerticalChainsArray = new ConstraintWidget[4];
    private ConstraintWidget[] mHorizontalChainsArray = new ConstraintWidget[4];

    // Optimization levels
    public static final int OPTIMIZATION_NONE = 1;
    public static final int OPTIMIZATION_ALL = 2;
    public static final int OPTIMIZATION_BASIC = 4;
    public static final int OPTIMIZATION_CHAIN = 8;

    private int mOptimizationLevel = OPTIMIZATION_ALL;

    // Internal use.
    private boolean[] flags = new boolean[3];
    private static final int FLAG_USE_OPTIMIZE = 0; // simple enough to use optimizer
    private static final int FLAG_CHAIN_DANGLING = 1;
    private static final int FLAG_RECOMPUTE_BOUNDS = 2;

    // Internal -- used to keep track of first/last visible/invisible widgets
    // TODO: we could save the array by using the children array and simply store the indexes.
    private ConstraintWidget[] mChainEnds = new ConstraintWidget[4];
    private static final int CHAIN_FIRST = 0;
    private static final int CHAIN_LAST = 1;
    private static final int CHAIN_FIRST_VISIBLE = 2;
    private static final int CHAIN_LAST_VISIBLE = 3;

    private boolean mWidthMeasuredTooSmall = false;
    private boolean mHeightMeasuredTooSmall = false;

    /*-----------------------------------------------------------------------*/
    // Construction
    /*-----------------------------------------------------------------------*/

    /**
     * Default constructor
     */
    public ConstraintWidgetContainer() {
    }

    /**
     * Constructor
     *
     * @param x      x position
     * @param y      y position
     * @param width  width of the layout
     * @param height height of the layout
     */
    public ConstraintWidgetContainer(int x, int y, int width, int height) {
        super(x, y, width, height);
    }

    /**
     * Constructor
     *
     * @param width  width of the layout
     * @param height height of the layout
     */
    public ConstraintWidgetContainer(int width, int height) {
        super(width, height);
    }

    /**
     * Resolves the system directly when possible
     *
     * @param value optimization level
     */
    public void setOptimizationLevel(int value) {
        mOptimizationLevel = value;
    }

    /**
     * Specify the xml type for the container
     *
     * @return
     */
    @Override
    public String getType() {
        return "ConstraintLayout";
    }

    @Override
    public void reset() {
        mSystem.reset();
        if (USE_THREAD && mBackgroundSystem != null) {
            mBackgroundSystem.reset();
        }
        mPaddingLeft = 0;
        mPaddingRight = 0;
        mPaddingTop = 0;
        mPaddingBottom = 0;
        super.reset();
    }

    /**
     * Return true if the width given is too small for the content layed out
     */
    public boolean isWidthMeasuredTooSmall() { return mWidthMeasuredTooSmall; }

    /**
     * Return true if the height given is too small for the content layed out
     */
    public boolean isHeightMeasuredTooSmall() { return mHeightMeasuredTooSmall; }

    /**
     * Set a new ConstraintWidgetContainer containing the list of supplied
     * children. The dimensions of the container will be the bounding box
     * containing all the children.
     *
     * @param container the container instance
     * @param name      the name / id of the container
     * @param widgets   the list of widgets we want to move inside the container
     * @param padding   if padding > 0, the container returned will be enlarged by this amount
     * @return
     */
    public static ConstraintWidgetContainer createContainer(ConstraintWidgetContainer
                                                                    container, String name, ArrayList<ConstraintWidget> widgets, int padding) {
        Rectangle bounds = getBounds(widgets);
        if (bounds.width == 0 || bounds.height == 0) {
            return null;
        }
        if (padding > 0) {
            int maxPadding = Math.min(bounds.x, bounds.y);
            if (padding > maxPadding) {
                padding = maxPadding;
            }
            bounds.grow(padding, padding);
        }
        container.setOrigin(bounds.x, bounds.y);
        container.setDimension(bounds.width, bounds.height);
        container.setDebugName(name);

        ConstraintWidget parent = widgets.get(0).getParent();
        for (int i = 0, widgetsSize = widgets.size(); i < widgetsSize; i++) {
            final ConstraintWidget widget = widgets.get(i);
            if (widget.getParent() != parent) {
                continue; // only allow widgets sharing a parent to be counted
            }
            container.add(widget);
            widget.setX(widget.getX() - bounds.x);
            widget.setY(widget.getY() - bounds.y);
        }
        return container;
    }

    /*-----------------------------------------------------------------------*/
    // Overloaded methods from ConstraintWidget
    /*-----------------------------------------------------------------------*/

    /**
     * Add this widget to the solver
     *
     * @param system the solver we want to add the widget to
     */
    public boolean addChildrenToSolver(LinearSystem system, int group) {
        addToSolver(system, group);
        final int count = mChildren.size();
        boolean setMatchParent = false;
        if (mOptimizationLevel == OPTIMIZATION_ALL
                || mOptimizationLevel == OPTIMIZATION_BASIC) {
            if (optimize(system)) {
                return false; // all work is done
            }
        } else {
            // need to check match_parent
            setMatchParent = true;
        }
        for (int i = 0; i < count; i++) {
            ConstraintWidget widget = mChildren.get(i);
            if (widget instanceof ConstraintWidgetContainer) {
                DimensionBehaviour horizontalBehaviour = widget.mHorizontalDimensionBehaviour;
                DimensionBehaviour verticalBehaviour = widget.mVerticalDimensionBehaviour;
                if (horizontalBehaviour == DimensionBehaviour.WRAP_CONTENT) {
                    widget.setHorizontalDimensionBehaviour(DimensionBehaviour.FIXED);
                }
                if (verticalBehaviour == DimensionBehaviour.WRAP_CONTENT) {
                    widget.setVerticalDimensionBehaviour(DimensionBehaviour.FIXED);
                }
                widget.addToSolver(system, group);
                if (horizontalBehaviour == DimensionBehaviour.WRAP_CONTENT) {
                    widget.setHorizontalDimensionBehaviour(horizontalBehaviour);
                }
                if (verticalBehaviour == DimensionBehaviour.WRAP_CONTENT) {
                    widget.setVerticalDimensionBehaviour(verticalBehaviour);
                }
            } else {
                if (setMatchParent) {
                    Optimizer.checkMatchParent(this, system, widget);
                }
                widget.addToSolver(system, group);
            }
        }
        if (mHorizontalChainsSize > 0) {
            applyHorizontalChain(system);
        }
        if (mVerticalChainsSize > 0) {
            applyVerticalChain(system);
        }
        return true;
    }

    /**
     * Optimize the given system (try to do direct resolutions)
     *
     * @param system
     * @return
     */
    private boolean optimize(LinearSystem system) {
        final int count = mChildren.size();
        boolean done = false;
        int dv = 0;
        int dh = 0;
        int n = 0;
        for (int i = 0; i < count; i++) {
            ConstraintWidget widget = mChildren.get(i);
            // TODO: we should try to cache some of that
            widget.mHorizontalResolution = UNKNOWN;
            widget.mVerticalResolution = UNKNOWN;
            if (widget.mHorizontalDimensionBehaviour == DimensionBehaviour.MATCH_CONSTRAINT
                    || widget.mVerticalDimensionBehaviour == DimensionBehaviour.MATCH_CONSTRAINT) {
                widget.mHorizontalResolution = SOLVER;
                widget.mVerticalResolution = SOLVER;
            }
            if (widget instanceof Barrier) {
                widget.mHorizontalResolution = SOLVER;
                widget.mVerticalResolution = SOLVER;
            }
        }
        while (!done) {
            int prev = dv;
            int preh = dh;
            dv = 0;
            dh = 0;
            n++;
            if (DEBUG_OPTIMIZE) {
                System.out.println("Iteration " + n);
            }
            for (int i = 0; i < count; i++) {
                ConstraintWidget widget = mChildren.get(i);
                if (widget.mHorizontalResolution == UNKNOWN) {
                    if (mHorizontalDimensionBehaviour == DimensionBehaviour.WRAP_CONTENT) {
                        widget.mHorizontalResolution = SOLVER;
                    } else {
                        Optimizer.checkHorizontalSimpleDependency(this, system, widget);
                    }
                }
                if (widget.mVerticalResolution == UNKNOWN) {
                    if (mVerticalDimensionBehaviour == DimensionBehaviour.WRAP_CONTENT) {
                        widget.mVerticalResolution = SOLVER;
                    } else {
                        Optimizer.checkVerticalSimpleDependency(this, system, widget);
                    }
                }
                if (DEBUG_OPTIMIZE) {
                    System.out.println("[" + i + "]" + widget
                            + " H: " + widget.mHorizontalResolution
                            + " V: " + widget.mVerticalResolution);
                }
                if (widget.mVerticalResolution == UNKNOWN) {
                    dv++;
                }
                if (widget.mHorizontalResolution == UNKNOWN) {
                    dh++;
                }
            }
            if (DEBUG_OPTIMIZE) {
                System.out.println("dv: " + dv + " dh: " + dh);
            }
            if (dv == 0 && dh == 0) {
                done = true;
            } else if (prev == dv && preh == dh) {
                done = true;
                if (DEBUG_OPTIMIZE) {
                    System.out.println("Escape clause");
                }
            }
        }

        int sh = 0;
        int sv = 0;
        for (int i = 0; i < count; i++) {
            ConstraintWidget widget = mChildren.get(i);
            if (widget.mHorizontalResolution == SOLVER
                    || widget.mHorizontalResolution == UNKNOWN) {
                sh++;
            }
            if (widget.mVerticalResolution == SOLVER
                    || widget.mVerticalResolution == UNKNOWN) {
                sv++;
            }
        }
        if (sh == 0 && sv == 0) {
            return true;
        }
        return false;
    }

    /**
     * Apply specific rules for dealing with horizontal chains of widgets.
     * Horizontal chains are defined as a list of widget linked together with bi-directional horizontal connections
     *
     * @param system
     */
    private void applyHorizontalChain(LinearSystem system) {
        // TODO: we need to refactor applyHorizontalChain and applyVerticalChain in a single function, once
        // we get the anchors in an array. Let's do that post-1.0...
        for (int i = 0; i < mHorizontalChainsSize; i++) {
            ConstraintWidget first = mHorizontalChainsArray[i];
            int numMatchConstraints = countMatchConstraintsChainedWidgets(system, mChainEnds, mHorizontalChainsArray[i], HORIZONTAL, flags);
            // First, let's get to the first visible widget...
            ConstraintWidget currentWidget = mChainEnds[CHAIN_FIRST_VISIBLE];
            if (currentWidget == null) {
                // entire chain is gone...
                continue;
            }
            if (flags[FLAG_CHAIN_DANGLING]) {
                int x = first.getDrawX();
                while (currentWidget != null) {
                    system.addEquality(currentWidget.mLeft.mSolverVariable, x);
                    ConstraintWidget next = currentWidget.mHorizontalNextWidget;
                    x += currentWidget.mLeft.getMargin() + currentWidget.getWidth() + currentWidget.mRight.getMargin();
                    currentWidget = next;
                }
                continue;
            }
            boolean isChainSpread = first.mHorizontalChainStyle == CHAIN_SPREAD;
            boolean isChainPacked = first.mHorizontalChainStyle == CHAIN_PACKED;
            ConstraintWidget widget = first;
            boolean isWrapContent = mHorizontalDimensionBehaviour == DimensionBehaviour.WRAP_CONTENT;
            if ((mOptimizationLevel == OPTIMIZATION_ALL || mOptimizationLevel == OPTIMIZATION_CHAIN) && flags[FLAG_USE_OPTIMIZE]
                    && widget.mHorizontalChainFixedPosition && !isChainPacked && !isWrapContent
                    && first.mHorizontalChainStyle == CHAIN_SPREAD) {
                // TODO: implements direct resolution for CHAIN_SPREAD_INSIDE and CHAIN_PACKED
                Optimizer.applyDirectResolutionHorizontalChain(this, system, numMatchConstraints, widget);
            } else { // use the solver
                if (numMatchConstraints == 0 || isChainPacked) {
                    ConstraintWidget previousVisibleWidget = null;
                    ConstraintWidget lastWidget = null;
                    ConstraintWidget firstVisibleWidget = currentWidget;

                    // Then iterate on the widgets, skipping the ones with visibility == GONE
                    boolean isLast = false;
                    ConstraintWidget next = null;
                    while (currentWidget != null) {
                        next = currentWidget.mHorizontalNextWidget;
                        if (next == null) {
                            lastWidget = mChainEnds[CHAIN_LAST];
                            isLast = true;
                        }
                        if (isChainPacked) {
                            ConstraintAnchor left = currentWidget.mLeft;
                            int margin = left.getMargin();
                            if (previousVisibleWidget != null) {
                                int previousMargin = previousVisibleWidget.mRight.getMargin();
                                margin += previousMargin;
                            }
                            int strength = SolverVariable.STRENGTH_LOW;
                            if (firstVisibleWidget != currentWidget) {
                                strength = SolverVariable.STRENGTH_HIGH;
                            }
                            system.addGreaterThan(left.mSolverVariable, left.mTarget.mSolverVariable, margin, strength);
                            if (currentWidget.mHorizontalDimensionBehaviour == DimensionBehaviour.MATCH_CONSTRAINT) {
                                ConstraintAnchor right = currentWidget.mRight;
                                if (currentWidget.mMatchConstraintDefaultWidth == ConstraintWidget.MATCH_CONSTRAINT_WRAP) {
                                    int dimension = Math.max(currentWidget.mMatchConstraintMinWidth, currentWidget.getWidth());
                                    system.addEquality(right.mSolverVariable, left.mSolverVariable,
                                            dimension, SolverVariable.STRENGTH_HIGH);
                                } else {
                                    system.addGreaterThan(left.mSolverVariable, left.mTarget.mSolverVariable,
                                            left.mMargin, SolverVariable.STRENGTH_HIGH);
                                    system.addLowerThan(right.mSolverVariable, left.mSolverVariable,
                                            currentWidget.mMatchConstraintMinWidth, SolverVariable.STRENGTH_HIGH);
                                }
                            }
                        } else {
                            if (!isChainSpread && isLast && previousVisibleWidget != null) {
                                if (currentWidget.mRight.mTarget == null) {
                                    system.addEquality(currentWidget.mRight.mSolverVariable, currentWidget.getDrawRight());
                                } else {
                                    int margin = currentWidget.mRight.getMargin();
                                    system.addEquality(currentWidget.mRight.mSolverVariable, lastWidget.mRight.mTarget.mSolverVariable, -margin, SolverVariable.STRENGTH_EQUALITY);
                                }
                            } else if (!isChainSpread && !isLast && previousVisibleWidget == null) { // First element
                                if (currentWidget.mLeft.mTarget == null) {
                                    system.addEquality(currentWidget.mLeft.mSolverVariable, currentWidget.getDrawX());
                                } else {
                                    int margin = currentWidget.mLeft.getMargin();
                                    system.addEquality(currentWidget.mLeft.mSolverVariable, first.mLeft.mTarget.mSolverVariable, margin, SolverVariable.STRENGTH_EQUALITY);
                                }
                            } else {
                                // Middle elements, let's center things
                                ConstraintAnchor left = currentWidget.mLeft;
                                ConstraintAnchor right = currentWidget.mRight;
                                int leftMargin = left.getMargin();
                                int rightMargin = right.getMargin();
                                system.addGreaterThan(left.mSolverVariable, left.mTarget.mSolverVariable, leftMargin, SolverVariable.STRENGTH_LOW);
                                system.addLowerThan(right.mSolverVariable, right.mTarget.mSolverVariable, -rightMargin, SolverVariable.STRENGTH_LOW);
                                SolverVariable leftTarget = left.mTarget != null ? left.mTarget.mSolverVariable : null;
                                if (previousVisibleWidget == null) {
                                    // just in case we are dealing with a chain with only one visible element...
                                    leftTarget = first.mLeft.mTarget != null ? first.mLeft.mTarget.mSolverVariable : null;
                                }
                                if (next == null) {
                                    next = lastWidget.mRight.mTarget != null ? lastWidget.mRight.mTarget.mOwner : null;
                                }
                                if (next != null) {
                                    SolverVariable rightTarget = next.mLeft.mSolverVariable;
                                    if (isLast) {
                                        rightTarget = lastWidget.mRight.mTarget != null ? lastWidget.mRight.mTarget.mSolverVariable : null;
                                    }
                                    if (leftTarget != null && rightTarget != null) {
                                        system.addCentering(left.mSolverVariable, leftTarget, leftMargin, 0.5f,
                                                rightTarget, right.mSolverVariable, rightMargin, SolverVariable.STRENGTH_HIGHEST);
                                    }
                                }
                            }
                        }
                        previousVisibleWidget = currentWidget;
                        currentWidget = isLast ? null : next;
                    }
                    if (isChainPacked) {
                        ConstraintAnchor left = firstVisibleWidget.mLeft;
                        ConstraintAnchor right = lastWidget.mRight;
                        int leftMargin = left.getMargin();
                        int rightMargin = right.getMargin();
                        SolverVariable leftTarget = first.mLeft.mTarget != null ? first.mLeft.mTarget.mSolverVariable : null;
                        SolverVariable rightTarget = lastWidget.mRight.mTarget != null ? lastWidget.mRight.mTarget.mSolverVariable : null;
                        if (leftTarget != null && rightTarget != null) {
                            system.addLowerThan(right.mSolverVariable, rightTarget, -rightMargin, SolverVariable.STRENGTH_LOW);
                            system.addCentering(left.mSolverVariable, leftTarget, leftMargin, first.mHorizontalBiasPercent,
                                    rightTarget, right.mSolverVariable, rightMargin, SolverVariable.STRENGTH_HIGHEST);
                        }
                    }
                } else {
                    ConstraintWidget previous = null;
                    float totalWeights = 0;
                    while (currentWidget != null) {
                        if (currentWidget.mHorizontalDimensionBehaviour != DimensionBehaviour.MATCH_CONSTRAINT) {
                            int margin = currentWidget.mLeft.getMargin();
                            if (previous != null) {
                                margin += previous.mRight.getMargin();
                            }
                            int strength = SolverVariable.STRENGTH_HIGH;
                            if (currentWidget.mLeft.mTarget.mOwner.mHorizontalDimensionBehaviour == DimensionBehaviour.MATCH_CONSTRAINT) {
                                strength = SolverVariable.STRENGTH_MEDIUM;
                            }
                            system.addGreaterThan(currentWidget.mLeft.mSolverVariable, currentWidget.mLeft.mTarget.mSolverVariable, margin, strength);
                            margin = currentWidget.mRight.getMargin();
                            if (currentWidget.mRight.mTarget.mOwner.mLeft.mTarget != null && currentWidget.mRight.mTarget.mOwner.mLeft.mTarget.mOwner == currentWidget) {
                                margin += currentWidget.mRight.mTarget.mOwner.mLeft.getMargin();
                            }
                            strength = SolverVariable.STRENGTH_HIGH;
                            if (currentWidget.mRight.mTarget.mOwner.mHorizontalDimensionBehaviour == DimensionBehaviour.MATCH_CONSTRAINT) {
                                strength = SolverVariable.STRENGTH_MEDIUM;
                            }
                            SolverVariable rightTarget = currentWidget.mRight.mTarget.mSolverVariable;
                            if (currentWidget == mChainEnds[CHAIN_LAST_VISIBLE]) {
                                rightTarget = mChainEnds[CHAIN_LAST].mRight.mTarget.mSolverVariable;
                                strength = SolverVariable.STRENGTH_HIGH;
                            }
                            system.addLowerThan(currentWidget.mRight.mSolverVariable, rightTarget, -margin, strength);
                        } else {
                            totalWeights += currentWidget.mHorizontalWeight;
                            int margin = 0;
                            if (currentWidget.mRight.mTarget != null) {
                                margin = currentWidget.mRight.getMargin();
                                if (currentWidget != mChainEnds[CHAIN_LAST_VISIBLE]) {
                                    margin += currentWidget.mRight.mTarget.mOwner.mLeft.getMargin();
                                }
                            }
                            system.addGreaterThan(currentWidget.mRight.mSolverVariable, currentWidget.mLeft.mSolverVariable, 0, SolverVariable.STRENGTH_LOW);
                            system.addLowerThan(currentWidget.mRight.mSolverVariable, currentWidget.mRight.mTarget.mSolverVariable, -margin, SolverVariable.STRENGTH_LOW);
                        }
                        previous = currentWidget;
                        currentWidget = currentWidget.mHorizontalNextWidget;
                    }
                    if (numMatchConstraints == 1) {
                        ConstraintWidget w = mMatchConstraintsChainedWidgets[0];
                        int leftMargin = w.mLeft.getMargin();
                        if (w.mLeft.mTarget != null) {
                            leftMargin += w.mLeft.mTarget.getMargin();
                        }
                        int rightMargin = w.mRight.getMargin();
                        if (w.mRight.mTarget != null) {
                            rightMargin += w.mRight.mTarget.getMargin();
                        }
                        SolverVariable rightTarget = widget.mRight.mTarget.mSolverVariable;
                        if (w == mChainEnds[CHAIN_LAST_VISIBLE]) {
                            rightTarget = mChainEnds[CHAIN_LAST].mRight.mTarget.mSolverVariable;
                        }

                        if (w.mMatchConstraintDefaultWidth == ConstraintWidget.MATCH_CONSTRAINT_WRAP) {
                            system.addGreaterThan(widget.mLeft.mSolverVariable, widget.mLeft.mTarget.mSolverVariable, leftMargin, SolverVariable.STRENGTH_LOW);
                            system.addLowerThan(widget.mRight.mSolverVariable, rightTarget, -rightMargin, SolverVariable.STRENGTH_LOW);
                            system.addEquality(widget.mRight.mSolverVariable, widget.mLeft.mSolverVariable, widget.getWidth(), SolverVariable.STRENGTH_MEDIUM);
                        } else {
                            system.addEquality(w.mLeft.mSolverVariable, w.mLeft.mTarget.mSolverVariable, leftMargin, SolverVariable.STRENGTH_LOW);
                            system.addEquality(w.mRight.mSolverVariable, rightTarget, -rightMargin, SolverVariable.STRENGTH_LOW);
                        }
                    } else {
                        for (int j = 0; j < numMatchConstraints - 1; j++) {
                            ConstraintWidget current = mMatchConstraintsChainedWidgets[j];
                            ConstraintWidget nextWidget = mMatchConstraintsChainedWidgets[j + 1];
                            SolverVariable left = current.mLeft.mSolverVariable;
                            SolverVariable right = current.mRight.mSolverVariable;
                            SolverVariable nextLeft = nextWidget.mLeft.mSolverVariable;
                            SolverVariable nextRight = nextWidget.mRight.mSolverVariable;
                            if (nextWidget == mChainEnds[CHAIN_LAST_VISIBLE]) {
                                nextRight = mChainEnds[CHAIN_LAST].mRight.mSolverVariable;
                            }
                            int margin = current.mLeft.getMargin();
                            if (current.mLeft.mTarget != null && current.mLeft.mTarget.mOwner.mRight.mTarget != null
                                    && current.mLeft.mTarget.mOwner.mRight.mTarget.mOwner == current) {
                                margin += current.mLeft.mTarget.mOwner.mRight.getMargin();
                            }
                            system.addGreaterThan(left, current.mLeft.mTarget.mSolverVariable, margin, SolverVariable.STRENGTH_MEDIUM);
                            margin = current.mRight.getMargin();
                            if (current.mRight.mTarget != null && current.mHorizontalNextWidget != null) {
                                margin += current.mHorizontalNextWidget.mLeft.mTarget != null ? current.mHorizontalNextWidget.mLeft.getMargin() : 0;
                            }
                            system.addLowerThan(right, current.mRight.mTarget.mSolverVariable, -margin, SolverVariable.STRENGTH_MEDIUM);
                            if (j + 1 == numMatchConstraints - 1) {
                                // last element
                                margin = nextWidget.mLeft.getMargin();
                                if (nextWidget.mLeft.mTarget != null && nextWidget.mLeft.mTarget.mOwner.mRight.mTarget != null
                                        && nextWidget.mLeft.mTarget.mOwner.mRight.mTarget.mOwner == nextWidget) {
                                    margin += nextWidget.mLeft.mTarget.mOwner.mRight.getMargin();
                                }
                                system.addGreaterThan(nextLeft, nextWidget.mLeft.mTarget.mSolverVariable, margin, SolverVariable.STRENGTH_MEDIUM);
                                ConstraintAnchor anchor = nextWidget.mRight;
                                if (nextWidget == mChainEnds[CHAIN_LAST_VISIBLE]) {
                                    anchor = mChainEnds[CHAIN_LAST].mRight;
                                }
                                margin = anchor.getMargin();
                                if (anchor.mTarget != null && anchor.mTarget.mOwner.mLeft.mTarget != null
                                        && anchor.mTarget.mOwner.mLeft.mTarget.mOwner == nextWidget) {
                                    margin += anchor.mTarget.mOwner.mLeft.getMargin();
                                }
                                system.addLowerThan(nextRight, anchor.mTarget.mSolverVariable, -margin, SolverVariable.STRENGTH_MEDIUM);
                            }

                            if (widget.mMatchConstraintMaxWidth > 0) {
                                system.addLowerThan(right, left, widget.mMatchConstraintMaxWidth, SolverVariable.STRENGTH_MEDIUM);
                            }

                            ArrayRow row = system.createRow();
                            row.createRowEqualDimension(current.mHorizontalWeight,
                                    totalWeights, nextWidget.mHorizontalWeight,
                                    left, current.mLeft.getMargin(),
                                    right, current.mRight.getMargin(),
                                    nextLeft, nextWidget.mLeft.getMargin(),
                                    nextRight, nextWidget.mRight.getMargin());
                            system.addConstraint(row);
                        }
                    }
                }
            }
        }
    }

    /**
     * Apply specific rules for dealing with vertical chains of widgets.
     * Vertical chains are defined as a list of widget linked together with bi-directional vertical connections
     *
     * @param system
     */
    private void applyVerticalChain(LinearSystem system) {
        for (int i = 0; i < mVerticalChainsSize; i++) {
            ConstraintWidget first = mVerticalChainsArray[i];
            int numMatchConstraints = countMatchConstraintsChainedWidgets(system, mChainEnds, mVerticalChainsArray[i], VERTICAL, flags);
            // First, let's get to the first visible widget...
            ConstraintWidget currentWidget = mChainEnds[CHAIN_FIRST_VISIBLE];
            if (currentWidget == null) {
                // entire chain is gone...
                continue;
            }
            if (flags[FLAG_CHAIN_DANGLING]) {
                int y = first.getDrawY();
                while (currentWidget != null) {
                    system.addEquality(currentWidget.mTop.mSolverVariable, y);
                    ConstraintWidget next = currentWidget.mVerticalNextWidget;
                    y += currentWidget.mTop.getMargin() + currentWidget.getHeight() + currentWidget.mBottom.getMargin();
                    currentWidget = next;
                }
                continue;
            }
            boolean isChainSpread = first.mVerticalChainStyle == CHAIN_SPREAD;
            boolean isChainPacked = first.mVerticalChainStyle == CHAIN_PACKED;
            ConstraintWidget widget = first;
            boolean isWrapContent = mVerticalDimensionBehaviour == DimensionBehaviour.WRAP_CONTENT;
            if ((mOptimizationLevel == OPTIMIZATION_ALL || mOptimizationLevel == OPTIMIZATION_CHAIN) && flags[FLAG_USE_OPTIMIZE]
                    && widget.mVerticalChainFixedPosition && !isChainPacked && !isWrapContent
                    && first.mVerticalChainStyle == CHAIN_SPREAD) {
                // TODO: implements direct resolution for CHAIN_SPREAD_INSIDE and CHAIN_PACKED
                Optimizer.applyDirectResolutionVerticalChain(this, system, numMatchConstraints, widget);
            } else { // use the solver
                if (numMatchConstraints == 0 || isChainPacked) {
                    ConstraintWidget previousVisibleWidget = null;
                    ConstraintWidget lastWidget = null;
                    ConstraintWidget firstVisibleWidget = currentWidget;

                    // Then iterate on the widgets, skipping the ones with visibility == GONE
                    boolean isLast = false;
                    ConstraintWidget next = null;
                    while (currentWidget != null) {
                        next = currentWidget.mVerticalNextWidget;
                        if (next == null) {
                            lastWidget = mChainEnds[CHAIN_LAST];
                            isLast = true;
                        }
                        if (isChainPacked) {
                            ConstraintAnchor top = currentWidget.mTop;
                            int margin = top.getMargin();
                            if (previousVisibleWidget != null) {
                                int previousMargin = previousVisibleWidget.mBottom.getMargin();
                                margin += previousMargin;
                            }
                            int strength = SolverVariable.STRENGTH_LOW;
                            if (firstVisibleWidget != currentWidget) {
                                strength = SolverVariable.STRENGTH_HIGH;
                            }
                            SolverVariable source = null;
                            SolverVariable target = null;
                            if (top.mTarget != null) {
                                source = top.mSolverVariable;
                                target = top.mTarget.mSolverVariable;
                            } else if (currentWidget.mBaseline.mTarget != null) {
                                source = currentWidget.mBaseline.mSolverVariable;
                                target = currentWidget.mBaseline.mTarget.mSolverVariable;
                                margin -= top.getMargin();
                            }
                            if (source != null && target != null) {
                                system.addGreaterThan(source, target, margin, strength);
                            }
                            if (currentWidget.mVerticalDimensionBehaviour == DimensionBehaviour.MATCH_CONSTRAINT) {
                                ConstraintAnchor bottom = currentWidget.mBottom;
                                if (currentWidget.mMatchConstraintDefaultHeight == ConstraintWidget.MATCH_CONSTRAINT_WRAP) {
                                    int dimension = Math.max(currentWidget.mMatchConstraintMinHeight, currentWidget.getHeight());
                                    system.addEquality(bottom.mSolverVariable, top.mSolverVariable,
                                            dimension, SolverVariable.STRENGTH_HIGH);
                                } else {
                                    system.addGreaterThan(top.mSolverVariable, top.mTarget.mSolverVariable,
                                            top.mMargin, SolverVariable.STRENGTH_HIGH);
                                    system.addLowerThan(bottom.mSolverVariable, top.mSolverVariable,
                                            currentWidget.mMatchConstraintMinHeight, SolverVariable.STRENGTH_HIGH);
                                }
                            }
                        } else {
                            if (!isChainSpread && isLast && previousVisibleWidget != null) {
                                if (currentWidget.mBottom.mTarget == null) {
                                    system.addEquality(currentWidget.mBottom.mSolverVariable, currentWidget.getDrawBottom());
                                } else {
                                    int margin = currentWidget.mBottom.getMargin();
                                    system.addEquality(currentWidget.mBottom.mSolverVariable, lastWidget.mBottom.mTarget.mSolverVariable, -margin, SolverVariable.STRENGTH_EQUALITY);
                                }
                            } else if (!isChainSpread && !isLast && previousVisibleWidget == null) { // First element
                                if (currentWidget.mTop.mTarget == null) {
                                    system.addEquality(currentWidget.mTop.mSolverVariable, currentWidget.getDrawY());
                                } else {
                                    int margin = currentWidget.mTop.getMargin();
                                    system.addEquality(currentWidget.mTop.mSolverVariable, first.mTop.mTarget.mSolverVariable, margin, SolverVariable.STRENGTH_EQUALITY);
                                }
                            } else {
                                // Middle elements, let's center things
                                ConstraintAnchor top = currentWidget.mTop;
                                ConstraintAnchor bottom = currentWidget.mBottom;
                                int topMargin = top.getMargin();
                                int bottomMargin = bottom.getMargin();
                                system.addGreaterThan(top.mSolverVariable, top.mTarget.mSolverVariable, topMargin, SolverVariable.STRENGTH_LOW);
                                system.addLowerThan(bottom.mSolverVariable, bottom.mTarget.mSolverVariable, -bottomMargin, SolverVariable.STRENGTH_LOW);
                                SolverVariable topTarget = top.mTarget != null ? top.mTarget.mSolverVariable : null;
                                if (previousVisibleWidget == null) {
                                    // just in case we are dealing with a chain with only one visible element...
                                    topTarget = first.mTop.mTarget != null ? first.mTop.mTarget.mSolverVariable : null;
                                }
                                if (next == null) {
                                    next = lastWidget.mBottom.mTarget != null ? lastWidget.mBottom.mTarget.mOwner : null;
                                }
                                if (next != null) {
                                    SolverVariable bottomTarget = next.mTop.mSolverVariable;
                                    if (isLast) {
                                        bottomTarget = lastWidget.mBottom.mTarget != null ? lastWidget.mBottom.mTarget.mSolverVariable : null;
                                    }
                                    if (topTarget != null && bottomTarget != null) {
                                        system.addCentering(top.mSolverVariable, topTarget, topMargin, 0.5f,
                                                bottomTarget, bottom.mSolverVariable, bottomMargin, SolverVariable.STRENGTH_HIGHEST);
                                    }
                                }
                            }
                        }
                        previousVisibleWidget = currentWidget;
                        currentWidget = isLast ? null : next;
                    }
                    if (isChainPacked) {
                        ConstraintAnchor top = firstVisibleWidget.mTop;
                        ConstraintAnchor bottom = lastWidget.mBottom;
                        int topMargin = top.getMargin();
                        int bottomMargin = bottom.getMargin();
                        SolverVariable topTarget = first.mTop.mTarget != null ? first.mTop.mTarget.mSolverVariable : null;
                        SolverVariable bottomTarget = lastWidget.mBottom.mTarget != null ? lastWidget.mBottom.mTarget.mSolverVariable : null;
                        if (topTarget != null && bottomTarget != null) {
                            system.addLowerThan(bottom.mSolverVariable, bottomTarget, -bottomMargin, SolverVariable.STRENGTH_LOW);
                            system.addCentering(top.mSolverVariable, topTarget, topMargin, first.mVerticalBiasPercent,
                                    bottomTarget, bottom.mSolverVariable, bottomMargin, SolverVariable.STRENGTH_HIGHEST);
                        }
                    }
                } else {
                    ConstraintWidget previous = null;
                    float totalWeights = 0;
                    while (currentWidget != null) {
                        if (currentWidget.mVerticalDimensionBehaviour != DimensionBehaviour.MATCH_CONSTRAINT) {
                            int margin = currentWidget.mTop.getMargin();
                            if (previous != null) {
                                margin += previous.mBottom.getMargin();
                            }
                            int strength = SolverVariable.STRENGTH_HIGH;
                            if (currentWidget.mTop.mTarget.mOwner.mVerticalDimensionBehaviour == DimensionBehaviour.MATCH_CONSTRAINT) {
                                strength = SolverVariable.STRENGTH_MEDIUM;
                            }
                            system.addGreaterThan(currentWidget.mTop.mSolverVariable, currentWidget.mTop.mTarget.mSolverVariable, margin, strength);
                            margin = currentWidget.mBottom.getMargin();
                            if (currentWidget.mBottom.mTarget.mOwner.mTop.mTarget != null && currentWidget.mBottom.mTarget.mOwner.mTop.mTarget.mOwner == currentWidget) {
                                margin += currentWidget.mBottom.mTarget.mOwner.mTop.getMargin();
                            }
                            strength = SolverVariable.STRENGTH_HIGH;
                            if (currentWidget.mBottom.mTarget.mOwner.mVerticalDimensionBehaviour == DimensionBehaviour.MATCH_CONSTRAINT) {
                                strength = SolverVariable.STRENGTH_MEDIUM;
                            }
                            SolverVariable bottomTarget = currentWidget.mBottom.mTarget.mSolverVariable;
                            if (currentWidget == mChainEnds[CHAIN_LAST_VISIBLE]) {
                                bottomTarget = mChainEnds[CHAIN_LAST].mBottom.mTarget.mSolverVariable;
                                strength = SolverVariable.STRENGTH_HIGH;
                            }
                            system.addLowerThan(currentWidget.mBottom.mSolverVariable, bottomTarget, -margin, strength);
                        } else {
                            totalWeights += currentWidget.mVerticalWeight;
                            int margin = 0;
                            if (currentWidget.mBottom.mTarget != null) {
                                margin = currentWidget.mBottom.getMargin();
                                if (currentWidget != mChainEnds[CHAIN_LAST_VISIBLE]) {
                                    margin += currentWidget.mBottom.mTarget.mOwner.mTop.getMargin();
                                }
                            }
                            system.addGreaterThan(currentWidget.mBottom.mSolverVariable, currentWidget.mTop.mSolverVariable, 0, SolverVariable.STRENGTH_LOW);
                            system.addLowerThan(currentWidget.mBottom.mSolverVariable, currentWidget.mBottom.mTarget.mSolverVariable, -margin, SolverVariable.STRENGTH_LOW);
                        }
                        previous = currentWidget;
                        currentWidget = currentWidget.mVerticalNextWidget;
                    }
                    if (numMatchConstraints == 1) {
                        ConstraintWidget w = mMatchConstraintsChainedWidgets[0];
                        int topMargin = w.mTop.getMargin();
                        if (w.mTop.mTarget != null) {
                            topMargin += w.mTop.mTarget.getMargin();
                        }
                        int bottomMargin = w.mBottom.getMargin();
                        if (w.mBottom.mTarget != null) {
                            bottomMargin += w.mBottom.mTarget.getMargin();
                        }
                        SolverVariable bottomTarget = widget.mBottom.mTarget.mSolverVariable;
                        if (w == mChainEnds[CHAIN_LAST_VISIBLE]) {
                            bottomTarget = mChainEnds[CHAIN_LAST].mBottom.mTarget.mSolverVariable;
                        }

                        if (w.mMatchConstraintDefaultHeight == ConstraintWidget.MATCH_CONSTRAINT_WRAP) {
                            system.addGreaterThan(widget.mTop.mSolverVariable, widget.mTop.mTarget.mSolverVariable, topMargin, SolverVariable.STRENGTH_LOW);
                            system.addLowerThan(widget.mBottom.mSolverVariable, bottomTarget, -bottomMargin, SolverVariable.STRENGTH_LOW);
                            system.addEquality(widget.mBottom.mSolverVariable, widget.mTop.mSolverVariable, widget.getHeight(), SolverVariable.STRENGTH_MEDIUM);
                        } else {
                            system.addEquality(w.mTop.mSolverVariable, w.mTop.mTarget.mSolverVariable, topMargin, SolverVariable.STRENGTH_LOW);
                            system.addEquality(w.mBottom.mSolverVariable, bottomTarget, -bottomMargin, SolverVariable.STRENGTH_LOW);
                        }
                    } else {
                        for (int j = 0; j < numMatchConstraints - 1; j++) {
                            ConstraintWidget current = mMatchConstraintsChainedWidgets[j];
                            ConstraintWidget nextWidget = mMatchConstraintsChainedWidgets[j + 1];
                            SolverVariable top = current.mTop.mSolverVariable;
                            SolverVariable bottom = current.mBottom.mSolverVariable;
                            SolverVariable nextTop = nextWidget.mTop.mSolverVariable;
                            SolverVariable nextBottom = nextWidget.mBottom.mSolverVariable;
                            if (nextWidget == mChainEnds[CHAIN_LAST_VISIBLE]) {
                                nextBottom = mChainEnds[CHAIN_LAST].mBottom.mSolverVariable;
                            }
                            int margin = current.mTop.getMargin();
                            if (current.mTop.mTarget != null && current.mTop.mTarget.mOwner.mBottom.mTarget != null
                                    && current.mTop.mTarget.mOwner.mBottom.mTarget.mOwner == current) {
                                margin += current.mTop.mTarget.mOwner.mBottom.getMargin();
                            }
                            system.addGreaterThan(top, current.mTop.mTarget.mSolverVariable, margin, SolverVariable.STRENGTH_MEDIUM);
                            margin = current.mBottom.getMargin();
                            if (current.mBottom.mTarget != null && current.mVerticalNextWidget != null) {
                                margin += current.mVerticalNextWidget.mTop.mTarget != null ? current.mVerticalNextWidget.mTop.getMargin() : 0;
                            }
                            system.addLowerThan(bottom, current.mBottom.mTarget.mSolverVariable, -margin, SolverVariable.STRENGTH_MEDIUM);
                            if (j + 1 == numMatchConstraints - 1) {
                                // last element
                                margin = nextWidget.mTop.getMargin();
                                if (nextWidget.mTop.mTarget != null && nextWidget.mTop.mTarget.mOwner.mBottom.mTarget != null
                                        && nextWidget.mTop.mTarget.mOwner.mBottom.mTarget.mOwner == nextWidget) {
                                    margin += nextWidget.mTop.mTarget.mOwner.mBottom.getMargin();
                                }
                                system.addGreaterThan(nextTop, nextWidget.mTop.mTarget.mSolverVariable, margin, SolverVariable.STRENGTH_MEDIUM);
                                ConstraintAnchor anchor = nextWidget.mBottom;
                                if (nextWidget == mChainEnds[CHAIN_LAST_VISIBLE]) {
                                    anchor = mChainEnds[CHAIN_LAST].mBottom;
                                }
                                margin = anchor.getMargin();
                                if (anchor.mTarget != null && anchor.mTarget.mOwner.mTop.mTarget != null
                                        && anchor.mTarget.mOwner.mTop.mTarget.mOwner == nextWidget) {
                                    margin += anchor.mTarget.mOwner.mTop.getMargin();
                                }
                                system.addLowerThan(nextBottom, anchor.mTarget.mSolverVariable, -margin, SolverVariable.STRENGTH_MEDIUM);
                            }

                            if (widget.mMatchConstraintMaxHeight > 0) {
                                system.addLowerThan(bottom, top, widget.mMatchConstraintMaxHeight, SolverVariable.STRENGTH_MEDIUM);
                            }

                            ArrayRow row = system.createRow();
                            row.createRowEqualDimension(current.mVerticalWeight,
                                    totalWeights, nextWidget.mVerticalWeight,
                                    top, current.mTop.getMargin(),
                                    bottom, current.mBottom.getMargin(),
                                    nextTop, nextWidget.mTop.getMargin(),
                                    nextBottom, nextWidget.mBottom.getMargin());
                            system.addConstraint(row);
                        }
                    }
                }
            }
        }
    }

    /**
     * Update the frame of the layout and its children from the solver
     *
     * @param system the solver we get the values from.
     */
    public void updateChildrenFromSolver(LinearSystem system, int group, boolean flags[]) {
        flags[FLAG_RECOMPUTE_BOUNDS] = false;
        updateFromSolver(system, group);
        final int count = mChildren.size();
        for (int i = 0; i < count; i++) {
            ConstraintWidget widget = mChildren.get(i);
            widget.updateFromSolver(system, group);
            if (widget.mHorizontalDimensionBehaviour == DimensionBehaviour.MATCH_CONSTRAINT
                && widget.getWidth() < widget.getWrapWidth()) {
                flags[FLAG_RECOMPUTE_BOUNDS] = true;
            }
            if (widget.mVerticalDimensionBehaviour == DimensionBehaviour.MATCH_CONSTRAINT
                    && widget.getHeight() < widget.getWrapHeight()) {
                flags[FLAG_RECOMPUTE_BOUNDS] = true;
            }
        }
    }

    /**
     * Set the padding on this container. It will apply to the position of the children.
     *
     * @param left   left padding
     * @param top    top padding
     * @param right  right padding
     * @param bottom bottom padding
     */
    public void setPadding(int left, int top, int right, int bottom) {
        mPaddingLeft = left;
        mPaddingTop = top;
        mPaddingRight = right;
        mPaddingBottom = bottom;
    }

    /**
     * Layout the tree of widgets
     */
    @Override
    public void layout() {
        int prex = mX;
        int prey = mY;
        int prew = Math.max(0, getWidth());
        int preh = Math.max(0, getHeight());
        mWidthMeasuredTooSmall = false;
        mHeightMeasuredTooSmall = false;

        if (mParent != null && USE_SNAPSHOT) {
            if (mSnapshot == null) {
                mSnapshot = new Snapshot(this);
            }
            mSnapshot.updateFrom(this);
            // We clear ourselves of external anchors as
            // well as repositioning us to (0, 0)
            // before inserting us in the solver, so that our
            // children's positions get computed relative to us.
            setX(mPaddingLeft);
            setY(mPaddingTop);
            resetAnchors();
            resetSolverVariables(mSystem.getCache());
        } else {
            mX = 0;
            mY = 0;
        }

        boolean wrap_override = false;
        DimensionBehaviour originalVerticalDimensionBehaviour = mVerticalDimensionBehaviour;
        DimensionBehaviour originalHorizontalDimensionBehaviour = mHorizontalDimensionBehaviour;

        if (DEBUG_LAYOUT) {
            System.out.println("layout with prew: " + prew + " (" + mHorizontalDimensionBehaviour
                + ") preh: " + preh + " (" + mVerticalDimensionBehaviour + ")");
        }
        if (mOptimizationLevel == OPTIMIZATION_ALL
                && (mVerticalDimensionBehaviour == DimensionBehaviour.WRAP_CONTENT
                || mHorizontalDimensionBehaviour == DimensionBehaviour.WRAP_CONTENT)) {
            // TODO: do the wrap calculation in two separate passes
            findWrapSize(mChildren, flags);
            wrap_override = flags[FLAG_USE_OPTIMIZE];
            if (DEBUG_LAYOUT) {
                System.out.println("layout with wrap width: " + mWrapWidth + " wrap height: " + mWrapHeight + " wrap override? " + wrap_override);
            }
            if (prew > 0 && preh > 0 && (mWrapWidth > prew || mWrapHeight > preh)) {
                // TODO: this could be better optimized with a tighter coupling between view measures and
                // wrap/layout. For now, simply escape to the solver.
                wrap_override = false;
            }
            if (wrap_override) {
                if (mHorizontalDimensionBehaviour == DimensionBehaviour.WRAP_CONTENT) {
                    mHorizontalDimensionBehaviour = DimensionBehaviour.FIXED;
                    if (prew > 0 && prew < mWrapWidth) {
                        mWidthMeasuredTooSmall = true;
                        setWidth(prew);
                    } else {
                        setWidth(Math.max(mMinWidth, mWrapWidth));
                    }
                }
                if (mVerticalDimensionBehaviour == DimensionBehaviour.WRAP_CONTENT) {
                    mVerticalDimensionBehaviour = DimensionBehaviour.FIXED;
                    if (preh > 0 && preh < mWrapHeight) {
                        mHeightMeasuredTooSmall = true;
                        setHeight(preh);
                    } else {
                        setHeight(Math.max(mMinHeight, mWrapHeight));
                    }
                }
            }
        }

        // Reset the chains before iterating on our children
        resetChains();

        // Before we solve our system, we should call layout() on any
        // of our children that is a container.
        final int count = mChildren.size();
        for (int i = 0; i < count; i++) {
            ConstraintWidget widget = mChildren.get(i);
            if (widget instanceof WidgetContainer) {
                ((WidgetContainer) widget).layout();
            }
        }

        // Now let's solve our system as usual
        boolean needsSolving = true;
        int countSolve = 0;
        while (needsSolving) {
            countSolve++;
            try {
                mSystem.reset();
                if (DEBUG) {
                    setDebugSolverName(mSystem, getDebugName());
                    for (int i = 0; i < count; i++) {
                        ConstraintWidget widget = mChildren.get(i);
                        if (widget.getDebugName() != null) {
                            widget.setDebugSolverName(mSystem, widget.getDebugName());
                        }
                    }
                }
                needsSolving = addChildrenToSolver(mSystem, ConstraintAnchor.ANY_GROUP);
                if (needsSolving) {
                    mSystem.minimize();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (needsSolving) {
                updateChildrenFromSolver(mSystem, ConstraintAnchor.ANY_GROUP, flags);
            } else {
                updateFromSolver(mSystem, ConstraintAnchor.ANY_GROUP);
                for (int i = 0; i < count; i++) {
                    ConstraintWidget widget = mChildren.get(i);
                    if (widget.mHorizontalDimensionBehaviour == DimensionBehaviour.MATCH_CONSTRAINT
                        && widget.getWidth() < widget.getWrapWidth()) {
                        flags[FLAG_RECOMPUTE_BOUNDS] = true;
                        break;
                    }
                    if (widget.mVerticalDimensionBehaviour == DimensionBehaviour.MATCH_CONSTRAINT
                        && widget.getHeight() < widget.getWrapHeight()) {
                        flags[FLAG_RECOMPUTE_BOUNDS] = true;
                        break;
                    }
                }
            }
            needsSolving = false;

            if (countSolve < MAX_ITERATIONS && flags[FLAG_RECOMPUTE_BOUNDS]) {
                // let's get the new bounds
                int maxX = 0;
                int maxY = 0;
                for (int i = 0; i < count; i++) {
                    ConstraintWidget widget = mChildren.get(i);
                    maxX = Math.max(maxX, widget.mX + widget.getWidth());
                    maxY = Math.max(maxY, widget.mY + widget.getHeight());
                }
                maxX = Math.max(mMinWidth, maxX);
                maxY = Math.max(mMinHeight, maxY);
                if (originalHorizontalDimensionBehaviour == DimensionBehaviour.WRAP_CONTENT) {
                    if (getWidth() < maxX) {
                        if (DEBUG_LAYOUT) {
                            System.out.println("layout override width from " + getWidth() + " vs " + maxX);
                        }
                        setWidth(maxX);
                        mHorizontalDimensionBehaviour = DimensionBehaviour.WRAP_CONTENT; // force using the solver
                        wrap_override = true;
                        needsSolving = true;
                    }
                }
                if (originalVerticalDimensionBehaviour == DimensionBehaviour.WRAP_CONTENT) {
                    if (getHeight() < maxY) {
                        if (DEBUG_LAYOUT) {
                            System.out.println("layout override height from " + getHeight() + " vs " + maxY);
                        }
                        setHeight(maxY);
                        mVerticalDimensionBehaviour = DimensionBehaviour.WRAP_CONTENT; // force using the solver
                        wrap_override = true;
                        needsSolving = true;
                    }
                }
            }

            int width = Math.max(mMinWidth, getWidth());
            if (width > getWidth()) {
                if (DEBUG_LAYOUT) {
                    System.out.println("layout override 2, width from " + getWidth() + " vs " + width);
                }
                setWidth(width);
                mHorizontalDimensionBehaviour = DimensionBehaviour.FIXED;
                wrap_override = true;
                needsSolving = true;
            }
            int height = Math.max(mMinHeight, getHeight());
            if (height > getHeight()) {
                if (DEBUG_LAYOUT) {
                    System.out.println("layout override 2, height from " + getHeight() + " vs " + height);
                }
                setHeight(height);
                mVerticalDimensionBehaviour = DimensionBehaviour.FIXED;
                wrap_override = true;
                needsSolving = true;
            }

            if (!wrap_override) {
                if (mHorizontalDimensionBehaviour == DimensionBehaviour.WRAP_CONTENT && prew > 0) {
                    if (getWidth() > prew) {
                        if (DEBUG_LAYOUT) {
                            System.out.println("layout override 3, width from " + getWidth() + " vs " + prew);
                        }
                        mWidthMeasuredTooSmall = true;
                        wrap_override = true;
                        mHorizontalDimensionBehaviour = DimensionBehaviour.FIXED;
                        setWidth(prew);
                        needsSolving = true;
                    }
                }
                if (mVerticalDimensionBehaviour == DimensionBehaviour.WRAP_CONTENT && preh > 0) {
                    if (getHeight() > preh) {
                        if (DEBUG_LAYOUT) {
                            System.out.println("layout override 3, height from " + getHeight() + " vs " + preh);
                        }
                        mHeightMeasuredTooSmall = true;
                        wrap_override = true;
                        mVerticalDimensionBehaviour = DimensionBehaviour.FIXED;
                        setHeight(preh);
                        needsSolving = true;
                    }
                }
            }
        }
        if (DEBUG_LAYOUT) {
            System.out.println("Solved system in " + countSolve + " iterations (" + getWidth() + " x " + getHeight() + ")");
        }
        if (mParent != null && USE_SNAPSHOT) {
            int width = Math.max(mMinWidth, getWidth());
            int height = Math.max(mMinHeight, getHeight());
            // Let's restore our state...
            mSnapshot.applyTo(this);
            setWidth(width + mPaddingLeft + mPaddingRight);
            setHeight(height + mPaddingTop + mPaddingBottom);
        } else {
            mX = prex;
            mY = prey;
        }
        if (wrap_override) {
            mHorizontalDimensionBehaviour = originalHorizontalDimensionBehaviour;
            mVerticalDimensionBehaviour = originalVerticalDimensionBehaviour;
        }
        resetSolverVariables(mSystem.getCache());
        if (this == getRootConstraintContainer()) {
            updateDrawPosition();
        }
    }

    /**
     * set the anchor to the group value if it less than my current group value
     * True if i was able to set it.
     * recurse to other if you were set.
     *
     * @param anchor
     * @return
     */
    static int setGroup(ConstraintAnchor anchor, int group) {
        int oldGroup = anchor.mGroup;
        if (anchor.mOwner.getParent() == null) {
            return group;
        }
        if (oldGroup <= group) {
            return oldGroup;
        }

        anchor.mGroup = group;
        ConstraintAnchor opposite = anchor.getOpposite();
        ConstraintAnchor target = anchor.mTarget;

        group = (opposite != null) ? setGroup(opposite, group) : group;
        group = (target != null) ? setGroup(target, group) : group;
        group = (opposite != null) ? setGroup(opposite, group) : group;

        anchor.mGroup = group;

        return group;
    }

    public int layoutFindGroupsSimple() {
        final int size = mChildren.size();
        for (int j = 0; j < size; j++) {
            ConstraintWidget widget = mChildren.get(j);
            widget.mLeft.mGroup = 0;
            widget.mRight.mGroup = 0;
            widget.mTop.mGroup = 1;
            widget.mBottom.mGroup = 1;
            widget.mBaseline.mGroup = 1;
        }
        return 2;
    }

    /**
     * This recursively walks the tree of connected components
     * calculating there distance to the left,right,top and bottom
     *
     * @param widget
     */
    public void findHorizontalWrapRecursive(ConstraintWidget widget, boolean[] flags) {
        if (widget.mHorizontalDimensionBehaviour == DimensionBehaviour.MATCH_CONSTRAINT
                && widget.mVerticalDimensionBehaviour == DimensionBehaviour.MATCH_CONSTRAINT
                && widget.mDimensionRatio > 0) {
            flags[FLAG_USE_OPTIMIZE] = false;
            return;
        }
        if (widget.mHorizontalDimensionBehaviour == DimensionBehaviour.MATCH_CONSTRAINT
                && widget.mMatchConstraintDefaultWidth == ConstraintWidget.MATCH_CONSTRAINT_PERCENT) {
            flags[FLAG_USE_OPTIMIZE] = false;
            return;
        }
        int w = widget.getOptimizerWrapWidth();

        if (widget.mHorizontalDimensionBehaviour == DimensionBehaviour.MATCH_CONSTRAINT) {
            if (widget.mVerticalDimensionBehaviour != DimensionBehaviour.MATCH_CONSTRAINT
                    && widget.mDimensionRatio > 0) {
                flags[FLAG_USE_OPTIMIZE] = false;
                return;
                // TODO: support ratio
                // w = (int) (widget.mDimensionRatio * h);
                // widget.setWidth(w);
            }
        }
        int distToRight = w;
        int distToLeft = w;
        ConstraintWidget leftWidget = null;
        ConstraintWidget rightWidget = null;
        widget.mHorizontalWrapVisited = true;

        if (widget instanceof Guideline) {
            Guideline guideline = (Guideline) widget;
            if (guideline.getOrientation() == ConstraintWidget.VERTICAL) {
                distToLeft = 0;
                distToRight = 0;
                if (guideline.getRelativeBegin() != Guideline.UNKNOWN) {
                    distToLeft = guideline.getRelativeBegin();
                } else if (guideline.getRelativeEnd() != Guideline.UNKNOWN) {
                    distToRight = guideline.getRelativeEnd();
                } else if (guideline.getRelativePercent() != Guideline.UNKNOWN) {
                    flags[FLAG_USE_OPTIMIZE] = false;
                    return;
                }
            }
        } else if (!(widget.mRight.isConnected() || (widget.mLeft.isConnected()))) {
            distToLeft += widget.getX();
        } else {
            if (widget.mRight.mTarget != null && widget.mLeft.mTarget != null
                && widget.mIsWidthWrapContent
                && widget.mVerticalDimensionBehaviour == DimensionBehaviour.MATCH_CONSTRAINT) {
                flags[FLAG_USE_OPTIMIZE] = false;
                return;
            }
            if (widget.mRight.mTarget != null && widget.mLeft.mTarget != null
                    && ((widget.mRight.mTarget == widget.mLeft.mTarget)
                        || ((widget.mRight.mTarget.mOwner == widget.mLeft.mTarget.mOwner)
                            && (widget.mRight.mTarget.mOwner != widget.mParent)))) {
                flags[FLAG_USE_OPTIMIZE] = false;
                return;
            }
            if (widget.mRight.mTarget != null) {
                rightWidget = widget.mRight.mTarget.mOwner;
                distToRight += widget.mRight.getMargin();
                if (!rightWidget.isRoot() && !rightWidget.mHorizontalWrapVisited) {
                    findHorizontalWrapRecursive(rightWidget, flags);
                }
            }
            if (widget.mLeft.mTarget != null) {
                leftWidget = widget.mLeft.mTarget.mOwner;
                distToLeft += widget.mLeft.getMargin();
                if (!leftWidget.isRoot() && !leftWidget.mHorizontalWrapVisited) {
                    findHorizontalWrapRecursive(leftWidget, flags);
                }
            }

            if (widget.mRight.mTarget != null && !rightWidget.isRoot()) {
                if (widget.mRight.mTarget.mType == ConstraintAnchor.Type.RIGHT) {
                    distToRight += rightWidget.mDistToRight - rightWidget.getOptimizerWrapWidth();
                } else if (widget.mRight.mTarget.getType() == ConstraintAnchor.Type.LEFT) {
                    distToRight += rightWidget.mDistToRight;
                }
                // Center connection
                widget.mRightHasCentered = rightWidget.mRightHasCentered
                        || (rightWidget.mLeft.mTarget != null && rightWidget.mRight.mTarget != null
                        && rightWidget.mHorizontalDimensionBehaviour != DimensionBehaviour.MATCH_CONSTRAINT);
                if (widget.mRightHasCentered
                        && (rightWidget.mLeft.mTarget == null ? true : rightWidget.mLeft.mTarget.mOwner != widget)) {
                    distToRight += distToRight - rightWidget.mDistToRight;
                }
            }

            if (widget.mLeft.mTarget != null && !leftWidget.isRoot()) {
                if (widget.mLeft.mTarget.getType() == ConstraintAnchor.Type.LEFT) {
                    distToLeft += leftWidget.mDistToLeft - leftWidget.getOptimizerWrapWidth();
                } else if (widget.mLeft.mTarget.getType() == ConstraintAnchor.Type.RIGHT) {
                    distToLeft += leftWidget.mDistToLeft;
                }
                // Center connection
                widget.mLeftHasCentered = leftWidget.mLeftHasCentered
                        || (leftWidget.mLeft.mTarget != null && leftWidget.mRight.mTarget != null
                            && leftWidget.mHorizontalDimensionBehaviour != DimensionBehaviour.MATCH_CONSTRAINT);
                if (widget.mLeftHasCentered
                        && (leftWidget.mRight.mTarget == null ? true : leftWidget.mRight.mTarget.mOwner != widget)) {
                    distToLeft += distToLeft - leftWidget.mDistToLeft;
                }
            }
        }
        if (widget.getVisibility() == GONE) {
            distToLeft -= widget.mWidth;
            distToRight -= widget.mWidth;
        }
        widget.mDistToLeft = distToLeft;
        widget.mDistToRight = distToRight;
    }

    public void findVerticalWrapRecursive(ConstraintWidget widget, boolean[] flags) {
        if (widget.mVerticalDimensionBehaviour == DimensionBehaviour.MATCH_CONSTRAINT) {
            if (widget.mHorizontalDimensionBehaviour != DimensionBehaviour.MATCH_CONSTRAINT
                    && widget.mDimensionRatio > 0) {
                flags[FLAG_USE_OPTIMIZE] = false;
                return;
                // TODO: support ratio
                // h = (int) (w / widget.mDimensionRatio);
                // widget.setHeight(h);
            }
        }
        if (widget.mVerticalDimensionBehaviour == DimensionBehaviour.MATCH_CONSTRAINT
                && widget.mMatchConstraintDefaultHeight == ConstraintWidget.MATCH_CONSTRAINT_PERCENT) {
            flags[FLAG_USE_OPTIMIZE] = false;
            return;
        }

        int h = widget.getOptimizerWrapHeight();

        int distToTop = h;
        int distToBottom = h;
        ConstraintWidget topWidget = null;
        ConstraintWidget bottomWidget = null;
        widget.mVerticalWrapVisited = true;

        if (widget instanceof Guideline) {
            Guideline guideline = (Guideline) widget;
            if (guideline.getOrientation() == ConstraintWidget.HORIZONTAL) {
                distToTop = 0;
                distToBottom = 0;
                if (guideline.getRelativeBegin() != Guideline.UNKNOWN) {
                    distToTop = guideline.getRelativeBegin();
                } else if (guideline.getRelativeEnd() != Guideline.UNKNOWN) {
                    distToBottom = guideline.getRelativeEnd();
                } else if (guideline.getRelativePercent() != Guideline.UNKNOWN) {
                    flags[FLAG_USE_OPTIMIZE] = false;
                    return;
                }
            }
        } else if (!(widget.mBaseline.mTarget != null || widget.mTop.mTarget != null || widget.mBottom.mTarget != null)) {
            distToTop += widget.getY();
        } else {
            if (widget.mBottom.mTarget != null && widget.mTop.mTarget != null
                && widget.mIsHeightWrapContent
                && widget.mHorizontalDimensionBehaviour == DimensionBehaviour.MATCH_CONSTRAINT) {
                flags[FLAG_USE_OPTIMIZE] = false;
                return;
            }
            if (widget.mBottom.mTarget != null && widget.mTop.mTarget != null
                    && ((widget.mBottom.mTarget == widget.mTop.mTarget)
                        || ((widget.mBottom.mTarget.mOwner == widget.mTop.mTarget.mOwner)
                            && (widget.mBottom.mTarget.mOwner != widget.mParent)))) {
                flags[FLAG_USE_OPTIMIZE] = false;
                return;
            }
            if (widget.mBaseline.isConnected()) {
                ConstraintWidget baseLineWidget = widget.mBaseline.mTarget.getOwner();
                if (!baseLineWidget.mVerticalWrapVisited) {
                    findVerticalWrapRecursive(baseLineWidget, flags);
                }
                distToTop = Math.max(baseLineWidget.mDistToTop - baseLineWidget.mHeight + h, h);
                distToBottom = Math.max(baseLineWidget.mDistToBottom - baseLineWidget.mHeight + h, h);
                if (widget.getVisibility() == GONE) {
                    distToTop -= widget.mHeight;
                    distToBottom -= widget.mHeight;
                }
                widget.mDistToTop = distToTop;
                widget.mDistToBottom = distToBottom;
                return; // if baseline connected no need to look at top or bottom
            }
            if (widget.mTop.isConnected()) {
                topWidget = widget.mTop.mTarget.getOwner();
                distToTop += widget.mTop.getMargin();
                if (!topWidget.isRoot() && !topWidget.mVerticalWrapVisited) {
                    findVerticalWrapRecursive(topWidget, flags);
                }
            }
            if (widget.mBottom.isConnected()) {
                bottomWidget = widget.mBottom.mTarget.getOwner();
                distToBottom += widget.mBottom.getMargin();
                if (!bottomWidget.isRoot() && !bottomWidget.mVerticalWrapVisited) {
                    findVerticalWrapRecursive(bottomWidget, flags);
                }
            }

            if (widget.mTop.mTarget != null && !topWidget.isRoot()) {
                if (widget.mTop.mTarget.getType() == ConstraintAnchor.Type.TOP) {
                    distToTop += topWidget.mDistToTop - topWidget.getOptimizerWrapHeight();
                } else if (widget.mTop.mTarget.getType() == ConstraintAnchor.Type.BOTTOM) {
                    distToTop += topWidget.mDistToTop;
                }
                // Center connection
                widget.mTopHasCentered = topWidget.mTopHasCentered
                        || (topWidget.mTop.mTarget != null && topWidget.mTop.mTarget.mOwner != widget
                        && topWidget.mBottom.mTarget != null
                        && topWidget.mBottom.mTarget.mOwner != widget
                        && topWidget.mVerticalDimensionBehaviour != DimensionBehaviour.MATCH_CONSTRAINT);
                if (widget.mTopHasCentered
                        && (topWidget.mBottom.mTarget == null ? true : topWidget.mBottom.mTarget.mOwner != widget)) {
                    distToTop += distToTop - topWidget.mDistToTop;
                }
            }
            if (widget.mBottom.mTarget != null && !bottomWidget.isRoot()) {
                if (widget.mBottom.mTarget.getType() == ConstraintAnchor.Type.BOTTOM) {
                    distToBottom += bottomWidget.mDistToBottom - bottomWidget.getOptimizerWrapHeight();
                } else if (widget.mBottom.mTarget.getType() == ConstraintAnchor.Type.TOP) {
                    distToBottom += bottomWidget.mDistToBottom;
                }
                // Center connection
                widget.mBottomHasCentered = bottomWidget.mBottomHasCentered
                        || (bottomWidget.mTop.mTarget != null && bottomWidget.mTop.mTarget.mOwner != widget
                        && bottomWidget.mBottom.mTarget != null
                        && bottomWidget.mBottom.mTarget.mOwner != widget
                        && bottomWidget.mVerticalDimensionBehaviour != DimensionBehaviour.MATCH_CONSTRAINT);
                if (widget.mBottomHasCentered
                        && (bottomWidget.mTop.mTarget == null ? true : bottomWidget.mTop.mTarget.mOwner != widget)) {
                    distToBottom += distToBottom - bottomWidget.mDistToBottom;
                }
            }
        }
        if (widget.getVisibility() == GONE) {
            distToTop -= widget.mHeight;
            distToBottom -= widget.mHeight;
        }

        widget.mDistToTop = distToTop;
        widget.mDistToBottom = distToBottom;
    }

    /**
     * calculates the wrapContent size.
     *
     * @param children
     */
    public void findWrapSize(ArrayList<ConstraintWidget> children, boolean[] flags) {
        int maxTopDist = 0;
        int maxLeftDist = 0;
        int maxRightDist = 0;
        int maxBottomDist = 0;

        int maxConnectWidth = 0;
        int maxConnectHeight = 0;
        final int size = children.size();
        flags[FLAG_USE_OPTIMIZE] = true;

        try {
            for (int j = 0; j < size; j++) {
                ConstraintWidget widget = children.get(j);
                if (widget.isRoot()) {
                    continue;
                }
                if (!widget.mHorizontalWrapVisited) {
                    findHorizontalWrapRecursive(widget, flags);
                }
                if (!flags[FLAG_USE_OPTIMIZE]) {
                    return;
                }
                if (!widget.mVerticalWrapVisited) {
                    findVerticalWrapRecursive(widget, flags);
                }
                if (!flags[FLAG_USE_OPTIMIZE]) {
                    return;
                }
                int connectWidth = widget.mDistToLeft + widget.mDistToRight - widget.getWidth();
                int connectHeight = widget.mDistToTop + widget.mDistToBottom - widget.getHeight();
                if (widget.mHorizontalDimensionBehaviour == DimensionBehaviour.MATCH_PARENT) {
                    connectWidth = widget.getWidth() + widget.mLeft.mMargin + widget.mRight.mMargin;
                }
                if (widget.mVerticalDimensionBehaviour == DimensionBehaviour.MATCH_PARENT) {
                    connectHeight =
                        widget.getHeight() + widget.mTop.mMargin + widget.mBottom.mMargin;
                }
                if (widget.getVisibility() == GONE) {
                    connectWidth = 0;
                    connectHeight = 0;
                }
                maxLeftDist = Math.max(maxLeftDist, widget.mDistToLeft);
                maxRightDist = Math.max(maxRightDist, widget.mDistToRight);
                maxBottomDist = Math.max(maxBottomDist, widget.mDistToBottom);
                maxTopDist = Math.max(maxTopDist, widget.mDistToTop);
                maxConnectWidth = Math.max(maxConnectWidth, connectWidth);
                maxConnectHeight = Math.max(maxConnectHeight, connectHeight);
            }
            int max = Math.max(maxLeftDist, maxRightDist);
            mWrapWidth = Math.max(mMinWidth, Math.max(max, maxConnectWidth));
            max = Math.max(maxTopDist, maxBottomDist);
            mWrapHeight = Math.max(mMinHeight, Math.max(max, maxConnectHeight));
        } finally {
            for (int j = 0; j < size; j++) {
                ConstraintWidget child = children.get(j);
                child.mHorizontalWrapVisited = false;
                child.mVerticalWrapVisited = false;
                child.mLeftHasCentered = false;
                child.mRightHasCentered = false;
                child.mTopHasCentered = false;
                child.mBottomHasCentered = false;
            }
        }
    }

    /**
     * Find groups
     */
    public int layoutFindGroups() {
        ConstraintAnchor.Type[] dir = {
                ConstraintAnchor.Type.LEFT, ConstraintAnchor.Type.RIGHT, ConstraintAnchor.Type.TOP,
                ConstraintAnchor.Type.BASELINE, ConstraintAnchor.Type.BOTTOM
        };

        int label = 1;
        final int size = mChildren.size();
        for (int j = 0; j < size; j++) {
            ConstraintWidget widget = mChildren.get(j);
            ConstraintAnchor anchor = null;

            anchor = widget.mLeft;
            if (anchor.mTarget != null) {
                if (setGroup(anchor, label) == label) {
                    label++;
                }
            } else {
                anchor.mGroup = ConstraintAnchor.ANY_GROUP;
            }

            anchor = widget.mTop;
            if (anchor.mTarget != null) {
                if (setGroup(anchor, label) == label) {
                    label++;
                }
            } else {
                anchor.mGroup = ConstraintAnchor.ANY_GROUP;
            }

            anchor = widget.mRight;
            if (anchor.mTarget != null) {
                if (setGroup(anchor, label) == label) {
                    label++;
                }
            } else {
                anchor.mGroup = ConstraintAnchor.ANY_GROUP;
            }

            anchor = widget.mBottom;
            if (anchor.mTarget != null) {
                if (setGroup(anchor, label) == label) {
                    label++;
                }
            } else {
                anchor.mGroup = ConstraintAnchor.ANY_GROUP;
            }

            anchor = widget.mBaseline;
            if (anchor.mTarget != null) {
                if (setGroup(anchor, label) == label) {
                    label++;
                }
            } else {
                anchor.mGroup = ConstraintAnchor.ANY_GROUP;
            }
        }
        boolean notDone = true;
        int count = 0;
        int fix = 0;

        // This cleans up the misses of the previous step
        // It is a brute force algorithm that is related to bubble sort O(N*Log(N))
        while (notDone) {
            notDone = false;
            count++;
            for (int j = 0; j < size; j++) {
                ConstraintWidget widget = mChildren.get(j);
                for (int i = 0; i < dir.length; i++) {
                    ConstraintAnchor.Type type = dir[i];
                    ConstraintAnchor anchor = null;
                    switch (type) {
                        case LEFT: {
                            anchor = widget.mLeft;
                        }
                        break;
                        case TOP: {
                            anchor = widget.mTop;
                        }
                        break;
                        case RIGHT: {
                            anchor = widget.mRight;
                        }
                        break;
                        case BOTTOM: {
                            anchor = widget.mBottom;
                        }
                        break;
                        case BASELINE: {
                            anchor = widget.mBaseline;
                        }
                        break;
                        case CENTER:
                        case CENTER_X:
                        case CENTER_Y:
                        case NONE:
                            break;
                    }
                    ConstraintAnchor target = anchor.mTarget;
                    if (target == null) {
                        continue;
                    }

                    if (target.mOwner.getParent() != null && target.mGroup != anchor.mGroup) {
                        target.mGroup = anchor.mGroup = (anchor.mGroup > target.mGroup) ? target.mGroup : anchor.mGroup;
                        fix++;
                        notDone = true;
                    }

                    ConstraintAnchor opposite = target.getOpposite();
                    if (opposite != null && opposite.mGroup != anchor.mGroup) {
                        opposite.mGroup = anchor.mGroup = (anchor.mGroup > opposite.mGroup) ? opposite.mGroup : anchor.mGroup;
                        fix++;
                        notDone = true;
                    }
                }
            }
        }

        // This remaps the groups to a compact range
        int index = 0;
        int[] table = new int[mChildren.size() * dir.length + 1];
        Arrays.fill(table, -1);
        for (int j = 0; j < size; j++) {
            ConstraintWidget widget = mChildren.get(j);
            ConstraintAnchor anchor = null;

            anchor = widget.mLeft;
            if (anchor.mGroup != ConstraintAnchor.ANY_GROUP) {
                int g = anchor.mGroup;
                if (table[g] == -1) {
                    table[g] = index++;
                }
                anchor.mGroup = table[g];
            }

            anchor = widget.mTop;
            if (anchor.mGroup != ConstraintAnchor.ANY_GROUP) {
                int g = anchor.mGroup;
                if (table[g] == -1) {
                    table[g] = index++;
                }
                anchor.mGroup = table[g];
            }

            anchor = widget.mRight;
            if (anchor.mGroup != ConstraintAnchor.ANY_GROUP) {
                int g = anchor.mGroup;
                if (table[g] == -1) {
                    table[g] = index++;
                }
                anchor.mGroup = table[g];
            }

            anchor = widget.mBottom;
            if (anchor.mGroup != ConstraintAnchor.ANY_GROUP) {
                int g = anchor.mGroup;
                if (table[g] == -1) {
                    table[g] = index++;
                }
                anchor.mGroup = table[g];
            }

            anchor = widget.mBaseline;
            if (anchor.mGroup != ConstraintAnchor.ANY_GROUP) {
                int g = anchor.mGroup;
                if (table[g] == -1) {
                    table[g] = index++;
                }
                anchor.mGroup = table[g];
            }
        }
        return index;
    }

    /**
     * Layout by groups
     */
    public void layoutWithGroup(int numOfGroups) {
        int prex = mX;
        int prey = mY;
        if (mParent != null && USE_SNAPSHOT) {
            if (mSnapshot == null) {
                mSnapshot = new Snapshot(this);
            }
            mSnapshot.updateFrom(this);
            // We clear ourselves of external anchors as
            // well as repositioning us to (0, 0)
            // before inserting us in the solver, so that our
            // children's positions get computed relative to us.
            mX = 0;
            mY = 0;
            resetAnchors();
            resetSolverVariables(mSystem.getCache());
        } else {
            mX = 0;
            mY = 0;
        }
        // Before we solve our system, we should call layout() on any
        // of our children that is a container.
        final int count = mChildren.size();
        for (int i = 0; i < count; i++) {
            ConstraintWidget widget = mChildren.get(i);
            if (widget instanceof WidgetContainer) {
                ((WidgetContainer) widget).layout();
            }
        }

        mLeft.mGroup = 0;
        mRight.mGroup = 0;
        mTop.mGroup = 1;
        mBottom.mGroup = 1;
        mSystem.reset();
        if (USE_THREAD) {
            if (mBackgroundSystem == null) {
                mBackgroundSystem = new LinearSystem();
            } else {
                mBackgroundSystem.reset();
            }
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        addToSolver(mBackgroundSystem, 1);
                        mBackgroundSystem.minimize();
                        updateFromSolver(mBackgroundSystem, 1);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            thread.start();
            try {
                addToSolver(mSystem, 0);
                mSystem.minimize();
                updateFromSolver(mSystem, 0);
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            updateFromSolver(mSystem, ConstraintAnchor.APPLY_GROUP_RESULTS);
        } else {
            for (int i = 0; i < numOfGroups; i++) {
                try {
                    addToSolver(mSystem, i);
                    mSystem.minimize();
                    updateFromSolver(mSystem, i);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                updateFromSolver(mSystem, ConstraintAnchor.APPLY_GROUP_RESULTS);
            }
        }

        if (mParent != null && USE_SNAPSHOT) {
            int width = getWidth();
            int height = getHeight();
            // Let's restore our state...
            mSnapshot.applyTo(this);
            setWidth(width);
            setHeight(height);
        } else {
            mX = prex;
            mY = prey;
        }

        if (this == getRootConstraintContainer()) {
            updateDrawPosition();
        }
    }

    /**
     * Indicates if the container knows how to layout its content on its own
     *
     * @return true if the container does the layout, false otherwise
     */
    public boolean handlesInternalConstraints() {
        return false;
    }

    /*-----------------------------------------------------------------------*/
    // Guidelines
    /*-----------------------------------------------------------------------*/

    /**
     * Accessor to the vertical guidelines contained in the table.
     *
     * @return array of guidelines
     */
    public ArrayList<Guideline> getVerticalGuidelines() {
        ArrayList<Guideline> guidelines = new ArrayList<>();
        for (int i = 0, mChildrenSize = mChildren.size(); i < mChildrenSize; i++) {
            final ConstraintWidget widget = mChildren.get(i);
            if (widget instanceof Guideline) {
                Guideline guideline = (Guideline) widget;
                if (guideline.getOrientation() == Guideline.VERTICAL) {
                    guidelines.add(guideline);
                }
            }
        }
        return guidelines;
    }

    /**
     * Accessor to the horizontal guidelines contained in the table.
     *
     * @return array of guidelines
     */
    public ArrayList<Guideline> getHorizontalGuidelines() {
        ArrayList<Guideline> guidelines = new ArrayList<>();
        for (int i = 0, mChildrenSize = mChildren.size(); i < mChildrenSize; i++) {
            final ConstraintWidget widget = mChildren.get(i);
            if (widget instanceof Guideline) {
                Guideline guideline = (Guideline) widget;
                if (guideline.getOrientation() == Guideline.HORIZONTAL) {
                    guidelines.add(guideline);
                }
            }
        }
        return guidelines;
    }

    public LinearSystem getSystem() {
        return mSystem;
    }

    /**
     * Reset the chains array. Need to be called before layout.
     */
    private void resetChains() {
        mHorizontalChainsSize = 0;
        mVerticalChainsSize = 0;
    }

    /**
     * Add the chain which constraintWidget is part of. Called by ConstraintWidget::addToSolver()
     *
     * @param constraintWidget
     * @param type             HORIZONTAL or VERTICAL chain
     */
    void addChain(ConstraintWidget constraintWidget, int type) {
        ConstraintWidget widget = constraintWidget;
        if (type == HORIZONTAL) {
            // find the left most widget that doesn't have a dual connection (i.e., start of chain)
            while (widget.mLeft.mTarget != null
                    && widget.mLeft.mTarget.mOwner.mRight.mTarget != null
                    && widget.mLeft.mTarget.mOwner.mRight.mTarget == widget.mLeft
                    && widget.mLeft.mTarget.mOwner != widget) {
                widget = widget.mLeft.mTarget.mOwner;
            }
            addHorizontalChain(widget);
        } else if (type == VERTICAL) {
            // find the top most widget that doesn't have a dual connection (i.e., start of chain)
            while (widget.mTop.mTarget != null
                    && widget.mTop.mTarget.mOwner.mBottom.mTarget != null
                    && widget.mTop.mTarget.mOwner.mBottom.mTarget == widget.mTop
                    && widget.mTop.mTarget.mOwner != widget) {
                widget = widget.mTop.mTarget.mOwner;
            }
            addVerticalChain(widget);
        }
    }

    /**
     * Add a widget to the list of horizontal chains. The widget is the left-most widget
     * of the chain which doesn't have a left dual connection.
     *
     * @param widget widget starting the chain
     */
    private void addHorizontalChain(ConstraintWidget widget) {
        for (int i = 0; i < mHorizontalChainsSize; i++) {
            if (mHorizontalChainsArray[i] == widget) {
                return;
            }
        }
        if (mHorizontalChainsSize + 1 >= mHorizontalChainsArray.length) {
            mHorizontalChainsArray = Arrays.copyOf(mHorizontalChainsArray, mHorizontalChainsArray.length * 2);
        }
        mHorizontalChainsArray[mHorizontalChainsSize] = widget;
        mHorizontalChainsSize++;
    }

    /**
     * Add a widget to the list of vertical chains. The widget is the top-most widget
     * of the chain which doesn't have a top dual connection.
     *
     * @param widget widget starting the chain
     */
    private void addVerticalChain(ConstraintWidget widget) {
        for (int i = 0; i < mVerticalChainsSize; i++) {
            if (mVerticalChainsArray[i] == widget) {
                return;
            }
        }
        if (mVerticalChainsSize + 1 >= mVerticalChainsArray.length) {
            mVerticalChainsArray = Arrays.copyOf(mVerticalChainsArray, mVerticalChainsArray.length * 2);
        }
        mVerticalChainsArray[mVerticalChainsSize] = widget;
        mVerticalChainsSize++;
    }

    /**
     * Traverse a chain and fill the mMatchConstraintsChainedWidgets array with widgets
     * that are set to MATCH_CONSTRAINT, as we need to apply a common behavior to those
     * (we set their dimensions to be equal, minus their margins)
     */
    private int countMatchConstraintsChainedWidgets(LinearSystem system, ConstraintWidget[] chainEnds, ConstraintWidget widget, int direction, boolean[] flags) {
        int count = 0;
        flags[FLAG_USE_OPTIMIZE] = true; // will set to false if the chain is not optimizable
        flags[FLAG_CHAIN_DANGLING] = false; // will set to true if the chain is not connected on one or both endpoints
        chainEnds[CHAIN_FIRST] = null;
        chainEnds[CHAIN_FIRST_VISIBLE] = null;
        chainEnds[CHAIN_LAST] = null;
        chainEnds[CHAIN_LAST_VISIBLE] = null;

        if (direction == HORIZONTAL) {
            boolean fixedPosition = true;
            ConstraintWidget first = widget;
            ConstraintWidget last = null;
            if (widget.mLeft.mTarget != null && widget.mLeft.mTarget.mOwner != this) {
                fixedPosition = false;
            }
            widget.mHorizontalNextWidget = null;
            ConstraintWidget firstVisible = null;
            if (widget.getVisibility() != GONE) {
                firstVisible = widget;
            }
            ConstraintWidget lastVisible = firstVisible;
            while (widget.mRight.mTarget != null) {
                widget.mHorizontalNextWidget = null;
                if (widget.getVisibility() != GONE) {
                    if (firstVisible == null) {
                        firstVisible = widget;
                    }
                    if (lastVisible != null && lastVisible != widget) {
                        lastVisible.mHorizontalNextWidget = widget;
                    }
                    lastVisible = widget;
                } else {
                    system.addEquality(widget.mLeft.mSolverVariable, widget.mLeft.mTarget.mSolverVariable, 0, SolverVariable.STRENGTH_EQUALITY);
                    system.addEquality(widget.mRight.mSolverVariable, widget.mLeft.mSolverVariable, 0, SolverVariable.STRENGTH_EQUALITY);
                }
                if (widget.getVisibility() != GONE && widget.mHorizontalDimensionBehaviour == DimensionBehaviour.MATCH_CONSTRAINT) {
                    if (widget.mVerticalDimensionBehaviour == DimensionBehaviour.MATCH_CONSTRAINT) {
                        flags[FLAG_USE_OPTIMIZE] = false; // signal that this chain is not optimizable.
                    }
                    if (widget.mDimensionRatio <= 0) {
                        flags[FLAG_USE_OPTIMIZE] = false; // signal that this chain is not optimizable.
                        if (count + 1 >= mMatchConstraintsChainedWidgets.length) {
                            mMatchConstraintsChainedWidgets = Arrays.copyOf(mMatchConstraintsChainedWidgets, mMatchConstraintsChainedWidgets.length * 2);
                        }
                        mMatchConstraintsChainedWidgets[count++] = widget;
                    }
                }
                if (widget.mRight.mTarget.mOwner.mLeft.mTarget == null) {
                    break;
                }
                if (widget.mRight.mTarget.mOwner.mLeft.mTarget.mOwner != widget) {
                    break;
                }
                if (widget.mRight.mTarget.mOwner == widget) {
                    break;
                }
                widget = widget.mRight.mTarget.mOwner;
                last = widget;
            }
            if (widget.mRight.mTarget != null && widget.mRight.mTarget.mOwner != this) {
                fixedPosition = false;
            }
            if (first.mLeft.mTarget == null || last.mRight.mTarget == null) {
                flags[FLAG_CHAIN_DANGLING] = true;
            }
            // keep track of the endpoints -- if both are fixed (for now, only look if they point to the parent)
            // we can optimize the resolution without passing by the solver
            first.mHorizontalChainFixedPosition = fixedPosition;
            last.mHorizontalNextWidget = null;
            chainEnds[CHAIN_FIRST] = first;
            chainEnds[CHAIN_FIRST_VISIBLE] = firstVisible;
            chainEnds[CHAIN_LAST] = last;
            chainEnds[CHAIN_LAST_VISIBLE] = lastVisible;
        } else {
            boolean fixedPosition = true;
            ConstraintWidget first = widget;
            ConstraintWidget last = null;
            if (widget.mTop.mTarget != null && widget.mTop.mTarget.mOwner != this) {
                fixedPosition = false;
            }
            widget.mVerticalNextWidget = null;
            ConstraintWidget firstVisible = null;
            if (widget.getVisibility() != GONE) {
                firstVisible = widget;
            }
            ConstraintWidget lastVisible = firstVisible;
            while (widget.mBottom.mTarget != null) {
                widget.mVerticalNextWidget = null;
                if (widget.getVisibility() != GONE) {
                    if (firstVisible == null) {
                        firstVisible = widget;
                    }
                    if (lastVisible != null && lastVisible != widget) {
                        lastVisible.mVerticalNextWidget = widget;
                    }
                    lastVisible = widget;
                } else {
                    system.addEquality(widget.mTop.mSolverVariable, widget.mTop.mTarget.mSolverVariable, 0, SolverVariable.STRENGTH_EQUALITY);
                    system.addEquality(widget.mBottom.mSolverVariable, widget.mTop.mSolverVariable, 0, SolverVariable.STRENGTH_EQUALITY);
                }
                if (widget.getVisibility() != GONE && widget.mVerticalDimensionBehaviour == DimensionBehaviour.MATCH_CONSTRAINT) {
                    if (widget.mHorizontalDimensionBehaviour == DimensionBehaviour.MATCH_CONSTRAINT) {
                        flags[FLAG_USE_OPTIMIZE] = false; // signal that this chain is not optimizable.
                    }
                    if (widget.mDimensionRatio <= 0) {
                        flags[FLAG_USE_OPTIMIZE] = false; // signal that this chain is not optimizable.
                        if (count + 1 >= mMatchConstraintsChainedWidgets.length) {
                            mMatchConstraintsChainedWidgets = Arrays.copyOf(mMatchConstraintsChainedWidgets, mMatchConstraintsChainedWidgets.length * 2);
                        }
                        mMatchConstraintsChainedWidgets[count++] = widget;
                    }
                }
                if (widget.mBottom.mTarget.mOwner.mTop.mTarget == null) {
                    break;
                }
                if (widget.mBottom.mTarget.mOwner.mTop.mTarget.mOwner != widget) {
                    break;
                }
                if (widget.mBottom.mTarget.mOwner == widget) {
                    break;
                }
                widget = widget.mBottom.mTarget.mOwner;
                last = widget;
            }
            if (widget.mBottom.mTarget != null && widget.mBottom.mTarget.mOwner != this) {
                fixedPosition = false;
            }
            if (first.mTop.mTarget == null || last.mBottom.mTarget == null) {
                flags[FLAG_CHAIN_DANGLING] = true;
            }
            // keep track of the endpoints -- if both are fixed (for now, only look if they point to the parent)
            // we can optimize the resolution without passing by the solver
            first.mVerticalChainFixedPosition = fixedPosition;
            last.mVerticalNextWidget = null;
            chainEnds[CHAIN_FIRST] = first;
            chainEnds[CHAIN_FIRST_VISIBLE] = firstVisible;
            chainEnds[CHAIN_LAST] = last;
            chainEnds[CHAIN_LAST_VISIBLE] = lastVisible;
        }
        return count;
    }

}
