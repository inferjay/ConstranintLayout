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

package android.support.constraint.solver;

import java.util.ArrayList;

public class EquationCreation {

    private static final boolean DEBUG = false;

    /**
     * Returns a row representing the expression Var = value
     * @param linearSystem
     * @param variable variable to set
     * @param value value of the variable
     * @return
     */
    public static ArrayRow createRowEquals(LinearSystem linearSystem, SolverVariable variable,
                                           int value) {
        // a = c
        // row : - c + a
        ArrayRow row = linearSystem.createRow();
        row.createRowEquals(variable, value);
        return row;
    }

    public static ArrayRow createRowEquals(LinearSystem linearSystem, SolverVariable variableA,
                                           SolverVariable variableB, int margin, boolean withError) {
        // expression is: variableA = variableB + margin
        // we turn it into row = margin - variableA + variableB
        ArrayRow row = linearSystem.createRow();
        row.createRowEquals(variableA, variableB, margin);
        if (withError) {
            linearSystem.addSingleError(row, 1);
            if (DEBUG) {
                System.out.println(
                        "Add " + variableA.getName() + " = " + variableB + " + " + margin + " + e");
            }
        } else {
            if (DEBUG) {
                System.out
                        .println("Add " + variableA.getName() + " = " + variableB + " + " + margin);
            }
        }
        return row;
    }

    /**
     * Create a constraint to express A = B + (C - B) * percent
     * @param linearSystem
     * @param variableA
     * @param variableB
     * @param variableC
     * @param percent
     * @return
     */
    public static ArrayRow createRowDimensionPercent(LinearSystem linearSystem,
                                                     SolverVariable variableA,
                                                     SolverVariable variableB, SolverVariable variableC, float percent, boolean withError) {
        ArrayRow row = linearSystem.createRow();
        if (withError) {
            linearSystem.addError(row);
        }
        return row.createRowDimensionPercent(variableA, variableB, variableC, percent);
    }

    public static ArrayRow createRowGreaterThan(LinearSystem linearSystem, SolverVariable variableA,
                                                SolverVariable variableB, int margin, boolean withError) {
        // expression is: variableA >= variableB + margin
        // we turn it into: variableA - slack = variableB + margin
        // row = margin - variableA + variableB + slack
        SolverVariable slack = linearSystem.createSlackVariable();
        ArrayRow row = linearSystem.createRow();
        row.createRowGreaterThan(variableA, variableB, slack, margin);
        if (withError) {
            float slackValue = row.variables.get(slack);
            linearSystem.addSingleError(row, (int) (-1 * slackValue));
            if (DEBUG) {
                System.out.println(
                        "Add " + variableA.getName() + " >= " + variableB.getName() + " + " +
                                margin + " + e");
            }
        } else {
            if (DEBUG) {
                System.out.println(
                        "Add " + variableA.getName() + " >= " + variableB.getName() + " + " +
                                margin);
            }
        }
        return row;
    }

    public static ArrayRow createRowLowerThan(LinearSystem linearSystem, SolverVariable variableA,
                                              SolverVariable variableB, int margin, boolean withError) {
        // expression is: variableA <= variableB + margin
        // we turn it into: variableA + slack = variableB + margin
        // row = margin - variableA + variableB - slack
        SolverVariable slack = linearSystem.createSlackVariable();
        ArrayRow row = linearSystem.createRow();
        row.createRowLowerThan(variableA, variableB, slack, margin);
        if (withError) {
            float slackValue = row.variables.get(slack);
            linearSystem.addSingleError(row, (int) (-1 * slackValue));
            if (DEBUG) {
                System.out.println(
                        "Add " + variableA.getName() + " <= " + variableB.getName() + " + " +
                                margin + " + e");
            }
        } else {
            if (DEBUG) {
                System.out.println(
                        "Add " + variableA.getName() + " <= " + variableB.getName() + " + " +
                                margin);
            }
        }
        return row;
    }

    public static ArrayRow createRowCentering(LinearSystem linearSystem,
                                              SolverVariable variableA, SolverVariable variableB, int marginA,
                                              float bias,
                                              SolverVariable variableC, SolverVariable variableD, int marginB,
                                              boolean withError) {
        // expression is: (1 - bias) * (variableA - variableB) = bias * (variableC - variableD)
        // we turn it into:
        // row = (1 - bias) * variableA - (1 - bias) * variableB - bias * variableC + bias * variableD
        ArrayRow row = linearSystem.createRow();
        row.createRowCentering(variableA, variableB, marginA, bias,
                variableC, variableD, marginB, withError);
        if (withError) {
            linearSystem.addError(row);
            if (DEBUG) {
                System.out.println(
                        "Add centering " + variableA.getName() + " - " + variableB.getName() +
                                " = " + variableC.getName() + " - " + variableD.getName() +
                                " +/- e");
            }
        } else {
            if (DEBUG) {
                System.out.println(
                        "Add centering " + variableA.getName() + " - " + variableB.getName() +
                                " = " + variableC.getName() + " - " + variableD.getName());
            }
        }
        return row;
    }

    /**
     * Transform a LinearEquation into a Row
     * @param linearSystem
     * @param e linear equation
     * @return a Row object
     */
    static ArrayRow createRowFromEquation(LinearSystem linearSystem, LinearEquation e) {
        e.normalize();
        e.moveAllToTheRight();
        // Let's build a row from the LinearEquation
        ArrayRow row = linearSystem.createRow();
        ArrayList<EquationVariable> eq = e.getRightSide();
        final int count = eq.size();
        for (int i = 0; i < count; i++) {
            EquationVariable v = eq.get(i);
            SolverVariable sv = v.getSolverVariable();
            if (sv != null) {
                row.variables.put(sv, v.getAmount().toFloat());
            } else {
                row.constantValue = v.getAmount().toFloat();
            }
        }
        return row;
    }
}
