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

import android.support.constraint.solver.widgets.ConstraintAnchor;

import java.util.Arrays;
import java.util.HashMap;

/**
 * Represents and solve a system of linear equations.
 */
public class LinearSystem {

    private static final boolean DEBUG = false;

    /*
     * Default size for the object pools
     */
    private static int POOL_SIZE = 1000;

    /*
     * Variable counter
     */
    int mVariablesID = 0;

    /*
     * Store a map between name->SolverVariable and SolverVariable->Float for the resolution.
     */
    private HashMap<String, SolverVariable> mVariables = null;

    /*
     * The goal that is used when minimizing the system.
     */
    private Goal mGoal = new Goal();

    private int TABLE_SIZE = 32; // default table size for the allocation
    private int mMaxColumns = TABLE_SIZE;
    private ArrayRow[] mRows = null;

    // Used in optimize()
    private boolean[] mAlreadyTestedCandidates = new boolean[TABLE_SIZE];

    int mNumColumns = 1;
    private int mNumRows = 0;
    private int mMaxRows = TABLE_SIZE;

    final Cache mCache;

    private SolverVariable[] mPoolVariables = new SolverVariable[POOL_SIZE];
    private int mPoolVariablesCount = 0;

    private ArrayRow[] tempClientsCopy = new ArrayRow[TABLE_SIZE];

    public LinearSystem() {
        mRows = new ArrayRow[TABLE_SIZE];
        releaseRows();
        mCache = new Cache();
    }

    /*--------------------------------------------------------------------------------------------*/
    // Memory management
    /*--------------------------------------------------------------------------------------------*/

    /**
     * Reallocate memory to accommodate increased amount of variables
     */
    private void increaseTableSize() {
        TABLE_SIZE *= 2;
        mRows = Arrays.copyOf(mRows, TABLE_SIZE);
        mCache.mIndexedVariables = Arrays.copyOf(mCache.mIndexedVariables, TABLE_SIZE);
        mAlreadyTestedCandidates = new boolean[TABLE_SIZE];
        mMaxColumns = TABLE_SIZE;
        mMaxRows = TABLE_SIZE;
        mGoal.variables.clear();
    }

    /**
     * Release ArrayRows back to their pool
     */
    private void releaseRows() {
        for (int i = 0; i < mRows.length; i++) {
            ArrayRow row = mRows[i];
            if (row != null) {
                mCache.arrayRowPool.release(row);
            }
            mRows[i] = null;
        }
    }

    /**
     * Reset the LinearSystem object so that it can be reused.
     */
    public void reset() {
        for (int i = 0; i < mCache.mIndexedVariables.length; i++) {
            SolverVariable variable = mCache.mIndexedVariables[i];
            if (variable != null) {
                variable.reset();
            }
        }
        mCache.solverVariablePool.releaseAll(mPoolVariables, mPoolVariablesCount);
        mPoolVariablesCount = 0;

        Arrays.fill(mCache.mIndexedVariables, null);
        if (mVariables != null) {
            mVariables.clear();
        }
        mVariablesID = 0;
        mGoal.variables.clear();
        mNumColumns = 1;
        for (int i = 0; i < mNumRows; i++) {
            mRows[i].used = false;
        }
        releaseRows();
        mNumRows = 0;
    }

    /*--------------------------------------------------------------------------------------------*/
    // Creation of rows / variables / errors
    /*--------------------------------------------------------------------------------------------*/

    public SolverVariable createObjectVariable(Object anchor) {
        if (anchor == null) {
            return null;
        }
        if (mNumColumns + 1 >= mMaxColumns) {
            increaseTableSize();
        }
        SolverVariable variable = null;
        if (anchor instanceof ConstraintAnchor) {
            variable = ((ConstraintAnchor) anchor).getSolverVariable();
            if (variable == null) {
                ((ConstraintAnchor) anchor).resetSolverVariable(mCache);
                variable = ((ConstraintAnchor) anchor).getSolverVariable();
            }
            if (variable.id == -1
                || variable.id > mVariablesID
                || mCache.mIndexedVariables[variable.id] == null) {
                if (variable.id != -1) {
                    variable.reset();
                }
                mVariablesID++;
                mNumColumns++;
                variable.id = mVariablesID;
                variable.mType = SolverVariable.Type.UNRESTRICTED;
                mCache.mIndexedVariables[mVariablesID] = variable;
            }
        }
        return variable;
    }

    public ArrayRow createRow() {
        ArrayRow row = mCache.arrayRowPool.acquire();
        if (row == null) {
            row = new ArrayRow(mCache);
        } else {
            row.reset();
        }
        return row;
    }

    public SolverVariable createSlackVariable() {
        if (mNumColumns + 1 >= mMaxColumns) {
            increaseTableSize();
        }
        SolverVariable variable = acquireSolverVariable(SolverVariable.Type.SLACK);
        mVariablesID++;
        mNumColumns++;
        variable.id = mVariablesID;
        mCache.mIndexedVariables[mVariablesID] = variable;
        return variable;
    }

    private void addError(ArrayRow row) {
        SolverVariable error1 = createErrorVariable();
        SolverVariable error2 = createErrorVariable();

        row.addError(error1, error2);
    }

    private void addSingleError(ArrayRow row, int sign) {
        SolverVariable error = createErrorVariable();
        row.addSingleError(error, sign);
    }

    private SolverVariable createVariable(String name, SolverVariable.Type type) {
        if (mNumColumns + 1 >= mMaxColumns) {
            increaseTableSize();
        }
        SolverVariable variable = acquireSolverVariable(type);
        variable.setName(name);
        mVariablesID++;
        mNumColumns++;
        variable.id = mVariablesID;
        if (mVariables == null) {
            mVariables = new HashMap<>();
        }
        mVariables.put(name, variable);
        mCache.mIndexedVariables[mVariablesID] = variable;
        return variable;
    }

    public SolverVariable createErrorVariable() {
        if (mNumColumns + 1 >= mMaxColumns) {
            increaseTableSize();
        }
        SolverVariable variable = acquireSolverVariable(SolverVariable.Type.ERROR);
        mVariablesID++;
        mNumColumns++;
        variable.id = mVariablesID;
        mCache.mIndexedVariables[mVariablesID] = variable;
        return variable;
    }

    /**
     * Returns a SolverVariable instance of the given type
     * @param type type of the SolverVariable
     * @return instance of SolverVariable
     */
    private SolverVariable acquireSolverVariable(SolverVariable.Type type) {
        SolverVariable variable = mCache.solverVariablePool.acquire();
        if (variable == null) {
            variable = new SolverVariable(type);
        } else {
            variable.reset();
            variable.setType(type);
        }
        if (mPoolVariablesCount >= POOL_SIZE) {
            POOL_SIZE *= 2;
            mPoolVariables = Arrays.copyOf(mPoolVariables, POOL_SIZE);
        }
        mPoolVariables[mPoolVariablesCount++] = variable;
        return variable;
    }

    /*--------------------------------------------------------------------------------------------*/
    // Accessors of rows / variables / errors
    /*--------------------------------------------------------------------------------------------*/

    /**
     * Simple accessor for the current goal. Used when minimizing the system's goal.
     * @return the current goal.
     */
    Goal getGoal() { return mGoal; }

    ArrayRow getRow(int n) {
        return mRows[n];
    }

    float getValueFor(String name) {
        SolverVariable v = getVariable(name, SolverVariable.Type.UNRESTRICTED);
        if (v == null) {
            return 0;
        }
        return v.computedValue;
    }

    public int getObjectVariableValue(Object anchor) {
        SolverVariable variable = ((ConstraintAnchor) anchor).getSolverVariable();
        if (variable != null) {
            return (int) (variable.computedValue + 0.5f);
        }
        return 0;
    }

    /**
     * Returns a SolverVariable instance given a name and a type.
     *
     * @param name name of the variable
     * @param type {@link SolverVariable.Type type} of the variable
     * @return a SolverVariable instance
     */
    SolverVariable getVariable(String name, SolverVariable.Type type) {
        if (mVariables == null) {
            mVariables = new HashMap<>();
        }
        SolverVariable variable = mVariables.get(name);
        if (variable == null) {
            variable = createVariable(name, type);
        }
        return variable;
    }

    /*--------------------------------------------------------------------------------------------*/
    // System resolution
    /*--------------------------------------------------------------------------------------------*/

    /**
     * Rebuild the goal from the errors and slack variables
     */
    void rebuildGoalFromErrors() {
        mGoal.updateFromSystem(this);
        if (DEBUG) {
            System.out.println("GOAL built from errors: " + mGoal);
        }
    }

    /**
     * Minimize the current goal of the system.
     */
    public void minimize() throws Exception {
        minimizeGoal(mGoal);
    }

    /**
     * Minimize the given goal with the current system.
     * @param goal the goal to minimize.
     */
    void minimizeGoal(Goal goal) throws Exception {
        // First, let's make sure that the system is in Basic Feasible Solved Form (BFS), i.e.
        // all the constants of the restricted variables should be positive.
        goal.updateFromSystem(this);
        enforceBFS(goal);
        if (DEBUG) {
            System.out.println("Goal after enforcing BFS " + goal);
            displayReadableRows();
        }
        optimize(goal);
        if (DEBUG) {
            System.out.println("Goal after optimization " + goal);
            displayReadableRows();
        }
        computeValues();
    }

    /**
     * Update the equation with the variables already defined in the system
     * @param row row to update
     */
    private void updateRowFromVariables(ArrayRow row) {
        if (mNumRows > 0) {
            row.variables.updateFromSystem(row, mRows);
            if (row.variables.currentSize == 0) {
                row.isSimpleDefinition = true;
            }
        }
    }

    /**
     * Add the equation to the system
     * @param row the equation we want to add expressed as a system row.
     */
    public void addConstraint(ArrayRow row) {
        if (row == null) {
            return;
        }
        if (mNumRows + 1 >= mMaxRows || mNumColumns + 1 >= mMaxColumns) {
            increaseTableSize();
        }
        if (DEBUG) {
            System.out.println("addConstraint: " + row.toReadableString());
        }

        if (!row.isSimpleDefinition) {
            // Update the equation with the variables already defined in the system
            updateRowFromVariables(row);

            // First, ensure that if we have a constant it's positive
            row.ensurePositiveConstant();

            if (DEBUG) {
                System.out.println("addConstraint, updated row : " + row.toReadableString());
            }

            // Then pick a good variable to use for the row
            row.pickRowVariable();

            if (!row.hasKeyVariable()) {
                // Can happen if row resolves to nil
                if (DEBUG) {
                    System.out.println("No variable found to pivot on " + row.toReadableString());
                    displayReadableRows();
                }
                // We haven't found a variable to pivot on. Normally, we should introduce a new stay
                // variable to solve the system, then solve, and possibly remove the stay variable.
                // But the equations we insert have (for this exact purpose) balanced +/- variables,
                // so should not be necessary. Let's simply exit.
                return;
            }
        }
        if (mRows[mNumRows] != null) {
            mCache.arrayRowPool.release(mRows[mNumRows]);
        }
        if (!row.isSimpleDefinition) {
            row.updateClientEquations();
        }
        mRows[mNumRows] = row;
        row.variable.definitionId = mNumRows;
        mNumRows++;

        final int count = row.variable.mClientEquationsCount;
        if (count > 0) {
            while (tempClientsCopy.length < count) {
                tempClientsCopy = new ArrayRow[tempClientsCopy.length * 2];
            }
            ArrayRow[] clients = tempClientsCopy;
            //noinspection ManualArrayCopy
            for (int i = 0; i < count; i++) {
                clients[i] = row.variable.mClientEquations[i];
            }
            for (int i = 0; i < count; i++) {
                ArrayRow client = clients[i];
                if (client == row) {
                    continue;
                }
                client.variables.updateFromRow(client, row);
                client.updateClientEquations();
            }
        }

        if (DEBUG) {
            System.out.println("Row added, here is the system:");
            displayReadableRows();
        }
    }

    /**
     * Optimize the system given a goal to minimize. The system should be in BFS form.
     * @param goal goal to optimize.
     * @return number of iterations.
     */
    private int optimize(Goal goal) {
        boolean done = false;
        int tries = 0;
        for (int i = 0; i < mNumColumns; i++) {
            mAlreadyTestedCandidates[i] = false;
        }
        int tested = 0;

        while (!done) {
            tries++;
            if (DEBUG) {
                System.out.println("iteration on system " + tries);
            }

            SolverVariable pivotCandidate = goal.getPivotCandidate();
            if (DEBUG) {
                System.out.println("pivot candidate: " + pivotCandidate);
            }
            if (pivotCandidate != null) {
                if (mAlreadyTestedCandidates[pivotCandidate.id]) {
                    pivotCandidate = null;
                } else {
                    mAlreadyTestedCandidates[pivotCandidate.id] = true;
                    tested++;
                    if (tested >= mNumColumns) {
                        done = true;
                    }
                }
            }

            if (pivotCandidate != null) {
                if (DEBUG) {
                    System.out.println("valid pivot candidate: " + pivotCandidate);
                }
                // there's a negative variable in the goal that we can pivot on.
                // We now need to select which equation of the system we should do
                // the pivot on.

                // Let's try to find the equation in the system that we can pivot on.
                // The rules are simple:
                // - only look at restricted variables equations (i.e. Cs)
                // - only look at equations containing the column we are trying to pivot on (duh)
                // - select preferably an equation with strong strength over weak strength

                float min = Float.MAX_VALUE;
                int pivotRowIndex = -1;

                for (int i = 0; i < mNumRows; i++) {
                    ArrayRow current = mRows[i];
                    SolverVariable variable = current.variable;
                    if (variable.mType == SolverVariable.Type.UNRESTRICTED) {
                        // skip unrestricted variables equations.
                        continue;
                    }
                    if (current.hasVariable(pivotCandidate)) {
                        // the current row does contains the variable
                        // we want to pivot on
                        float a_j = current.variables.get(pivotCandidate);
                        if (a_j < 0) {
                            float value = - current.constantValue / a_j;
                            if (value < min) {
                                min = value;
                                pivotRowIndex = i;
                            }
                        }
                    }
                }
                // At this point, we ought to have an equation to pivot on,
                // either weak or strong.

                if (pivotRowIndex > -1) {
                    // We found an equation to pivot on
                    if (DEBUG) {
                        System.out.println("We pivot on " + pivotRowIndex);
                    }
                    ArrayRow pivotEquation = mRows[pivotRowIndex];
                    pivotEquation.variable.definitionId = -1;
                    pivotEquation.pivot(pivotCandidate);
                    pivotEquation.variable.definitionId = pivotRowIndex;
                    // let's update the system with the new pivoted equation
                    for (int i = 0; i < mNumRows; i++) {
                        mRows[i].updateRowWithEquation(pivotEquation);
                    }
                    // let's update the goal equation as well
                    goal.updateFromSystem(this);
                    if (DEBUG) {
                        System.out.println("new goal after pivot: " + goal);
                        displayReadableRows();
                    }
                    try {
                        enforceBFS(goal);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    // now that we pivoted, we're going to continue looping on the next goal
                    // columns, until we exhaust all the possibilities of improving the system
                } else {
                    // We couldn't find an equation to pivot, we should exit the loop.
                    done = true;
                }

            } else {
                // There is no candidate goals columns we should try to pivot on,
                // so let's exit the loop.
                done = true;
            }
        }
        return tries;
    }

    /**
     * Make sure that the system is in Basic Feasible Solved form (BFS).
     * @param goal the row representing the system goal
     * @return number of iterations
     */
    private int enforceBFS(Goal goal) throws Exception {
        int tries = 0;
        boolean done;

        // At this point, we might not be in Basic Feasible Solved form (BFS),
        // i.e. one of the restricted equation has a negative constant.
        // Let's check if that's the case or not.
        boolean infeasibleSystem = false;
        for (int i = 0; i < mNumRows; i++) {
            SolverVariable variable = mRows[i].variable;
            if (variable.mType == SolverVariable.Type.UNRESTRICTED) {
                continue; // C can be either positive or negative.
            }
            if (mRows[i].constantValue < 0) {
                infeasibleSystem = true;
                break;
            }
        }

        // The system happens to not be in BFS form, we need to go back to it to properly solve it.
        if (infeasibleSystem) {
            if (DEBUG) {
                System.out.println("the current system is infeasible, let's try to fix this.");
            }

            // Going back to BFS form can be done by selecting any equations in Cs containing
            // a negative constant, then selecting a potential pivot variable that would remove
            // this negative constant. Once we have
            done = false;
            tries = 0;
            while (!done) {
                tries++;
                if (DEBUG) {
                    System.out.println("iteration on infeasible system " + tries);
                }
                float min = Float.MAX_VALUE;
                int strength = 0;
                int pivotRowIndex = -1;
                int pivotColumnIndex = -1;

                for (int i = 0; i < mNumRows; i++) {
                    ArrayRow current = mRows[i];
                    SolverVariable variable = current.variable;
                    if (variable.mType == SolverVariable.Type.UNRESTRICTED) {
                        // skip unrestricted variables equations, as C
                        // can be either positive or negative.
                        continue;
                    }
                    if (current.constantValue < 0) {
                        // let's examine this row, see if we can find a good pivot
                        if (DEBUG) {
                            System.out.println("looking at pivoting on row " + current);
                        }
                        for (int j = 1; j < mNumColumns; j++) {
                            SolverVariable candidate = mCache.mIndexedVariables[j];
                            float a_j = current.variables.get(candidate);
                            if (a_j <= 0) {
                                continue;
                            }
                            if (DEBUG) {
                                System.out.println("candidate for pivot " + candidate);
                            }
                            for (int k = 0; k < SolverVariable.MAX_STRENGTH; k++) {
                                float value = candidate.strengthVector[k] / a_j;
                                if (value < min && k == strength || k > strength) {
                                    min = value;
                                    pivotRowIndex = i;
                                    pivotColumnIndex = j;
                                    strength = k;
                                }
                            }
                        }
                    }
                }

                if (pivotRowIndex != -1) {
                    // We have a pivot!
                    ArrayRow pivotEquation = mRows[pivotRowIndex];
                    if (DEBUG) {
                        System.out.println("Pivoting on " + pivotEquation.variable + " with "
                                + mCache.mIndexedVariables[pivotColumnIndex]);
                    }
                    pivotEquation.variable.definitionId = -1;
                    pivotEquation.pivot(mCache.mIndexedVariables[pivotColumnIndex]);
                    pivotEquation.variable.definitionId = pivotRowIndex;
                    // let's update the system with the new pivoted equation
                    for (int i = 0; i < mNumRows; i++) {
                        mRows[i].updateRowWithEquation(pivotEquation);
                    }
                    // let's update the goal equation as well
                    goal.updateFromSystem(this);
                    if (DEBUG) {
                        System.out.println("new goal after pivot: " + goal);
                        displayRows();
                    }
                } else {
                    done = true;
                }
            }
        }

        if (DEBUG) {
            System.out.println("the current system should now be feasible [" + infeasibleSystem + "] after " + tries + " iterations");
            displayReadableRows();
        }

        // Let's make sure the system is correct
        //noinspection UnusedAssignment
        infeasibleSystem = false;
        for (int i = 0; i < mNumRows; i++) {
            SolverVariable variable = mRows[i].variable;
            if (variable.mType == SolverVariable.Type.UNRESTRICTED) {
                continue; // C can be either positive or negative.
            }
            if (mRows[i].constantValue < 0) {
                //noinspection UnusedAssignment
                infeasibleSystem = true;
                break;
            }
        }

        if (DEBUG && infeasibleSystem) {
            throw new Exception();
        }

        return tries;
    }

    private void computeValues() {
        for (int i = 0; i < mNumRows; i++) {
            ArrayRow row = mRows[i];
            row.variable.computedValue = row.constantValue;
        }
    }

    /*--------------------------------------------------------------------------------------------*/
    // Display utility functions
    /*--------------------------------------------------------------------------------------------*/

    @SuppressWarnings("unused")
    private void displayRows() {
        displaySolverVariables();
        String s = "";
        for (int i = 0; i < mNumRows; i++) {
            s += mRows[i];
            s += "\n";
        }
        if (mGoal.variables.size() != 0) {
            s += mGoal + "\n";
        }
        System.out.println(s);
    }

    void displayReadableRows() {
        displaySolverVariables();
        String s = "";
        for (int i = 0; i < mNumRows; i++) {
            s += mRows[i].toReadableString();
            s += "\n";
        }
        if (mGoal != null) {
            s += mGoal + "\n";
        }
        System.out.println(s);
    }

    @SuppressWarnings("unused")
    public void displayVariablesReadableRows() {
        displaySolverVariables();
        String s = "";
        for (int i = 0; i < mNumRows; i++) {
            if (mRows[i].variable.mType == SolverVariable.Type.UNRESTRICTED) {
                s += mRows[i].toReadableString();
                s += "\n";
            }
        }
        if (mGoal.variables.size() != 0) {
            s += mGoal + "\n";
        }
        System.out.println(s);
    }

    @SuppressWarnings("unused")
    public int getMemoryUsed() {
        int actualRowSize = 0;
        for (int i = 0; i < mNumRows; i++) {
            if (mRows[i] != null) {
                actualRowSize += mRows[i].sizeInBytes();
            }
        }
        return actualRowSize;
    }

    @SuppressWarnings("unused")
    public int getNumEquations() { return mNumRows; }
    @SuppressWarnings("unused")
    public int getNumVariables() { return mVariablesID; }

    /**
     * Display current system informations
     */
    void displaySystemInformations() {
        int count = 0;
        int rowSize = 0;
        for (int i = 0; i < TABLE_SIZE; i++) {
            if (mRows[i] != null) {
                rowSize += mRows[i].sizeInBytes();
            }
        }
        int actualRowSize = 0;
        for (int i = 0; i < mNumRows; i++) {
            if (mRows[i] != null) {
                actualRowSize += mRows[i].sizeInBytes();
            }
        }

        System.out.println("Linear System -> Table size: " + TABLE_SIZE
                + " (" + getDisplaySize(TABLE_SIZE * TABLE_SIZE)
                + ") -- row sizes: " + getDisplaySize(rowSize)
                + ", actual size: " + getDisplaySize(actualRowSize)
                + " rows: " + mNumRows + "/" + mMaxRows
                + " cols: " + mNumColumns + "/" + mMaxColumns
                + " " + count + " occupied cells, " + getDisplaySize(count)
        );
    }

    private void displaySolverVariables() {
        String s = "Display Rows (" + mNumRows + "x" + mNumColumns + ") :\n\t | C | ";
        for (int i = 1; i <= mNumColumns; i++) {
            SolverVariable v = mCache.mIndexedVariables[i];
            s += v;
            s += " | ";
        }
        s += "\n";
        System.out.println(s);
    }

    private String getDisplaySize(int n) {
        int mb = (n * 4) / 1024 / 1024;
        if (mb > 0) {
            return "" + mb + " Mb";
        }
        int kb = (n * 4) / 1024;
        if (kb > 0) {
            return "" + kb + " Kb";
        }
        return "" + (n * 4) + " bytes";
    }

    public Cache getCache() {
        return mCache;
    }

    /**
     * Add an equation of the form a >= b + margin
     * @param a variable a
     * @param b variable b
     * @param margin margin
     * @param strength strength used
     */
    public void addGreaterThan(SolverVariable a, SolverVariable b, int margin, int strength) {
        if (DEBUG) {
            System.out.println("-> " + a + " >= " + b + (margin != 0 ? " + " + margin : ""));
        }
        ArrayRow row = createRow();
        SolverVariable slack = createSlackVariable();
        slack.strength = strength;
        row.createRowGreaterThan(a, b, slack, margin);
        addConstraint(row);
    }

    /**
     * Add an equation of the form a <= b + margin
     * @param a variable a
     * @param b variable b
     * @param margin margin
     * @param strength strength used
     */
    public void addLowerThan(SolverVariable a, SolverVariable b, int margin, int strength) {
        if (DEBUG) {
            System.out.println("-> " + a + " <= " + b + (margin != 0 ? " + " + margin : ""));
        }
        ArrayRow row = createRow();
        SolverVariable slack = createSlackVariable();
        slack.strength = strength;
        row.createRowLowerThan(a, b, slack, margin);
        addConstraint(row);
    }

    /**
     * Add an equation of the form (1 - bias) * (a - b) = bias * (c - d)
     * @param a variable a
     * @param b variable b
     * @param m1 margin 1
     * @param bias bias between ab - cd
     * @param c variable c
     * @param d variable d
     * @param m2 margin 2
     * @param strength strength used
     */
    public void addCentering(SolverVariable a, SolverVariable b, int m1, float bias,
                             SolverVariable c, SolverVariable d, int m2, int strength) {
        if (DEBUG) {
            System.out.println("-> " + a + " - " + b + " = " + c + " - " + d);
        }
        ArrayRow row = createRow();
        row.createRowCentering(a, b, m1, bias, c, d, m2);
        SolverVariable error1 = createErrorVariable();
        SolverVariable error2 = createErrorVariable();
        error1.strength = strength;
        error2.strength = strength;
        row.addError(error1, error2);
        addConstraint(row);
    }

    /**
     * Add an equation of the form a = b + margin
     * @param a variable a
     * @param b variable b
     * @param margin margin used
     * @param strength strength used
     */
    public ArrayRow addEquality(SolverVariable a, SolverVariable b, int margin, int strength) {
        if (DEBUG) {
            System.out.println("-> " + a + " = " + b + (margin != 0 ? " + " + margin : ""));
        }
        ArrayRow row = createRow();
        row.createRowEquals(a, b, margin);
        SolverVariable error1 = createErrorVariable();
        SolverVariable error2 = createErrorVariable();
        error1.strength = strength;
        error2.strength = strength;
        row.addError(error1, error2);
        addConstraint(row);
        return row;
    }

    /**
     * Add an equation of the form a = value
     * @param a variable a
     * @param value the value we set
     */
    public void addEquality(SolverVariable a, int value) {
        if (DEBUG) {
            System.out.println("-> " + a + " = " + value);
        }
        int idx = a.definitionId;
        if (a.definitionId != -1) {
            ArrayRow row = mRows[idx];
            if (row.isSimpleDefinition) {
                row.constantValue = value;
            } else {
                ArrayRow newRow = createRow();
                newRow.createRowEquals(a, value);
                addConstraint(newRow);
            }
        } else {
            ArrayRow row = createRow();
            row.createRowDefinition(a, value);
            addConstraint(row);
        }
    }


    public static ArrayRow createRowEquals(LinearSystem linearSystem, SolverVariable variableA,
                                           SolverVariable variableB, int margin, boolean withError) {
        // expression is: variableA = variableB + margin
        // we turn it into row = margin - variableA + variableB
        ArrayRow row = linearSystem.createRow();
        row.createRowEquals(variableA, variableB, margin);
        if (withError) {
            linearSystem.addSingleError(row, 1);
            if (DEBUG) {
                System.out.println(
                        "Add " + variableA.getName() + " = " + variableB + " + " + margin + " + e");
            }
        } else {
            if (DEBUG) {
                System.out
                        .println("Add " + variableA.getName() + " = " + variableB + " + " + margin);
            }
        }
        return row;
    }

    /**
     * Create a constraint to express A = B + (C - B) * percent
     * @param linearSystem the system we create the row on
     * @param variableA variable a
     * @param variableB variable b
     * @param variableC variable c
     * @param percent the percent used
     * @return the created row
     */
    public static ArrayRow createRowDimensionPercent(LinearSystem linearSystem,
                                                     SolverVariable variableA,
                                                     SolverVariable variableB, SolverVariable variableC, float percent, boolean withError) {
        ArrayRow row = linearSystem.createRow();
        if (withError) {
            linearSystem.addError(row);
        }
        return row.createRowDimensionPercent(variableA, variableB, variableC, percent);
    }

    public static ArrayRow createRowGreaterThan(LinearSystem linearSystem, SolverVariable variableA,
                                                SolverVariable variableB, int margin, boolean withError) {
        // expression is: variableA >= variableB + margin
        // we turn it into: variableA - slack = variableB + margin
        // row = margin - variableA + variableB + slack
        SolverVariable slack = linearSystem.createSlackVariable();
        ArrayRow row = linearSystem.createRow();
        row.createRowGreaterThan(variableA, variableB, slack, margin);
        if (withError) {
            float slackValue = row.variables.get(slack);
            linearSystem.addSingleError(row, (int) (-1 * slackValue));
            if (DEBUG) {
                System.out.println(
                        "Add " + variableA.getName() + " >= " + variableB.getName() + " + " +
                                margin + " + e");
            }
        } else {
            if (DEBUG) {
                System.out.println(
                        "Add " + variableA.getName() + " >= " + variableB.getName() + " + " +
                                margin);
            }
        }
        return row;
    }

    public static ArrayRow createRowLowerThan(LinearSystem linearSystem, SolverVariable variableA,
                                              SolverVariable variableB, int margin, boolean withError) {
        // expression is: variableA <= variableB + margin
        // we turn it into: variableA + slack = variableB + margin
        // row = margin - variableA + variableB - slack
        SolverVariable slack = linearSystem.createSlackVariable();
        ArrayRow row = linearSystem.createRow();
        row.createRowLowerThan(variableA, variableB, slack, margin);
        if (withError) {
            float slackValue = row.variables.get(slack);
            linearSystem.addSingleError(row, (int) (-1 * slackValue));
            if (DEBUG) {
                System.out.println(
                        "Add " + variableA.getName() + " <= " + variableB.getName() + " + " +
                                margin + " + e");
            }
        } else {
            if (DEBUG) {
                System.out.println(
                        "Add " + variableA.getName() + " <= " + variableB.getName() + " + " +
                                margin);
            }
        }
        return row;
    }

    public static ArrayRow createRowCentering(LinearSystem linearSystem,
                                              SolverVariable variableA, SolverVariable variableB, int marginA,
                                              float bias,
                                              SolverVariable variableC, SolverVariable variableD, int marginB,
                                              boolean withError) {
        // expression is: (1 - bias) * (variableA - variableB) = bias * (variableC - variableD)
        // we turn it into:
        // row = (1 - bias) * variableA - (1 - bias) * variableB - bias * variableC + bias * variableD
        ArrayRow row = linearSystem.createRow();
        row.createRowCentering(variableA, variableB, marginA, bias,
                variableC, variableD, marginB);
        if (withError) {
            SolverVariable error1 = linearSystem.createErrorVariable();
            SolverVariable error2 = linearSystem.createErrorVariable();
            error1.strength = SolverVariable.STRENGTH_HIGHEST;
            error2.strength = SolverVariable.STRENGTH_HIGHEST;
            row.addError(error1, error2);
            if (DEBUG) {
                System.out.println(
                        "Add centering " + variableA.getName() + " - " + variableB.getName() +
                                " = " + variableC.getName() + " - " + variableD.getName() +
                                " +/- e");
            }
        } else {
            if (DEBUG) {
                System.out.println(
                        "Add centering " + variableA.getName() + " - " + variableB.getName() +
                                " = " + variableC.getName() + " - " + variableD.getName());
            }
        }
        return row;
    }
}
