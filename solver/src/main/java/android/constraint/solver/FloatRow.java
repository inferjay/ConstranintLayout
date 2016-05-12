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

import java.util.Arrays;

class FloatRow implements android.constraint.solver.IRow {
    private static final boolean DEBUG = false;
    private LinearSystem mLinearSystem;
    android.constraint.solver.SolverVariable variable = null;
    float[] row = null;
    final float epsilon = 0.001f;

    @Override
    public void setUsed(boolean b) {
    }

    @Override
    public int sizeInBytes() {
        int size = 16; // variable
        size += 4; // used
        size += row.length * 4; // array
        return size;
    }

    @Override
    public android.constraint.solver.IRow createRowDimensionPercent(android.constraint.solver.SolverVariable variableA,
            android.constraint.solver.SolverVariable variableB, android.constraint.solver.SolverVariable variableC, int percent) {
        float p = (percent / 100f);
        row[variableA.mId] = -1;
        row[variableB.mId] = (1 - p);
        row[variableC.mId] = p;
        return this;
    }

    @Override
    public android.constraint.solver.IRow createRowDimensionRatio(android.constraint.solver.SolverVariable variableA, android.constraint.solver.SolverVariable variableB,
                                        android.constraint.solver.SolverVariable variableC, android.constraint.solver.SolverVariable variableD, float ratio) {
        row[variableA.mId] = -1;
        row[variableB.mId] = 1;
        row[variableC.mId] = ratio;
        row[variableD.mId] = -ratio;
        return this;
    }

    @Override
    public float getConstant() {
        return row[0];
    }

    @Override
    public float getVariable(android.constraint.solver.SolverVariable v) {
        return row[v.mId];
    }

    @Override
    public android.constraint.solver.IRow createRowEquals(android.constraint.solver.SolverVariable variable, int value) {
        int indexSv = variable.mId;
        if (value < 0) {
            row[0] = -1 * value;
            row[indexSv] = 1;
        } else {
            row[0] = value;
            row[indexSv] = -1;
        }
        return this;
    }

    @Override
    public android.constraint.solver.IRow createRowEquals(android.constraint.solver.SolverVariable variableA, android.constraint.solver.SolverVariable variableB,
            int margin, boolean withError, int errorStrength) {
        int indexSvA = variableA.mId;
        int indexSvB = variableB.mId;
        boolean inverse = false;
        if (margin != 0) {
            int m = margin;
            if (m < 0) {
                m = -1 * m;
                inverse = true;
            }
            row[0] = m;
        }
        if (!inverse) {
            row[indexSvA] = -1;
            row[indexSvB] = 1;
        } else {
            row[indexSvA] = 1;
            row[indexSvB] = -1;
        }
        return this;
    }

    @Override
    public android.constraint.solver.IRow addSingleError(android.constraint.solver.SolverVariable error, int sign) {
        row[error.mId] = sign;
        return this;
    }

    @Override
    public android.constraint.solver.IRow createRowGreaterThan(android.constraint.solver.SolverVariable variableA,
            android.constraint.solver.SolverVariable variableB, android.constraint.solver.SolverVariable slack,
            int margin, boolean withError, int errorStrength) {
        int indexSvA = variableA.mId;
        int indexSvB = variableB.mId;
        int indexSlack = slack.mId;
        boolean inverse = false;
        if (margin != 0) {
            int m = margin;
            if (m < 0) {
                m = -1 * m;
                inverse = true;
            }
            row[0] = m;
        }
        if (!inverse) {
            row[indexSvA] = -1;
            row[indexSvB] = 1;
            row[indexSlack] = 1;
        } else {
            row[indexSvA] = 1;
            row[indexSvB] = -1;
            row[indexSlack] = -1;
        }
        return this;
    }

    @Override
    public android.constraint.solver.IRow createRowLowerThan(android.constraint.solver.SolverVariable variableA, android.constraint.solver.SolverVariable variableB,
            android.constraint.solver.SolverVariable slack, int margin, boolean withError, int errorStrength) {
        int indexSvA = variableA.mId;
        int indexSvB = variableB.mId;
        int indexSlack = slack.mId;
        boolean inverse = false;
        if (margin != 0) {
            int m = margin;
            if (m < 0) {
                m = -1 * m;
                inverse = true;
            }
            row[0] = m;
        }
        if (!inverse) {
            row[indexSvA] = -1;
            row[indexSvB] = 1;
            row[indexSlack] = -1;
        } else {
            row[indexSvA] = 1;
            row[indexSvB] = -1;
            row[indexSlack] = 1;
        }
        return this;
    }

    @Override
    public android.constraint.solver.IRow createRowCentering(android.constraint.solver.SolverVariable variableA, android.constraint.solver.SolverVariable variableB, int marginA,
            float bias, android.constraint.solver.SolverVariable variableC, android.constraint.solver.SolverVariable variableD, int marginB, boolean withError,
            int errorStrength) {
        int indexSvA = variableA.mId;
        int indexSvB = variableB.mId;
        int indexSvC = variableC.mId;
        int indexSvD = variableD.mId;
        if (variableB == variableC) {
            // centering on the same position
            // B - A == D - B
            // 0 = A + D - 2 * B
            row[indexSvA] = 1;
            row[indexSvD] = 1;
            row[indexSvB] = -2;
            return this;
        }
        row[indexSvA] = 1 * (1 - bias);
        row[indexSvB] = -1 * (1 - bias);
        row[indexSvC] = -1 * bias;
        row[indexSvD] = 1 * bias;
        row[0] = - marginA * (1 - bias) + marginB * bias;
        return this;
    }

    @Override
    public android.constraint.solver.IRow addError(android.constraint.solver.SolverVariable error1, android.constraint.solver.SolverVariable error2) {
        int indexError1 = error1.mId;
        int indexError2 = error2.mId;
        row[indexError1] = 1;
        row[indexError2] = -1;
        return this;
    }

    @Override
    public boolean hasAtLeastOneVariable() {
        for (int i = 1; i < mLinearSystem.mNumColumns; i++) {
            if (row[i] > 0) {
                return true;
            }
        }
        return false;
    }

    @Override
    public android.constraint.solver.SolverVariable findPivotCandidate() {
        int candidatePivotIndex = 0;
        for (int i = 1; i < mLinearSystem.mNumColumns; i++) {
            float amount = row[i];
            if (amount == 0) {
                continue;
            }
            if (amount < 0) {
                candidatePivotIndex = i;
                break;
            }
        }
        if (candidatePivotIndex > 0) {
            return mLinearSystem.mIndexedVariables[candidatePivotIndex];
        }
        return null;
    }

    @Override
    public boolean hasPositiveConstant() {
        return row[0] >= 0;
    }

    @Override
    public boolean hasKeyVariable() {
        return !(
                (variable == null)
                        || (variable.getType() != android.constraint.solver.SolverVariable.Type.UNRESTRICTED
                        && row[0] < 0)
        );
    }

    @Override
    public void addVariable(android.constraint.solver.SolverVariable v) {
        row[v.mId] = 1;
    }

    @Override
    public void setVariable(android.constraint.solver.SolverVariable v, float value) {
        row[v.mId] = value;
    }

    @Override
    public void setConstant(float v) {
        row[0] = v;
    }

    public FloatRow(LinearSystem linearSystem) {
        mLinearSystem = linearSystem;
        if (mLinearSystem.mNumRows + 1 >= mLinearSystem.mMaxRows) {
            mLinearSystem.increaseTableSize();
        }
        row = mLinearSystem.mBackend[mLinearSystem.mNumRows];
        Arrays.fill(row, 0);
    }

    public void reset() {
        Arrays.fill(row, 0);
        variable = null;
    }

    @Override
    public android.constraint.solver.SolverVariable getKeyVariable() {
        return variable;
    }

    @Override
    public boolean hasVariable(android.constraint.solver.SolverVariable v) {
        return (row[v.mId] != 0);
    }

    @Override
    public void updateClientEquations() {
        int count = mLinearSystem.mNumColumns;
        for (int i = 1; i < count; i++) {
            float variableAmount = row[i];
            if (variableAmount == 0) {
                continue;
            }
            android.constraint.solver.SolverVariable variable = mLinearSystem.mIndexedVariables[i];
            variable.addClientEquation(this);
        }
    }

    @Override
    public boolean updateRowWithEquation(android.constraint.solver.IRow row) {
        FloatRow source = (FloatRow) row;
        android.constraint.solver.SolverVariable v = source.variable;
        int indexSv = v.mId;
        float amount = this.row[indexSv];
        if (amount != 0) {
            // we find in this row a variable defined by rowSource.
            // let's replace things.
            for (int j = 0; j < mLinearSystem.mNumColumns; j++) {
                float sourceAmount = source.row[j];
                if (sourceAmount == 0) {
                    continue;
                }
                this.row[j] += sourceAmount * amount;
            }
            this.row[indexSv] = 0;
            return true;
        }
        return false;
    }

    @Override
    public void ensurePositiveConstant() {
        // Ensure that if we have a constant it's positive
        if (row[0] < 0) {
            // If not, simply multiply the equation by -1
            for (int i = 0; i < mLinearSystem.mNumColumns; i++) {
                row[i] *= -1;
            }
        }
    }

    @Override
    public void pickRowVariable() {
        // now we need to find an adequate variable to pivot on
        int restrictedCandidateIndex = 0;
        int unrestrictedCandidateIndex = 0;
        for (int i = 1; i < mLinearSystem.mNumColumns; i++) {
            float variableAmount = row[i];
            if (variableAmount == 0) {
                continue;
            }
            if (variableAmount < 0) {
                if (variableAmount > -epsilon) {
                    variableAmount = 0;
                }
            } else {
                if (variableAmount < epsilon) {
                    variableAmount = 0;
                }
            }
            if (variableAmount == 0) {
                continue;
            }
            android.constraint.solver.SolverVariable candidateVariable = mLinearSystem.mIndexedVariables[i];
            // let's see if this could be used to pivot on
            if (candidateVariable.getType() == android.constraint.solver.SolverVariable.Type.UNRESTRICTED) {
                // If there's an unrestricted variable, it's a candidate to pivot on.
                if (variableAmount < 0) {
                    // if it has a negative value, simply pivot immediately.
                    pivot(i);
                    break;
                } else if (unrestrictedCandidateIndex == 0) {
                    // otherwise, let's remember this candidate. If we don't find another
                    // negative unrestricted variable, this might be our best candidate.
                    unrestrictedCandidateIndex = i;
                }
            } else if (variableAmount < 0) {
                // The variable is restricted, but has a negative amount. If we don't already
                // have candidate for restricted variable, we should remember it as a candidate.
                if (restrictedCandidateIndex == 0) {
                    restrictedCandidateIndex = i;
                }
            }
        }

        // we were not able to pivot on an unrestricted negative variable. We might have a previously
        // unknown unrestricted variable with positive variable though, and we should pivot on it preferably.
        if (variable == null && unrestrictedCandidateIndex != 0) {
            pivot(unrestrictedCandidateIndex);
            if (DEBUG) {
                System.out.println("pivot on positive unrestricted variable at pos "
                        + unrestrictedCandidateIndex + " " +
                        mLinearSystem.mIndexedVariables[unrestrictedCandidateIndex]);
            }
        }
        // we were not able to pivot on an unrestricted variable, but we might have restricted
        // variable candidate for the pivot
        if (variable == null && restrictedCandidateIndex != 0) {
            pivot(restrictedCandidateIndex);
            if (DEBUG) {
                System.out.println("pivot on restricted variable at pos "
                        + restrictedCandidateIndex + " " +
                        mLinearSystem.mIndexedVariables[restrictedCandidateIndex]);
            }
        }

        if (DEBUG) {
            System.out.println("Row after pivot is: " + toReadableString());
        }
    }

    @Override
    public void pivot(android.constraint.solver.SolverVariable v) {
        pivot(v.mId);
    }

    private void pivot(int column) {
        if (variable != null) {
            // first, move back the variable to its column
            int variableIndex = variable.mId;
            row[variableIndex] = -1;
            variable = null;
        }
        // now grab the amount of the pivot, and divide
        // the columns by it.
        float amount = row[column] * -1;
        final int count = mLinearSystem.mNumColumns;
        for (int i = 0; i < count; i++) {
            float previous = row[i];
            if (i == column) {
                // no need to do the pivot column.
                continue;
            }
            if (previous == 0) {
                continue;
            }
            float a = previous / amount;
            if (a != 0) {
                if (a < 0) {
                    if (a > -epsilon) {
                        a = 0;
                    }
                } else {
                    if (a < epsilon) {
                        a = 0;
                    }
                }
            }
            row[i] = a;
        }
        row[column] = 0;
        variable = mLinearSystem.mIndexedVariables[column];
    }

    @Override
    public String toReadableString() {
        String s = "";
        if (variable == null) {
            s += "0";
        } else {
            s += variable;
        }
        s += " = ";
        final int count = mLinearSystem.mNumColumns;
        boolean addedVariable = false;
        if (row[0] != 0) {
            s += row[0];
            addedVariable = true;
        }
        for (int i = 1; i < count; i++) {
            String name = mLinearSystem.mIndexedVariables[i].getName();
            float amount = row[i];
            if (amount == 0) {
                continue;
            }
            if (!addedVariable) {
                if (amount < 0) {
                    s += "- ";
                    amount *= -1;
                }
            } else {
                if (amount > 0) {
                    s += " + ";
                } else {
                    s += " - ";
                    amount *= -1;
                }
            }
            if (amount == 1) {
                s += name;
            } else {
                s += amount + " " + name;
            }
            addedVariable = true;
        }
        if (!addedVariable) {
            s += "0.0";
        }
        return s;
    }

    @Override
    public String toString() {
        String s = "";
        s += variable;
        s += "\t| ";
        final int count = mLinearSystem.mNumColumns;
        for (int i = 0; i < count; i++) {
            float amount = row[i];
            if (amount != 0) {
                s += amount;
            } else {
                s += " . ";
            }
            s += " | ";
        }
        return s;
    }

    void updateBackend(float[] floats) {
        row = floats;
    }

    /**
     * Replaces in the target row instances of the variable defined
     * by the source row (if there are any)
     * @param source source row
     * @param target target row
     */
    void replaceVariables(FloatRow source, FloatRow target) {
        android.constraint.solver.SolverVariable v = source.variable;
        int indexSv = v.mId;
        float amount = target.row[indexSv];
        if (amount != 0) {
            // we find in this row a variable defined by rowSource.
            // let's replace things.
            for (int j = 0; j < mLinearSystem.mNumColumns; j++) {
                float sourceAmount = source.row[j];
                if (sourceAmount == 0) {
                    continue;
                }
                target.row[j] += sourceAmount * amount;
            }
            target.row[indexSv] = 0;
        }
    }
}
