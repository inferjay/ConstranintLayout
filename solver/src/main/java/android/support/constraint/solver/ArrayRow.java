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

import static android.support.constraint.solver.SolverVariable.*;

public class ArrayRow implements LinearSystem.Row {
    private static final boolean DEBUG = false;

    SolverVariable variable = null;
    float constantValue = 0;
    boolean used = false;
    private static final float epsilon = 0.001f;

    public final ArrayLinkedVariables variables;

    boolean isSimpleDefinition = false;

    public ArrayRow(Cache cache) {
        variables = new ArrayLinkedVariables(this, cache);
    }

    boolean hasKeyVariable() {
        return !(
                (variable == null)
                        || (variable.mType != SolverVariable.Type.UNRESTRICTED
                        && constantValue < 0)
        );
    }

    public String toString() {
        return toReadableString();
    }

    String toReadableString() {
        String s = "";
        if (variable == null) {
            s += "0";
        } else {
            s += variable;
        }
        s += " = ";
        boolean addedVariable = false;
        if (constantValue != 0) {
            s += constantValue;
            addedVariable = true;
        }
        int count = variables.currentSize;
        for (int i = 0; i < count; i++) {
            SolverVariable v = variables.getVariable(i);
            if (v == null) {
                continue;
            }
            float amount = variables.getVariableValue(i);
            if (amount == 0) {
                continue;
            }
            String name = v.toString();
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

    public void reset() {
        variable = null;
        variables.clear();
        constantValue = 0;
        isSimpleDefinition = false;
    }

    boolean hasVariable(SolverVariable v) {
        return variables.containsKey(v);
    }

    ArrayRow createRowDefinition(SolverVariable variable, int value) {
        this.variable = variable;
        variable.computedValue = value;
        constantValue = value;
        isSimpleDefinition = true;
        return this;
    }

    public ArrayRow createRowEquals(SolverVariable variable, int value) {
        if (value < 0) {
            constantValue = -1 * value;
            variables.put(variable, 1);
        } else {
            constantValue = value;
            variables.put(variable, -1);
        }
        return this;
    }

    public ArrayRow createRowEquals(SolverVariable variableA, SolverVariable variableB, int margin) {
        boolean inverse = false;
        if (margin != 0) {
            int m = margin;
            if (m < 0) {
                m = -1 * m;
                inverse = true;
            }
            constantValue = m;
        }
        if (!inverse) {
            variables.put(variableA, -1);
            variables.put(variableB, 1);
        } else {
            variables.put(variableA, 1);
            variables.put(variableB, -1);
        }
        return this;
    }

    ArrayRow addSingleError(SolverVariable error, int sign) {
        variables.put(error, (float) sign);
        return this;
    }

    public ArrayRow createRowGreaterThan(SolverVariable variableA,
                                         SolverVariable variableB, SolverVariable slack,
                                         int margin) {
        boolean inverse = false;
        if (margin != 0) {
            int m = margin;
            if (m < 0) {
                m = -1 * m;
                inverse = true;
            }
            constantValue = m;
        }
        if (!inverse) {
            variables.put(variableA, -1);
            variables.put(variableB, 1);
            variables.put(slack, 1);
        } else {
            variables.put(variableA, 1);
            variables.put(variableB, -1);
            variables.put(slack, -1);
        }
        return this;
    }

    public ArrayRow createRowGreaterThan(SolverVariable a, int b, SolverVariable slack) {
        constantValue = b;
        variables.put(a, -1);
        return this;
    }

    public ArrayRow createRowLowerThan(SolverVariable variableA, SolverVariable variableB,
                                       SolverVariable slack, int margin) {
        boolean inverse = false;
        if (margin != 0) {
            int m = margin;
            if (m < 0) {
                m = -1 * m;
                inverse = true;
            }
            constantValue = m;
        }
        if (!inverse) {
            variables.put(variableA, -1);
            variables.put(variableB, 1);
            variables.put(slack, -1);
        } else {
            variables.put(variableA, 1);
            variables.put(variableB, -1);
            variables.put(slack, 1);
        }
        return this;
    }

    public ArrayRow createRowEqualMatchDimensions(float currentWeight, float totalWeights, float nextWeight,
                                        SolverVariable variableStartA,
                                        SolverVariable variableEndA,
                                        SolverVariable variableStartB,
                                        SolverVariable variableEndB) {
        constantValue = 0;
        if (totalWeights == 0 || (currentWeight == nextWeight)) {
            // endA - startA == endB - startB
            // 0 = startA - endA + endB - startB
            variables.put(variableStartA, 1);
            variables.put(variableEndA, -1);
            variables.put(variableEndB, 1);
            variables.put(variableStartB, -1);
        } else {
            if (currentWeight == 0) {
                variables.put(variableStartA, 1);
                variables.put(variableEndA, -1);
            } else if (nextWeight == 0) {
                variables.put(variableStartB, 1);
                variables.put(variableEndB, -1);
            } else {
                float cw = currentWeight / totalWeights;
                float nw = nextWeight / totalWeights;
                float w = cw / nw;

                // endA - startA == w * (endB - startB)
                // 0 = startA - endA + w * (endB - startB)
                variables.put(variableStartA, 1);
                variables.put(variableEndA, -1);
                variables.put(variableEndB, w);
                variables.put(variableStartB, -w);
            }
        }
        return this;
    }

    public ArrayRow createRowEqualDimension(float currentWeight, float totalWeights, float nextWeight,
                                            SolverVariable variableStartA, int marginStartA,
                                            SolverVariable variableEndA, int marginEndA,
                                            SolverVariable variableStartB, int marginStartB,
                                            SolverVariable variableEndB, int marginEndB) {
        if (totalWeights == 0 || (currentWeight == nextWeight)) {
            // endA - startA + marginStartA + marginEndA == endB - startB + marginStartB + marginEndB
            // 0 = startA - endA - marginStartA - marginEndA + endB - startB + marginStartB + marginEndB
            // 0 = (- marginStartA - marginEndA + marginStartB + marginEndB) + startA - endA + endB - startB
            constantValue = -marginStartA - marginEndA + marginStartB + marginEndB;
            variables.put(variableStartA, 1);
            variables.put(variableEndA, -1);
            variables.put(variableEndB, 1);
            variables.put(variableStartB, -1);
        } else {
            float cw = currentWeight / totalWeights;
            float nw = nextWeight / totalWeights;
            float w = cw / nw;
            // (endA - startA + marginStartA + marginEndA) = w * (endB - startB) + marginStartB + marginEndB;
            // 0 = (startA - endA - marginStartA - marginEndA) + w * (endB - startB) + marginStartB + marginEndB
            // 0 = (- marginStartA - marginEndA + marginStartB + marginEndB) + startA - endA + w * endB - w * startB
            constantValue = - marginStartA - marginEndA + w * marginStartB + w * marginEndB;
            variables.put(variableStartA, 1);
            variables.put(variableEndA, -1);
            variables.put(variableEndB, w);
            variables.put(variableStartB, -w);
        }
        return this;
    }

    ArrayRow createRowCentering(SolverVariable variableA, SolverVariable variableB, int marginA,
                                float bias, SolverVariable variableC, SolverVariable variableD, int marginB) {
        if (variableB == variableC) {
            // centering on the same position
            // B - A == D - B
            // 0 = A + D - 2 * B
            variables.put(variableA, 1);
            variables.put(variableD, 1);
            variables.put(variableB, -2);
            return this;
        }
        if (bias == 0.5f) {
            // don't bother applying the bias, we are centered
            // A - B = C - D
            // 0 = A - B - C + D
            // with margin:
            // A - B - Ma = C - D - Mb
            // 0 = A - B - C + D - Ma + Mb
            variables.put(variableA, 1);
            variables.put(variableB, -1);
            variables.put(variableC, -1);
            variables.put(variableD, 1);
            if (marginA > 0 || marginB > 0) {
                constantValue = -marginA + marginB;
            }
        } else if (bias <= 0) {
            // A = B + m
            variables.put(variableA, -1);
            variables.put(variableB, 1);
            constantValue = marginA;
        } else if (bias >= 1) {
            // C = D - m
            variables.put(variableC, -1);
            variables.put(variableD, 1);
            constantValue = marginB;
        } else {
            variables.put(variableA, 1 * (1 - bias));
            variables.put(variableB, -1 * (1 - bias));
            variables.put(variableC, -1 * bias);
            variables.put(variableD, 1 * bias);
            if (marginA > 0 || marginB > 0) {
                constantValue = - marginA * (1 - bias) + marginB * bias;
            }
        }
        return this;
    }

    public ArrayRow addError(LinearSystem system, int strength) {
        variables.put(system.createErrorVariable(strength, "ep"), 1);
        variables.put(system.createErrorVariable(strength, "em"), -1);
        return this;
    }

    ArrayRow createRowDimensionPercent(SolverVariable variableA,
                                       SolverVariable variableB, SolverVariable variableC, float percent) {
        variables.put(variableA, -1);
        variables.put(variableB, (1 - percent));
        variables.put(variableC, percent);
        return this;
    }

    /**
     * Create a constraint to express A = B + (C - D) * ratio
     * We use this for ratio, where for exemple Right = Left + (Bottom - Top) * percent
     *
     * @param variableA variable A
     * @param variableB variable B
     * @param variableC variable C
     * @param variableD variable D
     * @param ratio ratio between AB and CD
     * @return the row
     */
    public ArrayRow createRowDimensionRatio(SolverVariable variableA, SolverVariable variableB,
                                            SolverVariable variableC, SolverVariable variableD, float ratio) {
        // A = B + (C - D) * ratio
        variables.put(variableA, -1);
        variables.put(variableB, 1);
        variables.put(variableC, ratio);
        variables.put(variableD, -ratio);
        return this;
    }

    /**
     * Create a constraint to express At + (Ab-At)/2 = Bt + (Bb-Bt)/2 - angle
     *
     * @param at
     * @param ab
     * @param bt
     * @param bb
     * @param angleComponent
     * @return
     */
    public ArrayRow createRowWithAngle(SolverVariable at, SolverVariable ab, SolverVariable bt, SolverVariable bb, float angleComponent) {
        variables.put(bt, 0.5f);
        variables.put(bb, 0.5f);
        variables.put(at, -0.5f);
        variables.put(ab, -0.5f);
        constantValue = - angleComponent;
        return this;
    }

    int sizeInBytes() {
        int size = 0;
        if (variable != null) {
            size += 4; // object
        }
        size += 4; // constantValue
        size += 4; // used

        size += variables.sizeInBytes();
        return size;
    }

    void ensurePositiveConstant() {
        // Ensure that if we have a constant it's positive
        if (constantValue < 0) {
            // If not, simply multiply the equation by -1
            constantValue *= -1;
            variables.invert();
        }
    }

    /**
     * Pick a subject variable out of the existing ones.
     * - if a variable is unrestricted
     * - or if it's a negative new variable (not found elsewhere)
     * - otherwise we have to add a new additional variable
     *
     * @return true if we added an extra variable to the system
     */
    boolean chooseSubject(LinearSystem system) {
        boolean addedExtra = false;
        SolverVariable pivotCandidate = variables.chooseSubject(system);
        if (pivotCandidate == null) {
            // need to add extra variable
            addedExtra = true;
        } else {
            pivot(pivotCandidate);
        }
        if (variables.currentSize == 0) {
            isSimpleDefinition = true;
        }
        return addedExtra;
    }

    SolverVariable  pickPivot(SolverVariable exclude) {
        return variables.getPivotCandidate(null, exclude);
    }

    void pivot(SolverVariable v) {
        if (variable != null) {
            // first, move back the variable to its column
            variables.put(variable, -1f);
            variable = null;
        }

        float amount = variables.remove(v, true) * -1;
        variable = v;
        if (amount == 1) {
            return;
        }
        constantValue = constantValue / amount;
        variables.divideByAmount(amount);
    }

    // Row compatibility

    @Override
    public boolean isEmpty() {
        return (variable == null && constantValue == 0 && variables.currentSize == 0);
    }

    @Override
    public SolverVariable getPivotCandidate(LinearSystem system, boolean[] avoid) {
        return variables.getPivotCandidate(avoid, null);
    }

    @Override
    public void clear() {
        variables.clear();
        variable = null;
        constantValue = 0;
    }

    /**
     * Used to initiate a goal from a given row (to see if we can remove an extra var)
     * @param row
     */
    @Override
    public void initFromRow(LinearSystem.Row row) {
        if (row instanceof ArrayRow) {
            ArrayRow copiedRow = (ArrayRow) row;
            variable = null;
            variables.clear();
            for (int i = 0; i < copiedRow.variables.currentSize; i++) {
                SolverVariable var = copiedRow.variables.getVariable(i);
                float val = copiedRow.variables.getVariableValue(i);
                variables.add(var, val, true);
            }
        }
    }

    @Override
    public void addError(SolverVariable error) {
        float weight = 1;
        if (error.strength == STRENGTH_LOW) {
            weight = 1F;
        } else if (error.strength == STRENGTH_MEDIUM) {
            weight = 1E3F;
        } else if (error.strength == STRENGTH_HIGH) {
            weight = 1E6F;
        } else if (error.strength == STRENGTH_HIGHEST) {
            weight = 1E9F;
        } else if (error.strength == STRENGTH_EQUALITY) {
            weight = 1E12F;
        }
        variables.put(error, weight);
    }

    @Override
    public SolverVariable getKey() {
        return variable;
    }

}
