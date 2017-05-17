/*
 * Copyright (C) 2017 The Android Open Source Project
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
import android.support.constraint.solver.SolverVariable;

import java.util.Arrays;

/**
 * A Barrier takes multiple widgets
 */
public class Barrier extends Helper {

    public static final int LEFT = 0;
    public static final int RIGHT = 1;
    public static final int TOP = 2;
    public static final int BOTTOM = 3;

    private int mBarrierType = LEFT;

    public void setBarrierType(int barrierType) {
        mBarrierType = barrierType;
    }

    /**
     * Add this widget to the solver
     *
     * @param system the solver we want to add the widget to
     */
    @Override
    public void addToSolver(LinearSystem system, int group) {
        ConstraintAnchor position;
        mListAnchors[LEFT] = mLeft;
        mListAnchors[TOP] = mTop;
        mListAnchors[RIGHT] = mRight;
        mListAnchors[BOTTOM] = mBottom;
        for (int i = 0; i < mListAnchors.length; i++) {
            mListAnchors[i].mSolverVariable = system.createObjectVariable(mListAnchors[i]);
        }
        if (mBarrierType >= 0 && mBarrierType < 4) {
            position = mListAnchors[mBarrierType];
        } else {
            return;
        }
        for (int i = 0; i < mWidgetsCount; i++) {
            SolverVariable target = system.createObjectVariable(mWidgets[i].mListAnchors[mBarrierType]);
            mWidgets[i].mListAnchors[mBarrierType].mSolverVariable = target;
            if (mBarrierType == LEFT || mBarrierType == TOP) {
                system.addLowerThan(position.mSolverVariable, target, 0, SolverVariable.STRENGTH_NONE);
            } else {
                system.addGreaterThan(position.mSolverVariable, target,  0, SolverVariable.STRENGTH_NONE);
            }
        }
        if (mBarrierType == LEFT) {
            system.addEquality(mRight.mSolverVariable, mLeft.mSolverVariable, 0, SolverVariable.STRENGTH_EQUALITY);
        } else if (mBarrierType == RIGHT) {
            system.addEquality(mLeft.mSolverVariable, mRight.mSolverVariable, 0, SolverVariable.STRENGTH_EQUALITY);
        } else if (mBarrierType == TOP) {
            system.addEquality(mBottom.mSolverVariable, mTop.mSolverVariable, 0, SolverVariable.STRENGTH_EQUALITY);
        } else if (mBarrierType == BOTTOM) {
            system.addEquality(mTop.mSolverVariable, mBottom.mSolverVariable, 0, SolverVariable.STRENGTH_EQUALITY);
        }
    }
}
