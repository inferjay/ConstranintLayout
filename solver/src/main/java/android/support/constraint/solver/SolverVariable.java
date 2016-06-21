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
 * The {@link EquationVariable} variables point to SolverVariable.
 */
public class SolverVariable {

    private static final boolean INTERNAL_DEBUG = false;

    // Set to true if mClientEquations is a linked list
    public static final boolean USE_LIST = false;

    static int uniqueId = 1;

    private String mName;

    public int id = -1;
    public int definitionId = -1;
    public float computedValue;

    Type mType;
    Strength mStrength = Strength.WEAK;

    final class Link {
        ArrayRow row;
        Link next;
    }

    private final Cache mCache;

    // if USE_LIST = true, set mClientEquations to be a linked list
    // instead of an array.
    //   Link mClientEquations = new Link();
    // It seems that the array, at this point, is still faster than
    // using the linked list, but we might want to switch to an array-based
    // linked list and reevaluate.
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

    public enum Strength {
        STRONG,
        WEAK,
        UNKNOWN
    }

    public static String getUniqueName() { uniqueId++; return "V" + uniqueId; }

    public static String getUniqueName(Type type, Strength strength) {
        uniqueId++;
        switch (type) {
            case UNRESTRICTED: return "U" + uniqueId;
            case CONSTANT: return "C" + uniqueId;
            case SLACK: return "S" + uniqueId;
            case ERROR: {
                if (strength == Strength.STRONG) {
                    return "E" + uniqueId;
                } else {
                    return "e" + uniqueId;
                }
            }
        }
        return "V" + uniqueId;
    }

    /**
     * Base constructor
     *
     * @param name the variable name
     * @param type the type of the variable
     */
    public SolverVariable(Cache cache, String name, Type type) {
        mCache = cache;
        mName = name;
        mType = type;
    }

    public SolverVariable(Cache cache, Type type) {
        mCache = cache;
        mType = type;
        if (INTERNAL_DEBUG) {
            mName = getUniqueName(type, Strength.UNKNOWN);
        }
    }

    public void addClientEquation(ArrayRow equation) {
        if (USE_LIST) {
            Link current = (Link) (Object) mClientEquations;
            while (current.next != null) {
                if (current.next.row == equation) {
                    return;
                }
                current = current.next;
            }
            Link newLink = mCache.linkedSolverVariablePool.acquire();
            if (newLink == null) {
                newLink = new Link();
            }
            newLink.row = equation;
            newLink.next = null;
            current.next = newLink;
        } else {

            for (int i = 0; i < mClientEquationsCount; i++) {
                if (mClientEquations[i] == equation) {
                    return;
                }
            }
            if (mClientEquationsCount >= mClientEquations.length) {
                mClientEquations = Arrays.copyOf(mClientEquations, mClientEquations.length * 2);
            }
            mClientEquations[mClientEquationsCount] = equation;
        }
        mClientEquationsCount++;
    }

    public void removeClientEquation(ArrayRow equation) {
        if (INTERNAL_DEBUG) {
            if (equation.variables.get(this) != 0) {
                return;
            }
        }
        if (USE_LIST) {
            Link current = (Link) (Object) mClientEquations;
            while (current.next != null) {
                if (current.next.row == equation) {
                    mCache.linkedSolverVariablePool.release(current.next);
                    current.next = current.next.next;
                    mClientEquationsCount--;
                    return;
                }
                current = current.next;
            }
        } else {
            for (int i = 0; i < mClientEquationsCount; i++) {
                if (mClientEquations[i] == equation) {
                    System.arraycopy(mClientEquations, i + 1, mClientEquations, i, (mClientEquationsCount - i - 1));
                    mClientEquationsCount--;
                    return;
                }
            }
        }
    }

    public void reset() {
        mName = null;
        mType = Type.UNKNOWN;
        mStrength = Strength.STRONG;
        id = -1;
        definitionId = -1;
        computedValue = 0;
        if (USE_LIST) {
            Link current = (Link) (Object) mClientEquations;
            while (current.next != null) {
                mCache.linkedSolverVariablePool.release(current.next);
                current = current.next;
            }
            ((Link) (Object) mClientEquations).next = null;
        }
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

}
