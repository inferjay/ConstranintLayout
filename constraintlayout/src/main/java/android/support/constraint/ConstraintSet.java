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

package android.support.constraint;

import android.content.Context;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;

import java.util.HashMap;
import java.util.HashSet;

/**
 * This class allows you to define programmatically a set of constraints to be used with {@link ConstraintLayout}.
 * It lets you create and save constraints, and apply them to an existing ConstraintLayout. ConstraintsSet can be created in various ways:
 * <ul>
 * <li>
 * Manually <br> {@code c = new ConstraintSet(); c.connect(....);}
 * </li>
 * <li>
 * from a R.layout.* object <br> {@code c.clone(context, R.layout.layout1);}
 * </li>
 * <li>
 * from a ConstraintLayout <br> {@code c.clone(clayout);}
 * </li>
 * </ul><p>
 * Example code:<br>
 * {@sample resources/examples/MainActivity.java
 *          Example}
 */
public class ConstraintSet {

    /**
     * Dimension will be controlled by constraints
     */
    public static final int MATCH_CONSTRAINT = ConstraintLayout.LayoutParams.MATCH_CONSTRAINT;

    /**
     * Dimension will set by the view's content
     */
    public static final int WRAP_CONTENT = ConstraintLayout.LayoutParams.WRAP_CONTENT;

    /**
     * References the id of the parent.
     * Used in:
     * <ul>
     *     <li>{@link #connect(int, int, int, int, int)}</li>
     *     <li>{@link #center(int, int, int, int, int, int, int, float)}</li>
     *   </ul>
     */
    public static final int PARENT_ID = ConstraintLayout.LayoutParams.PARENT_ID;

    /**
     * Used to create a horizontal create guidelines.
     */
    public static final int HORIZONTAL_GUIDELINE = 0;

    /**
     * Used to create a vertical create guidelines.
     * see {@link #create(int, int)}
     */
    public static final int VERTICAL_GUIDELINE = 1;

    /**
     * This view is visible.
     * Use with {@link #setVisibility} and <a href="#attr_android:visibility">{@code
     * android:visibility}.
     */
    public static final int VISIBLE = View.VISIBLE;

    /**
     * This view is invisible, but it still takes up space for layout purposes.
     * Use with {@link #setVisibility} and <a href="#attr_android:visibility">{@code
     * android:visibility}.
     */
    public static final int INVISIBLE = View.INVISIBLE;

    /**
     * This view is gone, and will not take any space for layout
     * purposes. Use with {@link #setVisibility} and <a href="#attr_android:visibility">{@code
     * android:visibility}.
     */
    public static final int GONE = View.GONE;

    /**
     * The left side of a view.
     */
    public static final int LEFT = ConstraintLayout.LayoutParams.LEFT;

    /**
     * The right side of a view.
     */
    public static final int RIGHT = ConstraintLayout.LayoutParams.RIGHT;;

    /**
     * The top of a view.
     */
    public static final int TOP = ConstraintLayout.LayoutParams.TOP;;

    /**
     * The bottom side of a view.
     */
    public static final int BOTTOM = ConstraintLayout.LayoutParams.BOTTOM;;

    /**
     * The baseline of the text in a view.
     */
    public static final int BASELINE = ConstraintLayout.LayoutParams.BASELINE;;

    /**
     * The left side of a view in left to right languages.
     * In right to left languages it corresponds to the right side of the view
     */
    public static final int START = ConstraintLayout.LayoutParams.START;

    /**
     * The right side of a view in right to left languages.
     * In right to left languages it corresponds to the left side of the view
     */
    public static final int END = ConstraintLayout.LayoutParams.END;;

    private HashMap<Integer, Constraint> mConstraints = new HashMap<Integer, Constraint>();

    private static class Constraint {
        boolean mIsGuideline = false;
        public int mWidth;
        public int mHeight;
        int mViewId;
        static final int UNSET = ConstraintLayout.LayoutParams.UNSET;
        public int guideBegin = UNSET;
        public int guideEnd = UNSET;
        public float guidePercent = UNSET;

        public int leftToLeft = UNSET;
        public int leftToRight = UNSET;
        public int rightToLeft = UNSET;
        public int rightToRight = UNSET;
        public int topToTop = UNSET;
        public int topToBottom = UNSET;
        public int bottomToTop = UNSET;
        public int bottomToBottom = UNSET;
        public int baselineToBaseline = UNSET;

        public int startToEnd = UNSET;
        public int startToStart = UNSET;
        public int endToStart = UNSET;
        public int endToEnd = UNSET;

        public float horizontalBias = 0.5f;
        public float verticalBias = 0.5f;
        public float dimensionRatio = 0f;

        public int editorAbsoluteX = UNSET;
        public int editorAbsoluteY = UNSET;

        public int orientation = UNSET;
        public int leftMargin;
        public int rightMargin;
        public int topMargin;
        public int bottomMargin;
        public int endMargin;
        public int startMargin;
        public int visibility;

        private void fillFrom(int viewId, ConstraintLayout.LayoutParams param) {
            mViewId = viewId;
            leftToLeft = param.leftToLeft;
            leftToRight = param.leftToRight;
            rightToLeft = param.rightToLeft;
            rightToRight = param.rightToRight;
            topToTop = param.topToTop;
            topToBottom = param.topToBottom;
            bottomToTop = param.bottomToTop;
            bottomToBottom = param.bottomToBottom;
            baselineToBaseline = param.baselineToBaseline;
            startToEnd = param.startToEnd;
            startToStart = param.startToStart;
            endToStart = param.endToStart;
            endToEnd = param.endToEnd;

            horizontalBias = param.horizontalBias;
            verticalBias = param.verticalBias;
            dimensionRatio = param.dimensionRatio;
            editorAbsoluteX = param.editorAbsoluteX;
            editorAbsoluteY = param.editorAbsoluteY;
            orientation = param.orientation;
            guidePercent = param.guidePercent;
            guideBegin = param.guideBegin;
            guideEnd = param.guideEnd;
            mWidth =  param.width;
            mHeight = param.height;
            leftMargin = param.leftMargin;
            rightMargin = param.rightMargin;
            topMargin = param.topMargin;
            bottomMargin = param.bottomMargin;
            endMargin = param.getMarginEnd();
            startMargin = param.getMarginStart();
        }

        public void applyTo(ConstraintLayout.LayoutParams param) {
            param.leftToLeft = leftToLeft;
            param.leftToRight = leftToRight;
            param.rightToLeft = rightToLeft;
            param.rightToRight = rightToRight;

            param.topToTop = topToTop;
            param.topToBottom = topToBottom;
            param.bottomToTop = bottomToTop;
            param.bottomToBottom = bottomToBottom;

            param.baselineToBaseline = baselineToBaseline;

            param.startToEnd = startToEnd;
            param.startToStart = startToStart;
            param.endToStart = endToStart;
            param.endToEnd = endToEnd;

            param.leftMargin = leftMargin;
            param.rightMargin = rightMargin;
            param.topMargin = topMargin;
            param.bottomMargin = bottomMargin;

            param.horizontalBias = horizontalBias;
            param.verticalBias = verticalBias;

            param.dimensionRatio = dimensionRatio;
            param.editorAbsoluteX = editorAbsoluteX;
            param.editorAbsoluteY = editorAbsoluteY;

            param.orientation = orientation;
            param.guidePercent = guidePercent;
            param.guideBegin = guideBegin;
            param.guideEnd = guideEnd;
            param.width = mWidth;
            param.height = mHeight;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                param.setMarginStart(startMargin);
                param.setMarginEnd(endMargin);
            }

            param.validate();
        }
    }

    /**
     * Copy the constraints from a layout.
     *
     * @param context            the context for the layout inflation
     * @param constraintLayoutId the id of the layout file
     */
    public void clone(Context context, int constraintLayoutId) {
        clone((ConstraintLayout) LayoutInflater.from(context).inflate(constraintLayoutId, null));
    }

    /**
     * Copy the layout parameters of a ConstraintLayout.
     *
     * @param constraintLayout The ConstraintLayout to be copied
     */
    public void clone(ConstraintLayout constraintLayout) {
        int count = constraintLayout.getChildCount();
        mConstraints.clear();
        for (int i = 0; i < count; i++) {
            View view = constraintLayout.getChildAt(i);
            ConstraintLayout.LayoutParams param = (ConstraintLayout.LayoutParams) view.getLayoutParams();

            int id = view.getId();
            if (!mConstraints.containsKey(id)) {
                mConstraints.put(id, new Constraint());
            }
            Constraint constraint = mConstraints.get(id);
            constraint.fillFrom(id, param);
            constraint.visibility = view.getVisibility();
        }
    }

    /**
     * Apply the constraints to a ConstraintLayout.
     *
     * @param constraintLayout to be modified
     */
    public void applyTo(ConstraintLayout constraintLayout) {
        int count = constraintLayout.getChildCount();
        HashSet<Integer> used = new HashSet<Integer>(mConstraints.keySet());

        for (int i = 0; i < count; i++) {
            View view = constraintLayout.getChildAt(i);
            int id = view.getId();
            if (mConstraints.containsKey(id)) {
                used.remove(id);
                Constraint constraint = mConstraints.get(id);
                ConstraintLayout.LayoutParams param = (ConstraintLayout.LayoutParams) view.getLayoutParams();
                constraint.applyTo(param);
                view.setLayoutParams(param);
                view.setVisibility(constraint.visibility);
            }
        }
        for (Integer id : used) {
            Constraint constraint = mConstraints.get(id);
            if (constraint.mIsGuideline) {
                Guideline g = new Guideline(constraintLayout.getContext());
                ConstraintLayout.LayoutParams param = constraintLayout.generateDefaultLayoutParams();
                constraint.applyTo(param);
                constraintLayout.addView(g, param);
            }
        }
    }

    /**
     * Center widget between the other two widgets.
     *
     * @param centerID     ID of the widget to be centered
     * @param firstID      ID of the first widget to connect the left or top of the widget to
     * @param firstSide    the side of the widget to connect to
     * @param firstMargin  the connection margin
     * @param secondId     the ID of the second widget to connect to right or top of the widget to
     * @param secondSide   the side of the widget to connect to
     * @param secondMargin the connection margin
     * @param bias         the ratio between two connections
     */
    public void center(int centerID,
                       int firstID, int firstSide, int firstMargin,
                       int secondId, int secondSide, int secondMargin,
                       float bias) {
        // Error checking

        if (firstMargin < 0) {
            throw new IllegalArgumentException("margin must be > 0");
        }
        if (secondMargin < 0) {
            throw new IllegalArgumentException("margin must be > 0");
        }
        if (bias <= 0 || bias > 1) {
            throw new IllegalArgumentException("bias must be between 0 and 1 inclusive");
        }

        if (firstSide == LEFT || firstSide == RIGHT) {
            connect(centerID, LEFT, firstID, firstSide, firstMargin);
            connect(centerID, RIGHT, secondId, secondSide, secondMargin);
            Constraint constraint = mConstraints.get(centerID);
            constraint.horizontalBias = bias;
        } else {
            connect(centerID, TOP, firstID, firstSide, firstMargin);
            connect(centerID, BOTTOM, secondId, secondSide, secondMargin);
            Constraint constraint = mConstraints.get(centerID);
            constraint.verticalBias = bias;
        }
    }

    private void centerHorizontally(int centerID, int leftId, int leftSide, int leftMargin, int rightId, int rightSide, int rightMargin, float bias) {
        connect(centerID, LEFT, leftId, leftSide, leftMargin);
        connect(centerID, RIGHT, rightId, rightSide, rightMargin);
        Constraint constraint = mConstraints.get(centerID);
        constraint.horizontalBias = bias;
    }

    private void centerVertically(int centerID, int topId, int topSide, int topMargin, int bottomId, int bottomSide, int bottomMargin, float bias) {
        connect(centerID, TOP, topId, topSide, topMargin);
        connect(centerID, BOTTOM, bottomId, bottomSide, bottomMargin);
        Constraint constraint = mConstraints.get(centerID);
        constraint.verticalBias = bias;
    }

    /**
     * Create a constraint between two widgets.
     *
     * @param startID   the ID of the widget to be constrained
     * @param startSide the side of the widget to constrain
     * @param endID     the id of the widget to constrain to
     * @param endSide   the side of widget to constrain to
     * @param margin    the margin to constrain (margin must be postive)
     */
    public void connect(int startID, int startSide, int endID, int endSide, int margin) {
        if (!mConstraints.containsKey(startID)) {
            mConstraints.put(startID, new Constraint());
        }
        Constraint constraint = mConstraints.get(startID);
        switch (startSide) {
            case LEFT:
                if (endSide == LEFT) {
                    constraint.leftToLeft = endID;
                    constraint.leftToRight = Constraint.UNSET;
                } else if (endSide == RIGHT) {
                    constraint.leftToRight = endID;
                    constraint.leftToLeft = Constraint.UNSET;

                } else {
                    throw new IllegalArgumentException("Left to " + sideToString(endSide) + " undefined");
                }
                constraint.leftMargin = margin;
                break;
            case RIGHT:
                if (endSide == LEFT) {
                    constraint.rightToLeft = endID;
                    constraint.rightToRight = Constraint.UNSET;

                } else if (endSide == RIGHT) {
                    constraint.rightToRight = endID;
                    constraint.rightToLeft = Constraint.UNSET;

                } else {
                    throw new IllegalArgumentException("right to " + sideToString(endSide) + " undefined");
                }
                constraint.rightMargin = margin;
                break;
            case TOP:
                if (endSide == TOP) {
                    constraint.topToTop = endID;
                    constraint.topToBottom = Constraint.UNSET;
                    constraint.baselineToBaseline = Constraint.UNSET;
                } else if (endSide == BOTTOM) {
                    constraint.topToBottom = endID;
                    constraint.topToTop = Constraint.UNSET;
                    constraint.baselineToBaseline = Constraint.UNSET;

                } else {
                    throw new IllegalArgumentException("right to " + sideToString(endSide) + " undefined");
                }
                constraint.topMargin = margin;
                break;
            case BOTTOM:
                if (endSide == BOTTOM) {
                    constraint.bottomToBottom = endID;
                    constraint.bottomToTop = Constraint.UNSET;
                    constraint.baselineToBaseline = Constraint.UNSET;
                    ;
                } else if (endSide == TOP) {
                    constraint.bottomToTop = endID;
                    constraint.bottomToBottom = Constraint.UNSET;
                    constraint.baselineToBaseline = Constraint.UNSET;
                    ;
                } else {
                    throw new IllegalArgumentException("right to " + sideToString(endSide) + " undefined");
                }
                constraint.bottomMargin = margin;
                break;
            case BASELINE:
                if (endSide == BASELINE) {
                    constraint.baselineToBaseline = endID;
                    constraint.bottomToBottom = Constraint.UNSET;
                    constraint.bottomToTop = Constraint.UNSET;
                    constraint.topToTop = Constraint.UNSET;
                    constraint.topToBottom = Constraint.UNSET;
                } else {
                    throw new IllegalArgumentException("right to " + sideToString(endSide) + " undefined");
                }
                break;
            case START:
                if (endSide == START) {
                    constraint.startToStart = endID;
                    constraint.startToEnd = Constraint.UNSET;
                } else if (endSide == END) {
                    constraint.startToEnd = endID;
                    constraint.startToStart = Constraint.UNSET;
                } else {
                    throw new IllegalArgumentException("right to " + sideToString(endSide) + " undefined");
                }
                constraint.startMargin = margin;
                break;
            case END:
                if (endSide == END) {
                    constraint.endToEnd = endID;
                    constraint.endToStart = Constraint.UNSET;
                } else if (endSide == START) {
                    constraint.endToStart = endID;
                    constraint.endToEnd = Constraint.UNSET;
                } else {
                    throw new IllegalArgumentException("right to " + sideToString(endSide) + " undefined");
                }
                constraint.endMargin = margin;
                break;
            default:
                throw new IllegalArgumentException(sideToString(startSide) + " to " + sideToString(endSide) + " unknown");
        }
    }

    /**
     * Centers the view horizontally relative to toView's position.
     *
     * @param viewId ID of view to center Horizontally
     * @param toView ID of view to center on (or in)
     */
    public void centerHorizontally(int viewId, int toView) {
        center(viewId, toView, ConstraintSet.LEFT, 0, toView, ConstraintSet.RIGHT, 0, .5f);
    }

    /**
     * Centers the view vertically relative to toView's position.
     *
     * @param viewId ID of view to center Horizontally
     * @param toView ID of view to center on (or in)
     */
    public void centerVertically(int viewId, int toView) {
        center(viewId, toView, ConstraintSet.TOP, 0, toView, ConstraintSet.BOTTOM, 0, .5f);
    }

    /**
     * Remove all constraints from this view.
     *
     * @param viewId ID of view to remove all connections to
     */
    public void clear(int viewId) {
        mConstraints.remove(viewId);
    }

    /**
     * Remove a constraint from this view.
     *
     * @param viewId ID of view to center on (or in)
     * @param anchor the Anchor to remove constraint from
     */
    public void clear(int viewId, int anchor) {
        if (mConstraints.containsKey(viewId)) {
            Constraint constraint = mConstraints.get(viewId);
            switch (anchor) {
                case LEFT:
                    constraint.leftToRight = Constraint.UNSET;
                    constraint.leftToLeft = Constraint.UNSET;
                    constraint.leftMargin = 0;
                    break;
                case RIGHT:
                    constraint.leftToRight = Constraint.UNSET;
                    constraint.leftToLeft = Constraint.UNSET;
                    constraint.rightMargin = 0;
                    break;
                case TOP:
                    constraint.topToBottom = Constraint.UNSET;
                    constraint.topToTop = Constraint.UNSET;
                    constraint.topMargin = 0;
                    break;
                case BOTTOM:
                    constraint.bottomToTop = Constraint.UNSET;
                    constraint.bottomToBottom = Constraint.UNSET;
                    constraint.bottomMargin = 0;
                    break;
                case BASELINE:

                    constraint.baselineToBaseline = Constraint.UNSET;
                    break;
                case START:
                    constraint.startToEnd = Constraint.UNSET;
                    constraint.startToStart = Constraint.UNSET;
                    constraint.startMargin = 0;
                    break;
                case END:
                    constraint.endToStart = Constraint.UNSET;
                    constraint.endToEnd = Constraint.UNSET;
                    constraint.endMargin = 0;
                    break;
                default:
                    throw new IllegalArgumentException("unknown constraint");
            }
        }
    }

    /**
     * Sets the margin.
     *
     * @param viewId ID of view to adjust the margin on
     * @param anchor The side to adjust the margin on
     * @param value  The new value for the margin
     */
    public void setMargin(int viewId, int anchor, int value) {
        Constraint constraint = get(viewId);
        switch (anchor) {
            case LEFT:
                constraint.leftMargin = value;
                break;
            case RIGHT:
                constraint.rightMargin = value;
                break;
            case TOP:
                constraint.topMargin = value;
                break;
            case BOTTOM:
                constraint.bottomMargin = value;
                break;
            case BASELINE:
                throw new IllegalArgumentException("baseline does not support margins");
            case START:
                constraint.startMargin = value;
                break;
            case END:
                constraint.endMargin = value;
                break;
            default:
                throw new IllegalArgumentException("unknown constraint");
        }
    }

    /**
     * Adjust the horizontal bias of the view (used with views constrained on left and right).
     *
     * @param viewId ID of view to adjust the horizontal
     * @param bias   the new bias 0.5 is in the middle
     */
    public void setHorizontalBias(int viewId, float bias) {
        get(viewId).horizontalBias = bias;
    }

    /**
     * Adjust the vertical bias of the view (used with views constrained on left and right).
     *
     * @param viewId ID of view to adjust the vertical
     * @param bias   the new bias 0.5 is in the middle
     */
    public void setVerticalBias(int viewId, float bias) {
        get(viewId).verticalBias = bias;
    }

    /**
     * Adjust the visibility of a view.
     *
     * @param viewId     ID of view to adjust the vertical
     * @param visibility the visibility
     */
    public void setVisibility(int viewId, int visibility) {
        get(viewId).visibility = visibility;
    }

    /**
     * Sets the height of the view. It can be a dimension, {@link #WRAP_CONTENT} or {@link #MATCH_CONSTRAINT}.
     *
     * @param viewId ID of view to adjust it height
     * @param height the height of the constraint
     */
    public void constrainHeight(int viewId, int height) {
        get(viewId).mHeight = height;
    }

    /**
     * Sets the width of the view. It can be a dimension, {@link #WRAP_CONTENT} or {@link #MATCH_CONSTRAINT}.
     *
     * @param viewId ID of view to adjust it height
     * @param width  the width of the view
     */
    public void constrainWidth(int viewId, int width) {
        get(viewId).mWidth = width;
    }

    /**
     * Creates a ConstraintLayout virtual object. Currently only horizontal or vertical GuideLines.
     *
     * @param guidelineID ID of guideline to create
     * @param orientation the Orientation of the guideline
     */
    public void create(int guidelineID, int orientation) {
        Constraint constraint = get(guidelineID);
        constraint.mIsGuideline = true;
        constraint.orientation = orientation;
    }

    /**
     * Set the guideline's distance form the top or left edge.
     *
     * @param guidelineID ID of the guideline
     * @param margin the distance to the top or left edge
     */
    public void setGuidelineBegin(int guidelineID, int margin) {
        get(guidelineID).guideBegin = margin;
        get(guidelineID).guideEnd = Constraint.UNSET;
        get(guidelineID).guidePercent = 0.5f;

    }

    /**
     * Set a guideline's distance to end.
     *
     * @param guidelineID ID of the guideline
     * @param margin      the margin to the right or bottom side of container
     */
    public void setGuidelineEnd(int guidelineID, int margin) {
        get(guidelineID).guideEnd = margin;
        get(guidelineID).guideBegin = Constraint.UNSET;
        get(guidelineID).guidePercent = 0.5f;
    }

    /**
     * Set a Guideline's percent.
     *
     * @param guidelineID ID of the guideline
     * @param ratio       the ratio between the gap on the left and right 0.0 is top/left 0.5 is middle
     */
    public void setGuidelinePercent(int guidelineID, float ratio) {
        get(guidelineID).guidePercent = ratio;
        get(guidelineID).guideEnd = Constraint.UNSET;
        get(guidelineID).guideBegin = Constraint.UNSET;
    }

    private Constraint get(int id) {
        if (!mConstraints.containsKey(id)) {
            mConstraints.put(id, new Constraint());
        }
        return mConstraints.get(id);
    }

    private String sideToString(int side) {
        switch (side) {
            case LEFT:
                return "left";
            case RIGHT:
                return "right";
            case TOP:
                return "top";
            case BOTTOM:
                return "bottom";
            case BASELINE:
                return "baseline";
            case START:
                return "start";
            case END:
                return "end";
        }
        return "undefined";
    }
}
