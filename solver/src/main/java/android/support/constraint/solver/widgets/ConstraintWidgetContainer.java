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

/**
 * A container of ConstraintWidget that can layout its children
 */
public class ConstraintWidgetContainer extends WidgetContainer {

    private static final boolean USE_THREAD = false;
    private static final boolean DEBUG = false;
    private static final boolean USE_SNAPSHOT = false;

    protected LinearSystem mSystem = new LinearSystem();
    protected LinearSystem mBackgroundSystem = null;

    private Snapshot mSnapshot;

    static boolean ALLOW_ROOT_GROUP = true;

    int mWrapWidth;
    int mWrapHeight;

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
        if (USE_SNAPSHOT) {
            if (mSnapshot == null) {
                mSnapshot = new Snapshot(this);
            }
            mSnapshot.updateFrom(this);
        }
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
    @Override
    public void addToSolver(LinearSystem system, int group) {
        super.addToSolver(system, group);
        final int count = mChildren.size();
        for (int i = 0; i < count; i++) {
            ConstraintWidget widget = mChildren.get(i);
            if (widget instanceof ConstraintWidgetContainer) {
                DimensionBehaviour horizontalBehaviour = widget.getHorizontalDimensionBehaviour();
                DimensionBehaviour verticalBehaviour = widget.getVerticalDimensionBehaviour();
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
    }

    /**
     * Update the frame of the layout and its children from the solver
     *
     * @param system the solver we get the values from.
     */
    @Override
    public void updateFromSolver(LinearSystem system, int group) {
        super.updateFromSolver(system, group);
        final int count = mChildren.size();
        for (int i = 0; i < count; i++) {
            ConstraintWidget widget = mChildren.get(i);
            widget.updateFromSolver(system, group);
        }
    }

    /**
     * Layout the tree of widgets
     */
    @Override
    public void layout() {
        int prex = mX;
        int prey = mY;
        if (USE_SNAPSHOT) {
            if (mSnapshot == null) {
                mSnapshot = new Snapshot(this);
            }
            mSnapshot.updateFrom(this);
            // We clear ourselves of external anchors as
            // well as repositioning us to (0, 0)
            // before inserting us in the solver, so that our
            // children's positions get computed relative to us.
            setX(0);
            setY(0);
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

        // Now let's solve our system as usual
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
            addToSolver(mSystem, ConstraintAnchor.ANY_GROUP);
            mSystem.minimize();
        } catch (Exception e) {
            e.printStackTrace();
        }
        updateFromSolver(mSystem, ConstraintAnchor.ANY_GROUP);

        if (USE_SNAPSHOT) {
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
     * @param widget
     */
    public void findWrapRecursive(ConstraintWidget widget) {
        int w = widget.getWrapWidth();

        int distToRight = w;
        int distToLeft = w;
        ConstraintWidget leftWidget = null;
        ConstraintWidget rightWidget = null;

        widget.mVisited = true;
        if (!(widget.mRight.isConnected() || (widget.mLeft.isConnected()))) {
            distToLeft += widget.getX();
        } else {
            if (widget.mRight.mTarget != null) {
                rightWidget = widget.mRight.mTarget.getOwner();
                distToRight += widget.mRight.getMargin();
                if (!rightWidget.isRoot() && !rightWidget.mVisited) {
                    findWrapRecursive(rightWidget);
                }
            }
            if (widget.mLeft.isConnected()) {
                leftWidget = widget.mLeft.mTarget.getOwner();
                distToLeft += widget.mLeft.getMargin();
                if (!leftWidget.isRoot() && !leftWidget.mVisited) {
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
                if (!baseLineWidget.mVisited) {
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
                if (!topWidget.isRoot() && !topWidget.mVisited) {
                    findWrapRecursive(topWidget);
                }
            }

            ConstraintWidget bottomWidget = null;
            if (widget.mBottom.isConnected()) {
                bottomWidget = widget.mBottom.mTarget.getOwner();
                distToBottom += widget.mBottom.getMargin();
                if (!bottomWidget.isRoot() && !bottomWidget.mVisited) {
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
            if (!widget.mVisited) {
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
            children.get(j).mVisited = false;
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
        if (USE_SNAPSHOT) {
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

        if (USE_SNAPSHOT) {
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
}
