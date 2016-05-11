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

import android.support.constraint.solver.EquationCreation;
import android.support.constraint.solver.LinearSystem;
import android.support.constraint.solver.SolverVariable;

import java.util.ArrayList;

/**
 * Implements a constraint Widget model supporting constraints relations between other widgets.
 * <p>
 * The widget has various anchors (i.e. Left, Top, Right, Bottom, representing their respective
 * sides, as well as Baseline, Center_X and Center_Y). Connecting anchors from one widget to another
 * represents a constraint relation between the two anchors; the {@link LinearSystem} will then
 * be able to use this model to try to minimize the distances between connected anchors.
 * </p>
 * <p>
 * If opposite anchors are connected (e.g. Left and Right anchors), if they have the same strength,
 * the widget will be equally pulled toward their respective target anchor positions; if the widget
 * has a fixed size, this means that the widget will be centered between the two target anchors. If
 * the widget's size is allowed to adjust, the size of the widget will change to be as large as
 * necessary so that the widget's anchors and the target anchors' distances are zero.
 * </p>
 * Constraints are set by connecting a widget's anchor to another via the
 * {@link #connect} function.
 */
public class ConstraintWidget implements Solvable {
    private static final boolean AUTOTAG_CENTER = false;

    private Animator mAnimator = new Animator(this);

    public final static int VISIBLE = 0;
    public final static int INVISIBLE = 4;
    public final static int GONE = 8;

    /**
     * Define how the content of a widget should align, if the widget has children
     */
    public enum ContentAlignment {
        BEGIN, MIDDLE, END, TOP, VERTICAL_MIDDLE, BOTTOM, LEFT, RIGHT
    }

    /**
     * Define how the widget will resize
     */
    public enum DimensionBehaviour {
        FIXED, WRAP_CONTENT, ANY
    }

    // The anchors available on the widget
    // note: all anchors should be added to the mAnchors array (see addAnchors())
    protected ConstraintAnchor mLeft = new ConstraintAnchor(this, ConstraintAnchor.Type.LEFT);
    protected ConstraintAnchor mTop = new ConstraintAnchor(this, ConstraintAnchor.Type.TOP);
    private ConstraintAnchor mRight = new ConstraintAnchor(this, ConstraintAnchor.Type.RIGHT);
    private ConstraintAnchor mBottom = new ConstraintAnchor(this, ConstraintAnchor.Type.BOTTOM);
    private ConstraintAnchor mBaseline = new ConstraintAnchor(this, ConstraintAnchor.Type.BASELINE);
    private ConstraintAnchor mCenterX = new ConstraintAnchor(this, ConstraintAnchor.Type.CENTER_X);
    private ConstraintAnchor mCenterY = new ConstraintAnchor(this, ConstraintAnchor.Type.CENTER_Y);
    private ConstraintAnchor mCenter = new ConstraintAnchor(this, ConstraintAnchor.Type.CENTER);

    protected ArrayList<ConstraintAnchor> mAnchors = new ArrayList<>();

    // Parent of this widget
    private ConstraintWidget mParent = null;

    // Dimensions of the widget
    private int mWidth = 0;
    private int mHeight = 0;
    private float mDimensionRatio = 0;

    // Origin of the widget
    private int mX = 0;
    private int mY = 0;

    // Current draw position in container's coordinate
    private int mDrawX = 0;
    private int mDrawY = 0;
    private int mDrawWidth = 0;
    private int mDrawHeight = 0;

    // Root offset
    protected int mOffsetX = 0;
    protected int mOffsetY = 0;

    // Baseline distance relative to the top of the widget
    private int mBaselineDistance = 0;

    // Minimum sizes for the widget
    private int mMinWidth;
    private int mMinHeight;

    // Wrap content sizes for the widget
    private int mWrapWidth;
    private int mWrapHeight;

    // Percentages used for biasing one connection over another when dual connections
    // of the same strength exist
    public static float DEFAULT_BIAS = 0.5f;
    private float mHorizontalBiasPercent = DEFAULT_BIAS;
    private float mVerticalBiasPercent = DEFAULT_BIAS;

    // The horizontal and vertical behaviour for the widgets' dimensions
    private DimensionBehaviour mHorizontalDimensionBehaviour = DimensionBehaviour.FIXED;
    private DimensionBehaviour mVerticalDimensionBehaviour = DimensionBehaviour.FIXED;

    // The companion widget (typically, the real widget we represent)
    private Object mCompanionWidget;

    // This is used to possibly "skip" a position while inside a container. For example,
    // a container like ConstraintTableLayout can use this to implement empty cells
    // (the item positioned after the empty cell will have a skip value of 1)
    private int mContainerItemSkip = 0;

    // Contains the visibility status of the widget (VISIBLE, INVISIBLE, or GONE)
    private int mVisibility = VISIBLE;

    private String mDebugName = null;
    private String mType = null;

    // TODO: see if we can make this simpler
    public void reset() {
        mLeft.reset();
        mTop.reset();
        mRight.reset();
        mBottom.reset();
        mBaseline.reset();
        mCenterX.reset();
        mCenterY.reset();
        mCenter.reset();
        mParent = null;
        mWidth = 0;
        mHeight = 0;
        mDimensionRatio = 0.0f;
        mX = 0;
        mY = 0;
        mDrawX = 0;
        mDrawY = 0;
        mDrawWidth = 0;
        mDrawHeight = 0;
        mOffsetX = 0;
        mOffsetY = 0;
        mBaselineDistance = 0;
        mMinWidth = 0;
        mMinHeight = 0;
        mWrapWidth = 0;
        mWrapHeight = 0;
        mHorizontalBiasPercent = DEFAULT_BIAS;
        mVerticalBiasPercent = DEFAULT_BIAS;
        mHorizontalDimensionBehaviour = DimensionBehaviour.FIXED;
        mVerticalDimensionBehaviour = DimensionBehaviour.FIXED;
        mCompanionWidget = null;
        mContainerItemSkip = 0;
        mVisibility = VISIBLE;
        mDebugName = null;
        mType = null;
    }

    /*-----------------------------------------------------------------------*/
    // Creation
    /*-----------------------------------------------------------------------*/

    /**
     * Default constructor
     */
    public ConstraintWidget() {
        addAnchors();
    }

    /**
     * Constructor
     *
     * @param x      x position
     * @param y      y position
     * @param width  width of the layout
     * @param height height of the layout
     */
    public ConstraintWidget(int x, int y, int width, int height) {
        mX = x;
        mY = y;
        mWidth = width;
        mHeight = height;
        addAnchors();
        forceUpdateDrawPosition();
    }

    /**
     * Constructor
     *
     * @param width  width of the layout
     * @param height height of the layout
     */
    public ConstraintWidget(int width, int height) {
        this(0, 0, width, height);
    }

    /**
     * Add all the anchors to the mAnchors array
     */
    private void addAnchors() {
        mAnchors.add(mLeft);
        mAnchors.add(mTop);
        mAnchors.add(mRight);
        mAnchors.add(mBottom);
        mAnchors.add(mCenterX);
        mAnchors.add(mCenterY);
        if (ConstraintAnchor.USE_CENTER_ANCHOR) {
           mAnchors.add(mCenter);
        }
        mAnchors.add(mBaseline);
    }

    /**
     * Returns true if the widget is the root widget
     *
     * @return true if root widget, false otherwise
     */
    public boolean isRoot() {
        return mParent == null;
    }

    /**
     * Return true if the widget is one of our ancestor
     *
     * @param widget widget we want to check
     * @return true if the given widget is one of our ancestor, false otherwise
     */
    public boolean hasAncestor(ConstraintWidget widget) {
        ConstraintWidget parent = getParent();
        if (parent == widget.getParent()) {
            return false; // the widget is one of our sibling
        }
        while (parent != null) {
            if (parent == widget) {
                return true;
            }
            if (parent == widget.getParent()) {
                // the widget is a sibling of one of our ancestor
                return true;
            }
            parent = parent.getParent();
        }
        return false;
    }

    /**
     * Returns the top-level container, regardless if it's a ConstraintWidgetContainer
     * or a simple WidgetContainer.
     *
     * @return top-level WidgetContainer
     */
    public WidgetContainer getRootWidgetContainer() {
        ConstraintWidget root = this;
        while (root.getParent() != null) {
            root = root.getParent();
        }
        if (root instanceof WidgetContainer) {
            return (WidgetContainer) root;
        }
        return null;
    }

    /**
     * Returns the parent of this widget if there is one
     *
     * @return parent
     */
    public ConstraintWidget getParent() {
        return mParent;
    }

    /**
     * Set the parent of this widget
     *
     * @param widget parent
     */
    public void setParent(ConstraintWidget widget) {
        mParent = widget;
    }

    /**
     * Returns the type string if set
     *
     * @return type (null if not set)
     */
    public String getType() {
        return mType;
    }

    /**
     * Set the type of the widget (as a String)
     *
     * @param type type of the widget
     */
    public void setType(String type) {
        mType = type;
    }

    /**
     * Set the visibility for this widget
     *
     * @param visibility either VISIBLE, INVISIBLE, or GONE
     */
    public void setVisibility(int visibility) {
        mVisibility = visibility;
    }

    /**
     * Returns the current visibility value for this widget
     *
     * @return the visibility (VISIBLE, INVISIBLE, or GONE)
     */
    public int getVisibility() {
        return mVisibility;
    }

    /**
     * Returns the name of this widget (used for debug purposes)
     *
     * @return the debug name
     */
    public String getDebugName() {
        return mDebugName;
    }

    /**
     * Set the debug name of this widget
     */
    public void setDebugName(String name) {
        mDebugName = name;
    }

    /**
     * Utility debug function. Sets the names of the anchors in the solver given
     * a widget's name. The given name is used as a prefix, resulting in anchors' names
     * of the form:
     * <p/>
     * <ul>
     * <li>{name}.left</li>
     * <li>{name}.top</li>
     * <li>{name}.right</li>
     * <li>{name}.bottom</li>
     * <li>{name}.baseline</li>
     * </ul>
     *
     * @param system solver used
     * @param name   name of the widget
     */
    @Override
    public void setDebugSolverName(LinearSystem system, String name) {
        mDebugName = name;
        SolverVariable left = system.createObjectVariable(mLeft);
        SolverVariable top = system.createObjectVariable(mTop);
        SolverVariable right = system.createObjectVariable(mRight);
        SolverVariable bottom = system.createObjectVariable(mBottom);
        left.setName(name + ".left");
        top.setName(name + ".top");
        right.setName(name + ".right");
        bottom.setName(name + ".bottom");
        if (mBaselineDistance > 0) {
            SolverVariable baseline = system.createObjectVariable(mBaseline);
            baseline.setName(name + ".baseline");
        }
    }

    /**
     * Returns true if the widget is animating
     *
     * @return
     */
    public boolean isAnimating() {
        if (Animator.doAnimation()) {
            return mAnimator.isAnimating();
        }
        return false;
    }

    /**
     * Returns a string representation of the ConstraintWidget
     *
     * @return string representation of the widget
     */
    @Override
    public String toString() {
        return (mType != null ? "type: " + mType + " " : "")
                + (mDebugName != null ? "id: " + mDebugName + " " : "")
                + "(" + mX + ", " + mY + ") - (" + mWidth + " x " + mHeight + ")";
    }

    /*-----------------------------------------------------------------------*/
    // Position
    /*-----------------------------------------------------------------------*/
    // The widget position is expressed in two ways:
    // - relative to its direct parent container (getX(), getY())
    // - relative to the root container (getDrawX(), getDrawY())
    // Additionally, getDrawX()/getDrawY() are used when animating the
    // widget position on screen
    /*-----------------------------------------------------------------------*/

    int getInternalDrawX() {
        return mDrawX;
    }

    int getInternalDrawY() {
        return mDrawY;
    }

    public int getInternalDrawRight() {
        return mDrawX + mDrawWidth;
    }

    public int getInternalDrawBottom() {
        return mDrawY + mDrawHeight;
    }


    /**
     * Return the x position of the widget, relative to its container
     *
     * @return x position
     */
    public int getX() {
        return mX;
    }

    /**
     * Return the y position of the widget, relative to its container
     *
     * @return y position
     */
    public int getY() {
        return mY;
    }

    /**
     * Return the width of the widget
     *
     * @return width width
     */
    public int getWidth() {
        if (mVisibility == ConstraintWidget.GONE) {
            return 0;
        }
        return mWidth;
    }

    /**
     * Return the wrap width of the widget
     *
     * @return the wrap width
     */
    public int getWrapWidth() {
        return mWrapWidth;
    }

    /**
     * Return the height of the widget
     *
     * @return height height
     */
    public int getHeight() {
        if (mVisibility == ConstraintWidget.GONE) {
            return 0;
        }
        return mHeight;
    }

    /**
     * Return the wrap height of the widget
     *
     * @return the wrap height
     */
    public int getWrapHeight() {
        return mWrapHeight;
    }

    /**
     * Return the x position of the widget, relative to the root
     *
     * @return x position
     */
    public int getDrawX() {
        return mDrawX + mOffsetX;
    }

    /**
     * Return the y position of the widget, relative to the root
     *
     * @return
     */
    public int getDrawY() {
        return mDrawY + mOffsetY;
    }

    public int getDrawWidth() {
        return mDrawWidth;
    }

    public int getDrawHeight() {
        return mDrawHeight;
    }

    /**
     * Return the bottom position of the widget, relative to the root
     *
     * @return bottom position of the widget
     */
    public int getDrawBottom() {
        return getDrawY() + mDrawHeight;
    }

    /**
     * Return the right position of the widget, relative to the root
     *
     * @return right position of the widget
     */
    public int getDrawRight() {
        return getDrawX() + mDrawWidth;
    }

    /**
     * Return the x position of the widget, relative to the root
     * (without animation)
     *
     * @return x position
     */
    protected int getRootX() {
        return mX + mOffsetX;
    }

    /**
     * Return the y position of the widget, relative to the root
     * (without animation)
     *
     * @return
     */
    protected int getRootY() {
        return mY + mOffsetY;
    }

    /**
     * Return the minimum width of the widget
     *
     * @return minimum width
     */
    public int getMinWidth() {
        return mMinWidth;
    }

    /**
     * Return the minimum height of the widget
     *
     * @return minimum height
     */
    public int getMinHeight() {
        return mMinHeight;
    }

    /**
     * Return the left position of the widget (similar to {@link #getX()})
     *
     * @return left position of the widget
     */
    public int getLeft() {
        return getX();
    }

    /**
     * Return the top position of the widget (similar to {@link #getY()})
     *
     * @return top position of the widget
     */
    public int getTop() {
        return getY();
    }

    /**
     * Return the right position of the widget
     *
     * @return right position of the widget
     */
    public int getRight() {
        return getX() + mWidth;
    }

    /**
     * Return the bottom position of the widget
     *
     * @return bottom position of the widget
     */
    public int getBottom() {
        return getY() + mHeight;
    }

    /**
     * Return the horizontal percentage bias that is used when two opposite connections
     * exist of the same strengh.
     *
     * @return horizontal percentage bias
     */
    public float getHorizontalBiasPercent() {
        return mHorizontalBiasPercent;
    }

    /**
     * Return the vertical percentage bias that is used when two opposite connections
     * exist of the same strengh.
     *
     * @return vertical percentage bias
     */
    public float getVerticalBiasPercent() {
        return mVerticalBiasPercent;
    }

    /**
     * Return true if this widget has a baseline
     *
     * @return true if the widget has a baseline, false otherwise
     */
    public boolean hasBaseline() {
        return mBaselineDistance > 0;
    }

    /**
     * Return the baseline distance relative to the top of the widget
     *
     * @return baseline
     */
    public int getBaselineDistance() {
        return mBaselineDistance;
    }

    /**
     * Return the companion widget. Typically, this would be the real
     * widget we represent with this instance of ConstraintWidget.
     *
     * @return the companion widget, if set.
     */
    public Object getCompanionWidget() {
        return mCompanionWidget;
    }

    /**
     * Return the array of anchors of this widget
     *
     * @return array of anchors
     */
    public ArrayList<ConstraintAnchor> getAnchors() {
        return mAnchors;
    }

    /**
     * Set the x position of the widget, relative to its container
     *
     * @param x x position
     */
    public void setX(int x) {
        mX = x;
    }

    /**
     * Set the y position of the widget, relative to its container
     *
     * @param y y position
     */
    public void setY(int y) {
        mY = y;
    }

    /**
     * Set both the origin in (x, y) of the widget, relative to its container
     *
     * @param x x position
     * @param y y position
     */
    public void setOrigin(int x, int y) {
        mX = x;
        mY = y;
    }

    /**
     * Set the offset of this widget relative to the root widget
     *
     * @param x horizontal offset
     * @param y vertical offset
     */
    public void setOffset(int x, int y) {
        mOffsetX = x;
        mOffsetY = y;
    }

    /**
     * Update the draw position to match the true position.
     * If animating is on, the transition between the old
     * position and new position will be animated...
     */
    public void updateDrawPosition() {
        int left = mX;
        int top = mY;
        int right = mX + mWidth;
        int bottom = mY + mHeight;
        if (Animator.doAnimation()) {
            mAnimator.animate(left, top, right, bottom);
            left = mAnimator.getCurrentLeft();
            top = mAnimator.getCurrentTop();
            right = mAnimator.getCurrentRight();
            bottom = mAnimator.getCurrentBottom();
        }
        mDrawX = left;
        mDrawY = top;
        mDrawWidth = right - left;
        mDrawHeight = bottom - top;
    }

  /**
   * Update the draw positition immediately to match the true position
   */
  public void forceUpdateDrawPosition() {
        int left = mX;
        int top = mY;
        int right = mX + mWidth;
        int bottom = mY + mHeight;
        mDrawX = left;
        mDrawY = top;
        mDrawWidth = right - left;
        mDrawHeight = bottom - top;
    }

    /**
     * Set both the origin in (x, y) of the widget, relative to the root
     *
     * @param x x position
     * @param y y position
     */
    public void setDrawOrigin(int x, int y) {
        mDrawX = x - mOffsetX;
        mDrawY = y - mOffsetY;
        mX = mDrawX;
        mY = mDrawY;
    }

    /**
     * Set the x position of the widget, relative to the root
     *
     * @param x x position
     */
    public void setDrawX(int x) {
        mDrawX = x - mOffsetX;
        mX = mDrawX;
    }

    /**
     * Set the y position of the widget, relative to its container
     *
     * @param y y position
     */
    public void setDrawY(int y) {
        mDrawY = y - mOffsetY;
        mY = mDrawY;
    }

    /**
     * Set the draw width of the widget
     *
     * @param drawWidth
     */
    public void setDrawWidth(int drawWidth) {
        mDrawWidth = drawWidth;
    }

    /**
     * Set the draw height of the widget
     *
     * @param drawHeight
     */
    public void setDrawHeight(int drawHeight) {
        mDrawHeight = drawHeight;
    }

    /**
     * Set the width of the widget
     *
     * @param w width
     */
    public void setWidth(int w) {
        mWidth = w;
        if (mWidth < mMinWidth) {
            mWidth = mMinWidth;
        }
    }

    /**
     * Set the height of the widget
     *
     * @param h height
     */
    public void setHeight(int h) {
        mHeight = h;
        if (mHeight < mMinHeight) {
            mHeight = mMinHeight;
        }
    }

    /**
     * Set the ratio of the widget
     * The ratio will be applied if at least one of the dimension (width or height) is set to a behaviour
     * of DimensionBehaviour.ANY -- the dimension's value will be set to the other dimension * ratio.
     */
    public void setDimensionRatio(float ratio) {
        mDimensionRatio = ratio;
    }

    /**
     * Return the current ratio of this widget
     * @return the dimension ratio
     */
    public float getDimensionRatio() {
        return mDimensionRatio;
    }

    /**
     * Set the horizontal bias percent to apply when we have two opposite constraints of
     * equal strength
     *
     * @param horizontalBiasPercent the percentage used
     */
    public void setHorizontalBiasPercent(float horizontalBiasPercent) {
        mHorizontalBiasPercent = horizontalBiasPercent;
    }

    /**
     * Set the vertical bias percent to apply when we have two opposite constraints of
     * equal strength
     *
     * @param verticalBiasPercent the percentage used
     */
    public void setVerticalBiasPercent(float verticalBiasPercent) {
        mVerticalBiasPercent = verticalBiasPercent;
    }

    /**
     * Set the minimum width of the widget
     *
     * @param w minimum width
     */
    public void setMinWidth(int w) {
        mMinWidth = w;
    }

    /**
     * Set the minimum height of the widget
     *
     * @param h minimum height
     */
    public void setMinHeight(int h) {
        mMinHeight = h;
    }

    /**
     * Set the wrap content width of the widget
     *
     * @param w wrap content width
     */
    public void setWrapWidth(int w) {
        mWrapWidth = w;
    }

    /**
     * Set the wrap content height of the widget
     *
     * @param h wrap content height
     */
    public void setWrapHeight(int h) {
        mWrapHeight = h;
    }

    /**
     * Set both width and height of the widget
     *
     * @param w width
     * @param h height
     */
    public void setDimension(int w, int h) {
        mWidth = w;
        if (mWidth < mMinWidth) {
            mWidth = mMinWidth;
        }
        mHeight = h;
        if (mHeight < mMinHeight) {
            mHeight = mMinHeight;
        }
    }

    /**
     * Set the position+dimension of the widget given left/top/right/bottom
     *
     * @param left   left side position of the widget
     * @param top    top side position of the widget
     * @param right  right side position of the widget
     * @param bottom bottom side position of the widget
     */
    public void setFrame(int left, int top, int right, int bottom) {
        int w = right - left;
        int h = bottom - top;
        // correct dimensional instability caused by rounding errors
        if (getHorizontalDimensionBehaviour() == DimensionBehaviour.FIXED) {
            if (w < getWidth()) {
                w = getWidth();
            }
        }
        if (getVerticalDimensionBehaviour() == DimensionBehaviour.FIXED) {
            if (h < getHeight()) {
                h = getHeight();
            }
        }
        mX = left;
        mY = top;
        setDimension(w, h);
    }

    /**
     * Set the baseline distance relative to the top of the widget
     *
     * @param baseline the distance of the baseline relative to the widget's top
     */
    public void setBaselineDistance(int baseline) {
        mBaselineDistance = baseline;
    }

    /**
     * Set the companion widget. Typically, this would be the real widget we
     * represent with this instance of ConstraintWidget.
     *
     * @param companion
     */
    public void setCompanionWidget(Object companion) {
        mCompanionWidget = companion;
    }

    /**
     * Set the skip value for this widget. This can be used when a widget is in a container,
     * so that container can position the widget as if it was positioned further in the list
     * of widgets. For example, with ConstraintTableLayout, this is used to skip empty cells
     * (the widget after an empty cell will have a skip value of one)
     *
     * @param skip
     */
    public void setContainerItemSkip(int skip) {
        if (skip >= 0) {
            mContainerItemSkip = skip;
        } else {
            mContainerItemSkip = 0;
        }
    }

    /**
     * Accessor for the skip value
     *
     * @return skip value
     */
    public int getContainerItemSkip() {
        return mContainerItemSkip;
    }

    /*-----------------------------------------------------------------------*/
    // Connections
    /*-----------------------------------------------------------------------*/

    /**
     * Callback when a widget connects to us
     *
     * @param source
     */
    public void connectedTo(ConstraintWidget source) {
        // do nothing by default
    }

    /**
     * Connect the given anchors together (the from anchor should be owned by this widget)
     *
     * @param from   the anchor we are connecting from (of this widget)
     * @param to     the anchor we are connecting to
     * @param margin how much margin we want to have
     * @param creator who created the connection
     */
    public void connect(ConstraintAnchor from, ConstraintAnchor to, int margin, int creator) {
        connect(from, to, margin, ConstraintAnchor.Strength.STRONG, creator);
    }

    public void connect(ConstraintAnchor from, ConstraintAnchor to, int margin) {
        connect(from, to, margin, ConstraintAnchor.Strength.STRONG, ConstraintAnchor.USER_CREATOR);
    }
    public void connect(ConstraintAnchor from, ConstraintAnchor to, int margin,
            ConstraintAnchor.Strength strength, int creator) {
        if (from.getOwner() == this) {
            connect(from.getType(), to.getOwner(), to.getType(), margin, strength, creator);
        }
    }

    /**
     * Connect a given anchor of this widget to another anchor of a target widget
     *
     * @param constraintFrom which anchor of this widget to connect from
     * @param target         the target widget
     * @param constraintTo   the target anchor on the target widget
     * @param margin         how much margin we want to keep as a minimum distance between the two anchors
     * @return the undo operation
     */
    public void connect(ConstraintAnchor.Type constraintFrom, ConstraintWidget target,
            ConstraintAnchor.Type constraintTo, int margin) {
        connect(constraintFrom, target, constraintTo, margin,
                ConstraintAnchor.Strength.STRONG);
    }

    /**
     * Connect a given anchor of this widget to another anchor of a target widget
     *
     * @param constraintFrom which anchor of this widget to connect from
     * @param target         the target widget
     * @param constraintTo   the target anchor on the target widget
     * @return the undo operation
     */
    public void connect(ConstraintAnchor.Type constraintFrom,
            ConstraintWidget target,
            ConstraintAnchor.Type constraintTo) {
        connect(constraintFrom, target, constraintTo, 0, ConstraintAnchor.Strength.STRONG);
    }


    /**
     * Connect a given anchor of this widget to another anchor of a target widget
     *
     * @param constraintFrom which anchor of this widget to connect from
     * @param target         the target widget
     * @param constraintTo   the target anchor on the target widget
     * @param margin         how much margin we want to keep as a minimum distance between the two anchors
     * @param strength       the constraint strength (Weak/Strong)
     */
    public void connect(ConstraintAnchor.Type constraintFrom,
            ConstraintWidget target,
            ConstraintAnchor.Type constraintTo, int margin,
            ConstraintAnchor.Strength strength) {
        connect(constraintFrom, target, constraintTo, margin, strength,
                ConstraintAnchor.USER_CREATOR);
    }

    /**
     * Connect a given anchor of this widget to another anchor of a target widget
     *
     * @param constraintFrom which anchor of this widget to connect from
     * @param target         the target widget
     * @param constraintTo   the target anchor on the target widget
     * @param margin         how much margin we want to keep as a minimum distance between the two anchors
     * @param strength       the constraint strength (Weak/Strong)
     * @param creator        who created the constraint
     */
    public void connect(ConstraintAnchor.Type constraintFrom,
            ConstraintWidget target,
            ConstraintAnchor.Type constraintTo, int margin,
            ConstraintAnchor.Strength strength, int creator) {
        if (constraintFrom == ConstraintAnchor.Type.CENTER) {
            // If we have center, we connect instead to the corresponding
            // left/right or top/bottom pairs
            if (constraintTo == ConstraintAnchor.Type.CENTER) {
                ConstraintAnchor left = getAnchor(ConstraintAnchor.Type.LEFT);
                ConstraintAnchor right = getAnchor(ConstraintAnchor.Type.RIGHT);
                ConstraintAnchor top = getAnchor(ConstraintAnchor.Type.TOP);
                ConstraintAnchor bottom = getAnchor(ConstraintAnchor.Type.BOTTOM);
                boolean centerX = false;
                boolean centerY = false;
                if ((left != null && left.isConnected())
                        || (right != null && right.isConnected())) {
                    // don't apply center here
                } else {
                    connect(ConstraintAnchor.Type.LEFT, target,
                            ConstraintAnchor.Type.LEFT, 0, strength, creator);
                    connect(ConstraintAnchor.Type.RIGHT, target,
                            ConstraintAnchor.Type.RIGHT, 0, strength, creator);
                    centerX = true;
                }
                if ((top != null && top.isConnected())
                        || (bottom != null && bottom.isConnected())) {
                    // don't apply center here
                } else {
                    connect(ConstraintAnchor.Type.TOP, target,
                            ConstraintAnchor.Type.TOP, 0, strength, creator);
                    connect(ConstraintAnchor.Type.BOTTOM, target,
                            ConstraintAnchor.Type.BOTTOM, 0, strength, creator);
                    centerY = true;
                }
                if (centerX && centerY) {
                    ConstraintAnchor center = getAnchor(ConstraintAnchor.Type.CENTER);
                    center.connect(target.getAnchor(ConstraintAnchor.Type.CENTER), 0, creator);
                } else if (centerX) {
                    ConstraintAnchor center = getAnchor(ConstraintAnchor.Type.CENTER_X);
                    center.connect(target.getAnchor(ConstraintAnchor.Type.CENTER_X), 0, creator);
                } else if (centerY) {
                    ConstraintAnchor center = getAnchor(ConstraintAnchor.Type.CENTER_Y);
                    center.connect(target.getAnchor(ConstraintAnchor.Type.CENTER_Y), 0, creator);
                }
            } else if ((constraintTo == ConstraintAnchor.Type.LEFT)
                    || (constraintTo == ConstraintAnchor.Type.RIGHT)) {
                connect(ConstraintAnchor.Type.LEFT, target,
                        constraintTo, 0, strength, creator);
                connect(ConstraintAnchor.Type.RIGHT, target,
                        constraintTo, 0, strength, creator);
                ConstraintAnchor center = getAnchor(ConstraintAnchor.Type.CENTER);
                center.connect(target.getAnchor(constraintTo), 0, creator);
            } else if ((constraintTo == ConstraintAnchor.Type.TOP)
                    || (constraintTo == ConstraintAnchor.Type.BOTTOM)) {
                connect(ConstraintAnchor.Type.TOP, target,
                        constraintTo, 0, strength, creator);
                connect(ConstraintAnchor.Type.BOTTOM, target,
                        constraintTo, 0, strength, creator);
                ConstraintAnchor center = getAnchor(ConstraintAnchor.Type.CENTER);
                center.connect(target.getAnchor(constraintTo), 0, creator);
            }
        } else if (constraintFrom == ConstraintAnchor.Type.CENTER_X
                && (constraintTo == ConstraintAnchor.Type.LEFT
                    || constraintTo == ConstraintAnchor.Type.RIGHT)) {
            ConstraintAnchor left = getAnchor(ConstraintAnchor.Type.LEFT);
            ConstraintAnchor targetAnchor = target.getAnchor(constraintTo);
            ConstraintAnchor right = getAnchor(ConstraintAnchor.Type.RIGHT);
            left.connect(targetAnchor, 0, creator);
            right.connect(targetAnchor, 0, creator);
            ConstraintAnchor centerX = getAnchor(ConstraintAnchor.Type.CENTER_X);
            centerX.connect(targetAnchor, 0, creator);
        } else if (constraintFrom == ConstraintAnchor.Type.CENTER_Y
                && (constraintTo == ConstraintAnchor.Type.TOP
                     || constraintTo == ConstraintAnchor.Type.BOTTOM)) {
            ConstraintAnchor targetAnchor = target.getAnchor(constraintTo);
            ConstraintAnchor top = getAnchor(ConstraintAnchor.Type.TOP);
            top.connect(targetAnchor, 0, creator);
            ConstraintAnchor bottom = getAnchor(ConstraintAnchor.Type.BOTTOM);
            bottom.connect(targetAnchor, 0, creator);
            ConstraintAnchor centerY = getAnchor(ConstraintAnchor.Type.CENTER_Y);
            centerY.connect(targetAnchor, 0, creator);
        } else if (constraintFrom == ConstraintAnchor.Type.CENTER_X
                && constraintTo == ConstraintAnchor.Type.CENTER_X) {
            // Center X connection will connect left & right
            ConstraintAnchor left = getAnchor(ConstraintAnchor.Type.LEFT);
            ConstraintAnchor leftTarget = target.getAnchor(ConstraintAnchor.Type.LEFT);
            left.connect(leftTarget, 0, creator);
            ConstraintAnchor right = getAnchor(ConstraintAnchor.Type.RIGHT);
            ConstraintAnchor rightTarget = target.getAnchor(ConstraintAnchor.Type.RIGHT);
            right.connect(rightTarget, 0, creator);
            ConstraintAnchor centerX = getAnchor(ConstraintAnchor.Type.CENTER_X);
            centerX.connect(target.getAnchor(constraintTo), 0, creator);
        } else if (constraintFrom == ConstraintAnchor.Type.CENTER_Y
                && constraintTo == ConstraintAnchor.Type.CENTER_Y) {
            // Center Y connection will connect top & bottom.
            ConstraintAnchor top = getAnchor(ConstraintAnchor.Type.TOP);
            ConstraintAnchor topTarget = target.getAnchor(ConstraintAnchor.Type.TOP);
            top.connect(topTarget, 0, creator);
            ConstraintAnchor bottom = getAnchor(ConstraintAnchor.Type.BOTTOM);
            ConstraintAnchor bottomTarget = target.getAnchor(ConstraintAnchor.Type.BOTTOM);
            bottom.connect(bottomTarget, 0, creator);
            ConstraintAnchor centerY = getAnchor(ConstraintAnchor.Type.CENTER_Y);
            centerY.connect(target.getAnchor(constraintTo), 0, creator);
        } else {
            ConstraintAnchor fromAnchor = getAnchor(constraintFrom);
            ConstraintAnchor toAnchor = target.getAnchor(constraintTo);
            if (fromAnchor.isValidConnection(toAnchor)) {
                // make sure that the baseline takes precedence over top/bottom
                // and reversely, reset the baseline if we are connecting top/bottom
                if (constraintFrom == ConstraintAnchor.Type.BASELINE) {
                    ConstraintAnchor top = getAnchor(ConstraintAnchor.Type.TOP);
                    ConstraintAnchor bottom = getAnchor(ConstraintAnchor.Type.BOTTOM);
                    if (top != null) {
                        top.reset();
                    }
                    if (bottom != null) {
                        bottom.reset();
                    }
                    margin = 0;
                } else if ((constraintFrom == ConstraintAnchor.Type.TOP)
                        || (constraintFrom == ConstraintAnchor.Type.BOTTOM)) {
                    ConstraintAnchor baseline = getAnchor(ConstraintAnchor.Type.BASELINE);
                    if (baseline != null) {
                        baseline.reset();
                    }
                    ConstraintAnchor center = getAnchor(ConstraintAnchor.Type.CENTER);
                    if (center.getTarget() != toAnchor) {
                        center.reset();
                    }
                    ConstraintAnchor opposite = getAnchor(constraintFrom).getOpposite();
                    ConstraintAnchor centerY = getAnchor(ConstraintAnchor.Type.CENTER_Y);
                    if (centerY.isConnected()) {
                        opposite.reset();
                        centerY.reset();
                    } else {
                        if (AUTOTAG_CENTER) {
                            // let's see if we need to mark center_y as connected
                            if (opposite.isConnected() && opposite.getTarget().getOwner()
                                    == toAnchor.getOwner()) {
                                ConstraintAnchor targetCenterY = toAnchor.getOwner().getAnchor(
                                        ConstraintAnchor.Type.CENTER_Y);
                                centerY.connect(targetCenterY, 0, creator);
                            }
                        }
                    }
                } else if ((constraintFrom == ConstraintAnchor.Type.LEFT)
                        || (constraintFrom == ConstraintAnchor.Type.RIGHT)) {
                    ConstraintAnchor center = getAnchor(ConstraintAnchor.Type.CENTER);
                    if (center.getTarget() != toAnchor) {
                        center.reset();
                    }
                    ConstraintAnchor opposite = getAnchor(constraintFrom).getOpposite();
                    ConstraintAnchor centerX = getAnchor(ConstraintAnchor.Type.CENTER_X);
                    if (centerX.isConnected()) {
                        opposite.reset();
                        centerX.reset();
                    } else {
                        if (AUTOTAG_CENTER) {
                            // let's see if we need to mark center_x as connected
                            if (opposite.isConnected() && opposite.getTarget().getOwner()
                                    == toAnchor.getOwner()) {
                                ConstraintAnchor targetCenterX = toAnchor.getOwner().getAnchor(
                                        ConstraintAnchor.Type.CENTER_X);
                                centerX.connect(targetCenterX, 0, creator);
                            }
                        }
                    }

                }
                fromAnchor.connect(toAnchor, margin, strength, creator);
                toAnchor.getOwner().connectedTo(fromAnchor.getOwner());
            }
        }
    }

    /**
     * Reset all the constraints set on this widget
     */
    public void resetAllConstraints() {
        resetAnchors();
        setVerticalBiasPercent(DEFAULT_BIAS);
        setHorizontalBiasPercent(DEFAULT_BIAS);
        if (getHorizontalDimensionBehaviour() == ConstraintWidget.DimensionBehaviour.ANY) {
            if (getWidth() == getWrapWidth()) {
                setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
            } else if (getWidth() > getMinWidth()) {
                setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
            }
        }
        if (getVerticalDimensionBehaviour() == ConstraintWidget.DimensionBehaviour.ANY) {
            if (getHeight() == getWrapHeight()) {
                setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
            } else if (getHeight() > getMinHeight()) {
                setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
            }
        }
    }

    /**
     * Reset the given anchor
     *
     * @param anchor the anchor we want to reset
     * @return the undo operation
     */
    public void resetAnchor(ConstraintAnchor anchor) {
        if (getParent() != null) {
            ConstraintWidgetContainer parent = (ConstraintWidgetContainer) getParent();
            if (parent.handlesInternalConstraints()) {
                return;
            }
        }
        ConstraintAnchor left = getAnchor(ConstraintAnchor.Type.LEFT);
        ConstraintAnchor right = getAnchor(ConstraintAnchor.Type.RIGHT);
        ConstraintAnchor top = getAnchor(ConstraintAnchor.Type.TOP);
        ConstraintAnchor bottom = getAnchor(ConstraintAnchor.Type.BOTTOM);
        ConstraintAnchor center = getAnchor(ConstraintAnchor.Type.CENTER);
        ConstraintAnchor centerX = getAnchor(ConstraintAnchor.Type.CENTER_X);
        ConstraintAnchor centerY = getAnchor(ConstraintAnchor.Type.CENTER_Y);

        if (anchor == center) {
            if (left.isConnected() && right.isConnected()
                    && left.getTarget() == right.getTarget()) {
                left.reset();
                right.reset();
            }
            if (top.isConnected() && bottom.isConnected()
                    && top.getTarget() == bottom.getTarget()) {
                top.reset();
                bottom.reset();
            }
            mHorizontalBiasPercent = 0.5f;
            mVerticalBiasPercent = 0.5f;
        } else if (anchor == centerX) {
            if (left.isConnected() && right.isConnected()
                    && left.getTarget().getOwner() == right.getTarget().getOwner()) {
                left.reset();
                right.reset();
            }
            mHorizontalBiasPercent = 0.5f;
        } else if (anchor == centerY) {
            if (top.isConnected() && bottom.isConnected()
                    && top.getTarget().getOwner() == bottom.getTarget().getOwner()) {
                top.reset();
                bottom.reset();
            }
            mVerticalBiasPercent = 0.5f;
        } else if (anchor == left || anchor == right) {
            if (left.isConnected() && left.getTarget() == right.getTarget()) {
                center.reset();
            }
        } else if (anchor == top || anchor == bottom) {
            if (top.isConnected() && top.getTarget() == bottom.getTarget()) {
                center.reset();
            }
        }
        anchor.reset();
    }

    /**
     * Reset all connections
     */
    public void resetAnchors() {
        ConstraintWidget parent = getParent();
        if (parent != null && parent instanceof ConstraintWidgetContainer) {
            ConstraintWidgetContainer parentContainer = (ConstraintWidgetContainer) getParent();
            if (parentContainer.handlesInternalConstraints()) {
                return;
            }
        }
        for (int i = 0, mAnchorsSize = mAnchors.size(); i < mAnchorsSize; i++) {
            final ConstraintAnchor anchor = mAnchors.get(i);
            anchor.reset();
        }
    }

    /**
     * Reset all connections that have this connectCreator
     */
    public void resetAnchors(int connectionCreator) {
        ConstraintWidget parent = getParent();
        if (parent != null && parent instanceof ConstraintWidgetContainer) {
            ConstraintWidgetContainer parentContainer = (ConstraintWidgetContainer) getParent();
            if (parentContainer.handlesInternalConstraints()) {
                return;
            }
        }
        for (int i = 0, mAnchorsSize = mAnchors.size(); i < mAnchorsSize; i++) {
            final ConstraintAnchor anchor = mAnchors.get(i);
            if (connectionCreator == anchor.getConnectionCreator()) {
                if (anchor.isVerticalAnchor()) {
                    setVerticalBiasPercent(ConstraintWidget.DEFAULT_BIAS);
                } else {
                    setHorizontalBiasPercent(ConstraintWidget.DEFAULT_BIAS);
                }
                anchor.reset();
            }
        }
    }

    /**
     * Disconnect this widget if we have a connection to it
     *
     * @param widget the widget we are removing
     */
    public void disconnectWidget(ConstraintWidget widget) {
        final ArrayList<ConstraintAnchor> anchors = getAnchors();
        for (int i = 0, anchorsSize = anchors.size(); i < anchorsSize; i++) {
            final ConstraintAnchor anchor = anchors.get(i);
            if (anchor.isConnected() && (anchor.getTarget().getOwner() == widget)) {
                anchor.reset();
            }
        }
    }

    /**
     * Disconnect this widget if we have a auto connection to it
     *
     * @param widget the widget we are removing
     */
    public void disconnectUnlockedWidget(ConstraintWidget widget) {
        final ArrayList<ConstraintAnchor> anchors = getAnchors();
        for (int i = 0, anchorsSize = anchors.size(); i < anchorsSize; i++) {
            final ConstraintAnchor anchor = anchors.get(i);
            if (anchor.isConnected() && (anchor.getTarget().getOwner() == widget)
                    && anchor.getConnectionCreator() == ConstraintAnchor.AUTO_CONSTRAINT_CREATOR) {
                anchor.reset();
            }
        }
    }

    /**
     * Given a type of anchor, returns the corresponding anchor.
     *
     * @param anchorType type of the anchor (LEFT, TOP, RIGHT, BOTTOM, BASELINE, CENTER_X, CENTER_Y)
     * @return the matching anchor
     */
    public ConstraintAnchor getAnchor(ConstraintAnchor.Type anchorType) {
        switch (anchorType) {
            case LEFT: {
                return mLeft;
            }
            case TOP: {
                return mTop;
            }
            case RIGHT: {
                return mRight;
            }
            case BOTTOM: {
                return mBottom;
            }
            case BASELINE: {
                return mBaseline;
            }
            case CENTER_X: {
                return mCenterX;
            }
            case CENTER_Y: {
                return mCenterY;
            }
            case CENTER: {
                return mCenter;
            }
        }
        return null;
    }

    /**
     * Accessor for the horizontal dimension behaviour
     *
     * @return dimension behaviour
     */
    public DimensionBehaviour getHorizontalDimensionBehaviour() {
        return mHorizontalDimensionBehaviour;
    }

    /**
     * Accessor for the vertical dimension behaviour
     *
     * @return dimension behaviour
     */
    public DimensionBehaviour getVerticalDimensionBehaviour() {
        return mVerticalDimensionBehaviour;
    }

    /**
     * Set the widget's behaviour for the horizontal dimension
     *
     * @param behaviour the horizontal dimension's behaviour
     */
    public void setHorizontalDimensionBehaviour(DimensionBehaviour behaviour) {
        mHorizontalDimensionBehaviour = behaviour;
        if (mHorizontalDimensionBehaviour == DimensionBehaviour.WRAP_CONTENT) {
            setWidth(mWrapWidth);
        }
    }

    /**
     * Set the widget's behaviour for the vertical dimension
     *
     * @param behaviour the vertical dimension's behaviour
     */
    public void setVerticalDimensionBehaviour(DimensionBehaviour behaviour) {
        mVerticalDimensionBehaviour = behaviour;
        if (mVerticalDimensionBehaviour == DimensionBehaviour.WRAP_CONTENT) {
            setHeight(mWrapHeight);
        }
    }

    /*-----------------------------------------------------------------------*/
    // Constraints
    /*-----------------------------------------------------------------------*/

    /**
     * Add this widget to the solver
     *
     * @param system the solver we want to add the widget to
     */
    @Override
    public void addToSolver(LinearSystem system) {
        if (getParent() != null) {

            // If the parent is set to wrap content, we need to:
            // - possibly add an extra constraint to ensure that the widget is contained into the parent
            // - or if there is an existing constraint to the parent, make sure it's a strict constraint
            //   (i.e. top of widget strictly superior to top of parent -- otherwise the error could be
            //   bi-directional, and would result in an unstable system where the widget would sometimes
            //   not be contained in the parent)

            if (getParent() instanceof ConstraintTableLayout) {
                ConstraintAnchor leftAnchor = getAnchor(ConstraintAnchor.Type.LEFT);
                leftAnchor.setConnectionType(ConstraintAnchor.ConnectionType.STRICT);
                ConstraintAnchor rightAnchor = getAnchor(ConstraintAnchor.Type.RIGHT);
                rightAnchor.setConnectionType(ConstraintAnchor.ConnectionType.STRICT);
                ConstraintAnchor topAnchor = getAnchor(ConstraintAnchor.Type.TOP);
                topAnchor.setConnectionType(ConstraintAnchor.ConnectionType.STRICT);
                ConstraintAnchor bottomAnchor = getAnchor(ConstraintAnchor.Type.BOTTOM);
                bottomAnchor.setConnectionType(ConstraintAnchor.ConnectionType.STRICT);
            } else {
                if (getParent().getHorizontalDimensionBehaviour()
                        == DimensionBehaviour.WRAP_CONTENT) {
                    ConstraintAnchor leftAnchor = getAnchor(ConstraintAnchor.Type.LEFT);
                    if (!leftAnchor.isConnected() ||
                            leftAnchor.getTarget().getOwner() != getParent()) {
                        system.addConstraint(EquationCreation.createRowGreaterThan(
                                system, system.createObjectVariable(mLeft),
                                system.createObjectVariable(getParent().mLeft),
                                0, false
                        ));
                    } else if (leftAnchor.isConnected() &&
                            leftAnchor.getTarget().getOwner() == getParent()) {
                        leftAnchor.setConnectionType(ConstraintAnchor.ConnectionType.STRICT);
                    }

                    ConstraintAnchor rightAnchor = getAnchor(ConstraintAnchor.Type.RIGHT);
                    if (!rightAnchor.isConnected() ||
                            rightAnchor.getTarget().getOwner() != getParent()) {
                        system.addConstraint(EquationCreation.createRowGreaterThan(
                                system, system.createObjectVariable(getParent().mRight),
                                system.createObjectVariable(mRight),
                                0, false
                        ));
                    } else if (rightAnchor.isConnected() &&
                            rightAnchor.getTarget().getOwner() == getParent()) {
                        rightAnchor.setConnectionType(ConstraintAnchor.ConnectionType.STRICT);
                    }
                }

                if (getParent().getVerticalDimensionBehaviour()
                        == DimensionBehaviour.WRAP_CONTENT) {
                    ConstraintAnchor topAnchor = getAnchor(ConstraintAnchor.Type.TOP);
                    if (!topAnchor.isConnected() ||
                            topAnchor.getTarget().getOwner() != getParent()) {
                        system.addConstraint(EquationCreation.createRowGreaterThan(
                                system, system.createObjectVariable(mTop),
                                system.createObjectVariable(getParent().mTop),
                                0, false
                        ));
                    } else if (topAnchor.isConnected() &&
                            topAnchor.getTarget().getOwner() == getParent()) {
                        topAnchor.setConnectionType(ConstraintAnchor.ConnectionType.STRICT);
                    }
                    ConstraintAnchor bottomAnchor = getAnchor(ConstraintAnchor.Type.BOTTOM);
                    if (!bottomAnchor.isConnected() ||
                            bottomAnchor.getTarget().getOwner() != getParent()) {
                        system.addConstraint(EquationCreation.createRowGreaterThan(
                                system, system.createObjectVariable(getParent().mBottom),
                                system.createObjectVariable(mBottom),
                                0, false
                        ));
                    } else if (bottomAnchor.isConnected() &&
                            bottomAnchor.getTarget().getOwner() == getParent()) {
                        bottomAnchor.setConnectionType(ConstraintAnchor.ConnectionType.STRICT);
                    }
                }
            }
        }

        int width = mWidth;
        if (width < mMinWidth) {
            width = mMinWidth;
        }
        int height = mHeight;
        if (height < mMinHeight) {
            height = mMinHeight;
        }

        boolean horizontalDimensionLocked = mHorizontalDimensionBehaviour != DimensionBehaviour.ANY;
        boolean verticalDimensionLocked = mVerticalDimensionBehaviour != DimensionBehaviour.ANY;

        boolean useRatio = false;
        if (mDimensionRatio > 0) {
            if (!horizontalDimensionLocked && !verticalDimensionLocked) {
                useRatio = true;
                // add an equation
                SolverVariable left = system.createObjectVariable(mLeft);
                SolverVariable right = system.createObjectVariable(mRight);
                SolverVariable top = system.createObjectVariable(mTop);
                SolverVariable bottom = system.createObjectVariable(mBottom);
                system.addConstraint(EquationCreation.createRowDimensionRatio(system, right, left, bottom, top,
                        mDimensionRatio, false));
            } else if (!horizontalDimensionLocked && verticalDimensionLocked) {
                width = (int) (mDimensionRatio * mHeight);
                horizontalDimensionLocked = true;
            } else if (horizontalDimensionLocked && !verticalDimensionLocked) {
                height = (int) (mDimensionRatio * mWidth);
                verticalDimensionLocked = true;
            }
        }

        boolean wrapContent = (mHorizontalDimensionBehaviour == DimensionBehaviour.WRAP_CONTENT)
                && (this instanceof ConstraintWidgetContainer);
        applyConstraints(system, wrapContent, horizontalDimensionLocked, mLeft, mRight,
                mX, mX + width, width, mHorizontalBiasPercent, useRatio);

        wrapContent = (mVerticalDimensionBehaviour == DimensionBehaviour.WRAP_CONTENT)
                && (this instanceof ConstraintWidgetContainer);

        if (mBaselineDistance > 0) {
            SolverVariable top = system.createObjectVariable(mTop);
            SolverVariable bottom = system.createObjectVariable(mBottom);
            SolverVariable baseline = system.createObjectVariable(mBaseline);
            ConstraintAnchor end = mBottom;
            system.addConstraint(
                    EquationCreation.createRowEquals(system, bottom, baseline,
                            height - getBaselineDistance(),
                            false));
            if (mBaseline.isConnected()) {
                height = getBaselineDistance();
                end = mBaseline;
            }
            applyConstraints(system, wrapContent, verticalDimensionLocked,
                    mTop, end, mY, mY + height, height, mVerticalBiasPercent, useRatio);
        } else {
            applyConstraints(system, wrapContent, verticalDimensionLocked,
                    mTop, mBottom, mY, mY + height, height, mVerticalBiasPercent, useRatio);
        }

    }

    /**
     * Apply the constraints in the system depending on the existing anchors, in one dimension
     *
     * @param system          the linear system we are adding constraints to
     * @param wrapContent     is the widget trying to wrap its content (i.e. its size will depends on its content)
     * @param dimensionLocked is the widget dimensions locked or can they change
     * @param beginAnchor     the first anchor
     * @param endAnchor       the second anchor
     * @param beginPosition   the original position of the anchor
     * @param endPosition     the original position of the anchor
     * @param dimension       the dimension
     */
    private void applyConstraints(LinearSystem system, boolean wrapContent, boolean dimensionLocked,
            ConstraintAnchor beginAnchor, ConstraintAnchor endAnchor,
            int beginPosition, int endPosition, int dimension, float bias, boolean useRatio) {
        SolverVariable begin = system.createObjectVariable(beginAnchor);
        SolverVariable end = system.createObjectVariable(endAnchor);
        SolverVariable beginTarget = system.createObjectVariable(beginAnchor.getTarget());
        SolverVariable endTarget = system.createObjectVariable(endAnchor.getTarget());

        if (mVisibility == ConstraintWidget.GONE) {
            dimension = 0;
        }
        if (beginTarget == null && endTarget == null) {
            system.addConstraint(EquationCreation.createRowEquals(system, begin, beginPosition));
            if (wrapContent) {
                system.addConstraint(EquationCreation.createRowEquals(system, end, begin, 0, true));
            } else {
                if (dimensionLocked) {
                    system.addConstraint(
                            EquationCreation.createRowEquals(system, end, begin, dimension, false));
                } else {
                    system.addConstraint(
                            EquationCreation.createRowEquals(system, end, endPosition));
                }
            }
        } else if (beginTarget != null && endTarget == null) {
            system.addConstraint(EquationCreation
                    .createRowEquals(system, begin, beginTarget, beginAnchor.getMargin(), false));
            if (wrapContent) {
                system.addConstraint(EquationCreation.createRowEquals(system, end, begin, 0, true));
            } else {
                if (dimensionLocked) {
                    system.addConstraint(
                            EquationCreation.createRowEquals(system, end, begin, dimension, false));
                } else {
                    system.addConstraint(
                            EquationCreation.createRowEquals(system, end, endPosition));
                }
            }
        } else if (beginTarget == null && endTarget != null) {
            system.addConstraint(EquationCreation
                    .createRowEquals(system, end, endTarget, -1 * endAnchor.getMargin(), false));
            if (wrapContent) {
                system.addConstraint(EquationCreation.createRowEquals(system, end, begin, 0, true));
            } else {
                if (dimensionLocked) {
                    system.addConstraint(
                            EquationCreation.createRowEquals(system, end, begin, dimension, false));
                } else {
                    system.addConstraint(
                            EquationCreation.createRowEquals(system, begin, beginPosition));
                }
            }
        } else { // both constraints set
            if (dimensionLocked) {
                if (wrapContent) {
                    system.addConstraint(
                            EquationCreation.createRowEquals(system, end, begin, 0, true));
                } else {
                    system.addConstraint(
                            EquationCreation.createRowEquals(system, end, begin, dimension, false));
                }

                int constraintStrength = 1;
                if (beginAnchor.getStrength() != endAnchor.getStrength()) {
                    if (beginAnchor.getStrength() == ConstraintAnchor.Strength.STRONG) {
                        system.addConstraint(EquationCreation
                                .createRowEquals(system, begin, beginTarget,
                                        beginAnchor.getMargin(), false, constraintStrength));
                        system.addConstraint(EquationCreation
                                .createRowLowerThan(system, end, endTarget,
                                        -1 * endAnchor.getMargin(), false, constraintStrength));
                    } else {
                        system.addConstraint(EquationCreation
                                .createRowGreaterThan(system, begin, beginTarget,
                                        beginAnchor.getMargin(), false, constraintStrength));
                        system.addConstraint(EquationCreation
                                .createRowEquals(system, end, endTarget, -1 * endAnchor.getMargin(),
                                        false, constraintStrength));
                    }
                } else {
                    if (beginTarget == endTarget) {
                        system.addConstraint(EquationCreation
                                .createRowCentering(system, begin, beginTarget,
                                        0, 0.5f, endTarget, end, 0, true,
                                        constraintStrength));
                    } else {
                        boolean useBidirectionalError = (beginAnchor.getConnectionType() !=
                                ConstraintAnchor.ConnectionType.STRICT);
                        system.addConstraint(EquationCreation
                                .createRowGreaterThan(system, begin, beginTarget,
                                        beginAnchor.getMargin(), useBidirectionalError,
                                        constraintStrength));
                        useBidirectionalError = (endAnchor.getConnectionType() !=
                                ConstraintAnchor.ConnectionType.STRICT);
                        system.addConstraint(EquationCreation
                                .createRowLowerThan(system, end, endTarget,
                                        -1 * endAnchor.getMargin(),
                                        useBidirectionalError, constraintStrength));
                        system.addConstraint(EquationCreation
                                .createRowCentering(system, begin, beginTarget,
                                        beginAnchor.getMargin(),
                                        bias, endTarget, end, endAnchor.getMargin(), false,
                                        constraintStrength));
                    }
                }
            } else  if (useRatio) {
                system.addConstraint(EquationCreation
                        .createRowEquals(system, begin, beginTarget, beginAnchor.getMargin(),
                                true));
                system.addConstraint(EquationCreation
                        .createRowEquals(system, end, endTarget, -1 * endAnchor.getMargin(),
                                true));
                system.addConstraint(EquationCreation
                        .createRowCentering(system, begin, beginTarget,
                                0, 0.5f, endTarget, end, 0, true, 1));
            } else {
                system.addConstraint(EquationCreation
                        .createRowEquals(system, begin, beginTarget, beginAnchor.getMargin(),
                                false));
                system.addConstraint(EquationCreation
                        .createRowEquals(system, end, endTarget, -1 * endAnchor.getMargin(),
                                false));
            }
        }
    }

    /**
     * Update the widget from the values generated by the solver
     *
     * @param system the solver we get the values from.
     */
    @Override
    public void updateFromSolver(LinearSystem system) {
        int left = system.getObjectVariableValue(mLeft);
        int top = system.getObjectVariableValue(mTop);
        int right = system.getObjectVariableValue(mRight);
        int bottom = system.getObjectVariableValue(mBottom);
        setFrame(left, top, right, bottom);
    }

}
