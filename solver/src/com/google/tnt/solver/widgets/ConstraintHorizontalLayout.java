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
package com.google.tnt.solver.widgets;

import com.google.tnt.solver.LinearSystem;

import java.util.ArrayList;

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
        ConstraintWidget previous = this;
        for (ConstraintWidget widget : mChildren) {
            if (previous != this) {
                widget.connect(ConstraintAnchor.Type.LEFT, previous, ConstraintAnchor.Type.RIGHT);
                previous.connect(ConstraintAnchor.Type.RIGHT, widget, ConstraintAnchor.Type.LEFT);
            } else {
                ConstraintAnchor.Strength strength = ConstraintAnchor.Strength.STRONG;
                if (mAlignment == ContentAlignment.END) {
                    strength = ConstraintAnchor.Strength.WEAK;
                }
                widget.connect(ConstraintAnchor.Type.LEFT, previous,
                               ConstraintAnchor.Type.LEFT, 0, strength);
            }
            widget.connect(ConstraintAnchor.Type.TOP, this, ConstraintAnchor.Type.TOP);
            widget.connect(ConstraintAnchor.Type.BOTTOM, this, ConstraintAnchor.Type.BOTTOM);
            previous = widget;
        }
        if (previous != this) {
            ConstraintAnchor.Strength strength = ConstraintAnchor.Strength.STRONG;
            if (mAlignment == ContentAlignment.BEGIN) {
                strength = ConstraintAnchor.Strength.WEAK;
            }
            previous.connect(ConstraintAnchor.Type.RIGHT, this,
                             ConstraintAnchor.Type.RIGHT, 0, strength);
        }
        for (ConstraintWidget widget : mChildren) {
            widget.addToSolver(system);
        }
    }

}
