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
    private static final boolean DEBUG = false;
    private static final boolean USE_SNAPSHOT = true;
    private static final boolean USE_DIRECT_CHAIN_RESOLUTION = true;

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

    // If true, we will resolve as much as we can directly, bypassing the solver
    private boolean mDirectResolution = true;

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
     * @param value true to resolve directly, false to use the generic solver
     */
    public void setDirectResolution(boolean value) {
        mDirectResolution = value;
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
        boolean done = false;
        int dv = 0;
        int dh = 0;
        int n = 0;
        if (mDirectResolution) {
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
            }
            while (!done) {
                int prev = dv;
                int preh = dh;
                dv = 0;
                dh = 0;
                n++;
                if (DEBUG) {
                    System.out.println("Iteration " + n);
                }
                for (int i = 0; i < count; i++) {
                    ConstraintWidget widget = mChildren.get(i);
                    if (widget.mHorizontalResolution == UNKNOWN) {
                        if (mHorizontalDimensionBehaviour == DimensionBehaviour.WRAP_CONTENT) {
                            widget.mHorizontalResolution = SOLVER;
                        }
                        else {
                            checkHorizontalSimpleDependency(system, widget);
                        }
                    }
                    if (widget.mVerticalResolution == UNKNOWN) {
                        if (mVerticalDimensionBehaviour == DimensionBehaviour.WRAP_CONTENT) {
                            widget.mVerticalResolution = SOLVER;
                        }
                        else {
                            checkVerticalSimpleDependency(system, widget);
                        }
                    }
                    if (DEBUG) {
                        System.out.println("[" + i + "]" + widget
                                  + " H: "+ widget.mHorizontalResolution
                                  + " V: " + widget.mVerticalResolution);
                    }
                    if (widget.mVerticalResolution == UNKNOWN) {
                        dv++;
                    }
                    if (widget.mHorizontalResolution == UNKNOWN) {
                        dh++;
                    }
                }
                if (DEBUG) {
                    System.out.println("dv: " + dv + " dh: " + dh);
                }
                if (dv == 0 && dh == 0) {
                    done = true;
                } else if (prev == dv && preh == dh) {
                    done = true;
                    if (DEBUG) {
                        System.out.println("Escape clause");
                    }
                }
            }
        }

        int sh = 0;
        int sv = 0;
        for (int i = 0; i < count; i++) {
            ConstraintWidget widget = mChildren.get(i);
            if (!mDirectResolution) {
                widget.mHorizontalResolution = SOLVER;
                widget.mVerticalResolution = SOLVER;
            } else {
                if (widget.mHorizontalResolution == SOLVER) {
                    sh++;
                }
                if (widget.mVerticalResolution == SOLVER) {
                    sv++;
                }
            }
        }
        if (mDirectResolution && sh == 0 && sv == 0) {
            return false;
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
     * Apply specific rules for dealing with horizontal chains of widgets.
     * Horizontal chains are defined as a list of widget linked together with bi-directional horizontal connections
     * @param system
     */
    private void applyHorizontalChain(LinearSystem system) {
        for (int i = 0; i < mHorizontalChainsSize; i++) {
            ConstraintWidget first = mHorizontalChainsArray[i];
            int numMatchConstraints = countMatchConstraintsChainedWidgets(mHorizontalChainsArray[i], HORIZONTAL);
            // For now, only allow packed chains if all widgets are in fixed dimensions
            boolean chainPacked = (first.mHorizontalChainStyle == CHAIN_PACKED) && (numMatchConstraints == 0);
            ConstraintWidget widget = first;
            if (mHorizontalDimensionBehaviour == DimensionBehaviour.WRAP_CONTENT) {
                ConstraintWidget previous = null;
                int strength = 1;
                while (previous == null || (widget.mLeft.mTarget != null && widget.mLeft.mTarget.mOwner == previous)) {
                    if (widget.mHorizontalDimensionBehaviour == DimensionBehaviour.MATCH_CONSTRAINT) {
                        system.addGreaterThan(widget.mRight.mSolverVariable, widget.mLeft.mSolverVariable, widget.getWidth(), strength);
                    }
                    int margin = widget.mLeft.getMargin();
                    if (previous != null) {
                        margin += previous.mRight.getMargin();
                    }
                    system.addGreaterThan(widget.mLeft.mSolverVariable, widget.mLeft.mTarget.mSolverVariable, margin, strength);
                    previous = widget;
                    widget = widget.mRight.mTarget.mOwner;
                }
                if (previous != null) {
                    system.addGreaterThan(previous.mRight.mTarget.mSolverVariable, previous.mRight.mSolverVariable, previous.mRight.getMargin(), 0);
                }
                continue;
            }
            if (USE_DIRECT_CHAIN_RESOLUTION && widget.mHorizontalChainFixedPosition && !chainPacked
                    && first.mHorizontalChainStyle == CHAIN_SPREAD) {
                // TODO: implements direct resolution for CHAIN_SPREAD_INSIDE and CHAIN_PACKED
                applyDirectResolutionHorizontalChain(system, numMatchConstraints, widget);
            } else { // use the solver
                if (numMatchConstraints == 0) {
                    ConstraintWidget previous = null;
                    while (previous == null || (widget.mLeft.mTarget != null && widget.mLeft.mTarget.mOwner == previous)) {
                        // No need to call createObjectVariable here, as we already did that in the first traversal of our widgets
                        int leftMargin = widget.mLeft.getMargin();
                        int rightMargin = widget.mRight.getMargin();
                        SolverVariable left = widget.mLeft.mSolverVariable;
                        SolverVariable leftTarget = widget.mLeft.mTarget != null ? widget.mLeft.mTarget.mSolverVariable : null;
                        SolverVariable right = widget.mRight.mSolverVariable;
                        SolverVariable rightTarget = widget.mRight.mTarget != null ? widget.mRight.mTarget.mSolverVariable : null;
                        int margin = leftMargin;
                        if (previous != null) {
                            margin += previous.mRight.getMargin();
                        }
                        if (leftTarget != null) {
                            if ((widget == first && widget.mHorizontalChainStyle == CHAIN_SPREAD_INSIDE)
                                    || (chainPacked && widget != first)) {
                                system.addEquality(left, leftTarget, margin, 3);
                            } else {
                                system.addGreaterThan(left, leftTarget, margin, 1);
                            }
                        }
                        if (rightTarget != null) {
                            margin = rightMargin;
                            ConstraintAnchor nextLeft = widget.mRight.mTarget.mOwner.mLeft;
                            ConstraintWidget nextLeftTarget = nextLeft.mTarget != null ? nextLeft.mTarget.mOwner : null;
                            boolean isLast = true;
                            if (nextLeftTarget == widget) {
                                margin += nextLeft.getMargin();
                                isLast = false;
                            }
                            if (!chainPacked) {
                                if (isLast && first.mHorizontalChainStyle == CHAIN_SPREAD_INSIDE) {
                                    system.addEquality(right, rightTarget, -margin, 3);
                                } else {
                                    system.addLowerThan(right, rightTarget, -margin, 1);
                                    if (leftTarget != null) {
                                        system.addCentering(left, leftTarget, leftMargin, 0.5f,
                                                rightTarget, right, rightMargin, 2);
                                    }
                                }
                            }
                        }
                        previous = widget;
                        if (rightTarget != null) {
                            widget = widget.mRight.mTarget.mOwner;
                        } else {
                            break;
                        }
                    }
                    if (chainPacked) {
                        int leftMargin = first.mLeft.getMargin();
                        int rightMargin = previous.mRight.getMargin();
                        SolverVariable left = first.mLeft.mSolverVariable;
                        SolverVariable leftTarget = first.mLeft.mTarget != null ? first.mLeft.mTarget.mSolverVariable : null;
                        SolverVariable right = previous.mRight.mSolverVariable;
                        SolverVariable rightTarget = previous.mRight.mTarget != null ? previous.mRight.mTarget.mSolverVariable : null;
                        if (leftTarget != null && rightTarget != null) {
                            system.addCentering(left, leftTarget, leftMargin, first.mHorizontalBiasPercent,
                                    rightTarget, right, rightMargin, 2);
                        }
                    }
                } else {
                    ConstraintWidget previous = null;
                    float totalWeights = 0;
                    while (previous == null || (widget.mLeft.mTarget != null && widget.mLeft.mTarget.mOwner == previous)) {
                        if (widget.mHorizontalDimensionBehaviour != DimensionBehaviour.MATCH_CONSTRAINT) {
                            int margin = widget.mLeft.getMargin();
                            if (previous != null) {
                                margin += previous.mRight.getMargin();
                            }
                            system.addGreaterThan(widget.mLeft.mSolverVariable, widget.mLeft.mTarget.mSolverVariable, margin, 1);
                            margin = widget.mRight.getMargin();
                            if (widget.mRight.mTarget.mOwner.mLeft.mTarget != null && widget.mRight.mTarget.mOwner.mLeft.mTarget.mOwner == widget) {
                                margin += widget.mRight.mTarget.mOwner.mLeft.getMargin();
                            }
                            system.addLowerThan(widget.mRight.mSolverVariable, widget.mRight.mTarget.mSolverVariable, -margin, 1);
                        } else {
                            totalWeights += widget.mHorizontalWeight;
                        }
                        previous = widget;
                        widget = widget.mRight.mTarget.mOwner;
                    }
                    if (numMatchConstraints == 1) {
                        ConstraintWidget w = mMatchConstraintsChainedWidgets[0];
                        int leftMargin = w.mLeft.getMargin();
                        if (w.mLeft.mTarget != null) {
                            leftMargin += w.mLeft.mTarget.getMargin();
                        }
                        system.addEquality(w.mLeft.mSolverVariable, w.mLeft.mTarget.mSolverVariable, leftMargin, 1);
                        int rightMargin = w.mRight.getMargin();
                        if (w.mRight.mTarget != null) {
                            rightMargin += w.mRight.mTarget.getMargin();
                        }
                        system.addEquality(w.mRight.mSolverVariable, w.mRight.mTarget.mSolverVariable, -rightMargin, 1);
                    } else {
                        for (int j = 0; j < numMatchConstraints - 1; j++) {
                            ConstraintWidget current = mMatchConstraintsChainedWidgets[j];
                            ConstraintWidget nextWidget = mMatchConstraintsChainedWidgets[j + 1];
                            SolverVariable left = current.mLeft.mSolverVariable;
                            SolverVariable right = current.mRight.mSolverVariable;
                            SolverVariable nextLeft = nextWidget.mLeft.mSolverVariable;
                            SolverVariable nextRight = nextWidget.mRight.mSolverVariable;
                            int margin = current.mLeft.getMargin();
                            if (current.mLeft.mTarget != null && current.mLeft.mTarget.mOwner.mRight.mTarget != null
                                    && current.mLeft.mTarget.mOwner.mRight.mTarget.mOwner == current) {
                                margin += current.mLeft.mTarget.mOwner.mRight.getMargin();
                            }
                            system.addGreaterThan(left, current.mLeft.mTarget.mSolverVariable, margin, 1);
                            margin = current.mRight.getMargin();
                            if (current.mRight.mTarget != null && current.mRight.mTarget.mOwner.mLeft.mTarget != null
                                    && current.mRight.mTarget.mOwner.mLeft.mTarget.mOwner == current) {
                                margin += current.mRight.mTarget.mOwner.mLeft.getMargin();
                            }
                            system.addLowerThan(right, current.mRight.mTarget.mSolverVariable, -margin, 1);
                            if (j + 1 == numMatchConstraints - 1) {
                                // last element
                                margin = nextWidget.mLeft.getMargin();
                                if (nextWidget.mLeft.mTarget != null && nextWidget.mLeft.mTarget.mOwner.mRight.mTarget != null
                                        && nextWidget.mLeft.mTarget.mOwner.mRight.mTarget.mOwner == nextWidget) {
                                    margin += nextWidget.mLeft.mTarget.mOwner.mRight.getMargin();
                                }
                                system.addGreaterThan(nextLeft, nextWidget.mLeft.mTarget.mSolverVariable, margin, 1);
                                margin = nextWidget.mRight.getMargin();
                                if (nextWidget.mRight.mTarget != null && nextWidget.mRight.mTarget.mOwner.mLeft.mTarget != null
                                        && nextWidget.mRight.mTarget.mOwner.mLeft.mTarget.mOwner == nextWidget) {
                                    margin += nextWidget.mRight.mTarget.mOwner.mLeft.getMargin();
                                }
                                system.addLowerThan(nextRight, nextWidget.mRight.mTarget.mSolverVariable, -margin, 1);
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
     * Implements a direct resolution of chain constraints without using the solver
     * @param system
     * @param numMatchConstraints
     * @param widget
     */
    private void applyDirectResolutionHorizontalChain(LinearSystem system, int numMatchConstraints, ConstraintWidget widget) {
        ConstraintWidget firstWidget = widget;
        // loop through the widgets
        int widgetSize = 0;
        int firstPosition = 0; // parent
        ConstraintWidget previous = null;
        int count = 0;
        float totalWeights = 0;

        // Let's first get the size occupied by all widgets not 0dp
        while (widget != null) {
            count++;
            if (widget.mHorizontalDimensionBehaviour != DimensionBehaviour.MATCH_CONSTRAINT) {
                widgetSize += widget.getWidth();
                widgetSize += widget.mLeft.mTarget != null ? widget.mLeft.getMargin() : 0;
                widgetSize += widget.mRight.mTarget != null ? widget.mRight.getMargin() : 0;
            } else {
                totalWeights += widget.mHorizontalWeight;
            }
            previous = widget;
            widget = widget.mRight.mTarget != null ? widget.mRight.mTarget.mOwner : null;
            if (widget != null && (widget.mLeft.mTarget == null
                    || (widget.mLeft.mTarget != null && widget.mLeft.mTarget.mOwner != previous))) {
                // end of chain
                widget = null;
            }
        }

        // Then, let's get the position of the last element
        int lastPosition = 0;
        if (previous != null) {
            lastPosition = previous.mRight.mTarget != null ? previous.mRight.mTarget.mOwner.getX() : 0;
            if (previous.mRight.mTarget != null) {
                ConstraintWidget endTarget = previous.mRight.mTarget.mOwner;
                if (endTarget == this) {
                    lastPosition = getRight();
                }
            }
        }
        float total = lastPosition - firstPosition;
        float spreadSpace = total - widgetSize;
        float split = spreadSpace / (count + 1);
        widget = firstWidget;
        float currentPosition = 0;
        if (numMatchConstraints == 0) {
            currentPosition = split;
        } else {
            split = spreadSpace / numMatchConstraints;
        }

        // Now, let's iterate on the widgets in the chain and set their position and size
        while (widget != null) {
            int left = widget.mLeft.mTarget != null ? widget.mLeft.getMargin() : 0;
            int right = widget.mRight.mTarget != null ? widget.mRight.getMargin() : 0;
            currentPosition += left;
            system.addEquality(widget.mLeft.mSolverVariable, (int) currentPosition);
            if (widget.mHorizontalDimensionBehaviour == DimensionBehaviour.MATCH_CONSTRAINT) {
                if (totalWeights == 0) {
                    currentPosition += split - left - right;
                } else {
                    currentPosition += (spreadSpace * widget.mHorizontalWeight / totalWeights) - left - right;
                }
            } else {
                currentPosition += widget.getWidth();
            }
            system.addEquality(widget.mRight.mSolverVariable, (int) currentPosition);
            if (numMatchConstraints == 0) {
                currentPosition += split;
            }
            currentPosition += right;
            previous = widget;
            widget = widget.mRight.mTarget != null ? widget.mRight.mTarget.mOwner : null;
            if (widget != null && widget.mLeft.mTarget != null && widget.mLeft.mTarget.mOwner != previous) {
                // end of chain
                widget = null;
            }
            if (widget == this) {
                widget = null;
            }
        }
    }

    /**
     * Apply specific rules for dealing with vertical chains of widgets.
     * Vertical chains are defined as a list of widget linked together with bi-directional vertical connections
     * @param system
     */
    private void applyVerticalChain(LinearSystem system) {
        for (int i = 0; i < mVerticalChainsSize; i++) {
            ConstraintWidget first = mVerticalChainsArray[i];
            int numMatchConstraints = countMatchConstraintsChainedWidgets(mVerticalChainsArray[i], VERTICAL);
            // For now, only allow packed chains if all widgets are in fixed dimensions
            boolean chainPacked = (first.mVerticalChainStyle == ConstraintWidget.CHAIN_PACKED) && (numMatchConstraints == 0);
            ConstraintWidget widget = first;
            if (mVerticalDimensionBehaviour == DimensionBehaviour.WRAP_CONTENT) {
                ConstraintWidget previous = null;
                int strength = 1;
                while (previous == null || (widget.mTop.mTarget != null && widget.mTop.mTarget.mOwner == previous)) {
                    if (widget.mVerticalDimensionBehaviour == DimensionBehaviour.MATCH_CONSTRAINT) {
                        system.addGreaterThan(widget.mBottom.mSolverVariable, widget.mTop.mSolverVariable, widget.getHeight(), strength);
                    }
                    int margin = widget.mTop.getMargin();
                    if (previous != null) {
                        margin += previous.mBottom.getMargin();
                    }
                    system.addGreaterThan(widget.mTop.mSolverVariable, widget.mTop.mTarget.mSolverVariable, margin, strength);
                    previous = widget;
                    widget = widget.mBottom.mTarget.mOwner;
                }
                if (previous != null) {
                    system.addGreaterThan(previous.mBottom.mTarget.mSolverVariable, previous.mBottom.mSolverVariable, previous.mBottom.getMargin(), 0);
                }
                continue;
            }
            if (USE_DIRECT_CHAIN_RESOLUTION && widget.mVerticalChainFixedPosition && !chainPacked
                    && first.mVerticalChainStyle == CHAIN_SPREAD) {
                // TODO: implements direct resolution for CHAIN_SPREAD_INSIDE and CHAIN_PACKED
                applyDirectResolutionVerticalChain(system, numMatchConstraints, widget);
            } else { // use the solver
                if (numMatchConstraints == 0) {
                    ConstraintWidget previous = null;
                    while (previous == null || (widget.mTop.mTarget != null && widget.mTop.mTarget.mOwner == previous)) {
                        // No need to call createObjectVariable here, as we already did that in the first traversal of our widgets
                        int topMargin = widget.mTop.getMargin();
                        int bottomMargin = widget.mBottom.getMargin();
                        SolverVariable top = widget.mTop.mSolverVariable;
                        SolverVariable topTarget = widget.mTop.mTarget != null ? widget.mTop.mTarget.mSolverVariable : null;
                        SolverVariable bottom = widget.mBottom.mSolverVariable;
                        SolverVariable bottomTarget = widget.mBottom.mTarget != null ? widget.mBottom.mTarget.mSolverVariable : null;
                        int margin = topMargin;
                        if (previous != null) {
                            margin += previous.mBottom.getMargin();
                        }
                        if (topTarget != null) {
                            if ((widget == first && widget.mVerticalChainStyle == CHAIN_SPREAD_INSIDE)
                                || (chainPacked && widget != first)) {
                                system.addEquality(top, topTarget, margin, 3);
                            } else {
                                system.addGreaterThan(top, topTarget, margin, 1);
                            }
                        }
                        if (bottomTarget != null) {
                            margin = bottomMargin;
                            ConstraintAnchor nextTop = widget.mBottom.mTarget.mOwner.mTop;
                            ConstraintWidget nextTopTarget = nextTop.mTarget != null ? nextTop.mTarget.mOwner : null;
                            boolean isLast = true;
                            if (nextTopTarget == widget) {
                                margin += nextTop.getMargin();
                                isLast = false;
                            }
                            if (!chainPacked) {
                                if (isLast && first.mVerticalChainStyle == CHAIN_SPREAD_INSIDE) {
                                    system.addEquality(bottom, bottomTarget, -margin, 3);
                                } else {
                                    system.addLowerThan(bottom, bottomTarget, -margin, 1);
                                    if (topTarget != null) {
                                        system.addCentering(top, topTarget, topMargin, 0.5f,
                                                bottomTarget, bottom, bottomMargin, 2);
                                    }
                                }
                            }
                        }
                        previous = widget;
                        if (bottomTarget != null) {
                            widget = widget.mBottom.mTarget.mOwner;
                        } else {
                            break;
                        }
                    }
                    if (chainPacked) {
                        int topMargin = first.mTop.getMargin();
                        int bottomMargin = previous.mBottom.getMargin();
                        SolverVariable top = first.mTop.mSolverVariable;
                        SolverVariable topTarget = first.mTop.mTarget != null ? first.mTop.mTarget.mSolverVariable : null;
                        SolverVariable bottom = previous.mBottom.mSolverVariable;
                        SolverVariable bottomTarget = previous.mBottom.mTarget != null ? previous.mBottom.mTarget.mSolverVariable : null;
                        if (topTarget != null && bottomTarget != null) {
                            system.addCentering(top, topTarget, topMargin, first.mVerticalBiasPercent,
                                    bottomTarget, bottom, bottomMargin, 2);
                        }
                    }
                } else {
                    ConstraintWidget previous = null;
                    float totalWeights = 0;
                    while (previous == null || (widget.mTop.mTarget != null && widget.mTop.mTarget.mOwner == previous)) {
                        if (widget.mVerticalDimensionBehaviour != DimensionBehaviour.MATCH_CONSTRAINT) {
                            int margin = widget.mTop.getMargin();
                            if (previous != null) {
                                margin += previous.mBottom.getMargin();
                            }
                            system.addGreaterThan(widget.mTop.mSolverVariable, widget.mTop.mTarget.mSolverVariable, margin, 1);
                            margin = widget.mBottom.getMargin();
                            if (widget.mBottom.mTarget.mOwner.mTop.mTarget != null && widget.mBottom.mTarget.mOwner.mTop.mTarget.mOwner == widget) {
                                margin += widget.mBottom.mTarget.mOwner.mTop.getMargin();
                            }
                            system.addLowerThan(widget.mBottom.mSolverVariable, widget.mBottom.mTarget.mSolverVariable, -margin, 1);
                        } else {
                            totalWeights += widget.mVerticalWeight;
                        }
                        previous = widget;
                        widget = widget.mBottom.mTarget.mOwner;
                    }
                    if (numMatchConstraints == 1) {
                        ConstraintWidget w = mMatchConstraintsChainedWidgets[0];
                        int topMargin = w.mTop.getMargin();
                        if (w.mTop.mTarget != null) {
                            topMargin += w.mTop.mTarget.getMargin();
                        }
                        system.addEquality(w.mTop.mSolverVariable, w.mTop.mTarget.mSolverVariable, topMargin, 1);
                        int bottomMargin = w.mBottom.getMargin();
                        if (w.mBottom.mTarget != null) {
                            bottomMargin += w.mBottom.mTarget.getMargin();
                        }
                        system.addEquality(w.mBottom.mSolverVariable, w.mBottom.mTarget.mSolverVariable, - bottomMargin, 1);
                    } else {
                        for (int j = 0; j < numMatchConstraints - 1; j++) {
                            ConstraintWidget current = mMatchConstraintsChainedWidgets[j];
                            ConstraintWidget nextWidget = mMatchConstraintsChainedWidgets[j + 1];
                            SolverVariable top = current.mTop.mSolverVariable;
                            SolverVariable bottom = current.mBottom.mSolverVariable;
                            SolverVariable nextLeft = nextWidget.mTop.mSolverVariable;
                            SolverVariable nextRight = nextWidget.mBottom.mSolverVariable;
                            int margin = current.mTop.getMargin();
                            if (current.mTop.mTarget != null && current.mTop.mTarget.mOwner.mBottom.mTarget != null
                                    && current.mTop.mTarget.mOwner.mBottom.mTarget.mOwner == current) {
                                margin += current.mTop.mTarget.mOwner.mBottom.getMargin();
                            }
                            system.addGreaterThan(top, current.mTop.mTarget.mSolverVariable, margin, 1);
                            margin = current.mBottom.getMargin();
                            if (current.mBottom.mTarget != null && current.mBottom.mTarget.mOwner.mTop.mTarget != null
                                    && current.mBottom.mTarget.mOwner.mTop.mTarget.mOwner == current) {
                                margin += current.mBottom.mTarget.mOwner.mTop.getMargin();
                            }
                            system.addLowerThan(bottom, current.mBottom.mTarget.mSolverVariable, -margin, 1);
                            if (j + 1 == numMatchConstraints - 1) {
                                // last element
                                margin = nextWidget.mTop.getMargin();
                                if (nextWidget.mTop.mTarget != null && nextWidget.mTop.mTarget.mOwner.mBottom.mTarget != null
                                        && nextWidget.mTop.mTarget.mOwner.mBottom.mTarget.mOwner == nextWidget) {
                                    margin += nextWidget.mTop.mTarget.mOwner.mBottom.getMargin();
                                }
                                system.addGreaterThan(nextLeft, nextWidget.mTop.mTarget.mSolverVariable, margin, 1);
                                margin = nextWidget.mBottom.getMargin();
                                if (nextWidget.mBottom.mTarget != null && nextWidget.mBottom.mTarget.mOwner.mTop.mTarget != null
                                        && nextWidget.mBottom.mTarget.mOwner.mTop.mTarget.mOwner == nextWidget) {
                                    margin += nextWidget.mBottom.mTarget.mOwner.mTop.getMargin();
                                }
                                system.addLowerThan(nextRight, nextWidget.mBottom.mTarget.mSolverVariable, -margin, 1);
                            }

                            ArrayRow row = system.createRow();
                            row.createRowEqualDimension(current.mVerticalWeight,
                                    totalWeights, nextWidget.mVerticalWeight,
                                    top, current.mTop.getMargin(),
                                    bottom, current.mBottom.getMargin(),
                                    nextLeft, nextWidget.mTop.getMargin(),
                                    nextRight, nextWidget.mBottom.getMargin());
                            system.addConstraint(row);
                        }
                    }
                }
            }
        }
    }

    /**
     * Implements a direct resolution of chain constraints without using the solver
     * @param system
     * @param numMatchConstraints
     * @param widget
     */
    private void applyDirectResolutionVerticalChain(LinearSystem system, int numMatchConstraints, ConstraintWidget widget) {
        ConstraintWidget firstWidget = widget;
        // loop through the widgets
        int widgetSize = 0;
        int firstPosition = 0; // parent
        ConstraintWidget previous = null;
        int count = 0;
        float totalWeights = 0;

        // Let's first get the size occupied by all widgets not 0dp
        while (widget != null) {
            count++;
            if (widget.mVerticalDimensionBehaviour != DimensionBehaviour.MATCH_CONSTRAINT) {
                widgetSize += widget.getHeight();
                widgetSize += widget.mTop.mTarget != null ? widget.mTop.getMargin() : 0;
                widgetSize += widget.mBottom.mTarget != null ? widget.mBottom.getMargin() : 0;
            } else {
                totalWeights += widget.mVerticalWeight;
            }
            previous = widget;
            widget = widget.mBottom.mTarget != null ? widget.mBottom.mTarget.mOwner : null;
            if (widget != null && (widget.mTop.mTarget == null
                    || (widget.mTop.mTarget != null && widget.mTop.mTarget.mOwner != previous))) {
                // end of chain
                widget = null;
            }
        }

        // Then, let's get the position of the last element
        int lastPosition = 0;
        if (previous != null) {
            lastPosition = previous.mBottom.mTarget != null ? previous.mBottom.mTarget.mOwner.getX() : 0;
            if (previous.mBottom.mTarget != null) {
                ConstraintWidget endTarget = previous.mBottom.mTarget.mOwner;
                if (endTarget == this) {
                    lastPosition = getBottom();
                }
            }
        }
        float total = lastPosition - firstPosition;
        float spreadSpace = total - widgetSize;
        float split = spreadSpace / (count + 1);
        widget = firstWidget;
        float currentPosition = 0;
        if (numMatchConstraints == 0) {
            currentPosition = split;
        } else {
            split = spreadSpace / numMatchConstraints;
        }

        // Now, let's iterate on the widgets in the chain and set their position and size
        while (widget != null) {
            int top = widget.mTop.mTarget != null ? widget.mTop.getMargin() : 0;
            int bottom = widget.mBottom.mTarget != null ? widget.mBottom.getMargin() : 0;
            currentPosition += top;
            system.addEquality(widget.mTop.mSolverVariable, (int) currentPosition);
            if (widget.mVerticalDimensionBehaviour == DimensionBehaviour.MATCH_CONSTRAINT) {
                if (totalWeights == 0) {
                    currentPosition += split - top - bottom;
                } else {
                    currentPosition += (spreadSpace * widget.mVerticalWeight / totalWeights) - top - bottom;
                }
            } else {
                currentPosition += widget.getHeight();
            }
            system.addEquality(widget.mBottom.mSolverVariable, (int) currentPosition);
            if (numMatchConstraints == 0) {
                currentPosition += split;
            }
            currentPosition += bottom;
            previous = widget;
            widget = widget.mBottom.mTarget != null ? widget.mBottom.mTarget.mOwner : null;
            if (widget != null && widget.mTop.mTarget != null && widget.mTop.mTarget.mOwner != previous) {
                // end of chain
                widget = null;
            }
            if (widget == this) {
                widget = null;
            }
        }
    }

    /**
     * Update the frame of the layout and its children from the solver
     *
     * @param system the solver we get the values from.
     */
    public void updateChildrenFromSolver(LinearSystem system, int group) {
        updateFromSolver(system, group);
        final int count = mChildren.size();
        for (int i = 0; i < count; i++) {
            ConstraintWidget widget = mChildren.get(i);
            widget.updateFromSolver(system, group);
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
        int prew = getWidth();
        int preh = getHeight();
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
            updateChildrenFromSolver(mSystem, ConstraintAnchor.ANY_GROUP);
        } else {
            updateFromSolver(mSystem, ConstraintAnchor.ANY_GROUP);
        }

        if (mParent != null && USE_SNAPSHOT) {
            int width = getWidth();
            int height = getHeight();
            // Let's restore our state...
            mSnapshot.applyTo(this);
            setWidth(width + mPaddingLeft + mPaddingRight);
            setHeight(height + mPaddingTop + mPaddingBottom);
        } else {
            mX = prex;
            mY = prey;
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
    public void findWrapRecursive(ConstraintWidget widget) {
        int w = widget.getWrapWidth();

        int distToRight = w;
        int distToLeft = w;
        ConstraintWidget leftWidget = null;
        ConstraintWidget rightWidget = null;

        widget.mWrapVisited = true;
        if (!(widget.mRight.isConnected() || (widget.mLeft.isConnected()))) {
            distToLeft += widget.getX();
        } else {
            if (widget.mRight.mTarget != null) {
                rightWidget = widget.mRight.mTarget.getOwner();
                distToRight += widget.mRight.getMargin();
                if (!rightWidget.isRoot() && !rightWidget.mWrapVisited) {
                    findWrapRecursive(rightWidget);
                }
            }
            if (widget.mLeft.isConnected()) {
                leftWidget = widget.mLeft.mTarget.getOwner();
                distToLeft += widget.mLeft.getMargin();
                if (!leftWidget.isRoot() && !leftWidget.mWrapVisited) {
                    findWrapRecursive(leftWidget);
                }
            }

            if (widget.mRight.mTarget != null && !rightWidget.isRoot()) {
                if (widget.mRight.mTarget.mType == ConstraintAnchor.Type.RIGHT) {
                    distToRight += rightWidget.mDistToRight - rightWidget.getWrapWidth();
                } else if (widget.mRight.mTarget.getType() == ConstraintAnchor.Type.LEFT) {
                    distToRight += rightWidget.mDistToRight;
                }
            }

            if (widget.mLeft.mTarget != null && !leftWidget.isRoot()) {
                if (widget.mLeft.mTarget.getType() == ConstraintAnchor.Type.LEFT) {
                    distToLeft += leftWidget.mDistToLeft - leftWidget.getWrapWidth();
                } else if (widget.mLeft.mTarget.getType() == ConstraintAnchor.Type.RIGHT) {
                    distToLeft += leftWidget.mDistToLeft;
                }
            }
        }
        widget.mDistToLeft = distToLeft;
        widget.mDistToRight = distToRight;

        // VERTICAL
        int h = widget.getWrapHeight();
        int distToTop = h;
        int distToBottom = h;
        ConstraintWidget topWidget = null;
        if (!(widget.mBaseline.mTarget != null || widget.mTop.mTarget != null || widget.mBottom.mTarget != null)) {
            distToTop += widget.getY();
        } else {
            if (widget.mBaseline.isConnected()) {
                ConstraintWidget baseLineWidget = widget.mBaseline.mTarget.getOwner();
                if (!baseLineWidget.mWrapVisited) {
                    findWrapRecursive(baseLineWidget);
                }
                if (baseLineWidget.mDistToBottom > distToBottom) {
                    distToBottom = baseLineWidget.mDistToBottom;
                }
                if (baseLineWidget.mDistToTop > distToTop) {
                    distToTop = baseLineWidget.mDistToTop;
                }
                widget.mDistToTop = distToTop;
                widget.mDistToBottom = distToBottom;
                return; // if baseline connected no need to look at top or bottom
            }

            if (widget.mTop.isConnected()) {
                topWidget = widget.mTop.mTarget.getOwner();
                distToTop += widget.mTop.getMargin();
                if (!topWidget.isRoot() && !topWidget.mWrapVisited) {
                    findWrapRecursive(topWidget);
                }
            }

            ConstraintWidget bottomWidget = null;
            if (widget.mBottom.isConnected()) {
                bottomWidget = widget.mBottom.mTarget.getOwner();
                distToBottom += widget.mBottom.getMargin();
                if (!bottomWidget.isRoot() && !bottomWidget.mWrapVisited) {
                    findWrapRecursive(bottomWidget);
                }
            }


            // TODO add center connection logic

            if (widget.mTop.mTarget != null && !topWidget.isRoot()) {
                if (widget.mTop.mTarget.getType() == ConstraintAnchor.Type.TOP) {
                    distToTop += topWidget.mDistToTop - topWidget.getWrapHeight();
                } else if (widget.mTop.mTarget.getType() == ConstraintAnchor.Type.BOTTOM) {
                    distToTop += topWidget.mDistToTop;
                }
            }
            if (widget.mBottom.mTarget != null && !bottomWidget.isRoot()) {
                if (widget.mBottom.mTarget.getType() == ConstraintAnchor.Type.BOTTOM) {
                    distToBottom += bottomWidget.mDistToBottom - bottomWidget.getWrapHeight();
                } else if (widget.mBottom.mTarget.getType() == ConstraintAnchor.Type.TOP) {
                    distToBottom += bottomWidget.mDistToBottom;
                }
            }
        }
        widget.mDistToTop = distToTop;
        widget.mDistToBottom = distToBottom;
    }

    /**
     * Resolve simple dependency directly, without using the equation solver
     * @param system
     * @param widget
     */
    private void checkHorizontalSimpleDependency(LinearSystem system, ConstraintWidget widget) {
        if (widget.mHorizontalDimensionBehaviour == DimensionBehaviour.MATCH_CONSTRAINT) {
            widget.mHorizontalResolution = SOLVER;
            return;
        }
        if (widget.mLeft.mTarget != null && widget.mRight.mTarget != null) {
            if (widget.mLeft.mTarget.mOwner == this && widget.mRight.mTarget.mOwner == this) {
                int left = 0;
                int right = 0;
                int leftMargin = widget.mLeft.getMargin();
                int rightMargin = widget.mRight.getMargin();
                if (mHorizontalDimensionBehaviour == DimensionBehaviour.MATCH_CONSTRAINT) {
                    left = leftMargin;
                    right = getWidth() - rightMargin;
                } else {
                    int w = widget.getWidth();
                    int dim = getWidth() - leftMargin - rightMargin - w;
                    left = leftMargin + (int) (dim * widget.mHorizontalBiasPercent);
                    right = left + widget.getWidth();
                }
                widget.mLeft.mSolverVariable = system.createObjectVariable(widget.mLeft);
                widget.mRight.mSolverVariable = system.createObjectVariable(widget.mRight);
                system.addEquality(widget.mLeft.mSolverVariable, left);
                system.addEquality(widget.mRight.mSolverVariable, right);
                widget.mHorizontalResolution = DIRECT;
                widget.setHorizontalDimension(left, right);
                return;
            }
            widget.mHorizontalResolution = SOLVER;
            return;
        }
        if (widget.mLeft.mTarget != null
                && widget.mLeft.mTarget.mOwner == this) {
            int left = widget.mLeft.getMargin();
            int right = left + widget.getWidth();
            widget.mLeft.mSolverVariable = system.createObjectVariable(widget.mLeft);
            widget.mRight.mSolverVariable = system.createObjectVariable(widget.mRight);
            system.addEquality(widget.mLeft.mSolverVariable, left);
            system.addEquality(widget.mRight.mSolverVariable, right);
            widget.mHorizontalResolution = ConstraintWidget.DIRECT;
            widget.setHorizontalDimension(left, right);
        } else
        if (widget.mRight.mTarget != null
                && widget.mRight.mTarget.mOwner == this) {
            widget.mLeft.mSolverVariable = system.createObjectVariable(widget.mLeft);
            widget.mRight.mSolverVariable = system.createObjectVariable(widget.mRight);
            int right = getWidth() - widget.mRight.getMargin();
            int left = right - widget.getWidth();
            system.addEquality(widget.mLeft.mSolverVariable, left);
            system.addEquality(widget.mRight.mSolverVariable, right);
            widget.mHorizontalResolution = ConstraintWidget.DIRECT;
            widget.setHorizontalDimension(left, right);
        } else
        if (widget.mLeft.mTarget != null && widget.mLeft.mTarget.mOwner.mHorizontalResolution == DIRECT) {
            SolverVariable target = widget.mLeft.mTarget.mSolverVariable;
            widget.mLeft.mSolverVariable = system.createObjectVariable(widget.mLeft);
            widget.mRight.mSolverVariable = system.createObjectVariable(widget.mRight);
            int left = (int) (target.computedValue + widget.mLeft.getMargin());
            int right = left + widget.getWidth();
            system.addEquality(widget.mLeft.mSolverVariable, left);
            system.addEquality(widget.mRight.mSolverVariable, right);
            widget.mHorizontalResolution = ConstraintWidget.DIRECT;
            widget.setHorizontalDimension(left, right);
        } else
        if (widget.mRight.mTarget != null && widget.mRight.mTarget.mOwner.mHorizontalResolution == DIRECT) {
            SolverVariable target = widget.mRight.mTarget.mSolverVariable;
            widget.mLeft.mSolverVariable = system.createObjectVariable(widget.mLeft);
            widget.mRight.mSolverVariable = system.createObjectVariable(widget.mRight);
            int right = (int) (target.computedValue - widget.mRight.getMargin());
            int left = right - widget.getWidth();
            system.addEquality(widget.mLeft.mSolverVariable, left);
            system.addEquality(widget.mRight.mSolverVariable, right);
            widget.mHorizontalResolution = ConstraintWidget.DIRECT;
            widget.setHorizontalDimension(left, right);
        } else {
            boolean hasLeft = widget.mLeft.mTarget != null;
            boolean hasRight = widget.mRight.mTarget != null;
            if (!hasLeft && !hasRight && !(widget instanceof Guideline)) {
                widget.mLeft.mSolverVariable = system.createObjectVariable(widget.mLeft);
                widget.mRight.mSolverVariable = system.createObjectVariable(widget.mRight);
                int left = widget.getX();
                int right = left + widget.getWidth();
                system.addEquality(widget.mLeft.mSolverVariable, left);
                system.addEquality(widget.mRight.mSolverVariable, right);
                widget.mHorizontalResolution = ConstraintWidget.DIRECT;
            }
        }
    }

    /**
     * Resolve simple dependency directly, without using the equation solver
     * @param system
     * @param widget
     */
    private void checkVerticalSimpleDependency(LinearSystem system, ConstraintWidget widget) {
        if (widget.mVerticalDimensionBehaviour == DimensionBehaviour.MATCH_CONSTRAINT) {
            widget.mVerticalResolution = SOLVER;
            return;
        }
        if (widget.mTop.mTarget != null && widget.mBottom.mTarget != null) {
            if (widget.mTop.mTarget.mOwner == this && widget.mBottom.mTarget.mOwner == this) {
                int top = 0;
                int bottom = 0;
                int topMargin = widget.mTop.getMargin();
                int bottomMargin = widget.mBottom.getMargin();
                if (mVerticalDimensionBehaviour == DimensionBehaviour.MATCH_CONSTRAINT) {
                    top = topMargin;
                    bottom = top + widget.getHeight();
                } else {
                    int h = widget.getHeight();
                    int dim = getHeight() - topMargin - bottomMargin - h;
                    top = topMargin + (int)(dim * widget.mVerticalBiasPercent);
                    bottom = top + widget.getHeight();
                }
                widget.mTop.mSolverVariable = system.createObjectVariable(widget.mTop);
                widget.mBottom.mSolverVariable = system.createObjectVariable(widget.mBottom);
                system.addEquality(widget.mTop.mSolverVariable, top);
                system.addEquality(widget.mBottom.mSolverVariable, bottom);
                if (widget.mBaselineDistance > 0) {
                    widget.mBaseline.mSolverVariable = system.createObjectVariable(widget.mBaseline);
                    system.addEquality(widget.mBaseline.mSolverVariable, top + widget.mBaselineDistance);
                }
                widget.mVerticalResolution = DIRECT;
                widget.setVerticalDimension(top, bottom);
                return;
            }
            widget.mVerticalResolution = SOLVER;
            return;
        }
        if (widget.mTop.mTarget != null
                && widget.mTop.mTarget.mOwner == this) {
            int top = widget.mTop.getMargin();
            int bottom = top + widget.getHeight();
            widget.mTop.mSolverVariable = system.createObjectVariable(widget.mTop);
            widget.mBottom.mSolverVariable = system.createObjectVariable(widget.mBottom);
            system.addEquality(widget.mTop.mSolverVariable, top);
            system.addEquality(widget.mBottom.mSolverVariable, bottom);
            if (widget.mBaselineDistance > 0) {
                widget.mBaseline.mSolverVariable = system.createObjectVariable(widget.mBaseline);
                system.addEquality(widget.mBaseline.mSolverVariable, top + widget.mBaselineDistance);
            }
            widget.mVerticalResolution = ConstraintWidget.DIRECT;
            widget.setVerticalDimension(top, bottom);
        } else
        if (widget.mBottom.mTarget != null
                && widget.mBottom.mTarget.mOwner == this) {
            widget.mTop.mSolverVariable = system.createObjectVariable(widget.mTop);
            widget.mBottom.mSolverVariable = system.createObjectVariable(widget.mBottom);
            int bottom = getHeight() - widget.mBottom.getMargin();
            int top = bottom - widget.getHeight();
            system.addEquality(widget.mTop.mSolverVariable, top);
            system.addEquality(widget.mBottom.mSolverVariable, bottom);
            if (widget.mBaselineDistance > 0) {
                widget.mBaseline.mSolverVariable = system.createObjectVariable(widget.mBaseline);
                system.addEquality(widget.mBaseline.mSolverVariable, top + widget.mBaselineDistance);
            }
            widget.mVerticalResolution = ConstraintWidget.DIRECT;
            widget.setVerticalDimension(top, bottom);
        } else
        if (widget.mTop.mTarget != null && widget.mTop.mTarget.mOwner.mVerticalResolution == DIRECT) {
            SolverVariable target = widget.mTop.mTarget.mSolverVariable;
            widget.mTop.mSolverVariable = system.createObjectVariable(widget.mTop);
            widget.mBottom.mSolverVariable = system.createObjectVariable(widget.mBottom);
            int top = (int) (target.computedValue + widget.mTop.getMargin());
            int bottom = top + widget.getHeight();
            system.addEquality(widget.mTop.mSolverVariable, top);
            system.addEquality(widget.mBottom.mSolverVariable, bottom);
            if (widget.mBaselineDistance > 0) {
                widget.mBaseline.mSolverVariable = system.createObjectVariable(widget.mBaseline);
                system.addEquality(widget.mBaseline.mSolverVariable, top + widget.mBaselineDistance);
            }
            widget.mVerticalResolution = ConstraintWidget.DIRECT;
            widget.setVerticalDimension(top, bottom);
        } else
        if (widget.mBottom.mTarget != null && widget.mBottom.mTarget.mOwner.mVerticalResolution == DIRECT) {
            SolverVariable target = widget.mBottom.mTarget.mSolverVariable;
            widget.mTop.mSolverVariable = system.createObjectVariable(widget.mTop);
            widget.mBottom.mSolverVariable = system.createObjectVariable(widget.mBottom);
            int bottom = (int) (target.computedValue - widget.mBottom.getMargin());
            int top = bottom - widget.getHeight();
            system.addEquality(widget.mTop.mSolverVariable, top);
            system.addEquality(widget.mBottom.mSolverVariable, bottom);
            if (widget.mBaselineDistance > 0) {
                widget.mBaseline.mSolverVariable = system.createObjectVariable(widget.mBaseline);
                system.addEquality(widget.mBaseline.mSolverVariable, top + widget.mBaselineDistance);
            }
            widget.mVerticalResolution = ConstraintWidget.DIRECT;
            widget.setVerticalDimension(top, bottom);
        } else
        if (widget.mBaseline.mTarget != null && widget.mBaseline.mTarget.mOwner.mVerticalResolution == DIRECT) {
            SolverVariable target = widget.mBaseline.mTarget.mSolverVariable;
            widget.mTop.mSolverVariable = system.createObjectVariable(widget.mTop);
            widget.mBottom.mSolverVariable = system.createObjectVariable(widget.mBottom);
            int top = (int) (target.computedValue - widget.mBaselineDistance);
            int bottom = top + widget.getHeight();
            system.addEquality(widget.mTop.mSolverVariable, top);
            system.addEquality(widget.mBottom.mSolverVariable, bottom);
            widget.mBaseline.mSolverVariable = system.createObjectVariable(widget.mBaseline);
            system.addEquality(widget.mBaseline.mSolverVariable, top + widget.mBaselineDistance);
            widget.mVerticalResolution = ConstraintWidget.DIRECT;
            widget.setVerticalDimension(top, bottom);
        } else {
            boolean hasBaseline = widget.mBaseline.mTarget != null;
            boolean hasTop = widget.mTop.mTarget != null;
            boolean hasBottom = widget.mBottom.mTarget != null;
            if (!hasBaseline && !hasTop && !hasBottom && !(widget instanceof Guideline)) {
                widget.mTop.mSolverVariable = system.createObjectVariable(widget.mTop);
                widget.mBottom.mSolverVariable = system.createObjectVariable(widget.mBottom);
                int top = widget.getY();
                int bottom = top + widget.getHeight();
                system.addEquality(widget.mTop.mSolverVariable, top);
                system.addEquality(widget.mBottom.mSolverVariable, bottom);
                if (widget.mBaselineDistance > 0) {
                    widget.mBaseline.mSolverVariable = system.createObjectVariable(widget.mBaseline);
                    system.addEquality(widget.mBaseline.mSolverVariable, top + widget.mBaselineDistance);
                }
                widget.mVerticalResolution = ConstraintWidget.DIRECT;
            }
        }
    }

    /**
     * calculates the wrapContent size.
     *
     * @param children
     */
    public void findWrapSize(ArrayList<ConstraintWidget> children) {
        int maxTopDist = 0;
        int maxLeftDist = 0;
        int maxRightDist = 0;
        int maxBottomDist = 0;

        int maxConnectWidth = 0;
        int maxConnectHeight = 0;
        final int size = children.size();
        for (int j = 0; j < size; j++) {
            ConstraintWidget widget = children.get(j);
            if (widget.isRoot()) {
                continue;
            }
            if (!widget.mWrapVisited) {
                findWrapRecursive(widget);
            }
            int connectWidth = widget.mDistToLeft + widget.mDistToRight - widget.getWrapWidth();
            int connectHeight = widget.mDistToTop + widget.mDistToBottom - widget.getWrapHeight();
            maxLeftDist = Math.max(maxLeftDist, widget.mDistToLeft);
            maxRightDist = Math.max(maxRightDist, widget.mDistToRight);
            maxBottomDist = Math.max(maxBottomDist, widget.mDistToBottom);
            maxTopDist = Math.max(maxTopDist, widget.mDistToTop);
            maxConnectWidth = Math.max(maxConnectWidth, connectWidth);
            maxConnectHeight = Math.max(maxConnectHeight, connectHeight);
        }
        int max = Math.max(maxLeftDist, maxRightDist);
        mWrapWidth = Math.max(max, maxConnectWidth);
        max = Math.max(maxTopDist, maxBottomDist);
        mWrapHeight = Math.max(max, maxConnectHeight);

        for (int j = 0; j < size; j++) {
            children.get(j).mWrapVisited = false;
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
     * Returns true if the widget is animating
     *
     * @return
     */
    @Override
    public boolean isAnimating() {
        if (super.isAnimating()) {
            return true;
        }
        for (int i = 0, mChildrenSize = mChildren.size(); i < mChildrenSize; i++) {
            final ConstraintWidget widget = mChildren.get(i);
            if (widget.isAnimating()) {
                return true;
            }
        }
        return false;
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
    private int countMatchConstraintsChainedWidgets(ConstraintWidget widget, int direction) {
        int count = 0;
        if (direction == HORIZONTAL) {
            boolean fixedPosition = true;
            ConstraintWidget first = widget;
            if (widget.mLeft.mTarget != null && widget.mLeft.mTarget.mOwner != this) {
                fixedPosition = false;
            }

            while (widget.mRight.mTarget != null) {
                if (widget.mHorizontalDimensionBehaviour == DimensionBehaviour.MATCH_CONSTRAINT) {
                    if (count + 1 >= mMatchConstraintsChainedWidgets.length) {
                        mMatchConstraintsChainedWidgets = Arrays.copyOf(mMatchConstraintsChainedWidgets, mMatchConstraintsChainedWidgets.length * 2);
                    }
                    mMatchConstraintsChainedWidgets[count++] = widget;
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

                widget =  widget.mRight.mTarget.mOwner;
            }
            if (widget.mRight.mTarget != null && widget.mRight.mTarget.mOwner != this) {
                fixedPosition = false;
            }
            // keep track of the endpoints -- if both are fixed (for now, only look if they point to the parent)
            // we can optimize the resolution without passing by the solver
            first.mHorizontalChainFixedPosition = fixedPosition;
        } else {
            boolean fixedPosition = true;
            ConstraintWidget first = widget;
            if (widget.mTop.mTarget != null && widget.mTop.mTarget.mOwner != this) {
                fixedPosition = false;
            }
            while (widget.mBottom.mTarget != null) {
                if (widget.mVerticalDimensionBehaviour == DimensionBehaviour.MATCH_CONSTRAINT) {
                    if (count + 1 >= mMatchConstraintsChainedWidgets.length) {
                        mMatchConstraintsChainedWidgets = Arrays.copyOf(mMatchConstraintsChainedWidgets, mMatchConstraintsChainedWidgets.length * 2);
                    }
                    mMatchConstraintsChainedWidgets[count++] = widget;
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
            }
            if (widget.mBottom.mTarget != null && widget.mBottom.mTarget.mOwner != this) {
                fixedPosition = false;
            }
            // keep track of the endpoints -- if both are fixed (for now, only look if they point to the parent)
            // we can optimize the resolution without passing by the solver
            first.mVerticalChainFixedPosition = fixedPosition;
        }
        return count;
    }

}
