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
 * Store a set of variables and their values in an array-based linked list.
 *
 * The general idea is that we want to store a list of variables that need to be ordered,
 * space efficient, and relatively fast to maintain (add/remove).
 *
 * ArrayBackedVariables implements a sparse array, so is rather space efficient, but maintaining
 * the array sorted is costly, as we spend quite a bit of time recopying parts of the array on element deletion.
 *
 * LinkedVariables implements a standard linked list structure, and is able to be faster than ArrayBackedVariables
 * even though it's more costly to set up (pool of objects...), as the elements removal and maintenance of the
 * structure is a lot more efficient.
 *
 * This ArrayLinkedVariables class takes inspiration from both of the above, and implement a linked list
 * stored in several arrays. This allows us to be a lot more efficient in terms of setup (no need to deal with pool
 * of objects...), resetting the structure, and insertion/deletion of elements.
 */
public class ArrayLinkedVariables {
    private static final boolean DEBUG = false;

    private final static int NONE = -1;
    private static final boolean FULL_NEW_CHECK = false; // full validation (debug purposes)

    int currentSize = 0; // current size, accessed by ArrayRow and LinearSystem

    private final ArrayRow mRow; // our owner
    private final Cache mCache; // pointer to the system-wide cache, allowing access to SolverVariables

    private int ROW_SIZE = 8; // default array size

    private SolverVariable candidate = null;

    // mArrayIndices point to indexes in mCache.mIndexedVariables (i.e., the SolverVariables)
    private int[] mArrayIndices = new int[ROW_SIZE];

    // mArrayNextIndices point to indexes in mArrayIndices
    private int[] mArrayNextIndices = new int[ROW_SIZE];

    // mArrayValues contain the associated value from mArrayIndices
    private float[] mArrayValues = new float[ROW_SIZE];

    // mHead point to indexes in mArrayIndices
    private int mHead = NONE;

    // mLast point to indexes in mArrayIndices
    //
    // While mDidFillOnce is not set, mLast is simply incremented
    // monotonically in order to be sure to traverse the entire array; the idea here is that
    // when we clear a linked list, we only set the counters to zero without traversing the array to fill
    // it with NONE values, which would be costly.
    // But if we do not fill the array with NONE values, we cannot safely simply check if an entry
    // is set to NONE to know if we can use it or not, as it might contains a previous value...
    // So, when adding elements, we first ensure with this mechanism of mLast/mDidFillOnce
    // that we do traverse the array linearly, avoiding for that first pass the need to check for the value
    // of the item in mArrayIndices.
    // This does mean that removed elements will leave empty spaces, but we /then/ set the removed element
    // to NONE, so that once we did that first traversal filling the array, we can safely revert to linear traversal
    // finding an empty spot by checking the values of mArrayIndices (i.e. finding an item containing NONE).
    private int mLast = NONE;

    // flag to keep trace if we did a full pass of the array or not, see above description
    private boolean mDidFillOnce = false;

    // Exemple of a basic loop
    // current or previous point to mArrayIndices
    //
    // int current = mHead;
    // int counter = 0;
    // while (current != NONE && counter < currentSize) {
    //  SolverVariable currentVariable = mCache.mIndexedVariables[mArrayIndices[current]];
    //  float currentValue = mArrayValues[current];
    //  ...
    //  current = mArrayNextIndices[current]; counter++;
    // }

    /**
     * Constructor
     * @param arrayRow the row owning us
     * @param cache instances cache
     */
    ArrayLinkedVariables(ArrayRow arrayRow, Cache cache) {
        mRow = arrayRow;
        mCache = cache;
        if (DEBUG) {
            for (int i = 0; i < mArrayIndices.length; i++) {
                mArrayIndices[i] = NONE;
            }
        }
    }

    /**
     * Insert a variable with a given value in the linked list
     *
     * @param variable the variable to add in the list
     * @param value the value of the variable
     */
    public final void put(SolverVariable variable, float value) {
        if (value == 0) {
            remove(variable, true);
            return;
        }
        // Special casing empty list...
        if (mHead == NONE) {
            mHead = 0;
            mArrayValues[mHead] = value;
            mArrayIndices[mHead] = variable.id;
            mArrayNextIndices[mHead] = NONE;
            variable.usageInRowCount++;
            variable.addToRow(mRow);
            currentSize++;
            if (!mDidFillOnce) {
                // only increment mLast if we haven't done the first filling pass
                mLast++;
                if (mLast >= mArrayIndices.length) {
                    mDidFillOnce = true;
                    mLast = mArrayIndices.length-1;
                }
            }
            return;
        }
        int current = mHead;
        int previous = NONE;
        int counter = 0;
        while (current != NONE && counter < currentSize) {
            if (mArrayIndices[current] == variable.id) {
                mArrayValues[current] = value;
                return;
            }
            if (mArrayIndices[current] < variable.id) {
                previous = current;
            }
            current = mArrayNextIndices[current]; counter++;
        }

        // Not found, we need to insert

        // First, let's find an available spot
        int availableIndice = mLast + 1; // start from the previous spot
        if (mDidFillOnce) {
            // ... but if we traversed the array once, check the last index, which might have been
            // set by an element removed
            if (mArrayIndices[mLast] == NONE) {
                availableIndice = mLast;
            } else {
                availableIndice = mArrayIndices.length;
            }
        }
        if (availableIndice >= mArrayIndices.length) {
            if (currentSize < mArrayIndices.length) {
                // find an available spot
                for (int i = 0; i < mArrayIndices.length; i++) {
                    if (mArrayIndices[i] == NONE) {
                        availableIndice = i;
                        break;
                    }
                }
            }
        }
        // ... make sure to grow the array as needed
        if (availableIndice >= mArrayIndices.length) {
            availableIndice = mArrayIndices.length;
            ROW_SIZE *= 2;
            mDidFillOnce = false;
            mLast = availableIndice - 1;
            mArrayValues = Arrays.copyOf(mArrayValues, ROW_SIZE);
            mArrayIndices = Arrays.copyOf(mArrayIndices, ROW_SIZE);
            mArrayNextIndices = Arrays.copyOf(mArrayNextIndices, ROW_SIZE);
        }

        // Finally, let's insert the element
        mArrayIndices[availableIndice] = variable.id;
        mArrayValues[availableIndice] = value;
        if (previous != NONE) {
            mArrayNextIndices[availableIndice] = mArrayNextIndices[previous];
            mArrayNextIndices[previous] = availableIndice;
        } else {
            mArrayNextIndices[availableIndice] = mHead;
            mHead = availableIndice;
        }
        variable.usageInRowCount++;
        variable.addToRow(mRow);
        currentSize++;
        if (!mDidFillOnce) {
            // only increment mLast if we haven't done the first filling pass
            mLast++;
        }
        if (currentSize >= mArrayIndices.length) {
            mDidFillOnce = true;
        }
        if (mLast >= mArrayIndices.length) {
            mDidFillOnce = true;
            mLast = mArrayIndices.length-1;
        }
    }

    /**
     * Add value to an existing variable
     *
     * The code is broadly identical to the put() method, only differing
     * in in-line deletion, and of course doing an add rather than a put
     *  @param variable the variable we want to add
     * @param value its value
     * @param removeFromDefinition
     */
    final void add(SolverVariable variable, float value, boolean removeFromDefinition) {
        if (value == 0) {
            return;
        }
        // Special casing empty list...
        if (mHead == NONE) {
            mHead = 0;
            mArrayValues[mHead] = value;
            mArrayIndices[mHead] = variable.id;
            mArrayNextIndices[mHead] = NONE;
            variable.usageInRowCount++;
            variable.addToRow(mRow);
            currentSize++;
            if (!mDidFillOnce) {
                // only increment mLast if we haven't done the first filling pass
                mLast++;
                if (mLast >= mArrayIndices.length) {
                    mDidFillOnce = true;
                    mLast = mArrayIndices.length-1;
                }
            }
            return;
        }
        int current = mHead;
        int previous = NONE;
        int counter = 0;
        while (current != NONE && counter < currentSize) {
            int idx = mArrayIndices[current];
            if (idx == variable.id) {
                mArrayValues[current] += value;
                // Possibly delete immediately
                if (mArrayValues[current] == 0) {
                    if (current == mHead) {
                        mHead = mArrayNextIndices[current];
                    } else {
                        mArrayNextIndices[previous] = mArrayNextIndices[current];
                    }
                    if (removeFromDefinition) {
                        variable.removeFromRow(mRow);
                    }
                    if (mDidFillOnce) {
                        // If we did a full pass already, remember that spot
                        mLast = current;
                    }
                    variable.usageInRowCount--;
                    currentSize--;
                }
                return;
            }
            if (mArrayIndices[current] < variable.id) {
                previous = current;
            }
            current = mArrayNextIndices[current]; counter++;
        }

        // Not found, we need to insert

        // First, let's find an available spot
        int availableIndice = mLast + 1; // start from the previous spot
        if (mDidFillOnce) {
            // ... but if we traversed the array once, check the last index, which might have been
            // set by an element removed
            if (mArrayIndices[mLast] == NONE) {
                availableIndice = mLast;
            } else {
                availableIndice = mArrayIndices.length;
            }
        }
        if (availableIndice >= mArrayIndices.length) {
            if (currentSize < mArrayIndices.length) {
                // find an available spot
                for (int i = 0; i < mArrayIndices.length; i++) {
                    if (mArrayIndices[i] == NONE) {
                        availableIndice = i;
                        break;
                    }
                }
            }
        }
        // ... make sure to grow the array as needed
        if (availableIndice >= mArrayIndices.length) {
            availableIndice = mArrayIndices.length;
            ROW_SIZE *= 2;
            mDidFillOnce = false;
            mLast = availableIndice - 1;
            mArrayValues = Arrays.copyOf(mArrayValues, ROW_SIZE);
            mArrayIndices = Arrays.copyOf(mArrayIndices, ROW_SIZE);
            mArrayNextIndices = Arrays.copyOf(mArrayNextIndices, ROW_SIZE);
        }

        // Finally, let's insert the element
        mArrayIndices[availableIndice] = variable.id;
        mArrayValues[availableIndice] = value;
        if (previous != NONE) {
            mArrayNextIndices[availableIndice] = mArrayNextIndices[previous];
            mArrayNextIndices[previous] = availableIndice;
        } else {
            mArrayNextIndices[availableIndice] = mHead;
            mHead = availableIndice;
        }
        variable.usageInRowCount++;
        variable.addToRow(mRow);
        currentSize++;
        if (!mDidFillOnce) {
            // only increment mLast if we haven't done the first filling pass
            mLast++;
        }
        if (mLast >= mArrayIndices.length) {
            mDidFillOnce = true;
            mLast = mArrayIndices.length-1;
        }
    }

    /**
     * Remove a variable from the list
     *
     * @param variable the variable we want to remove
     * @param removeFromDefinition
     * @return the value of the removed variable
     */
    public final float remove(SolverVariable variable, boolean removeFromDefinition) {
        if (candidate == variable) {
            candidate = null;
        }
        if (mHead == NONE) {
            return 0;
        }
        int current = mHead;
        int previous = NONE;
        int counter = 0;
        while (current != NONE && counter < currentSize) {
            int idx = mArrayIndices[current];
            if (idx == variable.id) {
                if (current == mHead) {
                    mHead = mArrayNextIndices[current];
                } else {
                    mArrayNextIndices[previous] = mArrayNextIndices[current];
                }

                if (removeFromDefinition) {
                    variable.removeFromRow(mRow);
                }
                variable.usageInRowCount--;
                currentSize--;
                mArrayIndices[current] = NONE;
                if (mDidFillOnce) {
                    // If we did a full pass already, remember that spot
                    mLast = current;
                }
                return mArrayValues[current];
            }
            previous = current;
            current = mArrayNextIndices[current]; counter++;
        }
        return 0;
    }

    /**
     * Clear the list of variables
     */
    public final void clear() {
        int current = mHead;
        int counter = 0;
        while (current != NONE && counter < currentSize) {
            SolverVariable variable = mCache.mIndexedVariables[mArrayIndices[current]];
            if (variable != null) {
                variable.removeFromRow(mRow);
            }
            current = mArrayNextIndices[current]; counter++;
        }

        mHead = NONE;
        mLast = NONE;
        mDidFillOnce = false;
        currentSize = 0;
    }

    /**
     * Returns true if the variable is contained in the list
     *
     * @param variable the variable we are looking for
     * @return return true if we found the variable
     */
    final boolean containsKey(SolverVariable variable) {
        if (mHead == NONE) {
            return false;
        }
        int current = mHead;
        int counter = 0;
        while (current != NONE && counter < currentSize) {
            if (mArrayIndices[current] == variable.id) {
                return true;
            }
            current = mArrayNextIndices[current]; counter++;
        }
        return false;
    }

    /**
     * Returns true if at least one of the variable is positive
     *
     * @return true if at least one of the variable is positive
     */
    boolean hasAtLeastOnePositiveVariable() {
        int current = mHead;
        int counter = 0;
        while (current != NONE && counter < currentSize) {
            if (mArrayValues[current] > 0) {
                return true;
            }
            current = mArrayNextIndices[current]; counter++;
        }
        return false;
    }

    /**
     * Invert the values of all the variables in the list
     */
    void invert() {
        int current = mHead;
        int counter = 0;
        while (current != NONE && counter < currentSize) {
            mArrayValues[current] *= -1;
            current = mArrayNextIndices[current]; counter++;
        }
    }

    /**
     * Divide the values of all the variables in the list
     * by the given amount
     *
     * @param amount aount to divide by
     */
    void divideByAmount(float amount) {
        int current = mHead;
        int counter = 0;
        while (current != NONE && counter < currentSize) {
            mArrayValues[current] /= amount;
            current = mArrayNextIndices[current]; counter++;
        }
    }

    /**
     * Returns true if the variable is new to the system, i.e. is already present
     * in one of the rows. This function is called while choosing the subject of a new row.
     *
     * @param variable the variable to check for
     * @param system the linear system we check
     * @return
     */
    private boolean isNew(SolverVariable variable, LinearSystem system) {
        if (FULL_NEW_CHECK) {
            boolean isNew = true;
            for (int i = 0; i < system.mNumRows; i++) {
                ArrayRow row = system.mRows[i];
                if (row.hasVariable(variable)) {
                    isNew = false;
                }
            }
            if (variable.usageInRowCount <= 1 != isNew) {
                System.out.println("Problem with usage tracking");
            }
            return isNew;
        }
        // We maintain a usage count -- variables are ref counted if they are present
        // in the right side of a row or not. If the count is zero or one, the variable
        // is new (if one, it means it exist in a row, but this is the row we insert)
        return variable.usageInRowCount <= 1;
    }

    /**
     * Pick a subject variable out of the existing ones.
     * - if a variable is unrestricted
     * - or if it's a negative new variable (not found elsewhere)
     * - otherwise we return null
     *
     * @return a candidate variable we can pivot on or null if not found
     */
    SolverVariable chooseSubject(LinearSystem system) {
        // if unrestricted, pick it
        // if restricted, needs to be < 0 and new
        //
        SolverVariable restrictedCandidate = null;
        SolverVariable unrestrictedCandidate = null;
        float unrestrictedCandidateAmount = 0;
        float restrictedCandidateAmount = 0;
        boolean unrestrictedCandidateIsNew = false;
        boolean restrictedCandidateIsNew = false;
        int current = mHead;
        int counter = 0;
        float candidateAmount = 0;
        while (current != NONE && counter < currentSize) {
            float amount = mArrayValues[current];
            float epsilon = 0.001f;
            SolverVariable variable = mCache.mIndexedVariables[mArrayIndices[current]];
            if (amount < 0) {
                if (amount > -epsilon) {
                    mArrayValues[current] = 0;
                    amount = 0;
                    variable.removeFromRow(mRow);
                }
            } else {
                if (amount < epsilon) {
                    mArrayValues[current] = 0;
                    amount = 0;
                    variable.removeFromRow(mRow);
                }
            }
            if (amount != 0) {
                if (variable.mType == SolverVariable.Type.UNRESTRICTED) {
                    if (unrestrictedCandidate == null) {
                        unrestrictedCandidate = variable;
                        unrestrictedCandidateAmount = amount;
                        unrestrictedCandidateIsNew = isNew(variable, system);
                    } else if (unrestrictedCandidateAmount > amount) {
                        unrestrictedCandidate = variable;
                        unrestrictedCandidateAmount = amount;
                        unrestrictedCandidateIsNew = isNew(variable, system);
                    } else if (!unrestrictedCandidateIsNew && isNew(variable, system)) {
                        unrestrictedCandidate = variable;
                        unrestrictedCandidateAmount = amount;
                        unrestrictedCandidateIsNew = true;
                    }
                } else if (unrestrictedCandidate == null) {
                    if (amount < 0) {
                        if (restrictedCandidate == null) {
                            restrictedCandidate = variable;
                            restrictedCandidateAmount = amount;
                            restrictedCandidateIsNew = isNew(variable, system);
                        } else if (restrictedCandidateAmount > amount) {
                            restrictedCandidate = variable;
                            restrictedCandidateAmount = amount;
                            restrictedCandidateIsNew = isNew(variable, system);
                        } else if (!restrictedCandidateIsNew && isNew(variable, system)) {
                            restrictedCandidate = variable;
                            restrictedCandidateAmount = amount;
                            restrictedCandidateIsNew = true;
                        }
                    }
                }
            }
            current = mArrayNextIndices[current]; counter++;
        }
        if (unrestrictedCandidate != null) {
            return unrestrictedCandidate;
        }
        return restrictedCandidate;
    }

    /**
     * Update the current list with a new definition
     *  @param self the row we will update with the definition
     * @param definition the row containing the definition
     * @param removeFromDefinition
     */
    final void updateFromRow(ArrayRow self, ArrayRow definition, boolean removeFromDefinition) {
        // This is one of the two method (the other being updateFromSystem())
        // that is constantly being called while building and solving the linear system
        // performances are critical
        int current = mHead;
        int counter = 0;
        while (current != NONE && counter < currentSize) {
            if (mArrayIndices[current] == definition.variable.id) {
                float value = mArrayValues[current];
                remove(definition.variable, removeFromDefinition);
                // now, let's add all values from the definition
                ArrayLinkedVariables definitionVariables = (ArrayLinkedVariables) (Object) definition.variables;
                int definitionCurrent = definitionVariables.mHead;
                int definitionCounter = 0;
                while (definitionCurrent != NONE && definitionCounter < definitionVariables.currentSize) {
                    SolverVariable definitionVariable = mCache.mIndexedVariables[
                            definitionVariables.mArrayIndices[definitionCurrent]];
                    float definitionValue = definitionVariables.mArrayValues[definitionCurrent];
                    add(definitionVariable, definitionValue * value, removeFromDefinition);
                    definitionCurrent = definitionVariables.mArrayNextIndices[definitionCurrent]; definitionCounter++;
                }
                self.constantValue += definition.constantValue * value;
                if (removeFromDefinition) {
                    definition.variable.removeFromRow(self);
                }

                // Here we reset our counter as the linked list has changed, if we weren't doing that
                // we could potentially skip some of the original elements. On the other hand, this approach
                // allows us to not have to created a temporary list...
                current = mHead;
                counter = 0;
                continue;
            }
            current = mArrayNextIndices[current]; counter++;
        }
    }

    /**
     * Update the list of elements from the system definitions
     *
     * @param self the row to update
     * @param rows the array of rows we will update from (i.e., the system of equations)
     */
    void updateFromSystem(ArrayRow self, ArrayRow[] rows) {
        // This is one of the two method (the other being updateFromRow())
        // that is constantly being called while building and solving the linear system
        // performances are critical
        int current = mHead;
        int counter = 0;
        while (current != NONE && counter < currentSize) {
            SolverVariable variable = mCache.mIndexedVariables[mArrayIndices[current]];
            if (variable.definitionId != -1) {
                float value = mArrayValues[current];
                remove(variable, true);
                // now, let's add all values from the definition
                ArrayRow definition = rows[variable.definitionId];
                if (!definition.isSimpleDefinition) {
                    ArrayLinkedVariables definitionVariables = (ArrayLinkedVariables) (Object) definition.variables;
                    int definitionCurrent = definitionVariables.mHead;
                    int definitionCounter = 0;
                    while (definitionCurrent != NONE && definitionCounter < definitionVariables.currentSize) {
                        SolverVariable definitionVariable = mCache.mIndexedVariables[
                                definitionVariables.mArrayIndices[definitionCurrent]];
                        float definitionValue = definitionVariables.mArrayValues[definitionCurrent];
                        add(definitionVariable, definitionValue * value, true);
                        definitionCurrent = definitionVariables.mArrayNextIndices[definitionCurrent];
                        definitionCounter++;
                    }
                }
                self.constantValue += definition.constantValue * value;
                definition.variable.removeFromRow(self);

                // Here we reset our counter as the linked list has changed, if we weren't doing that
                // we could potentially skip some of the original elements. On the other hand, this approach
                // allows us to not have to created a temporary list...
                current = mHead;
                counter = 0;
                continue;
            }
            current = mArrayNextIndices[current]; counter++;
        }
    }

    /**
     * TODO: check if still needed
     * Return a pivot candidate
     * @return return a variable we can pivot on
     */
    SolverVariable getPivotCandidate() {
        if (candidate == null) {
            // if no candidate is known, let's figure it out
            int current = mHead;
            int counter = 0;
            SolverVariable pivot = null;
            while (current != NONE && counter < currentSize) {
                if (mArrayValues[current] < 0) {
                    // We can return the first negative candidate as in ArrayLinkedVariables
                    // they are already sorted by id

                    SolverVariable v = mCache.mIndexedVariables[mArrayIndices[current]];
                    if (pivot == null || pivot.strength < v.strength) {
                        pivot = v;
                    }
                }
                current = mArrayNextIndices[current]; counter++;
            }
            return pivot;
        }
        return candidate;
    }

    SolverVariable getPivotCandidate(boolean[] avoid, SolverVariable exclude) {
        int current = mHead;
        int counter = 0;
        SolverVariable pivot = null;
        float value = 0;
        while (current != NONE && counter < currentSize) {
            if (mArrayValues[current] < 0) {
                // We can return the first negative candidate as in ArrayLinkedVariables
                // they are already sorted by id

                SolverVariable v = mCache.mIndexedVariables[mArrayIndices[current]];
                if (!((avoid != null && avoid[v.id]) || (v == exclude))) {
                    if (v.mType == SolverVariable.Type.SLACK
                            || v.mType == SolverVariable.Type.ERROR) {
                        float currentValue = mArrayValues[current];
                        if (currentValue < value) {
                            value = currentValue;
                            pivot = v;
                        }
                    }
                }
            }
            current = mArrayNextIndices[current]; counter++;
        }
        return pivot;
    }

    /**
     * Return a variable from its position in the linked list
     *
     * @param index the index of the variable we want to return
     * @return the variable found, or null
     */
    final SolverVariable getVariable(int index) {
        int current = mHead;
        int counter = 0;
        while (current != NONE && counter < currentSize) {
            if (counter == index) {
                return mCache.mIndexedVariables[mArrayIndices[current]];
            }
            current = mArrayNextIndices[current]; counter++;
        }
        return null;
    }

    /**
     * Return the value of a variable from its position in the linked list
     *
     * @param index the index of the variable we want to look up
     * @return the value of the found variable, or 0 if not found
     */
    final float getVariableValue(int index) {
        int current = mHead;
        int counter = 0;
        while (current != NONE && counter < currentSize) {
            if (counter == index) {
                return mArrayValues[current];
            }
            current = mArrayNextIndices[current]; counter++;
        }
        return 0;
    }

    /**
     * Return the value of a variable, 0 if not found
     * @param v the variable we are looking up
     * @return the value of the found variable, or 0 if not found
     */
    public final float get(SolverVariable v) {
        int current = mHead;
        int counter = 0;
        while (current != NONE && counter < currentSize) {
            if (mArrayIndices[current] == v.id) {
                return mArrayValues[current];
            }
            current = mArrayNextIndices[current]; counter++;
        }
        return 0;
    }


    int sizeInBytes() {
        int size = 0;
        size += 3 * (mArrayIndices.length * 4);
        size += 9 * 4;
        return size;
    }

    public void display() {
        int count = currentSize;
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

    /**
     * Returns a string representation of the list
     *
     * @return a string containing a representation of the list
     */
    @Override
    public String toString() {
        String result = "";
        int current = mHead;
        int counter = 0;
        while (current != NONE && counter < currentSize) {
            result += " -> ";
            result += mArrayValues[current] + " : ";
            result += mCache.mIndexedVariables[mArrayIndices[current]];
            current = mArrayNextIndices[current]; counter++;
        }
        return result;
    }

}
