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

import java.util.ArrayList;
import java.util.Arrays;

import static android.support.constraint.solver.LinearSystem.FULL_DEBUG;
import static android.support.constraint.solver.widgets.ConstraintWidget.DimensionBehaviour.WRAP_CONTENT;

/**
 * A container of ConstraintWidget that can layout its children
 */
public class ConstraintWidgetContainer extends WidgetContainer {

    private static final boolean USE_THREAD = false;
    private static final boolean USE_SNAPSHOT = true;
    private static final int MAX_ITERATIONS = 8;

    private static final boolean DEBUG = FULL_DEBUG;
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

    int mHorizontalChainsSize = 0;
    int mVerticalChainsSize = 0;
    ConstraintWidget[] mMatchConstraintsChainedWidgets = new ConstraintWidget[4];
    ConstraintWidget[] mVerticalChainsArray = new ConstraintWidget[4];
    ConstraintWidget[] mHorizontalChainsArray = new ConstraintWidget[4];

    private int mOptimizationLevel = Optimizer.OPTIMIZATION_NONE;

    // Internal -- used to keep track of first/last visible/invisible widgets
    // TODO: we could save the array by using the children array and simply store the indexes.
    ConstraintWidget[] mChainEnds = new ConstraintWidget[4];

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

    int mDebugSolverPassCount = 0;

    /**
     * Add this widget to the solver
     *
     * @param system the solver we want to add the widget to
     */
    public boolean addChildrenToSolver(LinearSystem system, int group) {
        if (DEBUG) {
            System.out.println("\n#######################################");
            System.out.println("##    ADD CHILDREN TO SOLVER  (" + mDebugSolverPassCount + ") ##");
            System.out.println("#######################################\n");
            mDebugSolverPassCount++;
        }
        addToSolver(system, group);
        final int count = mChildren.size();
        boolean setMatchParent = true;

        if (false && (mOptimizationLevel == Optimizer.OPTIMIZATION_ALL
                || mOptimizationLevel == Optimizer.OPTIMIZATION_BASIC)) {
            if (Optimizer.optimize(system, this)) {
                return false; // all work is done
            }
            // need to check match_parent
            setMatchParent = true;
        }
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
    public void updateChildrenFromSolver(LinearSystem system, int group, boolean flags[]) {
        flags[Optimizer.FLAG_RECOMPUTE_BOUNDS] = false;
        updateFromSolver(system, group);
        final int count = mChildren.size();
        for (int i = 0; i < count; i++) {
            ConstraintWidget widget = mChildren.get(i);
            widget.updateFromSolver(system, group);
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

        boolean wrap_override = false;
        DimensionBehaviour originalVerticalDimensionBehaviour = mListDimensionBehaviors[DIMENSION_VERTICAL];
        DimensionBehaviour originalHorizontalDimensionBehaviour = mListDimensionBehaviors[DIMENSION_HORIZONTAL];

        if (DEBUG_LAYOUT) {
            System.out.println("layout with prew: " + prew + " (" + mListDimensionBehaviors[DIMENSION_HORIZONTAL]
                + ") preh: " + preh + " (" + mListDimensionBehaviors[DIMENSION_VERTICAL] + ")");
        }
        if (false && mOptimizationLevel == Optimizer.OPTIMIZATION_ALL
                && (mListDimensionBehaviors[DIMENSION_VERTICAL] == DimensionBehaviour.WRAP_CONTENT
                || mListDimensionBehaviors[DIMENSION_HORIZONTAL] == DimensionBehaviour.WRAP_CONTENT)) {
            // TODO: do the wrap calculation in two separate passes
            Optimizer.findWrapSize(this, mChildren, Optimizer.flags);
            wrap_override = Optimizer.flags[Optimizer.FLAG_USE_OPTIMIZE];
            if (DEBUG_LAYOUT) {
                System.out.println("layout with wrap width: " + mWrapWidth + " wrap height: " + mWrapHeight + " wrap override? " + wrap_override);
            }
            if (prew > 0 && preh > 0 && (mWrapWidth > prew || mWrapHeight > preh)) {
                // TODO: this could be better optimized with a tighter coupling between view measures and
                // wrap/layout. For now, simply escape to the solver.
                wrap_override = false;
                if (DEBUG_LAYOUT) {
                    System.out.println("wrap override set to false, goes to solver");
                }
            }
            if (wrap_override) {
                if (mListDimensionBehaviors[DIMENSION_HORIZONTAL] == DimensionBehaviour.WRAP_CONTENT) {
                    mListDimensionBehaviors[DIMENSION_HORIZONTAL] = DimensionBehaviour.FIXED;
                    if (prew > 0 && prew < mWrapWidth) {
                        mWidthMeasuredTooSmall = true;
                        setWidth(prew);
                    } else {
                        setWidth(Math.max(mMinWidth, mWrapWidth));
                    }
                }
                if (mListDimensionBehaviors[DIMENSION_VERTICAL] == DimensionBehaviour.WRAP_CONTENT) {
                    mListDimensionBehaviors[DIMENSION_VERTICAL] = DimensionBehaviour.FIXED;
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
                System.out.println("EXCEPTION : " + e);
            }
            if (needsSolving) {
                updateChildrenFromSolver(mSystem, ConstraintAnchor.ANY_GROUP, Optimizer.flags);
            } else {
                updateFromSolver(mSystem, ConstraintAnchor.ANY_GROUP);
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
