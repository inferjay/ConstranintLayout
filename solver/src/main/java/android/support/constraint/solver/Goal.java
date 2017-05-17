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

package android.support.constraint.solver;

import java.util.ArrayList;

import static android.support.constraint.solver.SolverVariable.MAX_STRENGTH;

/**
 * Represents a goal to minimize
 */
public class Goal implements LinearSystem.Row {

    ArrayList<SolverVariable> variables = new ArrayList<>();

    /**
     * Return a SolverVariable that is a good pivot candidate
     * (higher strength variable of negative value)
     *
     * @param avoid if not null, will skip the given variable
     *
     * @return pivot candidate
     */
    public SolverVariable getPivotCandidate(LinearSystem system, boolean[] avoid) {
        final int count = variables.size();
        SolverVariable candidate = null;
        int strength = 0;

        //noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < count; i++) {
            SolverVariable element = variables.get(i);
            if (avoid[element.id]) {
                continue;
            }
            for (int k = MAX_STRENGTH - 1; k >= 0; k--) {
                float value = element.strengthVector[k];
                if (value > 0) {
                    break;
                }
                if (value < 0 && (k > strength || candidate == null)) {
                    strength = k;
                    candidate = element;
                }
            }
        }
        return candidate;
    }

    /**
     * Initialize the goal from the given system.
     * The goal will contain all the existing error variables
     *
     * @param system the linear system we initialize from
     */
    public void initFromSystemErrors(LinearSystem system) {
        variables.clear();
        for (int i = 1; i < system.mNumColumns; i++) {
            SolverVariable variable = system.mCache.mIndexedVariables[i];
            for (int j = 0; j < MAX_STRENGTH; j++) {
                variable.strengthVector[j] = 0;
            }
            variable.strengthVector[variable.strength] = 1;
            if (variable.mType != SolverVariable.Type.ERROR
                     /* && variable.mType != SolverVariable.Type.SLACK */) {
                continue;
            }
            variables.add(variable);
        }
        System.out.println("Original goal: " + this);
    }

    @Override
    public void clear() {
        variables.clear();
    }

    @Override
    public void initFromRow(LinearSystem.Row row) {
        updateFromRow((ArrayRow) row);
    }

    @Override
    public void addError(SolverVariable error) {
        // nothing
    }

    @Override
    public SolverVariable getKey() {
        return null;
    }

    @Override
    public SolverVariable getSubject(LinearSystem system) {
        return null;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    /**
     * Update the goal from the given system
     *
     * @param system the linear system we update from
     */
    public void updateFromSystem(LinearSystem system) {
        final int count = variables.size();
        for (int i = 0; i < count; i++) {
            SolverVariable element = variables.get(i);
            if (element.definitionId != -1) {
                ArrayRow definition = system.getRow(element.definitionId);
                ArrayLinkedVariables variables = definition.variables;
                int size = variables.currentSize;
                for (int j = 0; j < size; j++) {
                    SolverVariable var = variables.getVariable(j);
                    if (var == null) {
                        continue;
                    }
                    float value = variables.getVariableValue(j);
                    for (int k = 0; k < MAX_STRENGTH; k++) {
                        var.strengthVector[k] += element.strengthVector[k] * value;
                    }
                    if (!this.variables.contains(var)) {
                        this.variables.add(var);
                    }
                }
                element.clearStrengths();
            }
        }
    }

    void updateFromRow(ArrayRow row) {
        variables.clear();
        SolverVariable variable = row.variable;
        for (int j = 0; j < MAX_STRENGTH; j++) {
            variable.strengthVector[j] = 0;
        }
        variable.strengthVector[0] = -1; //variable.strength] = value;
        variables.add(row.variable);
        for (int i = 0; i < row.variables.currentSize; i++) {
            variable = row.variables.getVariable(i);
            for (int j = 0; j < MAX_STRENGTH; j++) {
                variable.strengthVector[j] = 0;
            }
            float value = row.variables.getVariableValue(i);
            variable.strengthVector[0] = value; //variable.strength] = value;
            variables.add(variable);
        }
        System.out.println("Goal from row: " + this);
    }

    /**
     * String representation of the goal
     *
     * @return a string representation
     */
    @Override
    public String toString() {
        String representation = "Goal: ";
        final int count = variables.size();
        //noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < count; i++) {
            SolverVariable element = variables.get(i);
            representation += element.strengthsToString();
            representation += "\n      ";
        }
        return representation;
    }

}
