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
import java.util.HashSet;

/**
 * Represents and solve a system of linear equations.
 */
public class LinearSystem {

    private static final boolean DEBUG = false;

    /*
     * Default size for the object pools
     */
    private static final int POOL_SIZE = 1000;

    /*
     * Variable counter
     */
    private int mVariablesID = 0;

    /*
     * Store a map between name->SolverVariable and SolverVariable->Float for the resolution.
     */
    private HashMap<String, SolverVariable> mVariables = null;

    /*
     * The goal that is used when minimizing the system.
     */
    private ArrayRow mGoal;

    private int TABLE_SIZE = 32; // default table size for the allocation
    private int mMaxColumns = TABLE_SIZE;
    private ArrayRow[] mRows = null;

    // Used in optimize()
    private HashSet<SolverVariable> mAlreadyTestedCandidates = new HashSet<>();

    int mNumColumns = 1;
    SolverVariable[] mIndexedVariables = new SolverVariable[TABLE_SIZE];
    int mNumRows = 0;
    int mMaxRows = TABLE_SIZE;

    private static Pools.Pool<ArrayRow> sArrayRowPool;
    private static Pools.Pool<SolverVariable> sSolverVariablePool = new Pools.SimplePool<>(POOL_SIZE);
    private static SolverVariable[] sPoolVariables = new SolverVariable[POOL_SIZE];
    private static int sPoolVariablesCount = 0;

    public LinearSystem() {
        mRows = new ArrayRow[TABLE_SIZE];
        if (sArrayRowPool == null) {
            sArrayRowPool = new Pools.SimplePool<>(POOL_SIZE);
        }
        releaseRows();
    }

    /*--------------------------------------------------------------------------------------------*/
    // Memory management
    /*--------------------------------------------------------------------------------------------*/

    /**
     * Reallocate memory to accommodate increased amount of variables
     */
    void increaseTableSize() {
        TABLE_SIZE *= 2;
        mRows = Arrays.copyOf(mRows, TABLE_SIZE);
        mIndexedVariables = Arrays.copyOf(mIndexedVariables, TABLE_SIZE);
        mMaxColumns = TABLE_SIZE;
        mMaxRows = TABLE_SIZE;
        releaseGoal();
        mGoal = null;
    }

    /**
     * Release ArrayRows back to their pool
     */
    private void releaseRows() {
        for (int i = 0; i < mRows.length; i++) {
            ArrayRow row = (ArrayRow) mRows[i];
            if (row != null) {
                row.reset();
                sArrayRowPool.release(row);
            }
            mRows[i] = null;
        }
    }

    /**
     * Release the ArrayRow used for the goal back to the pool
     */
    private void releaseGoal() {
        if (mGoal != null) {
            mGoal.reset();
            sArrayRowPool.release((ArrayRow) mGoal);
        }
    }

    /**
     * Reset the LinearSystem object so that it can be reused.
     */
    public void reset() {
        for (int i = 0; i < mIndexedVariables.length; i++) {
            SolverVariable variable = mIndexedVariables[i];
            if (variable != null) {
                variable.reset();
            }
        }
        for (int i = 0; i < sPoolVariablesCount; i++) {
            sSolverVariablePool.release(sPoolVariables[i]);
        }
        sPoolVariablesCount = 0;

        Arrays.fill(mIndexedVariables, null);
        if (mVariables != null) {
            mVariables.clear();
        }
        mVariablesID = 0;
        releaseGoal();
        mGoal = null;
        mNumColumns = 1;
        for (int i = 0; i < mNumRows; i++) {
            mRows[i].setUsed(false);
        }
        releaseRows();
        mNumRows = 0;
        mAlreadyTestedCandidates.clear();
    }

    /*--------------------------------------------------------------------------------------------*/
    // Creation of rows / variables / errors
    /*--------------------------------------------------------------------------------------------*/

    /**
     * Add the equation to the system
     * @param e the equation we want to add.
     */
    public void addConstraint(LinearEquation e) {
        ArrayRow row = EquationCreation.createRowFromEquation(this, e);
        addConstraint(row);
    }

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
            if (variable.id == -1) {
                mVariablesID++;
                mNumColumns++;
                variable.id = mVariablesID;
                mIndexedVariables[mVariablesID] = variable;
            }
        }
        return variable;
    }

    ArrayRow createRow() {
        return createRow(1);
    }

    ArrayRow createRow(int sizeHint) {
        ArrayRow row = sArrayRowPool.acquire();
        if (row == null) {
            row = new ArrayRow();
        } else {
            row.reset();
        }
        return row;
    }

    SolverVariable createSlackVariable() {
        if (mNumColumns + 1 >= mMaxColumns) {
            increaseTableSize();
        }
        SolverVariable variable = acquireSolverVariable(SolverVariable.Type.SLACK);
        mVariablesID++;
        mNumColumns++;
        variable.id = mVariablesID;
        mIndexedVariables[mVariablesID] = variable;
        return variable;
    }

    void addError(ArrayRow row, int strength) {
        SolverVariable error1 = createErrorVariable(strength);
        SolverVariable error2 = createErrorVariable(strength);

        row.addError(error1, error2);
    }

    void addSingleError(ArrayRow row, int sign, int strength) {
        SolverVariable error = createErrorVariable(strength);
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
        mIndexedVariables[mVariablesID] = variable;
        return variable;
    }

    private SolverVariable createErrorVariable() {
        return createErrorVariable(1);
    }

    private SolverVariable createErrorVariable(int strength) {
        if (mNumColumns + 1 >= mMaxColumns) {
            increaseTableSize();
        }
        SolverVariable variable = acquireSolverVariable(SolverVariable.Type.ERROR);
        if (strength == 2) {
            variable.setStrength(SolverVariable.Strength.STRONG);
        }
        mVariablesID++;
        mNumColumns++;
        variable.id = mVariablesID;
        mIndexedVariables[mVariablesID] = variable;
        return variable;
    }

    /**
     * Returns a SolverVariable instance of the given type
     * @param type type of the SolverVariable
     * @return instance of SolverVariable
     */
    private SolverVariable acquireSolverVariable(SolverVariable.Type type) {
        SolverVariable variable = sSolverVariablePool.acquire();
        if (variable == null) {
            variable = new SolverVariable(type);
        } else {
            variable.setType(type);
            sPoolVariables[sPoolVariablesCount] = variable;
            sPoolVariablesCount++;
        }
        return variable;
    }

    /*--------------------------------------------------------------------------------------------*/
    // Accessors of rows / variables / errors
    /*--------------------------------------------------------------------------------------------*/

    /**
     * Simple accessor for the current goal. Used when minimizing the system's goal.
     * @return the current goal.
     */
    public ArrayRow getGoal() { return mGoal; }

    public ArrayRow getRow(int n) {
        return mRows[n];
    }

    public float getValueFor(String name) {
        SolverVariable v = getVariable(name, SolverVariable.Type.UNRESTRICTED);
        if (v == null) {
            return 0;
        }
        return v.copmutedValue;
    }

    public int getObjectVariableValue(Object anchor) {
        SolverVariable variable = ((ConstraintAnchor) anchor).getSolverVariable();
        if (variable != null) {
            return (int) variable.copmutedValue;
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
    public SolverVariable getVariable(String name, SolverVariable.Type type) {
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
    public void rebuildGoalFromErrors() {
        if (mGoal != null) {
            mGoal.reset();
        } else {
            mGoal = createRow();
        }
        for (int i = 1; i < mNumColumns; i++) {
            SolverVariable variable = mIndexedVariables[i];
            if (variable.mType == SolverVariable.Type.ERROR
                /* || variable.getType() == SolverVariable.Type.SLACK */) {
                mGoal.addVariable(variable);
            }
        }
        if (DEBUG) {
            System.out.println("system after rebuilding: ");
            displayReadableRows();
            System.out.println("rebuilt goal from errors: " + mGoal);
        }
    }

    /**
     * Minimize the current goal of the system.
     */
    public void minimize() throws Exception {
        rebuildGoalFromErrors();
        minimizeGoal(mGoal);
    }

    /**
     * Minimize the given goal with the current system.
     * @param goal the goal to minimize.
     */
    public void minimizeGoal(ArrayRow goal) throws Exception {
        // Update the equation with the variables already defined in the system

        for (int i = 0; i < mNumRows; i++) {
            goal.updateRowWithEquation(mRows[i]);
        }
        boolean validGoal = goal.hasAtLeastOneVariable();

        if (!validGoal) {
            computeValues();
            return;
        }

        if (DEBUG) {
            System.out.println("Minimize goal " + goal);
            displayReadableRows();
        }

        try {
            // First, let's make sure that the system is in Basic Feasible Solved Form (BFS), i.e.
            // all the constants of the restricted variables should be positive.
            int tries = enforceBFS(goal);
            if (DEBUG) {
                System.out.println("System in BFS ( " + tries + ") " + goal.toReadableString());
            }

            // The system at this point is supposed to be in Basic Feasible Solved Form (BFS),
            // so the only thing we have to do here is pivot on any candidates variables in the goal,
            // i.e. any variables that are negative (as they would decrease the goal's result).
            tries = optimize(goal);

            if (DEBUG) {
                System.out.println("Goal minimized ( " + tries + ") " + goal.toReadableString());
            }
            computeValues();
        } catch (Exception e) {
            computeValues();
            throw e;
        }
    }

    /**
     * Update the equation with the variables already defined in the system
     * @param row row to update
     */
    private void updateRowFromVariables(ArrayRow row) {
        ArrayRow equation = (ArrayRow) row;
        int numVariables = equation.variables.currentSize;
        for (int i = 0; i < numVariables; i++) {
            SolverVariable variable = equation.variables.variables[i];
            if (variable != null) {
                this.replaceVariable(row, variable);
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

        if (mRows[mNumRows] != null) {
            sArrayRowPool.release(mRows[mNumRows]);
        }
        row.updateClientEquations();
        mRows[mNumRows] = row;
        row.variable.definitionId = mNumRows;
        mNumRows++;

        ArrayRow[] clients = row.variable.mClientEquations;
        final int count = row.variable.mClientEquationsCount;
        for (int i = 0; i < count; i++) {
            ArrayRow client = clients[i];
            if (client == row) {
                continue;
            }
            client.updateRowWithEquation(row);
            client.updateClientEquations();
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
    private int optimize(ArrayRow goal) {
        boolean done = false;
        int tries = 0;
        mAlreadyTestedCandidates.clear();

        while (!done) {
            tries++;
            if (DEBUG) {
                System.out.println("iteration on system " + tries);
            }

            SolverVariable pivotCandidate = goal.findPivotCandidate();
            if (mAlreadyTestedCandidates.contains(pivotCandidate)) {
                pivotCandidate = null;
            } else if (pivotCandidate != null) {
                mAlreadyTestedCandidates.add(pivotCandidate);
                if (mAlreadyTestedCandidates.size() == mNumColumns) {
                    done = true;
                }
            }

            if (pivotCandidate != null) {
                // there's a negative variable in the goal that we can pivot on.
                // We now need to select which equation of the system we should do
                // the pivot on.

                // Let's try to find the equation in the system that we can pivot on.
                // The rules are simple:
                // - only look at restricted variables equations (i.e. Cs)
                // - only look at equations containing the column we are trying to pivot on (duh)
                // - select preferably an equation with strong strength over weak strength

                float minWeak = Float.MAX_VALUE;
                float minStrong = Float.MAX_VALUE;
                int pivotRowIndex;
                int pivotRowIndexWeak = -1;
                int pivotRowIndexStrong = -1;

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
                        float C = current.constantValue;
                        float a_j = current.getVariable(pivotCandidate);
                        if (a_j < 0) {
                            float value = (C * -1) / a_j;
                            if (pivotCandidate.mStrength ==
                                    SolverVariable.Strength.STRONG) {
                                if (value < minStrong) {
                                    minStrong = value;
                                    pivotRowIndexStrong = i;
                                }
                            } else {
                                if (value < minWeak) {
                                    minWeak = value;
                                    pivotRowIndexWeak = i;
                                }
                            }
                        }
                    }
                }
                if (pivotRowIndexStrong > -1) {
                    pivotRowIndex = pivotRowIndexStrong;
                } else {
                    pivotRowIndex = pivotRowIndexWeak;
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
                    goal.updateRowWithEquation(pivotEquation);
                    if (DEBUG) {
                        System.out.println("new goal after pivot: " + goal);
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
     * @param goal
     * @return number of iterations
     */
    private int enforceBFS(ArrayRow goal) throws Exception {
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
            if (!mRows[i].hasPositiveConstant()) {
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
                float minWeak = Float.MAX_VALUE;
                float minStrong = Float.MAX_VALUE;
                int pivotRowIndexWeak = -1;
                int pivotRowIndexStrong = -1;
                int pivotRowIndex;
                int pivotColumnIndexStrong = -1;
                int pivotColumnIndexWeak = -1;
                int pivotColumnIndex;

                for (int i = 0; i < mNumRows; i++) {
                    ArrayRow current = mRows[i];
                    SolverVariable variable = current.variable;
                    if (variable.mType == SolverVariable.Type.UNRESTRICTED) {
                        // skip unrestricted variables equations, as C
                        // can be either positive or negative.
                        continue;
                    }
                    if (!current.hasPositiveConstant()) {
                        // let's examine this row, see if we can find a good pivot
                        for (int j = 1; j < mNumColumns; j++) {
                            SolverVariable candidate = mIndexedVariables[j];
                            float a_j = current.getVariable(candidate);
                            if (a_j <= 0) {
                                continue;
                            }
                            float d_j = goal.getVariable(candidate);
                            float value = d_j / a_j;

                            if (variable.mStrength == SolverVariable.Strength.STRONG) {
                                if (value < minStrong) {
                                    minStrong = value;
                                    pivotRowIndexStrong = i;
                                    pivotColumnIndexStrong = j;
                                }
                            } else {
                                if (value < minWeak) {
                                    minWeak = value;
                                    pivotRowIndexWeak = i;
                                    pivotColumnIndexWeak = j;
                                }
                            }
                        }
                    }
                }

                if (pivotRowIndexStrong != -1) {
                    pivotRowIndex = pivotRowIndexStrong;
                    pivotColumnIndex = pivotColumnIndexStrong;
                } else {
                    pivotRowIndex = pivotRowIndexWeak;
                    pivotColumnIndex = pivotColumnIndexWeak;
                }

                if (pivotRowIndex != -1) {
                    // We have a pivot!
                    ArrayRow pivotEquation = mRows[pivotRowIndex];
                    pivotEquation.variable.definitionId = -1;
                    pivotEquation.pivot(mIndexedVariables[pivotColumnIndex]);
                    pivotEquation.variable.definitionId = pivotRowIndex;
                    // let's update the system with the new pivoted equation
                    for (int i = 0; i < mNumRows; i++) {
                        mRows[i].updateRowWithEquation(pivotEquation);
                    }
                    // let's update the goal equation as well
                    // TODO: might not be necessary here
                    goal.updateRowWithEquation(pivotEquation);
                    if (DEBUG) {
                        System.out.println("new goal after pivot: " + goal);
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
        infeasibleSystem = false;
        for (int i = 0; i < mNumRows; i++) {
            SolverVariable variable = mRows[i].variable;
            if (variable.mType == SolverVariable.Type.UNRESTRICTED) {
                continue; // C can be either positive or negative.
            }
            if (!mRows[i].hasPositiveConstant()) {
                infeasibleSystem = true;
                break;
            }
        }

        if (infeasibleSystem) {
//            throw new Exception();
        }

        return tries;
    }

    private void computeValues() {
        for (int i = 0; i < mNumRows; i++) {
            ArrayRow row = mRows[i];
            row.variable.copmutedValue = row.constantValue;
        }
    }

    /**
     * Replaces a given variable of target with its definition coming from the system
     * (if there is one)
     * @param target target row
     * @param variable variable to replace
     */
    void replaceVariable(ArrayRow target, SolverVariable variable) {
        int idx = variable.definitionId;
        if (idx != -1) {
            target.updateRowWithEquation(mRows[idx]);
        }
    }

    /*--------------------------------------------------------------------------------------------*/
    // Display utility functions
    /*--------------------------------------------------------------------------------------------*/

    public void displayRows() {
        displaySolverVariables();
        String s = "";
        for (int i = 0; i < mNumRows; i++) {
            s += mRows[i];
            s += "\n";
        }
        if (mGoal != null) {
            s += mGoal + "\n";
        }
        System.out.println(s);
    }

    public void displayReadableRows() {
        displaySolverVariables();
        String s = "";
        for (int i = 0; i < mNumRows; i++) {
            s += mRows[i].toReadableString();
            s += "\n";
        }
        if (mGoal != null) {
            s += mGoal.toReadableString() + "\n";
        }
        System.out.println(s);
    }

    public void displayVariablesReadableRows() {
        displaySolverVariables();
        String s = "";
        for (int i = 0; i < mNumRows; i++) {
            if (mRows[i].variable.mType == SolverVariable.Type.UNRESTRICTED) {
                s += mRows[i].toReadableString();
                s += "\n";
            }
        }
        if (mGoal != null) {
            s += mGoal.toReadableString() + "\n";
        }
        System.out.println(s);
    }

    public int getMemoryUsed() {
        int actualRowSize = 0;
        for (int i = 0; i < mNumRows; i++) {
            if (mRows[i] != null) {
                actualRowSize += mRows[i].sizeInBytes();
            }
        }
        return actualRowSize;
    }

    public int getNumEquations() { return mNumRows; }
    public int getNumVariables() { return mVariablesID; }

    /**
     * Display current system informations
     */
    public void displaySystemInformations() {
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
            SolverVariable v = mIndexedVariables[i];
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

}
