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

package android.constraint.solver;

interface IRow {

    void pivot(SolverVariable v);
    boolean updateRowWithEquation(IRow row);
    void ensurePositiveConstant();
    void pickRowVariable();
    boolean hasKeyVariable();
    String toReadableString();
    SolverVariable findPivotCandidate();
    void addVariable(SolverVariable v);
    boolean hasAtLeastOneVariable();
    boolean hasPositiveConstant();
    void reset();
    SolverVariable getKeyVariable();
    boolean hasVariable(SolverVariable v);
    float getConstant();
    float getVariable(SolverVariable pivotCandidate);
    void setVariable(SolverVariable sv, float v);
    void setConstant(float v);
    void setUsed(boolean b);
    int sizeInBytes();

    // creation
    IRow createRowEquals(SolverVariable variable, int value);

    IRow createRowEquals(SolverVariable variableA, SolverVariable variableB, int margin,
            boolean withError, int errorStrength);

    IRow addSingleError(SolverVariable error, int sign);

    IRow createRowGreaterThan(SolverVariable variableA, SolverVariable variableB,
            SolverVariable slack,
            int margin, boolean withError, int errorStrength);

    IRow createRowLowerThan(SolverVariable variableA, SolverVariable variableB,
            SolverVariable slack,
            int margin, boolean withError, int errorStrength);

    IRow createRowCentering(SolverVariable variableA, SolverVariable variableB, int marginA,
            float bias, SolverVariable variableC, SolverVariable variableD, int marginB,
            boolean withError, int errorStrength);

    IRow addError(SolverVariable error1, SolverVariable error2);

    IRow createRowDimensionPercent(SolverVariable variableA, SolverVariable variableB,
            SolverVariable variableC, int percent);

    IRow createRowDimensionRatio(SolverVariable variableA, SolverVariable variableB,
                                   SolverVariable variableC, SolverVariable variableD, float ratio);

    void updateClientEquations();
}
