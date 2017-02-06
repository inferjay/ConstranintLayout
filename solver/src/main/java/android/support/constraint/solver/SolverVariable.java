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

import java.util.Arrays;

/**
 * Represents a given variable used in the {@link LinearSystem linear expression solver}.
 */
public class SolverVariable {

    private static final boolean INTERNAL_DEBUG = false;

    @SuppressWarnings("WeakerAccess")
    public static final int STRENGTH_NONE = 0;
    public static final int STRENGTH_LOW = 1;
    public static final int STRENGTH_MEDIUM = 2;
    public static final int STRENGTH_HIGH = 3;
    @SuppressWarnings("WeakerAccess")
    public static final int STRENGTH_HIGHEST = 4;
    public static final int STRENGTH_EQUALITY = 5;

    private static int uniqueId = 1;

    private String mName;

    public int id = -1;
    int definitionId = -1;
    public int strength = 0;
    public float computedValue;

    final static int MAX_STRENGTH = 6;
    float[] strengthVector = new float[MAX_STRENGTH];
    Type mType;

    ArrayRow[] mClientEquations = new ArrayRow[8];
    int mClientEquationsCount = 0;

    /**
     * Type of variables
     */
    public enum Type {
        /**
         * The variable can take negative or positive values
         */
        UNRESTRICTED,
        /**
         * The variable is actually not a variable :) , but a constant number
         */
        CONSTANT,
        /**
         * The variable is restricted to positive values and represents a slack
         */
        SLACK,
        /**
         * The variable is restricted to positive values and represents an error
         */
        ERROR,
        /**
         * Unknown (invalid) type.
         */
        UNKNOWN
    }

    private static String getUniqueName(Type type) {
        uniqueId++;
        switch (type) {
            case UNRESTRICTED: return "U" + uniqueId;
            case CONSTANT: return "C" + uniqueId;
            case SLACK: return "S" + uniqueId;
            case ERROR: {
                return "e" + uniqueId;
            }
        }
        return "V" + uniqueId;
    }

    /**
     * Base constructor
     *  @param name the variable name
     * @param type the type of the variable
     */
    public SolverVariable(String name, Type type) {
        mName = name;
        mType = type;
    }

    public SolverVariable(Type type) {
        mType = type;
        if (INTERNAL_DEBUG) {
            mName = getUniqueName(type);
        }
    }

    void clearStrengths() {
        for (int i = 0; i < MAX_STRENGTH; i++) {
            strengthVector[i] = 0;
        }
    }

    String strengthsToString() {
        String representation = this + "[";
        for (int j = 0; j < strengthVector.length; j++) {
            representation += strengthVector[j];
            if (j < strengthVector.length - 1) {
                representation += ", ";
            } else {
                representation += "] ";
            }
        }
        return representation;
    }

    void addClientEquation(ArrayRow equation) {
        for (int i = 0; i < mClientEquationsCount; i++) {
            if (mClientEquations[i] == equation) {
                return;
            }
        }
        if (mClientEquationsCount >= mClientEquations.length) {
            mClientEquations = Arrays.copyOf(mClientEquations, mClientEquations.length * 2);
        }
        mClientEquations[mClientEquationsCount] = equation;
        mClientEquationsCount++;
    }

    void removeClientEquation(ArrayRow equation) {
        if (INTERNAL_DEBUG) {
            if (equation.variables.get(this) != 0) {
                return;
            }
        }
        for (int i = 0; i < mClientEquationsCount; i++) {
            if (mClientEquations[i] == equation) {
                for (int j = 0; j < (mClientEquationsCount - i - 1); j++) {
                    mClientEquations[i + j] = mClientEquations[i + j + 1];
                }
                mClientEquationsCount--;
                return;
            }
        }
    }

    public void reset() {
        mName = null;
        mType = Type.UNKNOWN;
        strength = SolverVariable.STRENGTH_NONE;
        id = -1;
        definitionId = -1;
        computedValue = 0;
        mClientEquationsCount = 0;
    }

    /**
     * Accessor for the name
     *
     * @return the name of the variable
     */
    public String getName() {
        return mName;
    }

    public void setName(String name) { mName = name; }
    public void setType(Type type) {
        mType = type;
        if (INTERNAL_DEBUG && mName == null) {
            mName = getUniqueName(type);
        }
    }

    /**
     * Override the toString() method to display the variable
     */
    @Override
    public String toString() {
        String result = "";
        if (INTERNAL_DEBUG) {
            result += mName + ":" + strength;
        } else {
            result += mName;
        }
        return result;
    }

}
