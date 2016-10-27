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
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.os.Build;
import android.util.*;
import android.view.LayoutInflater;
import android.view.View;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.lang.reflect.Field;
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
 * {@sample resources/examples/ExampleConstraintSet.java
 * Example}
 */
public class ConstraintSet {
    private static final String TAG = "ConstraintSet";

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
     * <li>{@link #connect(int, int, int, int, int)}</li>
     * <li>{@link #center(int, int, int, int, int, int, int, float)}</li>
     * </ul>
     */
    public static final int PARENT_ID = ConstraintLayout.LayoutParams.PARENT_ID;

    /**
     * The horizontal orientation.
     */
    public static final int HORIZONTAL = ConstraintLayout.LayoutParams.HORIZONTAL;

    /**
     * The vertical orientation.
     */
    public static final int VERTICAL = ConstraintLayout.LayoutParams.VERTICAL;

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
    public static final int RIGHT = ConstraintLayout.LayoutParams.RIGHT;

    /**
     * The top of a view.
     */
    public static final int TOP = ConstraintLayout.LayoutParams.TOP;
    ;

    /**
     * The bottom side of a view.
     */
    public static final int BOTTOM = ConstraintLayout.LayoutParams.BOTTOM;
    ;

    /**
     * The baseline of the text in a view.
     */
    public static final int BASELINE = ConstraintLayout.LayoutParams.BASELINE;
    ;

    /**
     * The left side of a view in left to right languages.
     * In right to left languages it corresponds to the right side of the view
     */
    public static final int START = ConstraintLayout.LayoutParams.START;

    /**
     * The right side of a view in right to left languages.
     * In right to left languages it corresponds to the left side of the view
     */
    public static final int END = ConstraintLayout.LayoutParams.END;
    private static final boolean DEBUG = false;
    private static final int[] VISIBILITY_FLAGS = new int[]{VISIBLE, INVISIBLE, GONE};

    private HashMap<Integer, Constraint> mConstraints = new HashMap<Integer, Constraint>();

    private static SparseIntArray mapToConstant = new SparseIntArray();
    private static final int BASELINE_TO_BASELINE = 1;
    private static final int BOTTOM_MARGIN = 2;
    private static final int BOTTOM_TO_BOTTOM = 3;
    private static final int BOTTOM_TO_TOP = 4;
    private static final int DIMENSION_RATIO = 5;
    private static final int EDITOR_ABSOLUTE_X = 6;
    private static final int EDITOR_ABSOLUTE_Y = 7;
    private static final int END_MARGIN = 8;
    private static final int END_TO_END = 9;
    private static final int END_TO_START = 10;
    private static final int GONE_BOTTOM_MARGIN = 11;
    private static final int GONE_END_MARGIN = 12;
    private static final int GONE_LEFT_MARGIN = 13;
    private static final int GONE_RIGHT_MARGIN = 14;
    private static final int GONE_START_MARGIN = 15;
    private static final int GONE_TOP_MARGIN = 16;
    private static final int GUIDE_BEGIN = 17;
    private static final int GUIDE_END = 18;
    private static final int GUIDE_PERCENT = 19;
    private static final int HORIZONTAL_BIAS = 20;
    private static final int LAYOUT_HEIGHT = 21;
    private static final int LAYOUT_VISIBILITY = 22;
    private static final int LAYOUT_WIDTH = 23;
    private static final int LEFT_MARGIN = 24;
    private static final int LEFT_TO_LEFT = 25;
    private static final int LEFT_TO_RIGHT = 26;
    private static final int ORIENTATION = 27;
    private static final int RIGHT_MARGIN = 28;
    private static final int RIGHT_TO_LEFT = 29;
    private static final int RIGHT_TO_RIGHT = 30;
    private static final int START_MARGIN = 31;
    private static final int START_TO_END = 32;
    private static final int START_TO_START = 33;
    private static final int TOP_MARGIN = 34;
    private static final int TOP_TO_BOTTOM = 35;
    private static final int TOP_TO_TOP = 36;
    private static final int VERTICAL_BIAS = 37;
    private static final int VIEW_ID = 38;
    private static final int HORIZONTAL_WEIGHT = 39;
    private static final int VERTICAL_WEIGHT = 40;
    private static final int HORIZONTAL_STYLE = 41;
    private static final int VERTICAL_STYLE = 42;


    private static final int UNUSED = 43;

    static {
        mapToConstant.append(R.styleable.ConstraintSet_layout_constraintLeft_toLeftOf, LEFT_TO_LEFT);
        mapToConstant.append(R.styleable.ConstraintSet_layout_constraintLeft_toRightOf, LEFT_TO_RIGHT);
        mapToConstant.append(R.styleable.ConstraintSet_layout_constraintRight_toLeftOf, RIGHT_TO_LEFT);
        mapToConstant.append(R.styleable.ConstraintSet_layout_constraintRight_toRightOf, RIGHT_TO_RIGHT);
        mapToConstant.append(R.styleable.ConstraintSet_layout_constraintTop_toTopOf, TOP_TO_TOP);
        mapToConstant.append(R.styleable.ConstraintSet_layout_constraintTop_toBottomOf, TOP_TO_BOTTOM);
        mapToConstant.append(R.styleable.ConstraintSet_layout_constraintBottom_toTopOf, BOTTOM_TO_TOP);
        mapToConstant.append(R.styleable.ConstraintSet_layout_constraintBottom_toBottomOf, BOTTOM_TO_BOTTOM);
        mapToConstant.append(R.styleable.ConstraintSet_layout_constraintBaseline_toBaselineOf, BASELINE_TO_BASELINE);

        mapToConstant.append(R.styleable.ConstraintSet_layout_editor_absoluteX, EDITOR_ABSOLUTE_X);
        mapToConstant.append(R.styleable.ConstraintSet_layout_editor_absoluteY, EDITOR_ABSOLUTE_Y);
        mapToConstant.append(R.styleable.ConstraintSet_layout_constraintGuide_begin, GUIDE_BEGIN);
        mapToConstant.append(R.styleable.ConstraintSet_layout_constraintGuide_end, GUIDE_END);
        mapToConstant.append(R.styleable.ConstraintSet_layout_constraintGuide_percent, GUIDE_PERCENT);
        mapToConstant.append(R.styleable.ConstraintSet_android_orientation, ORIENTATION);
        mapToConstant.append(R.styleable.ConstraintSet_layout_constraintStart_toEndOf, START_TO_END);
        mapToConstant.append(R.styleable.ConstraintSet_layout_constraintStart_toStartOf, START_TO_START);
        mapToConstant.append(R.styleable.ConstraintSet_layout_constraintEnd_toStartOf, END_TO_START);
        mapToConstant.append(R.styleable.ConstraintSet_layout_constraintEnd_toEndOf, END_TO_END);
        mapToConstant.append(R.styleable.ConstraintSet_layout_goneMarginLeft, GONE_LEFT_MARGIN);
        mapToConstant.append(R.styleable.ConstraintSet_layout_goneMarginTop, GONE_TOP_MARGIN);
        mapToConstant.append(R.styleable.ConstraintSet_layout_goneMarginRight, GONE_RIGHT_MARGIN);
        mapToConstant.append(R.styleable.ConstraintSet_layout_goneMarginBottom, GONE_BOTTOM_MARGIN);
        mapToConstant.append(R.styleable.ConstraintSet_layout_goneMarginStart, GONE_START_MARGIN);
        mapToConstant.append(R.styleable.ConstraintSet_layout_goneMarginEnd, GONE_END_MARGIN);
        mapToConstant.append(R.styleable.ConstraintSet_layout_constraintVertical_weight, VERTICAL_WEIGHT);
        mapToConstant.append(R.styleable.ConstraintSet_layout_constraintHorizontal_weight, HORIZONTAL_WEIGHT);
        mapToConstant.append(R.styleable.ConstraintSet_layout_constraintHorizontal_chainStyle, HORIZONTAL_STYLE);
        mapToConstant.append(R.styleable.ConstraintSet_layout_constraintVertical_chainStyle, VERTICAL_STYLE);



        mapToConstant.append(R.styleable.ConstraintSet_layout_constraintHorizontal_bias, HORIZONTAL_BIAS);
        mapToConstant.append(R.styleable.ConstraintSet_layout_constraintVertical_bias, VERTICAL_BIAS);
        mapToConstant.append(R.styleable.ConstraintSet_layout_constraintDimensionRatio, DIMENSION_RATIO);
        mapToConstant.append(R.styleable.ConstraintSet_layout_constraintLeft_creator, UNUSED);
        mapToConstant.append(R.styleable.ConstraintSet_layout_constraintTop_creator, UNUSED);
        mapToConstant.append(R.styleable.ConstraintSet_layout_constraintRight_creator, UNUSED);
        mapToConstant.append(R.styleable.ConstraintSet_layout_constraintBottom_creator, UNUSED);
        mapToConstant.append(R.styleable.ConstraintSet_layout_constraintBaseline_creator, UNUSED);
        mapToConstant.append(R.styleable.ConstraintSet_android_layout_marginLeft, LEFT_MARGIN);
        mapToConstant.append(R.styleable.ConstraintSet_android_layout_marginRight, RIGHT_MARGIN);
        mapToConstant.append(R.styleable.ConstraintSet_android_layout_marginStart, START_MARGIN);
        mapToConstant.append(R.styleable.ConstraintSet_android_layout_marginEnd, END_MARGIN);
        mapToConstant.append(R.styleable.ConstraintSet_android_layout_marginTop, TOP_MARGIN);
        mapToConstant.append(R.styleable.ConstraintSet_android_layout_marginBottom, BOTTOM_MARGIN);
        mapToConstant.append(R.styleable.ConstraintSet_android_layout_width, LAYOUT_WIDTH);
        mapToConstant.append(R.styleable.ConstraintSet_android_layout_height, LAYOUT_HEIGHT);
        mapToConstant.append(R.styleable.ConstraintSet_android_visibility, LAYOUT_VISIBILITY);
        mapToConstant.append(R.styleable.ConstraintSet_android_id, VIEW_ID);
    }

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
        public String dimensionRatio = null;

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
        public int goneLeftMargin;
        public int goneTopMargin;
        public int goneRightMargin;
        public int goneBottomMargin;
        public int goneEndMargin;
        public int goneStartMargin;
        public float verticalWeight;
        public float horizontalWeight;
        public int horizontalChainStyle;
        public int verticalChainStyle;

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
            mWidth = param.width;
            mHeight = param.height;
            leftMargin = param.leftMargin;
            rightMargin = param.rightMargin;
            topMargin = param.topMargin;
            bottomMargin = param.bottomMargin;
            verticalWeight = param.verticalWeight;
            horizontalWeight = param.horizontalWeight;
            verticalChainStyle = param.verticalChainStyle;
            horizontalChainStyle = param.horizontalChainStyle;
            int currentapiVersion = android.os.Build.VERSION.SDK_INT;
            if (currentapiVersion >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
                endMargin = param.getMarginEnd();
                startMargin = param.getMarginStart();
            }
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
            param.verticalWeight = verticalWeight;
            param.horizontalWeight = horizontalWeight;
            param.verticalChainStyle = verticalChainStyle;
            param.horizontalChainStyle = horizontalChainStyle;

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
                g.setId(id);
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
     * Spaces a set of widgets vertically between the view topId and bottomId.
     * Widgets can be spaced with weights.
     *
     * @param topId
     * @param bottomId
     * @param chainIds widgets to use as a chain
     * @param weights can be null
     * @param style set the style of the chain
     */
    public void createVerticalChain(int topId, int bottomId, int[] chainIds, float[] weights, int style) {
        if (chainIds.length < 2) {
            throw new IllegalArgumentException("must have 2 or more widgets in a chain");
        }
        if (weights != null && weights.length != chainIds.length) {
            throw new IllegalArgumentException("must have 2 or more widgets in a chain");
        }
        if (weights != null) {
            get(chainIds[0]).verticalWeight = weights[0];
        }
        get(chainIds[0]).verticalChainStyle = style;

        connect(chainIds[0], TOP, topId, TOP, 0);
        for (int i = 1; i < chainIds.length - 1; i++) {
            int chainId = chainIds[i];
            connect(chainIds[i], TOP, chainIds[i - 1], BOTTOM, 0);
            connect(chainIds[i - 1], BOTTOM, chainIds[i], TOP, 0);
            if (weights != null) {
                get(chainIds[i]).verticalWeight = weights[i];
            }
        }
        connect(chainIds[chainIds.length - 1], BOTTOM, bottomId, TOP, 0);
        if (weights != null) {
            get(chainIds[chainIds.length - 1]).verticalWeight = weights[chainIds.length - 1];
        }
    }

    /**
     * Spaces a set of widgets horizontal between the view topId and bottomId.
     * Widgets can be spaced with weights.
     *
     * @param leftId
     * @param rightId
     * @param chainIds widgets to use as a chain
     * @param weights can be null
     * @param style set the style of the chain
     */
    public void createHorizontalChain(int leftId, int rightId, int[] chainIds, float[] weights, int style) {
        if (chainIds.length < 2) {
            throw new IllegalArgumentException("must have 2 or more widgets in a chain");
        }
        if (weights != null && weights.length != chainIds.length) {
            throw new IllegalArgumentException("must have 2 or more widgets in a chain");
        }
        if (weights != null) {
            get(chainIds[0]).verticalWeight = weights[0];
        }
        get(chainIds[0]).horizontalChainStyle = style;
        connect(chainIds[0], TOP, leftId, TOP, 0);
        for (int i = 1; i < chainIds.length - 1; i++) {
            int chainId = chainIds[i];
            connect(chainIds[i], TOP, chainIds[i - 1], BOTTOM, 0);
            connect(chainIds[i - 1], BOTTOM, chainIds[i], TOP, 0);
            if (weights != null) {
                get(chainIds[i]).verticalWeight = weights[i];
            }
        }
        connect(chainIds[chainIds.length - 1], BOTTOM, rightId, TOP, 0);
        if (weights != null) {
            get(chainIds[chainIds.length - 1]).verticalWeight = weights[chainIds.length - 1];
        }
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
     * Constrains the views aspect ratio.
     * For Example a HD screen is 16 by 9 = 16/(float)9 = 1.777f.
     *
     * @param viewId ID of view to constrain
     * @param ratio  The ratio of the width to height (width / height)
     */
    public void setDimensionRatio(int viewId, String ratio) {
        get(viewId).dimensionRatio = ratio;
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
     * @param margin      the distance to the top or left edge
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

    /**
     * Load a constraint set from a constraintSet.xml file
     *
     * @param context    the context for the inflation
     * @param resourceId id of xml file in res/xml/
     */
    public void load(Context context, int resourceId) {
        Resources res = context.getResources();
        XmlPullParser parser = res.getXml(resourceId);
        String document = null;
        String tagName = null;
        try {

            for (int eventType = parser.getEventType();
                 eventType != XmlResourceParser.END_DOCUMENT;
                 eventType = parser.next()) {
                switch (eventType) {
                    case XmlResourceParser.START_DOCUMENT:
                        document = parser.getName();
                        break;
                    case XmlResourceParser.START_TAG:
                        tagName = parser.getName();
                        Constraint constraint = fillFromAttributeList(context, Xml.asAttributeSet(parser));
                        if (tagName.equalsIgnoreCase("Guideline")) {
                            constraint.mIsGuideline = true;
                        }
                        mConstraints.put(constraint.mViewId, constraint);
                        break;
                    case XmlResourceParser.END_TAG:
                        tagName = null;
                        break;
                    case XmlResourceParser.TEXT:
                        break;
                }
            }
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static int lookupID(TypedArray a, int index, int def) {
        int ret = a.getResourceId(index, def);
        if (ret == Constraint.UNSET) {
            ret = a.getInt(index, Constraint.UNSET);
        }
        return ret;
    }

    private Constraint fillFromAttributeList(Context context, AttributeSet attrs) {
        Constraint c = new Constraint();
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ConstraintSet);
        populateConstraint(c, a);
        a.recycle();
        return c;
    }

    private void populateConstraint(Constraint c, TypedArray a) {
        final int N = a.getIndexCount();
        for (int i = 0; i < N; i++) {
            int attr = a.getIndex(i);
            if (DEBUG) { // USEFUL when adding features to track tags being parsed
                try {
                    Field[] campos = R.styleable.class.getFields();
                    boolean found = false;
                    for (Field f : campos) {
                        try {
                            if (f.getType().isPrimitive() && attr == f.getInt(null) && f.getName().contains("ConstraintSet")) {
                                found = true;
                                Log.v(TAG, "L id " + f.getName() + " #" + attr);
                                break;
                            }
                        } catch (Exception e) {

                        }
                    }
                    if (!found) {
                        campos = android.R.attr.class.getFields();
                        for (Field f : campos) {
                            try {
                                if (f.getType().isPrimitive() && attr == f.getInt(null) && f.getName().contains("ConstraintSet")) {
                                    found = false;
                                    Log.v(TAG, "x id " + f.getName());
                                    break;
                                }
                            } catch (Exception e) {

                            }
                        }
                    }
                    if (!found) Log.v(TAG, " ? " + attr);
                } catch (Exception e) {
                    Log.v(TAG, " " + e.toString());
                }
            }
            switch (mapToConstant.get(attr)) {
                case LEFT_TO_LEFT:
                    c.leftToLeft = lookupID(a, attr, c.leftToLeft);
                    break;
                case LEFT_TO_RIGHT:
                    c.leftToRight = lookupID(a, attr, c.leftToRight);
                    break;
                case RIGHT_TO_LEFT:
                    c.rightToLeft = lookupID(a, attr, c.rightToLeft);
                    break;
                case RIGHT_TO_RIGHT:
                    c.rightToRight = lookupID(a, attr, c.rightToRight);
                    break;
                case TOP_TO_TOP:
                    c.topToTop = lookupID(a, attr, c.topToTop);
                    break;
                case TOP_TO_BOTTOM:
                    c.topToBottom = lookupID(a, attr, c.topToBottom);
                    break;
                case BOTTOM_TO_TOP:
                    c.bottomToTop = lookupID(a, attr, c.bottomToTop);
                    break;
                case BOTTOM_TO_BOTTOM:
                    c.bottomToBottom = lookupID(a, attr, c.bottomToBottom);
                    break;
                case BASELINE_TO_BASELINE:
                    c.baselineToBaseline = lookupID(a, attr, c.baselineToBaseline);
                    break;
                case EDITOR_ABSOLUTE_X:
                    c.editorAbsoluteX = a.getDimensionPixelOffset(attr, c.editorAbsoluteX);
                    break;
                case EDITOR_ABSOLUTE_Y:
                    c.editorAbsoluteY = a.getDimensionPixelOffset(attr, c.editorAbsoluteY);
                    break;
                case GUIDE_BEGIN:
                    c.guideBegin = a.getDimensionPixelOffset(attr, c.guideBegin);
                    break;
                case GUIDE_END:
                    c.guideEnd = a.getDimensionPixelOffset(attr, c.guideEnd);
                    break;
                case GUIDE_PERCENT:
                    c.guidePercent = a.getFloat(attr, c.guidePercent);
                    break;
                case ORIENTATION:
                    c.orientation = a.getInt(attr, c.orientation);
                    break;
                case START_TO_END:
                    c.startToEnd = lookupID(a, attr, c.startToEnd);
                    break;
                case START_TO_START:
                    c.startToStart = lookupID(a, attr, c.startToStart);
                    break;
                case END_TO_START:
                    c.endToStart = lookupID(a, attr, c.endToStart);
                    break;
                case END_TO_END:
                    c.bottomToTop = lookupID(a, attr, c.endToEnd);
                    break;
                case GONE_LEFT_MARGIN:
                    c.goneLeftMargin = a.getDimensionPixelSize(attr, c.goneLeftMargin);
                    break;
                case GONE_TOP_MARGIN:
                    c.goneTopMargin = a.getDimensionPixelSize(attr, c.goneTopMargin);
                    break;
                case GONE_RIGHT_MARGIN:
                    c.goneRightMargin = a.getDimensionPixelSize(attr, c.goneRightMargin);
                    break;
                case GONE_BOTTOM_MARGIN:
                    c.goneBottomMargin = a.getDimensionPixelSize(attr, c.goneBottomMargin);
                    break;
                case GONE_START_MARGIN:
                    c.goneStartMargin = a.getDimensionPixelSize(attr, c.goneStartMargin);
                    break;
                case GONE_END_MARGIN:
                    c.goneEndMargin = a.getDimensionPixelSize(attr, c.goneEndMargin);
                    break;
                case HORIZONTAL_BIAS:
                    c.horizontalBias = a.getFloat(attr, c.horizontalBias);
                    break;
                case VERTICAL_BIAS:
                    c.verticalBias = a.getFloat(attr, c.verticalBias);
                    break;
                case LEFT_MARGIN:
                    c.leftMargin = a.getDimensionPixelSize(attr, c.leftMargin);
                    break;
                case RIGHT_MARGIN:
                    c.rightMargin = a.getDimensionPixelSize(attr, c.rightMargin);
                    break;
                case START_MARGIN:
                    c.startMargin = a.getDimensionPixelSize(attr, c.startMargin);
                    break;
                case END_MARGIN:
                    c.endMargin = a.getDimensionPixelSize(attr, c.endMargin);
                    break;
                case TOP_MARGIN:
                    c.topMargin = a.getDimensionPixelSize(attr, c.topMargin);
                    break;
                case BOTTOM_MARGIN:
                    c.bottomMargin = a.getDimensionPixelSize(attr, c.bottomMargin);
                    break;
                case LAYOUT_WIDTH:
                    c.mWidth = a.getLayoutDimension(attr, c.mWidth);
                    break;
                case LAYOUT_HEIGHT:
                    c.mHeight = a.getLayoutDimension(attr, c.mHeight);
                    break;
                case LAYOUT_VISIBILITY:
                    c.visibility = a.getInt(attr, c.visibility);
                    c.visibility = VISIBILITY_FLAGS[c.visibility];
                    break;
                case VERTICAL_WEIGHT:
                    c.verticalWeight = a.getFloat(attr, c.verticalWeight);
                    break;
                case HORIZONTAL_WEIGHT:
                    c.horizontalWeight = a.getFloat(attr, c.horizontalWeight);
                    break;
                case VERTICAL_STYLE:
                    c.verticalChainStyle = a.getInt(attr, c.verticalChainStyle);
                    break;
                case HORIZONTAL_STYLE:
                    c.horizontalChainStyle = a.getInt(attr, c.horizontalChainStyle);
                    break;
                case VIEW_ID:
                    c.mViewId = a.getResourceId(attr, c.mViewId);
                    break;
                case DIMENSION_RATIO:
                    c.dimensionRatio = a.getString(attr);
                    break;
                default:
                    Log.w(TAG, "Unknown attribute 0x" + Integer.toHexString(attr) + "   " + mapToConstant.get(attr));
            }
        }
    }

}
