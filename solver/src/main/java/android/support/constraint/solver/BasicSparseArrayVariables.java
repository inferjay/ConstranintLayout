/*
 * Copyright (C) 2016 The Android Open Source Project
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

import java.util.Arrays;

/**
 * Store a set of variables and their values in an array.
 */
public class BasicSparseArrayVariables {
    private static final boolean DEBUG = false;

    private SolverVariable[] variables = null;
    private float[] values = null;
    private int[] indexes = null;
    private int maxSize = 8;
    private int currentSize = 0;
    private SolverVariable candidate = null;

    public SolverVariable getPivotCandidate() {
        if (candidate == null) {
            for (int i = 0; i < currentSize; i++) {
                int idx = indexes[i];
                if (values[idx] < 0) {
                    SolverVariable variable = variables[idx];
                    if (candidate == null || variable.definitionId < candidate.definitionId) {
                        candidate = variable;
                    }
                }
            }
        }
        return candidate;
    }

    public BasicSparseArrayVariables() {
        variables = new SolverVariable[maxSize];
        values = new float[maxSize];
        indexes = new int[maxSize];
    }

    final void increaseSize() {
        maxSize *= 2;
        variables = Arrays.copyOf(variables, maxSize);
        values = Arrays.copyOf(values, maxSize);
        indexes = Arrays.copyOf(indexes, maxSize);
    }

    public final int size() {
        return currentSize;
    }

    public final SolverVariable getVariable(int index) {
        return variables[indexes[index]];
    }

    public final float getVariableValue(int index) {
        return values[indexes[index]];
    }

    public final void updateArray(BasicSparseArrayVariables target, float amount) {
        for (int i = 0; i < currentSize; i++) {
            final int idx = indexes[i];
            SolverVariable v = variables[idx];
            float value = values[idx];
            float previousValue = target.get(v);
            float finalValue = previousValue + (value * amount);
            target.put(v, finalValue);
        }
    }

    public final void setVariable(int index, float value) {
        int idx = indexes[index];
        values[idx] = value;
        if (value < 0 && candidate == null) {
            candidate = variables[idx];
        }
    }

    public final float get(SolverVariable v) {
        for (int i = 0; i < currentSize; i++) {
            int idx = indexes[i];
            if (variables[idx] == v) {
                return values[idx];
            }
        }
        return 0;
    }

    public final void put(SolverVariable variable, float value) {
        if (value == 0) {
            remove(variable);
            return;
        }
        if (currentSize == maxSize) {
            increaseSize();
        }
        for (int i = 0; i < currentSize; i++) {
            if (variables[i] == variable) {
                values[i] = value;
                if (value < 0 && candidate == null) {
                    candidate = variable;
                }
                return;
            }
            if (variables[i] == null) {
                variables[i] = variable;
                values[i] = value;
                indexes[currentSize] = i;
                currentSize++;
                return;
            }
        }
        variables[currentSize] = variable;
        values[currentSize] = value;
        indexes[currentSize] = currentSize;
        currentSize++;
    }

    public final void clear() {
        currentSize = 0;
    }

    public final boolean containsKey(SolverVariable variable) {
        for (int i = 0; i < currentSize; i++) {
            int idx = indexes[i];
            if (variables[idx] == variable) {
                return true;
            }
        }
        return false;
    }

    public final void remove(SolverVariable variable) {
        if (candidate == variable) {
            candidate = null;
        }
        for (int i = 0; i < currentSize; i++) {
            int idx = indexes[i];
            if (variables[idx] == variable) {
                variables[idx] = null;
                System.arraycopy(indexes, i + 1, indexes, i, (currentSize - i - 1));
                currentSize--;
                return;
            }
        }
    }

    public int sizeInBytes() {
        int size = 0;
        size += (maxSize * 4);
        size += (maxSize * 4);
        size += (maxSize * 4);
        size += 4 + 4 + 4 + 4;
        return size;
    }

    public void display() {
        int count = size();
        System.out.print("{ ");
        for (int i = 0; i < count; i++) {
            SolverVariable v = getVariable(i);
            if (v == null) {
                continue;
            }
            System.out.print(v + " = " + getVariableValue(i) + " ");
        }
        System.out.println(" }");
    }
}
