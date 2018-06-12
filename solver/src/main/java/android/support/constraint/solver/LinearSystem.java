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
import android.support.constraint.solver.widgets.ConstraintWidget;

import java.util.Arrays;
import java.util.HashMap;

/**
 * Represents and solve a system of linear equations.
 */
public class LinearSystem {

    public static final boolean FULL_DEBUG = false;

    private static final boolean DEBUG = FULL_DEBUG;

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
    private Row mGoal;

    private int TABLE_SIZE = 32; // default table size for the allocation
    private int mMaxColumns = TABLE_SIZE;
    ArrayRow[] mRows = null;

    // if true, will use graph optimizations
    public boolean graphOptimizer = false;

    // Used in optimize()
    private boolean[] mAlreadyTestedCandidates = new boolean[TABLE_SIZE];

    int mNumColumns = 1;
    int mNumRows = 0;
    private int mMaxRows = TABLE_SIZE;

    final Cache mCache;

    private SolverVariable[] mPoolVariables = new SolverVariable[POOL_SIZE];
    private int mPoolVariablesCount = 0;

    private ArrayRow[] tempClientsCopy = new ArrayRow[TABLE_SIZE];
    public static Metrics sMetrics;

    private final Row mTempGoal;

    public LinearSystem() {
        mRows = new ArrayRow[TABLE_SIZE];
        releaseRows();
        mCache = new Cache();
        mGoal = new GoalRow(mCache);
        mTempGoal = new ArrayRow(mCache);
    }

    public void fillMetrics(Metrics metrics) {
        sMetrics = metrics;
    }

    public static Metrics getMetrics() {
        return sMetrics;
    }

    interface Row {
        SolverVariable getPivotCandidate(LinearSystem system, boolean[] avoid);
        void clear();
        void initFromRow(Row row);
        void addError(SolverVariable variable);

        SolverVariable getKey();
        boolean isEmpty();
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
        if (sMetrics != null) {
            sMetrics.tableSizeIncrease++;
            sMetrics.maxTableSize = Math.max(sMetrics.maxTableSize, TABLE_SIZE);
            sMetrics.lastTableSize = sMetrics.maxTableSize;
        }
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
        mGoal.clear();
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
        SolverVariable.increaseErrorId();
        return row;
    }

    public SolverVariable createSlackVariable() {
        if (sMetrics != null) {
            sMetrics.slackvariables++;
        }
        if (mNumColumns + 1 >= mMaxColumns) {
            increaseTableSize();
        }
        SolverVariable variable = acquireSolverVariable(SolverVariable.Type.SLACK, null);
        mVariablesID++;
        mNumColumns++;
        variable.id = mVariablesID;
        mCache.mIndexedVariables[mVariablesID] = variable;
        return variable;
    }

    public SolverVariable createExtraVariable() {
        if (sMetrics != null) {
            sMetrics.extravariables++;
        }
        if (mNumColumns + 1 >= mMaxColumns) {
            increaseTableSize();
        }
        SolverVariable variable = acquireSolverVariable(SolverVariable.Type.SLACK, null);
        mVariablesID++;
        mNumColumns++;
        variable.id = mVariablesID;
        mCache.mIndexedVariables[mVariablesID] = variable;
        return variable;
    }

    private void addError(ArrayRow row) {
        row.addError(this, SolverVariable.STRENGTH_NONE);
    }

    private void addSingleError(ArrayRow row, int sign) {
        addSingleError(row, sign, SolverVariable.STRENGTH_NONE);
    }

    void addSingleError(ArrayRow row, int sign, int strength) {
        String prefix = null;
        if (DEBUG) {
            if (sign > 0) {
                prefix = "ep";
            } else {
                prefix = "em";
            }
            prefix = "em";
        }
        SolverVariable error = createErrorVariable(strength, prefix);
        row.addSingleError(error, sign);
    }

    private SolverVariable createVariable(String name, SolverVariable.Type type) {
        if (sMetrics != null) {
            sMetrics.variables++;
        }
        if (mNumColumns + 1 >= mMaxColumns) {
            increaseTableSize();
        }
        SolverVariable variable = acquireSolverVariable(type, null);
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

    public SolverVariable createErrorVariable(int strength, String prefix) {
        if (sMetrics != null) {
            sMetrics.errors++;
        }
        if (mNumColumns + 1 >= mMaxColumns) {
            increaseTableSize();
        }
        SolverVariable variable = acquireSolverVariable(SolverVariable.Type.ERROR, prefix);
        mVariablesID++;
        mNumColumns++;
        variable.id = mVariablesID;
        variable.strength = strength;
        mCache.mIndexedVariables[mVariablesID] = variable;
        mGoal.addError(variable);
        return variable;
    }

    /**
     * Returns a SolverVariable instance of the given type
     * @param type type of the SolverVariable
     * @return instance of SolverVariable
     */
    private SolverVariable acquireSolverVariable(SolverVariable.Type type, String prefix) {
        SolverVariable variable = mCache.solverVariablePool.acquire();
        if (variable == null) {
            variable = new SolverVariable(type, prefix);
            variable.setType(type, prefix);
        } else {
            variable.reset();
            variable.setType(type, prefix);
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
    Row getGoal() { return mGoal; }

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
     * Minimize the current goal of the system.
     */
    public void minimize() throws Exception {
        if (sMetrics != null) {
            sMetrics.minimize++;
        }
        if (DEBUG) {
            System.out.println("\n*** MINIMIZE ***\n");
        }
        if (graphOptimizer) {
            if (sMetrics != null) {
                sMetrics.graphOptimizer++;
            }
            boolean fullySolved = true;
            for (int i = 0; i < mNumRows; i++) {
                ArrayRow r = mRows[i];
                if (!r.isSimpleDefinition) {
                    fullySolved = false;
                    break;
                }
            }
            if (!fullySolved) {
                minimizeGoal(mGoal);
            } else {
                if (sMetrics != null) {
                    sMetrics.fullySolved++;
                }
                computeValues();
            }
        } else {
            minimizeGoal(mGoal);
        }
        if (DEBUG) {
            System.out.println("\n*** END MINIMIZE ***\n");
        }
    }

    /**
     * Minimize the given goal with the current system.
     * @param goal the goal to minimize.
     */
    void minimizeGoal(Row goal) throws Exception {
        if (sMetrics != null) {
            sMetrics.minimizeGoal++;
            sMetrics.maxVariables = Math.max(sMetrics.maxVariables, mNumColumns);
            sMetrics.maxRows = Math.max(sMetrics.maxRows, mNumRows);
        }
        // First, let's make sure that the system is in Basic Feasible Solved Form (BFS), i.e.
        // all the constants of the restricted variables should be positive.
        if (DEBUG) {
            System.out.println("minimize goal: " + goal);
        }
        updateRowFromVariables((ArrayRow) goal);
        if (DEBUG) {
            displayReadableRows();
        }
        enforceBFS(goal);
        if (DEBUG) {
            System.out.println("Goal after enforcing BFS " + goal);
            displayReadableRows();
        }
        optimize(goal, false);
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
    private final void updateRowFromVariables(ArrayRow row) {
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
        if (sMetrics != null) {
            sMetrics.constraints++;
            if (row.isSimpleDefinition) {
                sMetrics.simpleconstraints++;
            }
        }
        if (mNumRows + 1 >= mMaxRows || mNumColumns + 1 >= mMaxColumns) {
            increaseTableSize();
        }
        if (DEBUG) {
            System.out.println("addConstraint <" + row.toReadableString() + ">");
        }

        boolean added = false;
        if (!row.isSimpleDefinition) {
            // Update the equation with the variables already defined in the system
            updateRowFromVariables(row);

            if (row.isEmpty()) {
                return;
            }

            // First, ensure that if we have a constant it's positive
            row.ensurePositiveConstant();

            if (DEBUG) {
                System.out.println("addConstraint, updated row : " + row.toReadableString());
            }

            // Then pick a good variable to use for the row
            if (row.chooseSubject(this)) {
                // extra variable added... let's try to see if we can remove it
                SolverVariable extra = createExtraVariable();
                row.variable = extra;
                addRow(row);
                added = true;
                mTempGoal.initFromRow(row);
                optimize(mTempGoal, true);
                if (extra.definitionId == -1) {
                    if (DEBUG) {
                        System.out.println("row added is 0, so get rid of it");
                    }
                    if (row.variable == extra) {
                        // move extra to be parametric
                        SolverVariable pivotCandidate = row.pickPivot(extra);
                        if (pivotCandidate != null) {
                            if (sMetrics != null) {
                                sMetrics.pivots++;
                            }
                            row.pivot(pivotCandidate);
                        }
                    }
                    if (!row.isSimpleDefinition) {
                        row.variable.updateReferencesWithNewDefinition(row);
                    }
                    mNumRows--;
                }
            }

            if (!row.hasKeyVariable()) {
                // Can happen if row resolves to nil
                if (DEBUG) {
                    System.out.println("No variable found to pivot on " + row.toReadableString());
                    displayReadableRows();
                }
                return;
            }
        }
        if (!added) {
            addRow(row);
        }
    }

    private final void addRow(ArrayRow row) {
        if (mRows[mNumRows] != null) {
            mCache.arrayRowPool.release(mRows[mNumRows]);
        }
        mRows[mNumRows] = row;
        row.variable.definitionId = mNumRows;
        mNumRows++;
        row.variable.updateReferencesWithNewDefinition(row);

        if (DEBUG) {
            System.out.println("Row added, here is the system:");
            displayReadableRows();
        }
    }

    /**
     * Optimize the system given a goal to minimize. The system should be in BFS form.
     * @param goal goal to optimize.
     * @param b
     * @return number of iterations.
     */
    private final int optimize(Row goal, boolean b) {
        if (sMetrics != null) {
            sMetrics.optimize++;
        }
        boolean done = false;
        int tries = 0;
        for (int i = 0; i < mNumColumns; i++) {
            mAlreadyTestedCandidates[i] = false;
        }

        if (DEBUG) {
            System.out.println("\n****************************");
            System.out.println("*       OPTIMIZATION       *");
            System.out.println("* mNumColumns: " + mNumColumns);
            System.out.println("* GOAL: " + goal);
            System.out.println("****************************\n");
        }

        while (!done) {
            if (sMetrics != null) {
                sMetrics.iterations++;
            }
            tries++;
            if (DEBUG) {
                System.out.println("\n******************************");
                System.out.println("* iteration: " + tries);
            }
            if (tries >= 2*mNumColumns) {
                return tries;
            }

            if (goal.getKey() != null) {
                mAlreadyTestedCandidates[goal.getKey().id] = true;
            }
            SolverVariable pivotCandidate = goal.getPivotCandidate(this, mAlreadyTestedCandidates);
            if (DEBUG) {
                System.out.println("* Pivot candidate: " + pivotCandidate);
                System.out.println("******************************\n");
            }
            if (pivotCandidate != null) {
                if (mAlreadyTestedCandidates[pivotCandidate.id]) {
                    return tries;
                } else {
                    mAlreadyTestedCandidates[pivotCandidate.id] = true;
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
                        // skip unrestricted variables equations (to only look at Cs)
                        continue;
                    }
                    if (current.isSimpleDefinition) {
                        continue;
                    }

                    if (current.hasVariable(pivotCandidate)) {
                        if (DEBUG) {
                            System.out.println("equation " + i + " " + current + " contains " + pivotCandidate);
                        }
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
                // At this point, we ought to have an equation to pivot on

                if (pivotRowIndex > -1) {
                    // We found an equation to pivot on
                    if (DEBUG) {
                        System.out.println("We pivot on " + pivotRowIndex);
                    }
                    ArrayRow pivotEquation = mRows[pivotRowIndex];
                    pivotEquation.variable.definitionId = -1;
                    if (sMetrics != null) {
                        sMetrics.pivots++;
                    }
                    pivotEquation.pivot(pivotCandidate);
                    pivotEquation.variable.definitionId = pivotRowIndex;
                    pivotEquation.variable.updateReferencesWithNewDefinition(pivotEquation);
                    if (DEBUG) {
                        System.out.println("new system after pivot:");
                        displayReadableRows();
                        System.out.println("optimizing: " + goal);
                    }
                    /*
                    try {
                        enforceBFS(goal);
                    } catch (Exception e) {
                        System.out.println("### EXCEPTION " + e);
                        e.printStackTrace();
                    }
                    */
                    // now that we pivoted, we're going to continue looping on the next goal
                    // columns, until we exhaust all the possibilities of improving the system
                } else {
//                    System.out.println("we couldn't find an equation to pivot upon");
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
    private int enforceBFS(Row goal) throws Exception {
        int tries = 0;
        boolean done;

        if (DEBUG) {
            System.out.println("\n#################");
            System.out.println("# ENFORCING BFS #");
            System.out.println("#################\n");
        }

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
                if (sMetrics != null) {
                    sMetrics.bfs++;
                }
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
                    if (current.isSimpleDefinition) {
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
                    if (sMetrics != null) {
                        sMetrics.pivots++;
                    }
                    pivotEquation.pivot(mCache.mIndexedVariables[pivotColumnIndex]);
                    pivotEquation.variable.definitionId = pivotRowIndex;
                    pivotEquation.variable.updateReferencesWithNewDefinition(pivotEquation);

                    if (DEBUG) {
                        System.out.println("new goal after pivot: " + goal);
                        displayRows();
                    }
                } else {
                    done = true;
                }
                if (tries > mNumColumns / 2) {
                    // failsafe -- tried too many times
                    done = true;
                }
            }
        }

        if (DEBUG) {
            System.out.println("the current system should now be feasible [" + infeasibleSystem + "] after " + tries + " iterations");
            displayReadableRows();

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
                System.out.println("IMPOSSIBLE SYSTEM, WTF");
                throw new Exception();
            }
            if (infeasibleSystem) {
                return tries;
            }
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
        s += mGoal + "\n";
        System.out.println(s);
    }

    void displayReadableRows() {
        displaySolverVariables();
        String s = " #  ";
        for (int i = 0; i < mNumRows; i++) {
            s += mRows[i].toReadableString();
            s += "\n #  ";
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
        s += mGoal + "\n";
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
        String s = "Display Rows (" + mNumRows + "x" + mNumColumns + ")\n";
        /*
        s += ":\n\t | C | ";
        for (int i = 1; i <= mNumColumns; i++) {
            SolverVariable v = mCache.mIndexedVariables[i];
            s += v;
            s += " | ";
        }
        s += "\n";
        */
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

    private String getDisplayStrength(int strength) {
        if (strength == SolverVariable.STRENGTH_LOW) {
            return "LOW";
        }
        if (strength == SolverVariable.STRENGTH_MEDIUM) {
            return "MEDIUM";
        }
        if (strength == SolverVariable.STRENGTH_HIGH) {
            return "HIGH";
        }
        if (strength == SolverVariable.STRENGTH_HIGHEST) {
            return "HIGHEST";
        }
        if (strength == SolverVariable.STRENGTH_EQUALITY) {
            return "EQUALITY";
        }
        if (strength == SolverVariable.STRENGTH_FIXED) {
            return "FIXED";
        }
        return "NONE";
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
            System.out.println("-> " + a + " >= " + b + (margin != 0 ? " + " + margin : "") + " " + getDisplayStrength(strength));
        }
        ArrayRow row = createRow();
        SolverVariable slack = createSlackVariable();
        slack.strength = 0;
        row.createRowGreaterThan(a, b, slack, margin);
        if (strength != SolverVariable.STRENGTH_FIXED) {
            float slackValue = row.variables.get(slack);
            addSingleError(row, (int) (-1 * slackValue), strength);
        }
        addConstraint(row);
    }

    public void addGreaterThan(SolverVariable a, int b) {
        if (DEBUG) {
            System.out.println("-> " + a + " >= " + b);
        }
        ArrayRow row = createRow();
        SolverVariable slack = createSlackVariable();
        slack.strength = 0;
        row.createRowGreaterThan(a, b, slack);
        addConstraint(row);
    }

    public void addGreaterBarrier(SolverVariable a, SolverVariable b, boolean hasMatchConstraintWidgets) {
        if (DEBUG) {
            System.out.println("-> Barrier " + a + " >= " + b);
        }
        ArrayRow row = createRow();
        SolverVariable slack = createSlackVariable();
        slack.strength = SolverVariable.STRENGTH_NONE;
        row.createRowGreaterThan(a, b, slack, 0);
        if (hasMatchConstraintWidgets) {
            // We set it to low, as constrained widgets (0d) will have a strength of low in default wrap
            float slackValue = row.variables.get(slack);
            addSingleError(row, (int) (-1 * slackValue), SolverVariable.STRENGTH_LOW);
        }
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
            System.out.println("-> " + a + " <= " + b + (margin != 0 ? " + " + margin : "") + " " + getDisplayStrength(strength));
        }
        ArrayRow row = createRow();
        SolverVariable slack = createSlackVariable();
        slack.strength = 0;
        row.createRowLowerThan(a, b, slack, margin);
        if (strength != SolverVariable.STRENGTH_FIXED) {
            float slackValue = row.variables.get(slack);
            addSingleError(row, (int) (-1 * slackValue), strength);
        }
        addConstraint(row);
    }

    public void addLowerBarrier(SolverVariable a, SolverVariable b, boolean hasMatchConstraintWidgets) {
        if (DEBUG) {
            System.out.println("-> Barrier " + a + " <= " + b);
        }
        ArrayRow row = createRow();
        SolverVariable slack = createSlackVariable();
        slack.strength = SolverVariable.STRENGTH_NONE;
        row.createRowLowerThan(a, b, slack, 0);
        if (hasMatchConstraintWidgets) {
            // We set it to low, as constrained widgets (0d) will have a strength of low in default wrap
            float slackValue = row.variables.get(slack);
            addSingleError(row, (int) (-1 * slackValue), SolverVariable.STRENGTH_LOW);
        }
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
            System.out.println("-> [center bias: " + bias + "] : " + a + " - " + b
                    + " - " + m1
                    + " = " + c + " - " + d + " - " + m2
                    + " " + getDisplayStrength(strength));
        }
        ArrayRow row = createRow();
        row.createRowCentering(a, b, m1, bias, c, d, m2);
        if (strength != SolverVariable.STRENGTH_FIXED) {
            row.addError(this, strength);
        }
        addConstraint(row);
    }

    public void addRatio(SolverVariable a, SolverVariable b, SolverVariable c, SolverVariable d, float ratio, int strength) {
        if (DEBUG) {
            System.out.println("-> [ratio: " + ratio + "] : " + a + " = " + b + " + (" + c + " - " + d + ") * " + ratio + " " + getDisplayStrength(strength));
        }
        ArrayRow row = createRow();
        row.createRowDimensionRatio(a, b, c, d, ratio);
        if (strength != SolverVariable.STRENGTH_FIXED) {
            row.addError(this, strength);
        }
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
            System.out.println("-> " + a + " = " + b + (margin != 0 ? " + " + margin : "") + " " + getDisplayStrength(strength));
        }
        ArrayRow row = createRow();
        row.createRowEquals(a, b, margin);
        if (strength != SolverVariable.STRENGTH_FIXED) {
            row.addError(this, strength);
        }
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
                if (row.variables.currentSize == 0) {
                    row.isSimpleDefinition = true;
                    row.constantValue = value;
                } else {
                    ArrayRow newRow = createRow();
                    newRow.createRowEquals(a, value);
                    addConstraint(newRow);
                }
            }
        } else {
            ArrayRow row = createRow();
            row.createRowDefinition(a, value);
            addConstraint(row);
        }
    }


    /**
     * Add an equation of the form a = value
     * @param a variable a
     * @param value the value we set
     */
    public void addEquality(SolverVariable a, int value, int strength) {
        if (DEBUG) {
            System.out.println("-> " + a + " = " + value + " " + getDisplayStrength(strength));
        }
        int idx = a.definitionId;
        if (a.definitionId != -1) {
            ArrayRow row = mRows[idx];
            if (row.isSimpleDefinition) {
                row.constantValue = value;
            } else {
                ArrayRow newRow = createRow();
                newRow.createRowEquals(a, value);
                newRow.addError(this, strength);
                addConstraint(newRow);
            }
        } else {
            ArrayRow row = createRow();
            row.createRowDefinition(a, value);
            row.addError(this, strength);
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
            row.addError(linearSystem, SolverVariable.STRENGTH_HIGHEST);
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

    /**
     * Add the equations constraining a widget center to another widget center, positioned
     * on a circle, following an angle and radius
     *
     * @param widget
     * @param target
     * @param angle from 0 to 360
     * @param radius the distance between the two centers
     */
    public void addCenterPoint(ConstraintWidget widget, ConstraintWidget target, float angle, int radius) {

        SolverVariable Al = createObjectVariable(widget.getAnchor(ConstraintAnchor.Type.LEFT));
        SolverVariable At = createObjectVariable(widget.getAnchor(ConstraintAnchor.Type.TOP));
        SolverVariable Ar = createObjectVariable(widget.getAnchor(ConstraintAnchor.Type.RIGHT));
        SolverVariable Ab = createObjectVariable(widget.getAnchor(ConstraintAnchor.Type.BOTTOM));

        SolverVariable Bl = createObjectVariable(target.getAnchor(ConstraintAnchor.Type.LEFT));
        SolverVariable Bt = createObjectVariable(target.getAnchor(ConstraintAnchor.Type.TOP));
        SolverVariable Br = createObjectVariable(target.getAnchor(ConstraintAnchor.Type.RIGHT));
        SolverVariable Bb = createObjectVariable(target.getAnchor(ConstraintAnchor.Type.BOTTOM));

        ArrayRow row = createRow();
        float angleComponent = (float) (Math.sin(angle) * radius);
        row.createRowWithAngle(At, Ab, Bt, Bb, angleComponent);
        addConstraint(row);
        row = createRow();
        angleComponent = (float) (Math.cos(angle) * radius);
        row.createRowWithAngle(Al, Ar, Bl, Br, angleComponent);
        addConstraint(row);
    }

}
