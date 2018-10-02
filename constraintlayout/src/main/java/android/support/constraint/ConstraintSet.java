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
import android.os.Build.VERSION_CODES;
import android.support.constraint.ConstraintLayout.LayoutParams;
import android.support.constraint.solver.widgets.ConstraintWidget;
import android.util.*;
import android.view.LayoutInflater;
import android.view.View;

import java.util.Arrays;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.lang.reflect.Array;
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
     * Used to indicate a parameter is cleared or not set
     */
    public static final int UNSET = LayoutParams.UNSET;

    /**
     * Dimension will be controlled by constraints
     */
    public static final int MATCH_CONSTRAINT = ConstraintLayout.LayoutParams.MATCH_CONSTRAINT;

    /**
     * Dimension will set by the view's content
     */
    public static final int WRAP_CONTENT = ConstraintLayout.LayoutParams.WRAP_CONTENT;

    /**
     * How to calculate the size of a view in 0 dp by using its wrap_content size
     */
    public static final int MATCH_CONSTRAINT_WRAP = ConstraintLayout.LayoutParams.MATCH_CONSTRAINT_WRAP;

    /**
     * Calculate the size of a view in 0 dp by reducing the constrains gaps as much as possible
     */
    public static final int MATCH_CONSTRAINT_SPREAD = ConstraintLayout.LayoutParams.MATCH_CONSTRAINT_SPREAD;

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

    /**
     * Chain spread style
     */
    public static final int CHAIN_SPREAD = ConstraintLayout.LayoutParams.CHAIN_SPREAD;

    /**
     * Chain spread inside style
     */
    public static final int CHAIN_SPREAD_INSIDE = ConstraintLayout.LayoutParams.CHAIN_SPREAD_INSIDE;

    /**
     * Chain packed style
     */
    public static final int CHAIN_PACKED = ConstraintLayout.LayoutParams.CHAIN_PACKED;

    private static final boolean DEBUG = false;
    private static final int[] VISIBILITY_FLAGS = new int[]{VISIBLE, INVISIBLE, GONE};
    private static final int BARRIER_TYPE = 1;

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
    private static final int ALPHA = 43;
    private static final int ELEVATION = 44;
    private static final int ROTATION_X = 45;
    private static final int ROTATION_Y = 46;
    private static final int SCALE_X = 47;
    private static final int SCALE_Y = 48;
    private static final int TRANSFORM_PIVOT_X = 49;
    private static final int TRANSFORM_PIVOT_Y = 50;
    private static final int TRANSLATION_X = 51;
    private static final int TRANSLATION_Y = 52;
    private static final int TRANSLATION_Z = 53;
    private static final int WIDTH_DEFAULT = 54;
    private static final int HEIGHT_DEFAULT = 55;
    private static final int WIDTH_MAX = 56;
    private static final int HEIGHT_MAX = 57;
    private static final int WIDTH_MIN = 58;
    private static final int HEIGHT_MIN = 59;
    private static final int ROTATION = 60;
    private static final int CIRCLE = 61;
    private static final int CIRCLE_RADIUS = 62;
    private static final int CIRCLE_ANGLE = 63;

    private static final int WIDTH_PERCENT = 69;
    private static final int HEIGHT_PERCENT = 70;
    private static final int CHAIN_USE_RTL = 71;
    private static final int BARRIER_DIRECTION = 72;
    private static final int CONSTRAINT_REFERENCED_IDS = 73;
    private static final int BARRIER_ALLOWS_GONE_WIDGETS = 74;
    private static final int UNUSED = 75;

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
        mapToConstant.append(R.styleable.ConstraintSet_android_alpha, ALPHA);
        mapToConstant.append(R.styleable.ConstraintSet_android_elevation, ELEVATION);
        mapToConstant.append(R.styleable.ConstraintSet_android_rotationX, ROTATION_X);
        mapToConstant.append(R.styleable.ConstraintSet_android_rotationY, ROTATION_Y);
        mapToConstant.append(R.styleable.ConstraintSet_android_rotation, ROTATION);
        mapToConstant.append(R.styleable.ConstraintSet_android_scaleX, SCALE_X);
        mapToConstant.append(R.styleable.ConstraintSet_android_scaleY, SCALE_Y);
        mapToConstant.append(R.styleable.ConstraintSet_android_transformPivotX, TRANSFORM_PIVOT_X);
        mapToConstant.append(R.styleable.ConstraintSet_android_transformPivotY, TRANSFORM_PIVOT_Y);
        mapToConstant.append(R.styleable.ConstraintSet_android_translationX, TRANSLATION_X);
        mapToConstant.append(R.styleable.ConstraintSet_android_translationY, TRANSLATION_Y);
        mapToConstant.append(R.styleable.ConstraintSet_android_translationZ, TRANSLATION_Z);
        mapToConstant.append(R.styleable.ConstraintSet_layout_constraintWidth_default, WIDTH_DEFAULT);
        mapToConstant.append(R.styleable.ConstraintSet_layout_constraintHeight_default, HEIGHT_DEFAULT);
        mapToConstant.append(R.styleable.ConstraintSet_layout_constraintWidth_max, WIDTH_MAX);
        mapToConstant.append(R.styleable.ConstraintSet_layout_constraintHeight_max, HEIGHT_MAX);
        mapToConstant.append(R.styleable.ConstraintSet_layout_constraintWidth_min, WIDTH_MIN);
        mapToConstant.append(R.styleable.ConstraintSet_layout_constraintHeight_min, HEIGHT_MIN);
        mapToConstant.append(R.styleable.ConstraintSet_layout_constraintCircle, CIRCLE);
        mapToConstant.append(R.styleable.ConstraintSet_layout_constraintCircleRadius, CIRCLE_RADIUS);
        mapToConstant.append(R.styleable.ConstraintSet_layout_constraintCircleAngle, CIRCLE_ANGLE);
        mapToConstant.append(R.styleable.ConstraintSet_android_id, VIEW_ID);

        mapToConstant.append(R.styleable.ConstraintSet_layout_constraintWidth_percent, WIDTH_PERCENT);
        mapToConstant.append(R.styleable.ConstraintSet_layout_constraintHeight_percent, HEIGHT_PERCENT);

        mapToConstant.append(R.styleable.ConstraintSet_chainUseRtl, CHAIN_USE_RTL);
        mapToConstant.append(R.styleable.ConstraintSet_barrierDirection, BARRIER_DIRECTION);
        mapToConstant.append(R.styleable.ConstraintSet_constraint_referenced_ids, CONSTRAINT_REFERENCED_IDS);
        mapToConstant.append(R.styleable.ConstraintSet_barrierAllowsGoneWidgets, BARRIER_ALLOWS_GONE_WIDGETS);
    }

    public Constraint getParameters(int mId) {
        return  get(mId);
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

        public int circleConstraint = UNSET;
        public int circleRadius = 0;
        public float circleAngle = 0;

        public int editorAbsoluteX = UNSET;
        public int editorAbsoluteY = UNSET;

        public int orientation = UNSET;
        public int leftMargin = UNSET;
        public int rightMargin = UNSET;
        public int topMargin = UNSET;
        public int bottomMargin = UNSET;
        public int endMargin = UNSET;
        public int startMargin = UNSET;
        public int visibility = VISIBLE;
        public int goneLeftMargin = UNSET;
        public int goneTopMargin = UNSET;
        public int goneRightMargin = UNSET;
        public int goneBottomMargin = UNSET;
        public int goneEndMargin = UNSET;
        public int goneStartMargin = UNSET;
        public float verticalWeight = 0;
        public float horizontalWeight = 0;
        public int horizontalChainStyle = CHAIN_SPREAD;
        public int verticalChainStyle = CHAIN_SPREAD;
        public float alpha = 1;
        public boolean applyElevation = false;
        public float elevation = 0;
        public float rotation = 0;
        public float rotationX = 0;
        public float rotationY = 0;
        public float scaleX = 1;
        public float scaleY = 1;
        public float transformPivotX = Float.NaN;
        public float transformPivotY = Float.NaN;
        public float translationX = 0;
        public float translationY = 0;
        public float translationZ = 0;
        public boolean constrainedWidth = false;
        public boolean constrainedHeight = false;
        public int widthDefault = ConstraintWidget.MATCH_CONSTRAINT_SPREAD;
        public int heightDefault = ConstraintWidget.MATCH_CONSTRAINT_SPREAD;
        public int widthMax = UNSET;
        public int heightMax = UNSET;
        public int widthMin = UNSET;
        public int heightMin = UNSET;
        public float widthPercent = 1;
        public float heightPercent = 1;
	public boolean mBarrierAllowsGoneWidgets = false;
        public int mBarrierDirection = UNSET;
        public int mHelperType = UNSET;
        public int [] mReferenceIds;
	public String mReferenceIdString;

        public Constraint clone() {
            Constraint clone = new Constraint();
            clone.mIsGuideline = mIsGuideline;
            clone.mWidth = mWidth;
            clone.mHeight = mHeight;
            clone.guideBegin = guideBegin;
            clone.guideEnd = guideEnd;
            clone.guidePercent = guidePercent;
            clone.leftToLeft = leftToLeft;
            clone.leftToRight = leftToRight;
            clone.rightToLeft = rightToLeft;
            clone.rightToRight = rightToRight;
            clone.topToTop = topToTop;
            clone.topToBottom = topToBottom;
            clone.bottomToTop = bottomToTop;
            clone.bottomToBottom = bottomToBottom;
            clone.baselineToBaseline = baselineToBaseline;
            clone.startToEnd = startToEnd;
            clone.startToStart = startToStart;
            clone.endToStart = endToStart;
            clone.endToEnd = endToEnd;
            clone.horizontalBias = horizontalBias;
            clone.verticalBias = verticalBias;
            clone.dimensionRatio = dimensionRatio;
            clone.editorAbsoluteX = editorAbsoluteX;
            clone.editorAbsoluteY = editorAbsoluteY;
            clone.horizontalBias = horizontalBias;
            clone.horizontalBias = horizontalBias;
            clone.horizontalBias = horizontalBias;
            clone.horizontalBias = horizontalBias;
            clone.horizontalBias = horizontalBias;
            clone.orientation = orientation;
            clone.leftMargin = leftMargin;
            clone.rightMargin = rightMargin;
            clone.topMargin = topMargin;
            clone.bottomMargin = bottomMargin;
            clone.endMargin = endMargin;
            clone.startMargin = startMargin;
            clone.visibility = visibility;
            clone.goneLeftMargin = goneLeftMargin;
            clone.goneTopMargin = goneTopMargin;
            clone.goneRightMargin = goneRightMargin;
            clone.goneBottomMargin = goneBottomMargin;
            clone.goneEndMargin = goneEndMargin;
            clone.goneStartMargin = goneStartMargin;
            clone.verticalWeight = verticalWeight;
            clone.horizontalWeight = horizontalWeight;
            clone.horizontalChainStyle = horizontalChainStyle;
            clone.verticalChainStyle = verticalChainStyle;
            clone.alpha = alpha;
            clone.applyElevation = applyElevation;
            clone.elevation = elevation;
            clone.rotation = rotation;
            clone.rotationX = rotationX;
            clone.rotationY = rotationY;
            clone.scaleX = scaleX;
            clone.scaleY = scaleY;
            clone.transformPivotX = transformPivotX;
            clone.transformPivotY = transformPivotY;
            clone.translationX = translationX;
            clone.translationY = translationY;
            clone.translationZ = translationZ;
            clone.constrainedWidth = constrainedWidth;
            clone.constrainedHeight = constrainedHeight;
            clone.widthDefault = widthDefault;
            clone.heightDefault = heightDefault;
            clone.widthMax = widthMax;
            clone.heightMax = heightMax;
            clone.widthMin = widthMin;
            clone.heightMin = heightMin;
            clone.widthPercent = widthPercent;
            clone.heightPercent = heightPercent;
            clone.mBarrierDirection = mBarrierDirection;
            clone.mHelperType = mHelperType;
            if (mReferenceIds != null) {
                clone.mReferenceIds = Arrays.copyOf(mReferenceIds, mReferenceIds.length);
            }
            clone.circleConstraint = circleConstraint;
            clone.circleRadius = circleRadius;
            clone.circleAngle = circleAngle;
            clone.mBarrierAllowsGoneWidgets = mBarrierAllowsGoneWidgets;
            return clone;
        }

        private void fillFromConstraints(ConstraintHelper helper, int viewId, Constraints.LayoutParams param) {
            fillFromConstraints(viewId,param);
            if (helper instanceof Barrier) {
                mHelperType = BARRIER_TYPE;
                Barrier barrier = (Barrier)helper;
                mBarrierDirection = barrier.getType();
                mReferenceIds = barrier.getReferencedIds();
            }
        }

        private void fillFromConstraints(int viewId, Constraints.LayoutParams param) {
            fillFrom(viewId, param);
            alpha = param.alpha;
            rotation = param.rotation;
            rotationX = param.rotationX;
            rotationY = param.rotationY;
            scaleX = param.scaleX;
            scaleY = param.scaleY;
            transformPivotX = param.transformPivotX;
            transformPivotY = param.transformPivotY;
            translationX = param.translationX;
            translationY = param.translationY;
            translationZ = param.translationZ;
            elevation = param.elevation;
            applyElevation = param.applyElevation;
        }

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

            circleConstraint = param.circleConstraint;
            circleRadius = param.circleRadius;
            circleAngle = param.circleAngle;

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
            constrainedWidth = param.constrainedWidth;
            constrainedHeight = param.constrainedHeight;
            widthDefault = param.matchConstraintDefaultWidth;
            heightDefault = param.matchConstraintDefaultHeight;
            constrainedWidth = param.constrainedWidth;
            widthMax = param.matchConstraintMaxWidth;
            heightMax = param.matchConstraintMaxHeight;
            widthMin = param.matchConstraintMinWidth;
            heightMin = param.matchConstraintMinHeight;
            widthPercent = param.matchConstraintPercentWidth;
            heightPercent = param.matchConstraintPercentHeight;

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
            param.goneStartMargin = goneStartMargin;
            param.goneEndMargin = goneEndMargin;

            param.horizontalBias = horizontalBias;
            param.verticalBias = verticalBias;

            param.circleConstraint = circleConstraint;
            param.circleRadius = circleRadius;
            param.circleAngle = circleAngle;

            param.dimensionRatio = dimensionRatio;
            param.editorAbsoluteX = editorAbsoluteX;
            param.editorAbsoluteY = editorAbsoluteY;
            param.verticalWeight = verticalWeight;
            param.horizontalWeight = horizontalWeight;
            param.verticalChainStyle = verticalChainStyle;
            param.horizontalChainStyle = horizontalChainStyle;
            param.constrainedWidth = constrainedWidth;
            param.constrainedHeight = constrainedHeight;
            param.matchConstraintDefaultWidth = widthDefault;
            param.matchConstraintDefaultHeight = heightDefault;
            param.matchConstraintMaxWidth = widthMax;
            param.matchConstraintMaxHeight = heightMax;
            param.matchConstraintMinWidth = widthMin;
            param.matchConstraintMinHeight = heightMin;
            param.matchConstraintPercentWidth = widthPercent;
            param.matchConstraintPercentHeight = heightPercent;
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
     * @param context the context for the layout inflation
     * @param constraintLayoutId the id of the layout file
     */
    public void clone(Context context, int constraintLayoutId) {
        clone((ConstraintLayout) LayoutInflater.from(context).inflate(constraintLayoutId, null));
    }

    /**
     * Copy the constraints from a layout.
     *
     * @param set constraint set to copy
     */
    public void clone(ConstraintSet set) {
        mConstraints.clear();
        for (Integer key : set.mConstraints.keySet()) {
            mConstraints.put(key, set.mConstraints.get(key).clone());
        }
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
            if (id == -1) {
                throw new RuntimeException("All children of ConstraintLayout must have ids to use ConstraintSet");
            }
            if (!mConstraints.containsKey(id)) {
                mConstraints.put(id, new Constraint());
            }
            Constraint constraint = mConstraints.get(id);
            constraint.fillFrom(id, param);
            constraint.visibility = view.getVisibility();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                constraint.alpha = view.getAlpha();
                constraint.rotation = view.getRotation();
                constraint.rotationX = view.getRotationX();
                constraint.rotationY = view.getRotationY();
                constraint.scaleX = view.getScaleX();
                constraint.scaleY = view.getScaleY();

                float pivotX = view.getPivotX(); // we assume it is not set if set to 0.0
                float pivotY = view.getPivotY(); // we assume it is not set if set to 0.0

                if (pivotX != 0.0 || pivotY != 0.0) {
                    constraint.transformPivotX = pivotX;
                    constraint.transformPivotY = pivotY;
                }

                constraint.translationX = view.getTranslationX();
                constraint.translationY = view.getTranslationY();
                if (Build.VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
                    constraint.translationZ = view.getTranslationZ();
                    if (constraint.applyElevation) {
                        constraint.elevation = view.getElevation();
                    }
                }
            }
            if (view instanceof Barrier) {
                Barrier barrier = ((Barrier) view);
                constraint.mBarrierAllowsGoneWidgets = barrier.allowsGoneWidget();
                constraint.mReferenceIds = barrier.getReferencedIds();
                constraint.mBarrierDirection = barrier.getType();
            }
        }
    }

    /**
     * Copy the layout parameters of a ConstraintLayout.
     *
     * @param constraints The ConstraintLayout to be copied
     */
    public void clone(Constraints constraints) {
        int count = constraints.getChildCount();
        mConstraints.clear();
        for (int i = 0; i < count; i++) {
            View view = constraints.getChildAt(i);
            Constraints.LayoutParams param = (Constraints.LayoutParams) view.getLayoutParams();

            int id = view.getId();
            if (id == -1) {
                throw new RuntimeException("All children of ConstraintLayout must have ids to use ConstraintSet");
            }
            if (!mConstraints.containsKey(id)) {
                mConstraints.put(id, new Constraint());
            }
            Constraint constraint = mConstraints.get(id);
            if (view instanceof ConstraintHelper) {
                ConstraintHelper helper = (ConstraintHelper) view;
                constraint.fillFromConstraints(helper, id, param);
            }
            constraint.fillFromConstraints(id, param);
        }
    }

    /**
     * Apply the constraints to a ConstraintLayout.
     *
     * @param constraintLayout to be modified
     */
    public void applyTo(ConstraintLayout constraintLayout) {
        applyToInternal(constraintLayout);
        constraintLayout.setConstraintSet(null);
    }

    /**
     * Used to set constraints when used by constraint layout
     */
    void applyToInternal(ConstraintLayout constraintLayout) {
        int count = constraintLayout.getChildCount();
        HashSet<Integer> used = new HashSet<Integer>(mConstraints.keySet());

        for (int i = 0; i < count; i++) {
            View view = constraintLayout.getChildAt(i);
            int id = view.getId();
            if (id == -1) {
                throw new RuntimeException("All children of ConstraintLayout must have ids to use ConstraintSet");
            }
            if (mConstraints.containsKey(id)) {
                used.remove(id);
                Constraint constraint = mConstraints.get(id);
                if (view instanceof Barrier) {
                    constraint.mHelperType = BARRIER_TYPE;
                }
                if (constraint.mHelperType != UNSET) {
                    switch (constraint.mHelperType) {
                        case BARRIER_TYPE:
                            Barrier barrier = (Barrier) view;
                            barrier.setId(id);
                            barrier.setType(constraint.mBarrierDirection);
                            barrier.setAllowsGoneWidget(constraint.mBarrierAllowsGoneWidgets);
                            if (constraint.mReferenceIds != null) {
                                barrier.setReferencedIds(constraint.mReferenceIds);
                            } else if (constraint.mReferenceIdString != null) {
                                constraint.mReferenceIds = convertReferenceString(barrier,
                                        constraint.mReferenceIdString);
                                barrier.setReferencedIds(constraint.mReferenceIds);
                            }
                            break;
                    }
                }
                ConstraintLayout.LayoutParams param = (ConstraintLayout.LayoutParams) view
                    .getLayoutParams();
                constraint.applyTo(param);
                view.setLayoutParams(param);
                view.setVisibility(constraint.visibility);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    view.setAlpha(constraint.alpha);
                    view.setRotation(constraint.rotation);
                    view.setRotationX(constraint.rotationX);
                    view.setRotationY(constraint.rotationY);
                    view.setScaleX(constraint.scaleX);
                    view.setScaleY(constraint.scaleY);
                    if (!Float.isNaN(constraint.transformPivotX)) {
                        view.setPivotX(constraint.transformPivotX);
                    }
                    if (!Float.isNaN(constraint.transformPivotY)) {
                        view.setPivotY(constraint.transformPivotY);
                    }
                    view.setTranslationX(constraint.translationX);
                    view.setTranslationY(constraint.translationY);
                    if (Build.VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
                        view.setTranslationZ(constraint.translationZ);
                        if (constraint.applyElevation) {
                            view.setElevation(constraint.elevation);
                        }
                    }
                }
            }
        }
        for (Integer id : used) {
            Constraint constraint = mConstraints.get(id);
            if (constraint.mHelperType != UNSET) {
                switch (constraint.mHelperType) {
                    case BARRIER_TYPE:
                        Barrier barrier = new Barrier(constraintLayout.getContext());
                        barrier.setId(id);
                        if (constraint.mReferenceIds != null) {
                            barrier.setReferencedIds(constraint.mReferenceIds);
                        } else if (constraint.mReferenceIdString != null) {
                            constraint.mReferenceIds = convertReferenceString(barrier,
                                    constraint.mReferenceIdString);
                            barrier.setReferencedIds(constraint.mReferenceIds);
                        }
                        barrier.setType(constraint.mBarrierDirection);
                        ConstraintLayout.LayoutParams param = constraintLayout
                            .generateDefaultLayoutParams();
                        barrier.validateParams();
                        constraint.applyTo(param);
                        constraintLayout.addView(barrier, param);
                        break;
                }
            }
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
     * @param centerID ID of the widget to be centered
     * @param firstID ID of the first widget to connect the left or top of the widget to
     * @param firstSide the side of the widget to connect to
     * @param firstMargin the connection margin
     * @param secondId the ID of the second widget to connect to right or top of the widget to
     * @param secondSide the side of the widget to connect to
     * @param secondMargin the connection margin
     * @param bias the ratio between two connections
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
        } else if (firstSide == START || firstSide == END) {
            connect(centerID, START, firstID, firstSide, firstMargin);
            connect(centerID, END, secondId, secondSide, secondMargin);
            Constraint constraint = mConstraints.get(centerID);
            constraint.horizontalBias = bias;
        } else {
            connect(centerID, TOP, firstID, firstSide, firstMargin);
            connect(centerID, BOTTOM, secondId, secondSide, secondMargin);
            Constraint constraint = mConstraints.get(centerID);
            constraint.verticalBias = bias;
        }
    }

    /**
     * Centers the widget horizontally to the left and right side on another widgets sides.
     *
     * @param centerID ID of widget to be centered
     * @param leftId The Id of the widget on the left side
     * @param leftSide The side of the leftId widget to connect to
     * @param leftMargin The margin on the left side
     * @param rightId The Id of the widget on the right side
     * @param rightSide  The side  of the rightId widget to connect to
     * @param rightMargin The margin on the right side
     * @param bias The ratio of the space on the left vs. right sides 0.5 is centered (default)
     */
    public void centerHorizontally(int centerID, int leftId, int leftSide, int leftMargin,
        int rightId, int rightSide, int rightMargin, float bias) {
        connect(centerID, LEFT, leftId, leftSide, leftMargin);
        connect(centerID, RIGHT, rightId, rightSide, rightMargin);
        Constraint constraint = mConstraints.get(centerID);
        constraint.horizontalBias = bias;
    }

    /**
     * Centers the widgets horizontally to the left and right side on another widgets sides.
     *
     * @param centerID ID of widget to be centered
     * @param startId The Id of the widget on the start side (left in non rtl languages)
     * @param startSide The side of the startId widget to connect to
     * @param startMargin The margin on the start side
     * @param endId The Id of the widget on the start side (left in non rtl languages)
     * @param endSide The side of the endId widget to connect to
     * @param endMargin The margin on the end side
     * @param bias The ratio of the space on the start vs end side 0.5 is centered (default)
     */
    public void centerHorizontallyRtl(int centerID, int startId, int startSide, int startMargin,
        int endId, int endSide, int endMargin, float bias) {
        connect(centerID, START, startId, startSide, startMargin);
        connect(centerID, END, endId, endSide, endMargin);
        Constraint constraint = mConstraints.get(centerID);
        constraint.horizontalBias = bias;
    }

    /**
     * Centers the widgets Vertically to the top and bottom side on another widgets sides.
     *
     * @param centerID ID of widget to be centered
     * @param topId The Id of the widget on the top side
     * @param topSide The side of the leftId widget to connect to
     * @param topMargin The margin on the top side
     * @param bottomId The Id of the widget on the bottom side
     * @param bottomSide The side of the bottomId widget to connect to
     * @param bottomMargin The margin on the bottom side
     * @param bias The ratio of the space on the top vs. bottom sides 0.5 is centered (default)
     */
    public void centerVertically(int centerID, int topId, int topSide, int topMargin, int bottomId,
        int bottomSide, int bottomMargin, float bias) {
        connect(centerID, TOP, topId, topSide, topMargin);
        connect(centerID, BOTTOM, bottomId, bottomSide, bottomMargin);
        Constraint constraint = mConstraints.get(centerID);
        constraint.verticalBias = bias;
    }

    /**
     * Spaces a set of widgets vertically between the view topId and bottomId.
     * Widgets can be spaced with weights.
     *
     * @param topId The id of the widget to connect to or PARENT_ID
     * @param topSide the side of the start to connect to
     * @param bottomId The id of the widget to connect to or PARENT_ID
     * @param bottomSide the side of the right to connect to
     * @param chainIds widgets to use as a chain
     * @param weights can be null
     * @param style set the style of the chain
     */
    public void createVerticalChain(int topId, int topSide, int bottomId, int bottomSide, int[] chainIds, float[] weights,
        int style) {
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

        connect(chainIds[0], TOP, topId, topSide, 0);
        for (int i = 1; i < chainIds.length; i++) {
            int chainId = chainIds[i];
            connect(chainIds[i], TOP, chainIds[i - 1], BOTTOM, 0);
            connect(chainIds[i - 1], BOTTOM, chainIds[i], TOP, 0);
            if (weights != null) {
                get(chainIds[i]).verticalWeight = weights[i];
            }
        }
        connect(chainIds[chainIds.length - 1], BOTTOM, bottomId, bottomSide, 0);
    }

    /**
     * Spaces a set of widgets horizontal between the view startID and endId.
     * Widgets can be spaced with weights.
     *
     * @param leftId The id of the widget to connect to or PARENT_ID
     * @param leftSide the side of the start to connect to
     * @param rightId The id of the widget to connect to or PARENT_ID
     * @param rightSide the side of the right to connect to
     * @param chainIds The widgets in the chain
     * @param weights The weight to assign to each element in the chain or null
     * @param style The type of chain
     */
    public void createHorizontalChain(int leftId, int leftSide, int rightId, int rightSide, int[] chainIds, float[] weights,
        int style) {
        createHorizontalChain(leftId, leftSide, rightId, rightSide, chainIds, weights, style, LEFT, RIGHT);
    }

    /**
     * Spaces a set of widgets horizontal between the view startID and endId.
     * Widgets can be spaced with weights.
     *
     * @param startId The id of the widget to connect to or PARENT_ID
     * @param startSide the side of the start to connect to
     * @param endId The id of the widget to connect to or PARENT_ID
     * @param endSide the side of the end to connect to
     * @param chainIds The widgets in the chain
     * @param weights The weight to assign to each element in the chain or null
     * @param style The type of chain
     */
    public void createHorizontalChainRtl(int startId, int startSide, int endId, int endSide, int[] chainIds, float[] weights,
        int style) {
        createHorizontalChain(startId, startSide, endId, endSide, chainIds, weights, style, START, END);
    }

    private void createHorizontalChain(int leftId,int leftSide, int rightId, int rightSide, int[] chainIds, float[] weights,
        int style, int left, int right) {

        if (chainIds.length < 2) {
            throw new IllegalArgumentException("must have 2 or more widgets in a chain");
        }
        if (weights != null && weights.length != chainIds.length) {
            throw new IllegalArgumentException("must have 2 or more widgets in a chain");
        }
        if (weights != null) {
            get(chainIds[0]).horizontalWeight = weights[0];
        }
        get(chainIds[0]).horizontalChainStyle = style;
        connect(chainIds[0], left, leftId, leftSide, UNSET);
        for (int i = 1; i < chainIds.length; i++) {
            int chainId = chainIds[i];
            connect(chainIds[i], left, chainIds[i - 1], right, UNSET);
            connect(chainIds[i - 1], right, chainIds[i], left, UNSET);
            if (weights != null) {
                get(chainIds[i]).horizontalWeight = weights[i];
            }
        }

        connect(chainIds[chainIds.length - 1], right, rightId, rightSide,
            UNSET);

    }

    /**
     * Create a constraint between two widgets.
     *
     * @param startID the ID of the widget to be constrained
     * @param startSide the side of the widget to constrain
     * @param endID the id of the widget to constrain to
     * @param endSide the side of widget to constrain to
     * @param margin the margin to constrain (margin must be positive)
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
                throw new IllegalArgumentException(
                    sideToString(startSide) + " to " + sideToString(endSide) + " unknown");
        }
    }

    /**
     * Create a constraint between two widgets.
     *
     * @param startID the ID of the widget to be constrained
     * @param startSide the side of the widget to constrain
     * @param endID the id of the widget to constrain to
     * @param endSide the side of widget to constrain to
     */
    public void connect(int startID, int startSide, int endID, int endSide) {
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
                    throw new IllegalArgumentException("left to " + sideToString(endSide) + " undefined");
                }
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
                break;
            case BOTTOM:
                if (endSide == BOTTOM) {
                    constraint.bottomToBottom = endID;
                    constraint.bottomToTop = Constraint.UNSET;
                    constraint.baselineToBaseline = Constraint.UNSET;
                } else if (endSide == TOP) {
                    constraint.bottomToTop = endID;
                    constraint.bottomToBottom = Constraint.UNSET;
                    constraint.baselineToBaseline = Constraint.UNSET;
                } else {
                    throw new IllegalArgumentException("right to " + sideToString(endSide) + " undefined");
                }
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
                break;
            default:
                throw new IllegalArgumentException(
                    sideToString(startSide) + " to " + sideToString(endSide) + " unknown");
        }
    }

    /**
     * Centers the view horizontally relative to toView's position.
     *
     * @param viewId ID of view to center Horizontally
     * @param toView ID of view to center on (or in)
     */
    public void centerHorizontally(int viewId, int toView) {
        if (toView == PARENT_ID) {
            center(viewId, PARENT_ID , ConstraintSet.LEFT, 0, PARENT_ID, ConstraintSet.RIGHT, 0, 0.5f);
        } else {
            center(viewId, toView, ConstraintSet.RIGHT, 0, toView, ConstraintSet.LEFT, 0, 0.5f);
        }
    }

    /**
     * Centers the view horizontally relative to toView's position.
     *
     * @param viewId ID of view to center Horizontally
     * @param toView ID of view to center on (or in)
     */
    public void centerHorizontallyRtl(int viewId, int toView) {
        if (toView == PARENT_ID) {
            center(viewId, PARENT_ID , ConstraintSet.START, 0, PARENT_ID, ConstraintSet.END, 0, 0.5f);
        } else {
            center(viewId, toView, ConstraintSet.END, 0, toView, ConstraintSet.START, 0, 0.5f);
        }
    }


    /**
     * Centers the view vertically relative to toView's position.
     *
     * @param viewId ID of view to center Horizontally
     * @param toView ID of view to center on (or in)
     */
    public void centerVertically(int viewId, int toView) {
        if (toView == PARENT_ID) {
            center(viewId, PARENT_ID, ConstraintSet.TOP, 0, PARENT_ID, ConstraintSet.BOTTOM, 0, 0.5f);
        } else {
            center(viewId, toView, ConstraintSet.BOTTOM, 0, toView, ConstraintSet.TOP, 0, 0.5f);
        }
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
                    constraint.leftMargin = Constraint.UNSET;
                    constraint.goneLeftMargin = Constraint.UNSET;
                    break;
                case RIGHT:
                    constraint.rightToRight = Constraint.UNSET;
                    constraint.rightToLeft = Constraint.UNSET;
                    constraint.rightMargin = Constraint.UNSET;
                    constraint.goneRightMargin = Constraint.UNSET;
                    break;
                case TOP:
                    constraint.topToBottom = Constraint.UNSET;
                    constraint.topToTop = Constraint.UNSET;
                    constraint.topMargin = Constraint.UNSET;
                    constraint.goneTopMargin = Constraint.UNSET;
                    break;
                case BOTTOM:
                    constraint.bottomToTop = Constraint.UNSET;
                    constraint.bottomToBottom = Constraint.UNSET;
                    constraint.bottomMargin = Constraint.UNSET;
                    constraint.goneBottomMargin = Constraint.UNSET;
                    break;
                case BASELINE:

                    constraint.baselineToBaseline = Constraint.UNSET;
                    break;
                case START:
                    constraint.startToEnd = Constraint.UNSET;
                    constraint.startToStart = Constraint.UNSET;
                    constraint.startMargin = Constraint.UNSET;
                    constraint.goneStartMargin = Constraint.UNSET;
                    break;
                case END:
                    constraint.endToStart = Constraint.UNSET;
                    constraint.endToEnd = Constraint.UNSET;
                    constraint.endMargin = Constraint.UNSET;
                    constraint.goneEndMargin = Constraint.UNSET;
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
     * @param value The new value for the margin
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
     * Sets the gone margin.
     *
     * @param viewId ID of view to adjust the margin on
     * @param anchor The side to adjust the margin on
     * @param value The new value for the margin
     */
    public void setGoneMargin(int viewId, int anchor, int value) {
        Constraint constraint = get(viewId);
        switch (anchor) {
            case LEFT:
                constraint.goneLeftMargin = value;
                break;
            case RIGHT:
                constraint.goneRightMargin = value;
                break;
            case TOP:
                constraint.goneTopMargin = value;
                break;
            case BOTTOM:
                constraint.goneBottomMargin = value;
                break;
            case BASELINE:
                throw new IllegalArgumentException("baseline does not support margins");
            case START:
                constraint.goneStartMargin = value;
                break;
            case END:
                constraint.goneEndMargin = value;
                break;
            default:
                throw new IllegalArgumentException("unknown constraint");
        }
    }

    /**
     * Adjust the horizontal bias of the view (used with views constrained on left and right).
     *
     * @param viewId ID of view to adjust the horizontal
     * @param bias the new bias 0.5 is in the middle
     */
    public void setHorizontalBias(int viewId, float bias) {
        get(viewId).horizontalBias = bias;
    }

    /**
     * Adjust the vertical bias of the view (used with views constrained on left and right).
     *
     * @param viewId ID of view to adjust the vertical
     * @param bias the new bias 0.5 is in the middle
     */
    public void setVerticalBias(int viewId, float bias) {
        get(viewId).verticalBias = bias;
    }

    /**
     * Constrains the views aspect ratio.
     * For Example a HD screen is 16 by 9 = 16/(float)9 = 1.777f.
     *
     * @param viewId ID of view to constrain
     * @param ratio The ratio of the width to height (width / height)
     */
    public void setDimensionRatio(int viewId, String ratio) {
        get(viewId).dimensionRatio = ratio;
    }

    /**
     * Adjust the visibility of a view.
     *
     * @param viewId ID of view to adjust the vertical
     * @param visibility the visibility
     */
    public void setVisibility(int viewId, int visibility) {
        get(viewId).visibility = visibility;
    }

    /**
     * Adjust the alpha of a view.
     *
     * @param viewId ID of view to adjust the vertical
     * @param alpha the alpha
     */
    public void setAlpha(int viewId, float alpha) {
        get(viewId).alpha = alpha;
    }

    /**
     * return with the constraint set will apply elevation for the specified view.
     *
     * @return true if the elevation will be set on this view (default is false)
     */
    public boolean getApplyElevation(int viewId) {
        return get(viewId).applyElevation;
    }

    /**
     * set if elevation will be applied to the view.
     * Elevation logic is based on style and animation. By default it is not used because it would
     * lead to unexpected results.
     *
     * @param apply true if this constraint set applies elevation to this view
     */
    public void setApplyElevation(int viewId, boolean apply) {
        get(viewId).applyElevation = apply;
    }

    /**
     * Adjust the elevation of a view.
     *
     * @param viewId ID of view to adjust the elevation
     * @param elevation the elevation
     */
    public void setElevation(int viewId, float elevation) {
        get(viewId).elevation = elevation;
        get(viewId).applyElevation = true;
    }

    /**
     * Adjust the post-layout rotation about the Z axis of a view.
     *
     * @param viewId ID of view to adjust th X rotation
     * @param rotation the rotation about the X axis
     */
    public void setRotation(int viewId, float rotation) {
        get(viewId).rotation = rotation;
    }

    /**
     * Adjust the post-layout rotation about the X axis of a view.
     *
     * @param viewId ID of view to adjust th X rotation
     * @param rotationX the rotation about the X axis
     */
    public void setRotationX(int viewId, float rotationX) {
        get(viewId).rotationX = rotationX;
    }

    /**
     * Adjust the post-layout rotation about the Y axis of a view.
     *
     * @param viewId ID of view to adjust the Y rotation
     * @param rotationY the rotationY
     */
    public void setRotationY(int viewId, float rotationY) {
        get(viewId).rotationY = rotationY;
    }

    /**
     * Adjust the post-layout scale in X of a view.
     *
     * @param viewId ID of view to adjust the scale in X
     * @param scaleX the scale in X
     */
    public void setScaleX(int viewId, float scaleX) {
        get(viewId).scaleX = scaleX;
    }

    /**
     * Adjust the post-layout scale in Y of a view.
     *
     * @param viewId ID of view to adjust the scale in Y
     * @param scaleY the scale in Y
     */
    public void setScaleY(int viewId, float scaleY) {
        get(viewId).scaleY = scaleY;
    }

    /**
     * Set X location of the pivot point around which the view will rotate and scale.
     * use Float.NaN to clear the pivot value.
     * Note: once an actual View has had its pivot set it cannot be cleared.
     *
     * @param viewId ID of view to adjust the transforms pivot point about X
     * @param transformPivotX X location of the pivot point.
     */
    public void setTransformPivotX(int viewId, float transformPivotX) {
        get(viewId).transformPivotX = transformPivotX;
    }

    /**
     * Set Y location of the pivot point around which the view will rotate and scale.
     * use Float.NaN to clear the pivot value.
     * Note: once an actual View has had its pivot set it cannot be cleared.
     *
     * @param viewId ID of view to adjust the transforms pivot point about Y
     * @param transformPivotY Y location of the pivot point.
     */
    public void setTransformPivotY(int viewId, float transformPivotY) {
        get(viewId).transformPivotY = transformPivotY;
    }

    /**
     * Set X,Y location of the pivot point around which the view will rotate and scale.
     * use Float.NaN to clear the pivot value.
     * Note: once an actual View has had its pivot set it cannot be cleared.
     *
     * @param viewId ID of view to adjust the transforms pivot point
     * @param transformPivotX X location of the pivot point.
     * @param transformPivotY Y location of the pivot point.
     */
    public void setTransformPivot(int viewId, float transformPivotX, float transformPivotY) {
        Constraint constraint = get(viewId);
        constraint.transformPivotY = transformPivotY;
        constraint.transformPivotX = transformPivotX;
    }

    /**
     * Adjust the post-layout X translation of a view.
     *
     * @param viewId ID of view to translate in X
     * @param translationX the translation in X
     */
    public void setTranslationX(int viewId, float translationX) {
        get(viewId).translationX = translationX;
    }

    /**
     * Adjust the  post-layout Y translation of a view.
     *
     * @param viewId ID of view to to translate in Y
     * @param translationY the translation in Y
     */
    public void setTranslationY(int viewId, float translationY) {
        get(viewId).translationY = translationY;
    }

    /**
     * Adjust the post-layout translation of a view.
     *
     * @param viewId ID of view to adjust its translation in X & Y
     * @param translationX the translation in X
     * @param translationY the translation in Y
     */
    public void setTranslation(int viewId, float translationX, float translationY) {
        Constraint constraint = get(viewId);
        constraint.translationX = translationX;
        constraint.translationY = translationY;
    }

    /**
     * Adjust the translation in Z of a view.
     *
     * @param viewId ID of view to adjust
     * @param translationZ the translationZ
     */
    public void setTranslationZ(int viewId, float translationZ) {
        get(viewId).translationZ = translationZ;
    }

    /**
     * Sets the height of the view. It can be a dimension, {@link #WRAP_CONTENT} or {@link
     * #MATCH_CONSTRAINT}.
     *
     * @param viewId ID of view to adjust its height
     * @param height the height of the constraint
     *
     * @since 1.1
     */
    public void constrainHeight(int viewId, int height) {
        get(viewId).mHeight = height;
    }

    /**
     * Sets the width of the view. It can be a dimension, {@link #WRAP_CONTENT} or {@link
     * #MATCH_CONSTRAINT}.
     *
     * @param viewId ID of view to adjust its width
     * @param width the width of the view
     *
     * @since 1.1
     */
    public void constrainWidth(int viewId, int width) {
        get(viewId).mWidth = width;
    }

    /**
     * Constrain the view on a circle constraint
     *
     * @param viewId ID of the view we constrain
     * @param id ID of the view we constrain relative to
     * @param radius the radius of the circle in degrees
     * @param angle the angle
     *
     * @since 1.1
     */
    public void constrainCircle(int viewId, int id, int radius, float angle) {
        Constraint constraint = get(viewId);
        constraint.circleConstraint = id;
        constraint.circleRadius = radius;
        constraint.circleAngle = angle;
    }

    /**
     * Sets the maximum height of the view. It is a dimension, It is only applicable if height is
     * #MATCH_CONSTRAINT}.
     *
     * @param viewId ID of view to adjust it height
     * @param height the height of the constraint
     *
     * @since 1.1
     */
    public void constrainMaxHeight(int viewId, int height) {
        get(viewId).heightMax = height;
    }

    /**
     * Sets the maximum width of the view. It is a dimension, It is only applicable if width is
     * #MATCH_CONSTRAINT}.
     *
     * @param viewId ID of view to adjust its max height
     * @param width the width of the view
     *
     * @since 1.1
     */
    public void constrainMaxWidth(int viewId, int width) {
        get(viewId).widthMax = width;
    }

    /**
     * Sets the height of the view. It is a dimension, It is only applicable if height is
     * #MATCH_CONSTRAINT}.
     *
     * @param viewId ID of view to adjust its min height
     * @param height the minimum height of the view
     *
     * @since 1.1
     */
    public void constrainMinHeight(int viewId, int height) {
        get(viewId).heightMin = height;
    }

    /**
     * Sets the width of the view.  It is a dimension, It is only applicable if width is
     * #MATCH_CONSTRAINT}.
     *
     * @param viewId ID of view to adjust its min height
     * @param width the minimum width of the view
     *
     * @since 1.1
     */
    public void constrainMinWidth(int viewId, int width) {
        get(viewId).widthMin = width;
    }

    /**
     * Sets the width of the view as a percentage of the parent.
     * @param viewId
     * @param percent
     *
     * @since 1.1
     */
    public void constrainPercentWidth(int viewId, float percent) {
        get(viewId).widthPercent = percent;
    }

    /**
     * Sets the height of the view as a percentage of the parent.
     * @param viewId
     * @param percent
     *
     * @since 1.1
     */
    public void constrainPercentHeight(int viewId, float percent) {
        get(viewId).heightPercent = percent;
    }

    /**
     * Sets how the height is calculated ether MATCH_CONSTRAINT_WRAP or MATCH_CONSTRAINT_SPREAD.
     * Default is spread.
     *
     * @param viewId ID of view to adjust its matchConstraintDefaultHeight
     * @param height MATCH_CONSTRAINT_WRAP or MATCH_CONSTRAINT_SPREAD
     *
     * @since 1.1
     */
    public void constrainDefaultHeight(int viewId, int height) {
        get(viewId).heightDefault = height;
    }

    /**
     * Sets how the width is calculated ether MATCH_CONSTRAINT_WRAP or MATCH_CONSTRAINT_SPREAD.
     * Default is spread.
     *
     * @param viewId ID of view to adjust its matchConstraintDefaultWidth
     * @param width SPREAD or WRAP
     *
     * @since 1.1
     */
    public void constrainDefaultWidth(int viewId, int width) {
        get(viewId).widthDefault = width;
    }


    /**
     * The child's weight that we can use to distribute the available horizontal space
     * in a chain, if the dimension behaviour is set to MATCH_CONSTRAINT
     *
     * @param viewId ID of view to adjust its HorizontalWeight
     * @param weight the weight that we can use to distribute the horizontal space
     */
    public void setHorizontalWeight(int viewId, float weight) {
        get(viewId).horizontalWeight = weight;
    }

    /**
     * The child's weight that we can use to distribute the available horizontal space
     * in a chain, if the dimension behaviour is set to MATCH_CONSTRAINT
     *
     * @param viewId ID of view to adjust its VerticalWeight
     * @param weight the weight that we can use to distribute the horizontal space
     */
    public void setVerticalWeight(int viewId, float weight) {
        get(viewId).verticalWeight = weight;
    }

    /**
     * How the elements of the horizontal chain will be positioned. The possible values are:
     *
     * <ul> <li>{@see CHAIN_SPREAD} -- the elements will be spread out</li> <li>{@see
     * CHAIN_SPREAD_INSIDE} -- similar, but the endpoints of the chain will not be spread out</li>
     * <li>{@see CHAIN_PACKED} -- the elements of the chain will be packed together. The horizontal
     * bias attribute of the child will then affect the positioning of the packed elements</li> </ul>
     *
     * @param viewId ID of view to adjust its HorizontalChainStyle
     * @param chainStyle the weight that we can use to distribute the horizontal space
     */
    public void setHorizontalChainStyle(int viewId, int chainStyle) {
        get(viewId).horizontalChainStyle = chainStyle;
    }

    /**
     * How the elements of the vertical chain will be positioned. in a chain, if the dimension
     * behaviour is set to MATCH_CONSTRAINT
     *
     * <ul> <li>{@see CHAIN_SPREAD} -- the elements will be spread out</li> <li>{@see
     * CHAIN_SPREAD_INSIDE} -- similar, but the endpoints of the chain will not be spread out</li>
     * <li>{@see CHAIN_PACKED} -- the elements of the chain will be packed together. The horizontal
     * bias attribute of the child will then affect the positioning of the packed elements</li> </ul>
     *
     * @param viewId ID of view to adjust its VerticalChainStyle
     * @param chainStyle the weight that we can use to distribute the horizontal space
     */
    public void setVerticalChainStyle(int viewId, int chainStyle) {
        get(viewId).verticalChainStyle = chainStyle;
    }

    /**
     * Adds a view to a horizontal chain.
     *
     * @param viewId view to add
     * @param leftId view in chain to the left
     * @param rightId view in chain to the right
     */
    public void addToHorizontalChain(int viewId, int leftId, int rightId) {
        connect(viewId, LEFT, leftId, (leftId == PARENT_ID) ? LEFT : RIGHT, 0);
        connect(viewId, RIGHT, rightId, (rightId == PARENT_ID) ? RIGHT : LEFT, 0);
        if (leftId != PARENT_ID) {
            connect(leftId, RIGHT, viewId, LEFT, 0);
        }
        if (rightId != PARENT_ID) {
            connect(rightId, LEFT, viewId, RIGHT, 0);
        }
    }

    /**
     * Adds a view to a horizontal chain.
     *
     * @param viewId view to add
     * @param leftId view to the start side
     * @param rightId view to the end side
     */
    public void addToHorizontalChainRTL(int viewId, int leftId, int rightId) {
        connect(viewId, START, leftId, (leftId == PARENT_ID) ? START : END, 0);
        connect(viewId, END, rightId, (rightId == PARENT_ID) ? END : START, 0);
        if (leftId != PARENT_ID) {
            connect(leftId, END, viewId, START, 0);
        }
        if (rightId != PARENT_ID) {
            connect(rightId, START, viewId, END, 0);
        }
    }

    /**
     * Adds a view to a vertical chain.
     *
     * @param viewId view to add to a vertical chain
     * @param topId view above.
     * @param bottomId view below
     */
    public void addToVerticalChain(int viewId, int topId, int bottomId) {
        connect(viewId, TOP, topId, (topId == PARENT_ID) ? TOP : BOTTOM, 0);
        connect(viewId, BOTTOM, bottomId, (bottomId == PARENT_ID) ? BOTTOM : TOP, 0);
        if (topId != PARENT_ID) {
            connect(topId, BOTTOM, viewId, TOP, 0);
        }
        if (topId != PARENT_ID) {
            connect(bottomId, TOP, viewId, BOTTOM, 0);
        }
    }

    /**
     * Removes a view from a vertical chain.
     * This assumes the view is connected to a vertical chain.
     * Its behaviour is undefined if not part of a vertical chain.
     *
     * @param viewId the view to be removed
     */
    public void removeFromVerticalChain(int viewId) {
        if (mConstraints.containsKey(viewId)) {
            Constraint constraint = mConstraints.get(viewId);
            int topId = constraint.topToBottom;
            int bottomId = constraint.bottomToTop;
            if (topId != Constraint.UNSET || bottomId != Constraint.UNSET) {
                if (topId != Constraint.UNSET && bottomId != Constraint.UNSET) {
                    // top and bottom connected to views
                    connect(topId, BOTTOM, bottomId, TOP, 0);
                    connect(bottomId, TOP, topId, BOTTOM, 0);
                } else if (topId != Constraint.UNSET || bottomId != Constraint.UNSET) {
                    if (constraint.bottomToBottom != Constraint.UNSET) {
                        // top connected to view. Bottom connected to parent
                        connect(topId, BOTTOM, constraint.bottomToBottom, BOTTOM, 0);
                    } else if (constraint.topToTop != Constraint.UNSET) {
                        // bottom connected to view. Top connected to parent
                        connect(bottomId, TOP, constraint.topToTop, TOP, 0);
                    }
                }
            }
        }
        clear(viewId, TOP);
        clear(viewId, BOTTOM);
    }

    /**
     * Removes a view from a horizontal chain.
     * This assumes the view is connected to a horizontal chain.
     * Its behaviour is undefined if not part of a horizontal chain.
     *
     * @param viewId the view to be removed
     */
    public void removeFromHorizontalChain(int viewId) {
        if (mConstraints.containsKey(viewId)) {
            Constraint constraint = mConstraints.get(viewId);
            int leftId = constraint.leftToRight;
            int rightId = constraint.rightToLeft;
            if (leftId != Constraint.UNSET || rightId != Constraint.UNSET) {
                if (leftId != Constraint.UNSET && rightId != Constraint.UNSET) {
                    // left and right connected to views
                    connect(leftId, RIGHT, rightId, LEFT, 0);
                    connect(rightId, LEFT, leftId, RIGHT, 0);
                } else if (leftId != Constraint.UNSET || rightId != Constraint.UNSET) {
                    if (constraint.rightToRight != Constraint.UNSET) {
                        // left connected to view. right connected to parent
                        connect(leftId, RIGHT, constraint.rightToRight, RIGHT, 0);
                    } else if (constraint.leftToLeft != Constraint.UNSET) {
                        // right connected to view. left connected to parent
                        connect(rightId, LEFT, constraint.leftToLeft, LEFT, 0);
                    }
                }
                clear(viewId, LEFT);
                clear(viewId, RIGHT);
            } else {

                int startId = constraint.startToEnd;
                int endId = constraint.endToStart;
                if (startId != Constraint.UNSET || endId != Constraint.UNSET) {
                    if (startId != Constraint.UNSET && endId != Constraint.UNSET) {
                        // start and end connected to views
                        connect(startId, END, endId, START, 0);
                        connect(endId, START, leftId, END, 0);
                    } else if (leftId != Constraint.UNSET || endId != Constraint.UNSET) {
                        if (constraint.rightToRight != Constraint.UNSET) {
                            // left connected to view. right connected to parent
                            connect(leftId, END, constraint.rightToRight, END, 0);
                        } else if (constraint.leftToLeft != Constraint.UNSET) {
                            // right connected to view. left connected to parent
                            connect(endId, START, constraint.leftToLeft, START, 0);
                        }
                    }
                }
                clear(viewId, START);
                clear(viewId, END);
            }
        }
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
     * Creates a ConstraintLayout Barrier object.
     *
     * @param id
     * @param direction Barrier.{LEFT,RIGHT,TOP,BOTTOM,START,END}
     * @param referenced
     *
     * @since 1.1
     */
    public void createBarrier(int id, int direction, int... referenced) {
        Constraint constraint = get(id);
        constraint.mHelperType = BARRIER_TYPE;
        constraint.mBarrierDirection = direction;
        constraint.mIsGuideline = false;
        constraint.mReferenceIds = referenced;
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
        get(guidelineID).guidePercent = Constraint.UNSET;

    }

    /**
     * Set a guideline's distance to end.
     *
     * @param guidelineID ID of the guideline
     * @param margin the margin to the right or bottom side of container
     */
    public void setGuidelineEnd(int guidelineID, int margin) {
        get(guidelineID).guideEnd = margin;
        get(guidelineID).guideBegin = Constraint.UNSET;
        get(guidelineID).guidePercent = Constraint.UNSET;
    }

    /**
     * Set a Guideline's percent.
     *
     * @param guidelineID ID of the guideline
     * @param ratio the ratio between the gap on the left and right 0.0 is top/left 0.5 is middle
     */
    public void setGuidelinePercent(int guidelineID, float ratio) {
        get(guidelineID).guidePercent = ratio;
        get(guidelineID).guideEnd = Constraint.UNSET;
        get(guidelineID).guideBegin = Constraint.UNSET;
    }

    public void setBarrierType(int id, int type) {

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
     * Load a constraint set from a constraintSet.xml file.
     * Note. Do NOT use this to load a layout file.
     * It will fail silently as there is no efficient way to differentiate.
     *
     * @param context the context for the inflation
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
                            if (f.getType().isPrimitive() && attr == f.getInt(null) && f.getName()
                                .contains("ConstraintSet")) {
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
                                if (f.getType().isPrimitive() && attr == f.getInt(null) && f.getName()
                                    .contains("ConstraintSet")) {
                                    found = false;
                                    Log.v(TAG, "x id " + f.getName());
                                    break;
                                }
                            } catch (Exception e) {

                            }
                        }
                    }
                    if (!found) {
                        Log.v(TAG, " ? " + attr);
                    }
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
                    c.endToEnd = lookupID(a, attr, c.endToEnd);
                    break;
                case CIRCLE:
                    c.circleConstraint = lookupID(a, attr, c.circleConstraint);
                    break;
                case CIRCLE_RADIUS:
                    c.circleRadius = a.getDimensionPixelSize(attr, c.circleRadius);
                    break;
                case CIRCLE_ANGLE:
                    c.circleAngle = a.getFloat(attr, c.circleAngle);
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
                case ALPHA:
                    c.alpha = a.getFloat(attr, c.alpha);
                    break;
                case ELEVATION:
                    c.applyElevation = true;
                    c.elevation = a.getDimension(attr, c.elevation);
                    break;
                case ROTATION:
                    c.rotation = a.getFloat(attr, c.rotation);
                    break;
                case ROTATION_X:
                    c.rotationX = a.getFloat(attr, c.rotationX);
                    break;
                case ROTATION_Y:
                    c.rotationY = a.getFloat(attr, c.rotationY);
                    break;
                case SCALE_X:
                    c.scaleX = a.getFloat(attr, c.scaleX);
                    break;
                case SCALE_Y:
                    c.scaleY = a.getFloat(attr, c.scaleY);
                    break;
                case TRANSFORM_PIVOT_X:
                    c.transformPivotX = a.getFloat(attr, c.transformPivotX);
                    break;
                case TRANSFORM_PIVOT_Y:
                    c.transformPivotY = a.getFloat(attr, c.transformPivotY);
                    break;
                case TRANSLATION_X:
                    c.translationX = a.getDimension(attr, c.translationX);
                    break;
                case TRANSLATION_Y:
                    c.translationY = a.getDimension(attr, c.translationY);
                    break;
                case TRANSLATION_Z:
                    c.translationZ = a.getDimension(attr, c.translationZ);
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
                case WIDTH_PERCENT:
                    c.widthPercent = a.getFloat(attr, 1);
                    break;
                case HEIGHT_PERCENT:
                    c.heightPercent = a.getFloat(attr, 1);
                    break;
                case CHAIN_USE_RTL:
                    Log.e(TAG, "CURRENTLY UNSUPPORTED"); // TODO add support or remove
                 //  TODO add support or remove  c.mChainUseRtl = a.getBoolean(attr,c.mChainUseRtl);
                    break;
                case BARRIER_DIRECTION:
                    c.mBarrierDirection = a.getInt(attr,c.mBarrierDirection);
                    break;
                case CONSTRAINT_REFERENCED_IDS:
                    c.mReferenceIdString = a.getString(attr);
                     break;
                case BARRIER_ALLOWS_GONE_WIDGETS:
                    c.mBarrierAllowsGoneWidgets = a.getBoolean(attr,c.mBarrierAllowsGoneWidgets);
                    break;
                case UNUSED:
                    Log.w(TAG,
                        "unused attribute 0x" + Integer.toHexString(attr) + "   " + mapToConstant.get(attr));
                    break;
                default:
                    Log.w(TAG,
                        "Unknown attribute 0x" + Integer.toHexString(attr) + "   " + mapToConstant.get(attr));
            }
        }
    }

    private int[] convertReferenceString(View view, String referenceIdString) {
        String[] split = referenceIdString.split(",");
        Context context = view.getContext();
        int[]tags = new int[split.length];
        int count = 0;
        for (int i = 0; i < split.length; i++) {
            String idString = split[i];
            idString = idString.trim();
            int tag = 0;
            try {
                Class res = R.id.class;
                Field field = res.getField(idString);
                tag = field.getInt(null);
            }
            catch (Exception e) {
                // Do nothing
            }
            if (tag == 0) {
                tag = context.getResources().getIdentifier(idString, "id",
                        context.getPackageName());
            }

            if (tag == 0 && view.isInEditMode() && view.getParent() instanceof ConstraintLayout) {
                ConstraintLayout constraintLayout = (ConstraintLayout) view.getParent();
                Object value = constraintLayout.getDesignInformation(0, idString);
                if (value != null && value instanceof Integer) {
                    tag = (Integer) value;
                }
            }
            tags[count++] = tag;
        }
        if (count!=split.length) {
            tags = Arrays.copyOf(tags,count);
        }
        return tags;
    }

}
