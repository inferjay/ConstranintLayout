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

class ArrayRow implements IRow {
    private static final boolean DEBUG = false;

    SolverVariable variable = null;
    float variableValue = 0;
    float constantTerm = 0;
    boolean used = false;
    final float epsilon = 0.001f;

    final ArrayBackedVariables variables = new ArrayBackedVariables();

    @Override
    public void updateClientEquations() {
        int count = variables.size();
        for (int i = 0; i < count; i++) {
            SolverVariable v = variables.getVariable(i);
            v.addClientEquation(this);
        }
    }

    @Override
    public boolean hasPositiveConstant() {
        return constantTerm >= 0;
    }

    @Override
    public float getConstant() {
        return constantTerm;
    }

    @Override
    public boolean hasAtLeastOneVariable() {
        int count = variables.size();
        for (int i = 0; i < count; i++) {
            float value = variables.getVariableValue(i);
            if (value > 0) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void addVariable(SolverVariable v) {
        variables.put(v, 1.f);
    }

    @Override
    public boolean hasKeyVariable() {
        return !(
                (variable == null)
                        || (variable.getType() != SolverVariable.Type.UNRESTRICTED
                        && constantTerm < 0)
        );
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
        boolean addedVariable = false;
        if (constantTerm != 0) {
            s += constantTerm;
            addedVariable = true;
        }
        int count = variables.size();
        for (int i = 0; i < count; i++) {
            SolverVariable v = variables.getVariable(i);
            float amount = variables.getVariableValue(i);
            String name = v.getName();
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
        if (DEBUG) {
            variables.display();
        }
        return s;
    }

    @Override
    public void reset() {
        variable = null;
        variables.clear();
        variableValue = 0;
        constantTerm = 0;
    }

    @Override
    public SolverVariable getKeyVariable() {
        return variable;
    }

    @Override
    public boolean hasVariable(SolverVariable v) {
        return variables.containsKey(v);
    }

    @Override
    public float getVariable(SolverVariable v) {
        return variables.get(v);
    }

    @Override
    public void setVariable(SolverVariable v, float value) {
        variables.put(v, value);
    }

    @Override
    public void setConstant(float v) {
        constantTerm = v;
    }

    @Override
    public IRow createRowEquals(SolverVariable variable, int value) {
        if (value < 0) {
            this.constantTerm = -1 * value;
            setVariable(variable, 1);
        } else {
            this.constantTerm = value;
            setVariable(variable, -1);
        }
        return this;
    }

    @Override
    public IRow createRowEquals(SolverVariable variableA, SolverVariable variableB,
            int margin, boolean withError, int errorStrength) {
        boolean inverse = false;
        if (margin != 0) {
            int m = margin;
            if (m < 0) {
                m = -1 * m;
                inverse = true;
            }
            constantTerm = m;
        }
        if (!inverse) {
            setVariable(variableA, -1);
            setVariable(variableB, 1);
        } else {
            setVariable(variableA, 1);
            setVariable(variableB, -1);
        }
        return this;
    }

    @Override
    public IRow addSingleError(SolverVariable error, int sign) {
        setVariable(error, sign);
        return this;
    }

    @Override
    public IRow createRowGreaterThan(SolverVariable variableA,
            SolverVariable variableB, SolverVariable slack,
            int margin, boolean withError, int errorStrength) {
        boolean inverse = false;
        if (margin != 0) {
            int m = margin;
            if (m < 0) {
                m = -1 * m;
                inverse = true;
            }
            constantTerm = m;
        }
        if (!inverse) {
            setVariable(variableA, -1);
            setVariable(variableB, 1);
            setVariable(slack, 1);
        } else {
            setVariable(variableA, 1);
            setVariable(variableB, -1);
            setVariable(slack, -1);
        }
        return this;
    }

    @Override
    public IRow createRowLowerThan(SolverVariable variableA, SolverVariable variableB,
            SolverVariable slack, int margin, boolean withError, int errorStrength) {
        boolean inverse = false;
        if (margin != 0) {
            int m = margin;
            if (m < 0) {
                m = -1 * m;
                inverse = true;
            }
            constantTerm = m;
        }
        if (!inverse) {
            setVariable(variableA, -1);
            setVariable(variableB, 1);
            setVariable(slack, -1);
        } else {
            setVariable(variableA, 1);
            setVariable(variableB, -1);
            setVariable(slack, 1);
        }
        return this;
    }

    @Override
    public IRow createRowCentering(SolverVariable variableA, SolverVariable variableB, int marginA,
            float bias, SolverVariable variableC, SolverVariable variableD, int marginB,
            boolean withError, int errorStrength) {
        if (variableB == variableC) {
            // centering on the same position
            // B - A == D - B
            // 0 = A + D - 2 * B
            setVariable(variableA, 1);
            setVariable(variableD, 1);
            setVariable(variableB, -2);
            return this;
        }
        if (bias == 0.5f) {
            // don't bother applying the bias, we are centered
            // A - B = C - D
            // 0 = A - B - C + D
            // with margin:
            // A - B - Ma = C - D - Mb
            // 0 = A - B - C + D - Ma + Mb
            setVariable(variableA, 1);
            setVariable(variableB, -1);
            setVariable(variableC, -1);
            setVariable(variableD, 1);
            if (marginA > 0 || marginB > 0) {
                setConstant(- marginA + marginB);
            }
        } else {
            setVariable(variableA, 1 * (1 - bias));
            setVariable(variableB, -1 * (1 - bias));
            setVariable(variableC, -1 * bias);
            setVariable(variableD, 1 * bias);
            if (marginA > 0 || marginB > 0) {
                setConstant( - marginA * (1 - bias) + marginB * bias);
            }
        }
        return this;
    }

    @Override
    public IRow addError(SolverVariable error1, SolverVariable error2) {
        setVariable(error1, 1);
        setVariable(error2, -1);
        return this;
    }

    @Override
    public IRow createRowDimensionPercent(SolverVariable variableA,
            SolverVariable variableB, SolverVariable variableC, int percent) {
        float p = (percent / 100f);
        setVariable(variableA, -1);
        setVariable(variableB, (1 - p));
        setVariable(variableC, p);
        return this;
    }

    @Override
    public IRow createRowDimensionRatio(SolverVariable variableA, SolverVariable variableB,
                                          SolverVariable variableC, SolverVariable variableD, float ratio) {
        // A = B + (C - D) * ratio
        setVariable(variableA, -1);
        setVariable(variableB, 1);
        setVariable(variableC, ratio);
        setVariable(variableD, -ratio);
        return this;
    }

    @Override
    public void setUsed(boolean b) {
        this.used = b;
    }

    @Override
    public int sizeInBytes() {
        int size = 0;
        if (variable != null) {
            size += 4; // object
        }
        size += 4; // variableValue;
        size += 4; // constantTerm
        size += 4; // used

        size += variables.sizeInBytes();
        return size;
    }

    @Override
    public boolean updateRowWithEquation(IRow row) {
        ArrayRow equation = (ArrayRow) row;
        float amount = variables.get(equation.variable);
        // let's replace this with the new definition
        if (amount != 0) {
            int count = equation.variables.size();
            for (int i = 0; i < count; i++) {
                int idx = equation.variables.indexes[i];
                SolverVariable v = equation.variables.variables[idx];
                float sourceAmount = equation.variables.values[idx];
                float previousAmount;
                previousAmount = variables.get(v);
                float finalValue = previousAmount + (sourceAmount * amount);
                variables.put(v, finalValue);
            }
            constantTerm += equation.constantTerm * amount;
            variables.remove(equation.variable);
            return true;
        }
        return false;
    }

    @Override
    public void ensurePositiveConstant() {
        // Ensure that if we have a constant it's positive
        if (constantTerm < 0) {
            // If not, simply multiply the equation by -1
            constantTerm *= -1;
            int count = variables.size();
            for (int i = 0; i < count; i++) {
                float previousAmount = variables.getVariableValue(i);
                variables.setVariable(i, previousAmount * -1);
            }
        }
    }

    @Override
    public void pickRowVariable() {
        SolverVariable restrictedCandidate = null;
        SolverVariable unrestrictedCandidate = null;
        // let's find an adequate variable to pivot on
        int count = variables.size();
        for (int i = 0; i < count; i++) {
            int idx = variables.indexes[i];
            SolverVariable candidateVariable = variables.variables[idx];
            float variableAmount = variables.values[idx];
            if (variableAmount != 0) {
                if (variableAmount < 0) {
                    if (variableAmount > -epsilon) {
                        variableAmount = 0;
                    }
                } else {
                    if (variableAmount < epsilon) {
                        variableAmount = 0;
                    }
                }
            } else {
                continue;
            }
            if (variableAmount == 0) {
                variables.setVariable(i, 0);
                continue;
            }

            if (candidateVariable == null) {
                // TODO: Log error condition
                continue;
            }

            if (candidateVariable.getType() == SolverVariable.Type.UNRESTRICTED) {
                // If there's an unrestricted variable, it's a candidate to pivot on.
                if (variableAmount < 0) {
                    // if it has a negative value, simply pivot immediately
                    pivot(candidateVariable);
                    break;
                } else if (unrestrictedCandidate == null) {
                    // otherwise, let's remember this candidate. If we don't find another
                    // negative unrestricted variable, this might be our best candidate.
                    unrestrictedCandidate = candidateVariable;
                }
            } else if (variableAmount < 0) {
                // The variable is restricted, but has a negative amount. If we don't already
                // have candidate for restricted variable, we should remember it as a candidate.
                if (restrictedCandidate == null) {
                    restrictedCandidate = candidateVariable;
                }
            }
        }

        // we were not able to pivot on an unrestricted negative variable.
        if (variable == null && unrestrictedCandidate != null) {
            // We might have a previously unknown unrestricted variable with
            // positive variable though, and we should pivot on it preferably.
            pivot(unrestrictedCandidate);
        }
        if (variable == null && restrictedCandidate != null) {
            // we were not able to pivot on an unrestricted variable, but we might
            // have restricted variable candidate for the pivot
            pivot(restrictedCandidate);
        }
    }

    @Override
    public void pivot(SolverVariable v) {
        if (variable != null) {
            // first, move back the variable to its column
            variables.put(variable, -1f /* * variableValue */);
            variable = null;
        }

        // now grab the amount of the pivot, and divide the columns by it
        float amount = variables.get(v) * -1;
        variables.remove(v);
        variable = v;
        variableValue = 1;
        constantTerm = constantTerm / amount;
        int count = variables.size();
        for (int i = 0; i < count; i++) {
            float previousAmount = variables.getVariableValue(i);
            float value = previousAmount / amount;
            if (value < 0) {
                if (value > -epsilon) {
                    value = 0;
                }
            } else {
                if (value < epsilon) {
                    value = 0;
                }
            }
            variables.setVariable(i, value);
        }
    }

    @Override
    public SolverVariable findPivotCandidate() {
        SolverVariable candidate = variables.getPivotCandidate();
        if (candidate != null) {
            return candidate;
        }
        int count = variables.size();
        for (int i = 0; i < count; i++) {
            float amount = variables.getVariableValue(i);
            if (amount < 0) {
                return variables.getVariable(i);
            }
        }
        return null;
    }

}