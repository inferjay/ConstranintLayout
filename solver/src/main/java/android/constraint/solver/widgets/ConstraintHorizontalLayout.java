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
package android.constraint.solver.widgets;

import android.constraint.solver.LinearSystem;

/**
 * Implements a simple horizontal linear layout-like behaviour
 */
public class ConstraintHorizontalLayout extends ConstraintWidgetContainer {

    private ContentAlignment mAlignment = ContentAlignment.MIDDLE;

    /**
     * Default constructor
     */
    public ConstraintHorizontalLayout() {}

    /**
     * Constructor
     * @param x x position
     * @param y y position
     * @param width width of the layout
     * @param height height of the layout
     */
    public ConstraintHorizontalLayout(int x, int y, int width, int height) {
        super(x, y, width, height);
    }

    /**
     * Constructor
     * @param width width of the layout
     * @param height height of the layout
     */
    public ConstraintHorizontalLayout(int width, int height) {
        super(width, height);
    }

    /**
     * Add the layout and its children to the solver
     * @param system the solver we want to add the widget to
     */
    @Override
    public void addToSolver(LinearSystem system) {
        super.addToSolver(system);
        if (mChildren.size() == 0) {
            return;
        }
        android.constraint.solver.widgets.ConstraintWidget previous = this;
        for (int i = 0, mChildrenSize = mChildren.size(); i < mChildrenSize; i++) {
            final android.constraint.solver.widgets.ConstraintWidget widget = mChildren.get(i);
            if (previous != this) {
                widget.connect(android.constraint.solver.widgets.ConstraintAnchor.Type.LEFT, previous, android.constraint.solver.widgets.ConstraintAnchor.Type.RIGHT);
                previous.connect(android.constraint.solver.widgets.ConstraintAnchor.Type.RIGHT, widget, android.constraint.solver.widgets.ConstraintAnchor.Type.LEFT);
            } else {
                android.constraint.solver.widgets.ConstraintAnchor.Strength strength = android.constraint.solver.widgets.ConstraintAnchor.Strength.STRONG;
                if (mAlignment == ContentAlignment.END) {
                    strength = android.constraint.solver.widgets.ConstraintAnchor.Strength.WEAK;
                }
                widget.connect(android.constraint.solver.widgets.ConstraintAnchor.Type.LEFT, previous,
                        android.constraint.solver.widgets.ConstraintAnchor.Type.LEFT, 0, strength);
            }
            widget.connect(android.constraint.solver.widgets.ConstraintAnchor.Type.TOP, this, android.constraint.solver.widgets.ConstraintAnchor.Type.TOP);
            widget.connect(android.constraint.solver.widgets.ConstraintAnchor.Type.BOTTOM, this, android.constraint.solver.widgets.ConstraintAnchor.Type.BOTTOM);
            previous = widget;
        }
        if (previous != this) {
            android.constraint.solver.widgets.ConstraintAnchor.Strength strength = android.constraint.solver.widgets.ConstraintAnchor.Strength.STRONG;
            if (mAlignment == ContentAlignment.BEGIN) {
                strength = android.constraint.solver.widgets.ConstraintAnchor.Strength.WEAK;
            }
            previous.connect(android.constraint.solver.widgets.ConstraintAnchor.Type.RIGHT, this,
                             android.constraint.solver.widgets.ConstraintAnchor.Type.RIGHT, 0, strength);
        }
        for (int i = 0, mChildrenSize = mChildren.size(); i < mChildrenSize; i++) {
            final android.constraint.solver.widgets.ConstraintWidget widget = mChildren.get(i);
            widget.addToSolver(system);
        }
    }

}
