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

    protected LinearSystem mSystem = new LinearSystem();

    private Snapshot mSnapshot = new Snapshot(this);

    static boolean ALLOW_ROOT_GROUP = true;

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
        mSnapshot.updateFrom(this);
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
        if (system == mSystem) {
            final int count = mChildren.size();
            for (int i = 0; i < count; i++) {
                ConstraintWidget widget = mChildren.get(i);
                widget.updateFromSolver(system, group);
            }
        }
    }

    /**
     * Layout the tree of widgets
     */
    @Override
    public void layout() {
        mSnapshot.updateFrom(this);
        // We clear ourselves of external anchors as
        // well as repositioning us to (0, 0)
        // before inserting us in the solver, so that our
        // children's positions get computed relative to us.
        setX(0);
        setY(0);
        resetAnchors();
        resetSolverVariables();

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
            addToSolver(mSystem, ConstraintAnchor.ANY_GROUP);
            mSystem.minimize();
        } catch (Exception e) {
            e.printStackTrace();
        }
        updateFromSolver(mSystem, ConstraintAnchor.ANY_GROUP);

        int width = getWidth();
        int height = getHeight();
        // Let's restore our state...
        mSnapshot.applyTo(this);
        setWidth(width);
        setHeight(height);
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
        mSnapshot.updateFrom(this);
        // We clear ourselves of external anchors as
        // well as repositioning us to (0, 0)
        // before inserting us in the solver, so that our
        // children's positions get computed relative to us.
        setX(0);
        setY(0);
        resetAnchors();
        resetSolverVariables();

        // Before we solve our system, we should call layout() on any
        // of our children that is a container.
        final int count = mChildren.size();
        for (int i = 0; i < count; i++) {
            ConstraintWidget widget = mChildren.get(i);
            if (widget instanceof WidgetContainer) {
                ((WidgetContainer) widget).layout();
            }
        }

        for (int i = 0; i < numOfGroups; i++) {
            // Now let's solve our system as usual
            try {
                mSystem.reset();
                getAnchor(ConstraintAnchor.Type.LEFT).mGroup = i;
                getAnchor(ConstraintAnchor.Type.RIGHT).mGroup = i;
                getAnchor(ConstraintAnchor.Type.TOP).mGroup = i;
                getAnchor(ConstraintAnchor.Type.BOTTOM).mGroup = i;
                addToSolver(mSystem, i);
                mSystem.minimize();
            } catch (Exception e) {
                e.printStackTrace();
            }
            updateFromSolver(mSystem, i);
        }
        updateFromSolver(mSystem, ConstraintAnchor.APPLY_GROUP_RESULTS);

        int width = getWidth();
        int height = getHeight();
        // Let's restore our state...
        mSnapshot.applyTo(this);
        setWidth(width);
        setHeight(height);
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

}
