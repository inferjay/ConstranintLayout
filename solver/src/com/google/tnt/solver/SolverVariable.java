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

/**
 * Represents a given variable used in the {@link com.google.tnt.solver.LinearSystem linear expression solver}.
 * The {@link com.google.tnt.solver.EquationVariable} variables point to SolverVariable.
 */
public class SolverVariable {

    private static final boolean INTERNAL_DEBUG = false;
    static int n = 1;
    public float mComputedValue;

    public static String getUniqueName() { n++; return "V" + n; }

    public static String getUniqueName(Type type, Strength strength) {
        n++;
        switch (type) {
            case UNRESTRICTED: return "U" + n;
            case CONSTANT: return "C" + n;
            case SLACK: return "S" + n;
            case ERROR: {
                if (strength == Strength.STRONG) {
                    return "E" + n;
                } else {
                    return "e" + n;
                }
            }
        }
        return "V" + n;
    }

    ArrayList<IRow> mClientEquations = new ArrayList<IRow>();

    public ArrayList<IRow> getClientEquations() {
        return mClientEquations;
    }

    public void addClientEquation(IRow equation) {
        if (mClientEquations.contains(equation)) {
            return;
        }
        mClientEquations.add(equation);
    }

    public void removeClientEquation(IRow equation) {
        mClientEquations.remove(equation);
    }

    public void reset() {
        mName = null;
        mType = Type.UNKNOWN;
        mStrength = Strength.WEAK;
        mId = -1;
        mDefinitionId = -1;
        mComputedValue = 0;
        mClientEquations.clear();
    }

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

    public enum Strength {
        STRONG,
        WEAK,
        UNKNOWN
    }

    private String mName;
    private Type mType;
    private Strength mStrength = Strength.WEAK;
    public int mId;
    public int mDefinitionId = -1;

    /**
     * Base constructor
     *
     * @param name the variable name
     * @param type the type of the variable
     */
    public SolverVariable(String name, Type type) {
        mName = name;
        mType = type;
    }

    public SolverVariable(Type type) {
        mType = type;
        if (INTERNAL_DEBUG) {
            mName = getUniqueName(type, Strength.UNKNOWN);
        }
    }

    /**
     * Accessor for the name
     *
     * @return the name of the variable
     */
    public String getName() {
        return mName;
    }

    /**
     * Accessor for the type
     *
     * @return the type of the variable
     */
    public Type getType() {
        return mType;
    }

    public void setName(String name) { mName = name; }
    public void setType(Type type) {
        mType = type;
        if (INTERNAL_DEBUG && mName == null) {
            mName = getUniqueName(type, Strength.UNKNOWN);
        }
    }

    /**
     * Setter for the strength (used for errors and slack variables)
     * @param s the strength
     */
    public void setStrength(Strength s) {
        mStrength = s;
        if (INTERNAL_DEBUG && mName == null) {
            mName = getUniqueName(mType, mStrength);
        }
    }

    /**
     * Accessor for the strength (used for errors and slack variables in evaluating the goal)
     * @return the strength
     */
    public Strength getStrength() { return mStrength; }

    /**
     * Override the toString() method to display the variable
     */
    @Override
    public String toString() {
        String result = "";
        if (false && INTERNAL_DEBUG) {
            result += "{" + mName + ":" + mType + ":" + mStrength + "}";
        } else {
            result += mName;
        }
        return result;
    }

    /**
     * Set the global id for this variable
     * @param id
     */
    void setId(int id) {
        mId = id;
    }

    /**
     * Returns the global id of this variable
     * @return
     */
    int getId() {
        return mId;
    }
}
