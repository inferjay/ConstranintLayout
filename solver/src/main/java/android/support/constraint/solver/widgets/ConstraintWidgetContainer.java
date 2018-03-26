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

import android.support.constraint.solver.LinearSystem;
import android.support.constraint.solver.Metrics;

import java.util.ArrayList;
import java.util.Arrays;

import static android.support.constraint.solver.LinearSystem.FULL_DEBUG;

/**
 * A container of ConstraintWidget that can layout its children
 */
public class ConstraintWidgetContainer extends WidgetContainer {

    private static final boolean USE_SNAPSHOT = true;
    private static final int MAX_ITERATIONS = 8;

    private static final boolean DEBUG = FULL_DEBUG;
    private static final boolean DEBUG_LAYOUT = false;
    static final boolean DEBUG_GRAPH = false;

    private boolean mIsRtl = false;

    public void fillMetrics(Metrics metrics) {
        mSystem.fillMetrics(metrics);
    }

    protected LinearSystem mSystem = new LinearSystem();

    private Snapshot mSnapshot;

    int mPaddingLeft;
    int mPaddingTop;
    int mPaddingRight;
    int mPaddingBottom;

    int mHorizontalChainsSize = 0;
    int mVerticalChainsSize = 0;

    ConstraintWidget[] mVerticalChainsArray = new ConstraintWidget[4];
    ConstraintWidget[] mHorizontalChainsArray = new ConstraintWidget[4];

    private int mOptimizationLevel = Optimizer.OPTIMIZATION_STANDARD;

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
     * Returns the current optimization level
     *
     * @return
     */
    public int getOptimizationLevel() {
        return mOptimizationLevel;
    }

    /**
     * Returns true if the given feature should be optimized
     * @param feature
     * @return
     */
    public boolean optimizeFor(int feature) {
        return (mOptimizationLevel & feature) == feature;
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

    int mDebugSolverPassCount = 0;

    /**
     * Add this widget to the solver
     *
     * @param system the solver we want to add the widget to
     */
    public boolean addChildrenToSolver(LinearSystem system) {
        if (DEBUG) {
            System.out.println("\n#######################################");
            System.out.println("##    ADD CHILDREN TO SOLVER  (" + mDebugSolverPassCount + ") ##");
            System.out.println("#######################################\n");
            mDebugSolverPassCount++;
        }
        addToSolver(system);
        final int count = mChildren.size();

        for (int i = 0; i < count; i++) {
            ConstraintWidget widget = mChildren.get(i);
            if (widget instanceof ConstraintWidgetContainer) {
                DimensionBehaviour horizontalBehaviour = widget.mListDimensionBehaviors[DIMENSION_HORIZONTAL];
                DimensionBehaviour verticalBehaviour = widget.mListDimensionBehaviors[DIMENSION_VERTICAL];
                if (horizontalBehaviour == DimensionBehaviour.WRAP_CONTENT) {
                    widget.setHorizontalDimensionBehaviour(DimensionBehaviour.FIXED);
                }
                if (verticalBehaviour == DimensionBehaviour.WRAP_CONTENT) {
                    widget.setVerticalDimensionBehaviour(DimensionBehaviour.FIXED);
                }
                widget.addToSolver(system);
                if (horizontalBehaviour == DimensionBehaviour.WRAP_CONTENT) {
                    widget.setHorizontalDimensionBehaviour(horizontalBehaviour);
                }
                if (verticalBehaviour == DimensionBehaviour.WRAP_CONTENT) {
                    widget.setVerticalDimensionBehaviour(verticalBehaviour);
                }
            } else {
                Optimizer.checkMatchParent(this, system, widget);
                widget.addToSolver(system);
            }
        }

        if (mHorizontalChainsSize > 0) {
            Chain.applyChainConstraints(this, system, HORIZONTAL);
        }
        if (mVerticalChainsSize > 0) {
            Chain.applyChainConstraints(this, system, VERTICAL);
        }
        return true;
    }

    /**
     * Update the frame of the layout and its children from the solver
     *
     * @param system the solver we get the values from.
     */
    public void updateChildrenFromSolver(LinearSystem system, boolean flags[]) {
        flags[Optimizer.FLAG_RECOMPUTE_BOUNDS] = false;
        updateFromSolver(system);
        final int count = mChildren.size();
        for (int i = 0; i < count; i++) {
            ConstraintWidget widget = mChildren.get(i);
            widget.updateFromSolver(system);
            if (widget.mListDimensionBehaviors[DIMENSION_HORIZONTAL] == DimensionBehaviour.MATCH_CONSTRAINT
                && widget.getWidth() < widget.getWrapWidth()) {
                flags[Optimizer.FLAG_RECOMPUTE_BOUNDS] = true;
            }
            if (widget.mListDimensionBehaviors[DIMENSION_VERTICAL] == DimensionBehaviour.MATCH_CONSTRAINT
                    && widget.getHeight() < widget.getWrapHeight()) {
                flags[Optimizer.FLAG_RECOMPUTE_BOUNDS] = true;
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
     * Set the rtl status. This has implications for Chains.
     * @param isRtl true if we are in RTL.
     */
    public void setRtl(boolean isRtl) {
        mIsRtl = isRtl;
    }

    /**
     * Returns the rtl status.
     * @return true if in RTL, false otherwise.
     */
    public boolean isRtl() {
        return mIsRtl;
    }

    /*-----------------------------------------------------------------------*/
    // Overloaded methods from ConstraintWidget
    /*-----------------------------------------------------------------------*/

    /**
     * Graph analysis
     */
    @Override
    public void analyze() {
        super.analyze();
        final int count = mChildren.size();
        for (int i = 0; i < count; i++) {
            mChildren.get(i).analyze();
        }
    }

    /**
     * Layout the tree of widgets
     */
    @Override
    public void layout() {
        if (DEBUG) {
            System.out.println("\n#####################################");
            System.out.println("##           LAYOUT PASS           ##");
            System.out.println("#####################################\n");
            mDebugSolverPassCount = 0;
        }
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

        if (mOptimizationLevel != Optimizer.OPTIMIZATION_NONE) {
            if (DEBUG_GRAPH) {
                System.out.println("### Graph resolution... " + mWidth + " x " + mHeight + " ###");
            }
            final int count = mChildren.size();
            resetResolutionNodes();
            for (int i = 0; i < count; i++) {
                mChildren.get(i).resetResolutionNodes();
            }
            if (DEBUG_GRAPH) {
                System.out.println("### Update Constraints Graph ###");
                setDebugName("Root");
            }

            analyze();

            if (DEBUG_GRAPH) {
                for (int i = 0; i < mChildren.size(); i++) {
                    ConstraintWidget widget = mChildren.get(i);
                    System.out.println("(pre) child [" + i + "/" + mChildren.size() + "] - " + widget.mLeft.getResolutionNode()
                            + ", " + widget.mTop.getResolutionNode()
                            + ", " + widget.mRight.getResolutionNode()
                            + ", " + widget.mBottom.getResolutionNode());
                }
            }
            ResolutionNode leftNode = getAnchor(ConstraintAnchor.Type.LEFT).getResolutionNode();
            ResolutionNode topNode = getAnchor(ConstraintAnchor.Type.TOP).getResolutionNode();

            if (DEBUG_GRAPH) {
                System.out.println("### RESOLUTION ###");
            }

            leftNode.resolve(null, 0);
            topNode.resolve(null, 0);

            if (DEBUG_GRAPH) {
                for (int i = 0; i < mChildren.size(); i++) {
                    ConstraintWidget widget = mChildren.get(i);
                    System.out.println("child [" + i + "/" + mChildren.size() + "] - " + widget.mLeft.getResolutionNode()
                            + ", " + widget.mTop.getResolutionNode()
                            + ", " + widget.mRight.getResolutionNode()
                            + ", " + widget.mBottom.getResolutionNode());
                }
            }
            mSystem.graphOptimizer = true;
        } else {
            mSystem.graphOptimizer = false;
        }

        boolean wrap_override = false;
        DimensionBehaviour originalVerticalDimensionBehaviour = mListDimensionBehaviors[DIMENSION_VERTICAL];
        DimensionBehaviour originalHorizontalDimensionBehaviour = mListDimensionBehaviors[DIMENSION_HORIZONTAL];

        if (DEBUG_LAYOUT) {
            System.out.println("layout with prew: " + prew + " (" + mListDimensionBehaviors[DIMENSION_HORIZONTAL]
                + ") preh: " + preh + " (" + mListDimensionBehaviors[DIMENSION_VERTICAL] + ")");
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
                needsSolving = addChildrenToSolver(mSystem);
                if (needsSolving) {
                    mSystem.minimize();
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("EXCEPTION : " + e);
            }
            if (needsSolving) {
                updateChildrenFromSolver(mSystem, Optimizer.flags);
            } else {
                updateFromSolver(mSystem);
                for (int i = 0; i < count; i++) {
                    ConstraintWidget widget = mChildren.get(i);
                    if (widget.mListDimensionBehaviors[DIMENSION_HORIZONTAL] == DimensionBehaviour.MATCH_CONSTRAINT
                        && widget.getWidth() < widget.getWrapWidth()) {
                        Optimizer.flags[Optimizer.FLAG_RECOMPUTE_BOUNDS] = true;
                        break;
                    }
                    if (widget.mListDimensionBehaviors[DIMENSION_VERTICAL] == DimensionBehaviour.MATCH_CONSTRAINT
                        && widget.getHeight() < widget.getWrapHeight()) {
                        Optimizer.flags[Optimizer.FLAG_RECOMPUTE_BOUNDS] = true;
                        break;
                    }
                }
            }
            needsSolving = false;

            if (countSolve < MAX_ITERATIONS && Optimizer.flags[Optimizer.FLAG_RECOMPUTE_BOUNDS]) {
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
                        mListDimensionBehaviors[DIMENSION_HORIZONTAL] = DimensionBehaviour.WRAP_CONTENT; // force using the solver
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
                        mListDimensionBehaviors[DIMENSION_VERTICAL] = DimensionBehaviour.WRAP_CONTENT; // force using the solver
                        wrap_override = true;
                        needsSolving = true;
                    }
                }
            }

            if (true) {
                int width = Math.max(mMinWidth, getWidth());
                if (width > getWidth()) {
                    if (DEBUG_LAYOUT) {
                        System.out.println("layout override 2, width from " + getWidth() + " vs " + width);
                    }
                    setWidth(width);
                    mListDimensionBehaviors[DIMENSION_HORIZONTAL] = DimensionBehaviour.FIXED;
                    wrap_override = true;
                    needsSolving = true;
                }
                int height = Math.max(mMinHeight, getHeight());
                if (height > getHeight()) {
                    if (DEBUG_LAYOUT) {
                        System.out.println("layout override 2, height from " + getHeight() + " vs " + height);
                    }
                    setHeight(height);
                    mListDimensionBehaviors[DIMENSION_VERTICAL] = DimensionBehaviour.FIXED;
                    wrap_override = true;
                    needsSolving = true;
                }

                if (!wrap_override) {
                    if (mListDimensionBehaviors[DIMENSION_HORIZONTAL] == DimensionBehaviour.WRAP_CONTENT && prew > 0) {
                        if (getWidth() > prew) {
                            if (DEBUG_LAYOUT) {
                                System.out.println("layout override 3, width from " + getWidth() + " vs " + prew);
                            }
                            mWidthMeasuredTooSmall = true;
                            wrap_override = true;
                            mListDimensionBehaviors[DIMENSION_HORIZONTAL] = DimensionBehaviour.FIXED;
                            setWidth(prew);
                            needsSolving = true;
                        }
                    }
                    if (mListDimensionBehaviors[DIMENSION_VERTICAL] == DimensionBehaviour.WRAP_CONTENT && preh > 0) {
                        if (getHeight() > preh) {
                            if (DEBUG_LAYOUT) {
                                System.out.println("layout override 3, height from " + getHeight() + " vs " + preh);
                            }
                            mHeightMeasuredTooSmall = true;
                            wrap_override = true;
                            mListDimensionBehaviors[DIMENSION_VERTICAL] = DimensionBehaviour.FIXED;
                            setHeight(preh);
                            needsSolving = true;
                        }
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
            mListDimensionBehaviors[DIMENSION_HORIZONTAL] = originalHorizontalDimensionBehaviour;
            mListDimensionBehaviors[DIMENSION_VERTICAL] = originalVerticalDimensionBehaviour;
        }

        if (DEBUG_GRAPH) {
            for (int i = 0; i < mChildren.size(); i++) {
                ConstraintWidget widget = mChildren.get(i);
                System.out.println("final child [" + i + "/" + mChildren.size() + "] - " + widget.mLeft.getResolutionNode()
                        + ", " + widget.mTop.getResolutionNode()
                        + ", " + widget.mRight.getResolutionNode()
                        + ", " + widget.mBottom.getResolutionNode());
            }
        }

        resetSolverVariables(mSystem.getCache());
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

    /*-----------------------------------------------------------------------*/
    // Chains
    /*-----------------------------------------------------------------------*/

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

}
