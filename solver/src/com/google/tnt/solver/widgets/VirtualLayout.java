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

public class VirtualLayout implements Solvable {

    protected ConstraintWidget mParent = null;

    protected ArrayList<ConstraintWidget> mWidgets = null;

    public void setParent(ConstraintWidget widget) {
        mParent = widget;
    }

    public ConstraintWidget getParent() { return mParent; }

    public void addChild(ConstraintWidget widget) {
        if (mWidgets == null) {
            mWidgets = new ArrayList<ConstraintWidget>();
        }
        if (mWidgets.contains(widget)) {
            return;
        }
        mWidgets.add(widget);
    }

    public void removeAllChildren() {
        if (mWidgets == null) {
            return;
        }
        mWidgets.clear();
    }

    @Override
    public void addToSolver(LinearSystem system) {}

    @Override
    public void updateFromSolver(LinearSystem system) {}

    @Override
    public void setDebugSolverName(LinearSystem system, String name) {}
}
