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

package com.google.tnt.solver;

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
    public static IRow createRowEquals(LinearSystem linearSystem, SolverVariable variable,
            int value) {
        // a = c
        // row : - c + a
        IRow row = linearSystem.createRow(6);
        row.createRowEquals(variable, value);
        linearSystem.replaceVariable(row, variable);
        return row;
    }

    public static IRow createRowEquals(LinearSystem linearSystem, SolverVariable variableA,
            SolverVariable variableB, int margin, boolean withError, int errorStrength) {
        // expression is: variableA = variableB + margin
        // we turn it into row = margin - variableA + variableB
        IRow row = linearSystem.createRow(6);
        row.createRowEquals(variableA, variableB, margin, withError, errorStrength);
        linearSystem.replaceVariable(row, variableA);
        linearSystem.replaceVariable(row, variableB);
        if (withError) {
            linearSystem.addSingleError(row, 1, errorStrength);
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

    public static IRow createRowEquals(LinearSystem linearSystem, SolverVariable variableA,
            SolverVariable variableB, int margin, boolean withError) {
        return createRowEquals(linearSystem, variableA, variableB, margin, withError, 1);
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
    public static IRow createRowDimensionPercent(LinearSystem linearSystem,
            SolverVariable variableA,
            SolverVariable variableB, SolverVariable variableC, int percent, boolean withError) {
        IRow row = linearSystem.createRow(10);
        if (withError) {
            linearSystem.addError(row, 1);
        }
        return row.createRowDimensionPercent(variableA, variableB, variableC, percent);
    }

    public static IRow createRowGreaterThan(LinearSystem linearSystem, SolverVariable variableA,
            SolverVariable variableB, int margin,
            boolean withError, int errorStrength) {
        // expression is: variableA >= variableB + margin
        // we turn it into: variableA - slack = variableB + margin
        // row = margin - variableA + variableB + slack
        SolverVariable slack = linearSystem.createSlackVariable();
        IRow row = linearSystem.createRow(6);
        row.createRowGreaterThan(variableA, variableB, slack, margin, withError, errorStrength);
        linearSystem.replaceVariable(row, variableA);
        linearSystem.replaceVariable(row, variableB);
        linearSystem.replaceVariable(row, slack);
        if (withError) {
            float slackValue = row.getVariable(slack);
            linearSystem.addSingleError(row, (int) (-1 * slackValue), errorStrength);
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

    public static IRow createRowGreaterThan(LinearSystem linearSystem, SolverVariable variableA,
            SolverVariable variableB, int margin, boolean withError) {
        return createRowGreaterThan(linearSystem, variableA, variableB, margin, withError, 1);
    }

    public static IRow createRowLowerThan(LinearSystem linearSystem, SolverVariable variableA,
            SolverVariable variableB, int margin,
            boolean withError, int errorStrength) {
        // expression is: variableA <= variableB + margin
        // we turn it into: variableA + slack = variableB + margin
        // row = margin - variableA + variableB - slack
        SolverVariable slack = linearSystem.createSlackVariable();
        IRow row = linearSystem.createRow(6);
        row.createRowLowerThan(variableA, variableB, slack, margin, withError, errorStrength);
        linearSystem.replaceVariable(row, variableA);
        linearSystem.replaceVariable(row, variableB);
        linearSystem.replaceVariable(row, slack);
        if (withError) {
            float slackValue = row.getVariable(slack);
            linearSystem.addSingleError(row, (int) (-1 * slackValue), errorStrength);
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

    public static IRow createRowLowerThan(LinearSystem linearSystem, SolverVariable variableA,
            SolverVariable variableB, int margin, boolean withError) {
        return createRowLowerThan(linearSystem, variableA, variableB, margin, withError, 1);
    }

    public static IRow createRowCentering(LinearSystem linearSystem,
            SolverVariable variableA, SolverVariable variableB, int marginA,
            float bias,
            SolverVariable variableC, SolverVariable variableD, int marginB,
            boolean withError, int errorStrength) {
        // expression is: (1 - bias) * (variableA - variableB) = bias * (variableC - variableD)
        // we turn it into:
        // row = (1 - bias) * variableA - (1 - bias) * variableB - bias * variableC + bias * variableD
        IRow row = linearSystem.createRow(9);
        row.createRowCentering(variableA, variableB, marginA, bias,
                variableC, variableD, marginB, withError, errorStrength);
        linearSystem.replaceVariable(row, variableA);
        linearSystem.replaceVariable(row, variableB);
        linearSystem.replaceVariable(row, variableC);
        linearSystem.replaceVariable(row, variableD);
        if (withError) {
            linearSystem.addError(row, errorStrength);
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

    public static IRow createRowCentering(LinearSystem linearSystem,
            SolverVariable variableA, SolverVariable variableB, float bias,
            SolverVariable variableC, SolverVariable variableD, boolean withError) {
        return createRowCentering(linearSystem, variableA, variableB, 0,
                bias, variableC, variableD, 0, withError, 1);
    }

    /**
     * Transform a LinearEquation into a Row
     * @param linearSystem
     * @param e linear equation
     * @return a Row object
     */
    static IRow createRowFromEquation(LinearSystem linearSystem, LinearEquation e) {
        e.normalize();
        e.moveAllToTheRight();
        // Let's build a row from the LinearEquation
        IRow row = linearSystem.createRow();
        ArrayList<EquationVariable> eq = e.getRightSide();
        final int count = eq.size();
        for (int i = 0; i < count; i++) {
            EquationVariable v = eq.get(i);
            SolverVariable sv = v.getSolverVariable();
            if (sv != null) {
                row.setVariable(sv, v.getAmount().toFloat());
            } else {
                row.setConstant(v.getAmount().toFloat());
            }
        }
        return row;
    }
}
