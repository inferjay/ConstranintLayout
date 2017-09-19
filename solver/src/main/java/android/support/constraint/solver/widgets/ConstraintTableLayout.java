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
package android.support.constraint.solver.widgets;

import android.support.constraint.solver.LinearSystem;

import java.util.ArrayList;

/**
 * Implements a table-like layout. The table can grow automatically either horizontally
 * or vertically, depending on the given orientation (by default it will grow vertically).
 */
public class ConstraintTableLayout extends ConstraintWidgetContainer {

    private boolean mVerticalGrowth = true; // will grow vertically, with number of columns set.
    private int mNumCols = 0;
    private int mNumRows = 0;
    private int mPadding = 8;

    /**
     * Internal utility class representing an horizontal slice of the table.
     */
    class HorizontalSlice {
        ConstraintWidget top;
        ConstraintWidget bottom;
        int padding;
    }

    /**
     * Internal utility class representing a vertical slice of the table.
     */
    class VerticalSlice {
        ConstraintWidget left;
        ConstraintWidget right;
        int alignment = ALIGN_LEFT;
        int padding;
    }

    private ArrayList<VerticalSlice> mVerticalSlices = new ArrayList<>();
    private ArrayList<HorizontalSlice> mHorizontalSlices = new ArrayList<>();

    private ArrayList<Guideline> mVerticalGuidelines = new ArrayList<>();
    private ArrayList<Guideline> mHorizontalGuidelines = new ArrayList<>();

    public static final int ALIGN_CENTER = 0;
    public static final int ALIGN_LEFT = 1;
    public static final int ALIGN_RIGHT = 2;
    private static final int ALIGN_FULL = 3;

    /**
     * Default constructor
     */
    public ConstraintTableLayout() {
    }

    /**
     * Constructor
     *
     * @param x      x position
     * @param y      y position
     * @param width  width of the layout
     * @param height height of the layout
     */
    public ConstraintTableLayout(int x, int y, int width, int height) {
        super(x, y, width, height);
    }

    /**
     * Constructor
     *
     * @param width  width of the layout
     * @param height height of the layout
     */
    public ConstraintTableLayout(int width, int height) {
        super(width, height);
    }

    /**
     * Specify the xml type for the container
     *
     * @return
     */
    @Override
    public String getType() {
        return "ConstraintTableLayout";
    }

    /**
     * Accessor returning the number of rows in the table
     *
     * @return number of rows
     */
    public int getNumRows() {
        return mNumRows;
    }

    /**
     * Accessor returning the number of rows in the table
     *
     * @return number of rows
     */
    public int getNumCols() {
        return mNumCols;
    }

    /**
     * Accessor returning the padding of the table
     *
     * @return internal padding
     */
    public int getPadding() {
        return mPadding;
    }

    /**
     * Return a string representation of the current columns alignment.
     *
     * @return string representing the columns alignment
     */
    public String getColumnsAlignmentRepresentation() {
        final int numSlices = mVerticalSlices.size();
        String result = "";
        for (int i = 0; i < numSlices; i++) {
            VerticalSlice slice = mVerticalSlices.get(i);
            if (slice.alignment == ALIGN_LEFT) {
                result += "L";
            } else if (slice.alignment == ALIGN_CENTER) {
                result += "C";
            } else if (slice.alignment == ALIGN_FULL) {
                result += "F";
            } else if (slice.alignment == ALIGN_RIGHT) {
                result += "R";
            }
        }
        return result;
    }

    /**
     * Return a string representation of the given column's alignment
     *
     * @param column the column index
     * @return string representing the column's alignment
     */
    public String getColumnAlignmentRepresentation(int column) {
        VerticalSlice slice = mVerticalSlices.get(column);
        if (slice.alignment == ALIGN_LEFT) {
            return "L";
        } else if (slice.alignment == ALIGN_CENTER) {
            return "C";
        } else if (slice.alignment == ALIGN_FULL) {
            return "F";
        } else if (slice.alignment == ALIGN_RIGHT) {
            return "R";
        }
        return "!";
    }

    /**
     * Set the number of columns desired. Will only apply if the table is configured
     * to grow vertically -- the number of rows will be automatically derived from the
     * number of columns and the number of children.
     *
     * @param num
     */
    public void setNumCols(int num) {
        if (mVerticalGrowth && mNumCols != num) {
            mNumCols = num;
            setVerticalSlices();
            setTableDimensions();
        }
    }

    /**
     * Set the number of rows desired. Will only apply if the table is configured
     * to grow horizontally -- the number of columns will be automatically derived from the
     * number of rows and the number of children.
     *
     * @param num the number of desired rows.
     */
    public void setNumRows(int num) {
        if (!mVerticalGrowth && mNumCols != num) {
            mNumRows = num;
            setHorizontalSlices();
            setTableDimensions();
        }
    }

    /**
     * Return the growth type for the table.
     *
     * @return true if the table grow vertically when new items are added (i.e., add more rows),
     * and false if the table grow horizontally (i.e., add more columns)
     */
    public boolean isVerticalGrowth() {
        return mVerticalGrowth;
    }

    /**
     * Set the growth type for the table, either vertical or horizontal.
     *
     * @param value true to set a vertical growth (the default)
     */
    public void setVerticalGrowth(boolean value) {
        mVerticalGrowth = value;
    }

    /**
     * Set the value used for internal padding surrounding the cells.
     *
     * @param padding the padding value.
     */
    public void setPadding(int padding) {
        if (padding > 1) {
            mPadding = padding;
        }
    }

    /**
     * Set the alignment (left/center/right) for the given column. The column are numeroted
     * starting from zero.
     *
     * @param column    the column number
     * @param alignment the alignment type.
     */
    public void setColumnAlignment(int column, int alignment) {
        if (column < mVerticalSlices.size()) {
            VerticalSlice slice = mVerticalSlices.get(column);
            slice.alignment = alignment;
            setChildrenConnections();
        }
    }

    /**
     * Cycle the alignment (left/center/right) for the given column.
     *
     * @param column the column number
     */
    public void cycleColumnAlignment(int column) {
        VerticalSlice slice = mVerticalSlices.get(column);
        switch (slice.alignment) {
            case ALIGN_LEFT: {
                slice.alignment = ALIGN_CENTER;
            } break;
            case ALIGN_RIGHT: {
                slice.alignment = ALIGN_LEFT;
            } break;
            case ALIGN_CENTER: {
                slice.alignment = ALIGN_RIGHT;
            } break;
        }
        setChildrenConnections();
    }

    /**
     * Set the alignment (left/center/right) for columns, given a string representation.
     *
     * @param alignment
     */
    public void setColumnAlignment(String alignment) {
        for (int i = 0, n = alignment.length(); i < n; i++) {
            char c = alignment.charAt(i);
            if (c == 'L') {
                setColumnAlignment(i, ALIGN_LEFT);
            } else if (c == 'C') {
                setColumnAlignment(i, ALIGN_CENTER);
            } else if (c == 'F') {
                setColumnAlignment(i, ALIGN_FULL);
            } else if (c == 'R') {
                setColumnAlignment(i, ALIGN_RIGHT);
            } else {
                setColumnAlignment(i, ALIGN_CENTER);
            }
        }
    }

    /**
     * Accessor to the vertical guidelines contained in the table.
     *
     * @return array of guidelines
     */
    @Override
    public ArrayList<Guideline> getVerticalGuidelines() {
        return mVerticalGuidelines;
    }

    /**
     * Accessor to the horizontal guidelines contained in the table.
     *
     * @return array of guidelines
     */
    @Override
    public ArrayList<Guideline> getHorizontalGuidelines() {
        return mHorizontalGuidelines;
    }

    /**
     * Add the layout and its children to the solver
     *
     * @param system the solver we want to add the widget to
     */
    @Override
    public void addToSolver(LinearSystem system) {
        super.addToSolver(system);
        int count = mChildren.size();
        if (count == 0) {
            return;
        }
        setTableDimensions();

        // We don't want to add guidelines on a different system than our own
        if (system == mSystem) {
            int num = mVerticalGuidelines.size();
            for (int i = 0; i < num; i++) {
                Guideline guideline = mVerticalGuidelines.get(i);
                guideline.setPositionRelaxed(
                        getHorizontalDimensionBehaviour() == DimensionBehaviour.WRAP_CONTENT);
                guideline.addToSolver(system);
            }
            num = mHorizontalGuidelines.size();
            for (int i = 0; i < num; i++) {
                Guideline guideline = mHorizontalGuidelines.get(i);
                guideline.setPositionRelaxed(
                        getVerticalDimensionBehaviour() == DimensionBehaviour.WRAP_CONTENT);
                guideline.addToSolver(system);
            }
            for (int i = 0; i < count; i++) {
                ConstraintWidget child = mChildren.get(i);
                child.addToSolver(system);
            }
        }
    }

    /**
     * Will set up the dimensions of the table given the growth orientatio
     * (horizontal or vertical growth) and the set numbers of rows or columns.
     */
    public void setTableDimensions() {
        int extra = 0;
        int count = mChildren.size();
        for (int i = 0; i < count; i++) {
            ConstraintWidget widget = mChildren.get(i);
            extra += widget.getContainerItemSkip();
        }
        count += extra;
        if (mVerticalGrowth) {
            if (mNumCols == 0) {
                setNumCols(1);
            }
            int rows = count / mNumCols;
            if (rows * mNumCols < count) {
                rows++;
            }
            if (mNumRows == rows
                    && (mVerticalGuidelines.size() == mNumCols - 1)) {
                return;
            }
            mNumRows = rows;
            setHorizontalSlices();
        } else {
            if (mNumRows == 0) {
                setNumRows(1);
            }
            int cols = count / mNumRows;
            if (cols * mNumRows < count) {
                cols++;
            }
            if (mNumCols == cols
                    && (mHorizontalGuidelines.size() == mNumRows - 1)) {
                return;
            }
            mNumCols = cols;
            setVerticalSlices();
        }
        setChildrenConnections();
    }

    /**
     * Debug utility function to setup the names of the internal guidelines.
     *
     * @param s    solver used
     * @param name name of the widget
     */
    @Override
    public void setDebugSolverName(LinearSystem s, String name) {
        system = s;
        super.setDebugSolverName(s, name);
        updateDebugSolverNames();
    }

    private LinearSystem system = null;

    private void updateDebugSolverNames() {
        if (system == null) {
            return;
        }
        int num = mVerticalGuidelines.size();
        for (int i = 0; i < num; i++) {
            mVerticalGuidelines.get(i).setDebugSolverName(system, getDebugName() + ".VG" + i);
        }
        num = mHorizontalGuidelines.size();
        for (int i = 0; i < num; i++) {
            mHorizontalGuidelines.get(i).setDebugSolverName(system, getDebugName() + ".HG" + i);
        }
    }

    /**
     * Setup the vertical slices of the table. The internal ones need to create
     * a right-side guideline.
     */
    private void setVerticalSlices() {
        mVerticalSlices.clear();
        ConstraintWidget previous = this;
        float increment = 100 / (float) mNumCols;
        float percent = increment;
        for (int i = 0; i < mNumCols; i++) {
            VerticalSlice slice = new VerticalSlice();
            slice.left = previous;
            if (i < mNumCols - 1) {
                Guideline guideline = new Guideline();
                guideline.setOrientation(Guideline.VERTICAL);
                guideline.setParent(this);
                guideline.setGuidePercent((int) percent);
                percent += increment;
                slice.right = guideline;
                mVerticalGuidelines.add(guideline);
            } else {
                slice.right = this;
            }
            previous = slice.right;
            mVerticalSlices.add(slice);
        }
        updateDebugSolverNames();
    }

    /**
     * Setup the horizontal slices of the table. The internal ones need to create
     * a bottom-side guideline.
     */
    private void setHorizontalSlices() {
        mHorizontalSlices.clear();
        float increment = 100 / (float) mNumRows;
        float percent = increment;
        ConstraintWidget previous = this;
        for (int i = 0; i < mNumRows; i++) {
            HorizontalSlice slice = new HorizontalSlice();
            slice.top = previous;
            if (i < mNumRows - 1) {
                Guideline guideline = new Guideline();
                guideline.setOrientation(Guideline.HORIZONTAL);
                guideline.setParent(this);
                guideline.setGuidePercent((int) percent);
                percent += increment;
                slice.bottom = guideline;
                mHorizontalGuidelines.add(guideline);
            } else {
                slice.bottom = this;
            }
            previous = slice.bottom;
            mHorizontalSlices.add(slice);
        }
        updateDebugSolverNames();
    }

    /**
     * Set the children constraints (place them in the rows and columns)
     */
    private void setChildrenConnections() {
        int count = mChildren.size();
        int index = 0;
        for (int i = 0; i < count; i++) {
            ConstraintWidget target = mChildren.get(i);
            index += target.getContainerItemSkip();

            int col = index % mNumCols;
            int row = index / mNumCols;

            HorizontalSlice horizontalSlice = mHorizontalSlices.get(row);
            VerticalSlice verticalSlice = mVerticalSlices.get(col);
            ConstraintWidget targetLeft = verticalSlice.left;
            ConstraintWidget targetRight = verticalSlice.right;
            ConstraintWidget targetTop = horizontalSlice.top;
            ConstraintWidget targetBottom = horizontalSlice.bottom;

            target.getAnchor(ConstraintAnchor.Type.LEFT)
                    .connect(targetLeft.getAnchor(ConstraintAnchor.Type.LEFT), mPadding);
            if (targetRight instanceof Guideline) {
                target.getAnchor(ConstraintAnchor.Type.RIGHT)
                        .connect(targetRight.getAnchor(ConstraintAnchor.Type.LEFT), mPadding);
            } else {
                target.getAnchor(ConstraintAnchor.Type.RIGHT)
                        .connect(targetRight.getAnchor(ConstraintAnchor.Type.RIGHT), mPadding);
            }
//            target.getAnchor(ConstraintAnchor.Type.LEFT).setConnectionType(ConstraintAnchor.ConnectionType.STRICT);
//            target.getAnchor(ConstraintAnchor.Type.RIGHT).setConnectionType(ConstraintAnchor.ConnectionType.STRICT);
            switch (verticalSlice.alignment) {
                case ALIGN_FULL: {
                    target.setHorizontalDimensionBehaviour(DimensionBehaviour.MATCH_CONSTRAINT);
                }
                break;
                case ALIGN_LEFT: {
                    target.getAnchor(ConstraintAnchor.Type.LEFT).setStrength(
                            ConstraintAnchor.Strength.STRONG);
                    target.getAnchor(ConstraintAnchor.Type.RIGHT).setStrength(
                            ConstraintAnchor.Strength.WEAK);
                }
                break;
                case ALIGN_RIGHT: {
                    target.getAnchor(ConstraintAnchor.Type.LEFT).setStrength(
                            ConstraintAnchor.Strength.WEAK);
                    target.getAnchor(ConstraintAnchor.Type.RIGHT).setStrength(
                            ConstraintAnchor.Strength.STRONG);
                }
                break;
            }
            target.getAnchor(ConstraintAnchor.Type.TOP)
                    .connect(targetTop.getAnchor(ConstraintAnchor.Type.TOP), mPadding);
            if (targetBottom instanceof Guideline) {
                target.getAnchor(ConstraintAnchor.Type.BOTTOM)
                        .connect(targetBottom.getAnchor(ConstraintAnchor.Type.TOP), mPadding);
            } else {
                target.getAnchor(ConstraintAnchor.Type.BOTTOM)
                        .connect(targetBottom.getAnchor(ConstraintAnchor.Type.BOTTOM), mPadding);
            }

            index++;
        }
    }

    /**
     * Update the frame of the layout and its children from the solver
     *
     * @param system the solver we get the values from.
     */
    @Override
    public void updateFromSolver(LinearSystem system) {
        super.updateFromSolver(system);

        // We don't want to update guidelines on a different system than our own
        if (system == mSystem) {
            int num = mVerticalGuidelines.size();
            for (int i = 0; i < num; i++) {
                Guideline guideline = mVerticalGuidelines.get(i);
                guideline.updateFromSolver(system);
            }
            num = mHorizontalGuidelines.size();
            for (int i = 0; i < num; i++) {
                Guideline guideline = mHorizontalGuidelines.get(i);
                guideline.updateFromSolver(system);
            }
        }
    }

    /**
     * The table layout manages the positions of its children
     *
     * @return true
     */
    @Override
    public boolean handlesInternalConstraints() {
        return true;
    }

    /**
     * Recompute the percentage positions of the guidelines given their current position
     */
    public void computeGuidelinesPercentPositions() {
        int num = mVerticalGuidelines.size();
        for (int i = 0; i < num; i++) {
            mVerticalGuidelines.get(i).inferRelativePercentPosition();
        }
        num = mHorizontalGuidelines.size();
        for (int i = 0; i < num; i++) {
            mHorizontalGuidelines.get(i).inferRelativePercentPosition();
        }
    }

}
