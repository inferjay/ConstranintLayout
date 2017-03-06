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

package android.support.constraint;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.support.constraint.solver.widgets.ConstraintAnchor;
import android.support.constraint.solver.widgets.ConstraintWidget;
import android.support.constraint.solver.widgets.ConstraintWidgetContainer;
import android.support.constraint.solver.widgets.Guideline;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

import static android.support.constraint.ConstraintLayout.LayoutParams.*;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

/**
 * A {@code ConstraintLayout} is a {@link android.view.ViewGroup} which allows you
 * to position and size widgets in a flexible way.
 *<p>
 *     <b>Note:</b> {@code ConstraintLayout} is available as a support library that you can use
 *     on Android systems starting with API level 9 (Gingerbread).
 *     As such, we are planning in enriching its API and capabilities over time.
 *     This documentation will reflects the changes.
 *</p>
 * <p>
 * There are currently various types of constraints that you can use:
 * <ul>
 *     <li>
 *         <a href="#RelativePositioning">Relative positioning</a>
 *     </li>
 *     <li>
 *         <a href="#Margins">Margins</a>
 *     </li>
 *     <li>
 *         <a href="#CenteringPositioning">Centering positioning</a>
 *     </li>
 *     <li>
 *         <a href="#VisibilityBehavior">Visibility behavior</a>
 *     </li>
 *     <li>
 *         <a href="#DimensionConstraints">Dimension constraints</a>
 *     </li>
 *     <li>
 *         <a href="#Chains">Chains</a>
 *     </li>
 *     <li>
 *         <a href="#VirtualHelpers">Virtual Helpers objects</a>
 *     </li>
 * </ul>
 * </p>
 *
 * <p>
 * Note that you cannot have a circular dependency in constraints.
 * </p>
 * <p>
 * Also see {@link ConstraintLayout.LayoutParams
 * ConstraintLayout.LayoutParams} for layout attributes
 * </p>
 *
 * <div class="special reference">
 * <h3>Developer Guide</h3>
 *
 * <h4 id="RelativePositioning"> Relative positioning </h4>
 * <p>
 *     Relative positioning is one of the basic building block of creating layouts in ConstraintLayout.
 *     Those constraints allow you to position a given widget relative to another one. You can constrain
 *     a widget on the horizontal and vertical axis:
 *     <ul>
 *         <li>Horizontal Axis: Left, Right, Start and End sides</li>
 *         <li>Vertical Axis: top, bottom sides and text baseline</li>
 *     </ul>
 *     <p>
 *     The general concept is to constrain a given side of a widget to another side of any other widget.
 *     <p>
 *     For example, in order to position button B to the right of button A (Fig. 1):
 *     <br><div align="center">
 *       <img width="300px" src="resources/images/relative-positioning.png">
 *           <br><b><i>Fig. 1 - Relative Positioning Example</i></b>
 *     </div>
 *     </p>
 *     <p>
 *         you would need to do:
 *     </p>
 *     <pre>{@code
 *         <Button android:id="@+id/buttonA" ... />
 *         <Button android:id="@+id/buttonB" ...
 *                 app:layout_constraintLeft_toRightOf="@+id/buttonA" />
 *         }
 *     </pre>
 *     This tells the system that we want the left side of button B to be constrained to the right side of button A.
 *     Such a position constraint means that the system will try to have both sides share the same location.
 *     <br><div align="center" >
 *       <img width="350px" src="resources/images/relative-positioning-constraints.png">
 *           <br><b><i>Fig. 2 - Relative Positioning Constraints</i></b>
 *     </div>
 *
 *     <p>Here is the list of available constraints (Fig. 2):</p>
 *     <ul>
 *         <li>{@code layout_constraintLeft_toLeftOf}</li>
 *         <li>{@code layout_constraintLeft_toRightOf}</li>
 *         <li>{@code layout_constraintRight_toLeftOf}</li>
 *         <li>{@code layout_constraintRight_toRightOf}</li>
 *         <li>{@code layout_constraintTop_toTopOf}</li>
 *         <li>{@code layout_constraintTop_toBottomOf}</li>
 *         <li>{@code layout_constraintBottom_toTopOf}</li>
 *         <li>{@code layout_constraintBottom_toBottomOf}</li>
 *         <li>{@code layout_constraintBaseline_toBaselineOf}</li>
 *         <li>{@code layout_constraintStart_toEndOf}</li>
 *         <li>{@code layout_constraintStart_toStartOf}</li>
 *         <li>{@code layout_constraintEnd_toStartOf}</li>
 *         <li>{@code layout_constraintEnd_toEndOf}</li>
 *     </ul>
 *     <p>
 *     They all takes a reference {@code id} to another widget, or the {@code parent} (which will reference the parent container, i.e. the ConstraintLayout):
 *     <pre>{@code
 *         <Button android:id="@+id/buttonB" ...
 *                 app:layout_constraintLeft_toLeftOf="parent" />
 *         }
 *     </pre>
 *
 *     </p>
 *
 * <h4 id="Margins"> Margins </h4>
 * <p>
 *     <div align="center" >
 *       <img width="325px" src="resources/images/relative-positioning-margin.png">
 *           <br><b><i>Fig. 3 - Relative Positioning Margins</i></b>
 *     </div>
 *      <p>If side margins are set, they will be applied to the corresponding constraints (if they exist) (Fig. 3), enforcing
 *      the margin as a space between the target and the source side. The usual layout margins attributes can be used to this effect:
 *      <ul>
 *          <li>{@code android:layout_marginStart}</li>
 *          <li>{@code android:layout_marginEnd}</li>
 *          <li>{@code android:layout_marginLeft}</li>
 *          <li>{@code android:layout_marginTop}</li>
 *          <li>{@code android:layout_marginRight}</li>
 *          <li>{@code android:layout_marginBottom}</li>
 *      </ul>
 *      <p>Note that a margin can only be positive or equals to zero, and takes a {@code Dimension}.</p>
 * <h4 id="GoneMargin"> Margins when connected to a GONE widget</h4>
 *      <p>When a position constraint target's visibility is {@code View.GONE}, you can also indicates a different
 *      margin value to be used using the following attributes:</p>
 *      <ul>
 *          <li>{@code layout_goneMarginStart}</li>
 *          <li>{@code layout_goneMarginEnd}</li>
 *          <li>{@code layout_goneMarginLeft}</li>
 *          <li>{@code layout_goneMarginTop}</li>
 *          <li>{@code layout_goneMarginRight}</li>
 *          <li>{@code layout_goneMarginBottom}</li>
 *      </ul>
 * </p>

 * </p>
 * <h4 id="CenteringPositioning"> Centering positioning and bias</h4>
 * <p>
 *     A useful aspect of {@code ConstraintLayout} is in how it deals with "impossible" constrains. For example, if
 *     we have something like:
 *     <pre>{@code
 *         <android.support.constraint.ConstraintLayout ...>
 *             <Button android:id="@+id/button" ...
 *                 app:layout_constraintLeft_toLeftOf="parent"
 *                 app:layout_constraintRight_toRightOf="parent/>
 *         </>
 *         }
 *     </pre>
 * </p>
 * <p>
 *     Unless the {@code ConstraintLayout} happens to have the exact same size as the {@code Button}, both constraints
 *     cannot be satisfied at the same time (both sides cannot be where we want them to be).
 *     <p><div align="center" >
 *       <img width="325px" src="resources/images/centering-positioning.png">
 *           <br><b><i>Fig. 4 - Centering Positioning</i></b>
 *     </div>
 *     <p>
 *     What happens in this case is that the constraints act like opposite forces
 *     pulling the widget apart equally (Fig. 4); such that the widget will end up being centered in the parent container.
 *     This will apply similarly for vertical constraints.
 * </p>
 * <h5 id="Bias">Bias</h5>
 *     <p>
 *        The default when encountering such opposite constraints is to center the widget; but you can tweak
 *        the positioning to favor one side over another using the bias attributes:
 *        <ul>
 *            <li>{@code layout_constraintHorizontal_bias}</li>
 *            <li>{@code layout_constraintVertical_bias}</li>
 *        </ul>
 *     <p><div align="center" >
 *       <img width="325px" src="resources/images/centering-positioning-bias.png">
 *           <br><b><i>Fig. 5 - Centering Positioning with Bias</i></b>
 *     </div>
 *     <p>
 *        For example the following will make the left side with a 30% bias instead of the default 50%, such that the left side will be
 *        shorter, with the widget leaning more toward the left side (Fig. 5):
 *        </p>
 *     <pre>{@code
 *         <android.support.constraint.ConstraintLayout ...>
 *             <Button android:id="@+id/button" ...
 *                 app:layout_constraintHorizontal_bias="0.3"
 *                 app:layout_constraintLeft_toLeftOf="parent"
 *                 app:layout_constraintRight_toRightOf="parent/>
 *         </>
 *         }
 *     </pre>
 *     Using bias, you can craft User Interfaces that will better adapt to screen sizes changes.
 *     </p>
 * </p>
 *
 * <h4 id="VisibilityBehavior"> Visibility behavior </h4>
 * <p>
 *     {@code ConstraintLayout} has a specific handling of widgets being marked as {@code View.GONE}.
 *     <p>{@code GONE} widgets, as usual, are not going to be displayed and are not part of the layout itself (i.e. their actual dimensions
 *      will not be changed if marked as {@code GONE}).
 *
 *     <p>But in terms of the layout computations, {@code GONE} widgets are still part of it, with an important distinction:
 *     <ul>
 *         <li> For the layout pass, their dimension will be considered as if zero (basically, they will be resolved to a point)</li>
 *         <li> If they have constraints to other widgets they will still be respected, but any margins will be as if equals to zero</li>
 *     </ul>
 *
 *     <p><div align="center" >
 *       <img width="350px" src="resources/images/visibility-behavior.png">
 *           <br><b><i>Fig. 6 - Visibility Behavior</i></b>
 *     </div>
 *     <p>This specific behavior allows to build layouts where you can temporarily mark widgets as being {@code GONE},
 *     without breaking the layout (Fig. 6), which can be particularly useful when doing simple layout animations.
 *     <p><b>Note: </b>The margin used will be the margin that B had defined when connecting to A (see Fig. 6 for an example).
 *     In some cases, this might not be the margin you want (e.g. A had a 100dp margin to the side of its container,
 *     B only a 16dp to A, marking
 *     A as gone, B will have a margin of 16dp to the container).
 *     For this reason, you can specify an alternate
 *     margin value to be used when the connection is to a widget being marked as gone (see <a href="#GoneMargin">the section above about the gone margin attributes</a>).
 * </p>
 *
 * <h4 id="DimensionConstraints"> Dimensions constraints </h4>
 * <h5>Minimum dimensions on ConstraintLayout</h5>
 * <p>
 *     You can define minimum sizes for the {@code ConstraintLayout} itself:
 *     <ul>
 *         <li>{@code android:minWidth} set the minimum width for the layout</li>
 *         <li>{@code android:minHeight} set the minimum height for the layout</li>
 *     </ul>
 *     Those minimum dimensions will be used by {@code ConstraintLayout} when its dimensions are set to {@code WRAP_CONTENT}.
 * </p>
 * <h5>Widgets dimension constraints</h5>
 * <p>
 *     The dimension of the widgets can be specified by setting the {@code android:layout_width} and
 *     {@code android:layout_height} attributes in 3 different ways:
 *     <ul>
 *         <li>Using a specific dimension (either a literal value such as {@code 123dp} or a {@code Dimension} reference)</li>
 *         <li>Using {@code WRAP_CONTENT}, which will ask the widget to compute its own size</li>
 *         <li>Using {@code 0dp}, which is the equivalent of "{@code MATCH_CONSTRAINT}"</li>
 *     </ul>
 *     <p><div align="center" >
 *       <img width="325px" src="resources/images/dimension-match-constraints.png">
 *           <br><b><i>Fig. 7 - Dimension Constraints</i></b>
 *     </div>
 *     The first two works in a similar fashion as other layouts. The last one will resize the widget in such a way as
 *     matching the constraints that are set (see Fig. 7, (a) is wrap_content, (b) is 0dp). If margins are set, they will be taken in account
 *     in the computation (Fig. 7, (c) with 0dp).
 *     <p>
 *         <b>Important: </b> {@code MATCH_PARENT} is not recommended for widgets contained in a {@code ConstraintLayout}. Similar behavior can
 *         be defined by using {@code MATCH_CONSTRAINT} with the corresponding left/right or top/bottom constraints being set to {@code "parent"}.
 *     </p>
 * </p>
 * <h5>Ratio</h5>
 * <p>
 *     You can also define one dimension of a widget as a ratio of the other one. In order to do that, you
 *     need to have at least one constrained dimension be set to {@code 0dp} (i.e., {@code MATCH_CONSTRAINT}), and set the
 *     attribute {@code layout_constraintDimentionRatio} to a given ratio.
 *     For example:
 *     <pre>
 *         {@code
 *           <Button android:layout_width="wrap_content"
 *                   android:layout_height="0dp"
 *                   app:layout_constraintDimensionRatio="1:1" />
 *         }
 *     </pre>
 *     will set the height of the button to be the same as its width.
 * </p>
 * <p> The ratio can be expressed either as:
 * <ul>
 *     <li>a float value, representing a ratio between width and height</li>
 *     <li>a ratio in the form "width:height"</li>
 * </ul>
 * </p>
 * <p>
 *     You can also use ratio if both dimensions are set to {@code MATCH_CONSTRAINT} (0dp). In this case the system sets the
 *     largest dimensions the satisfies all constraints and maintains the aspect ratio specified. To constrain one specific side
 *     based on the dimensions of another. You can pre append {@code W,}" or {@code H,} to constrain the width or height
 *     respectively.
 *     For example,
 *     If one dimension is constrained by two targets (e.g. width is 0dp and centered on parent) you can indicate which
 *     side should be constrained, by adding the letter {@code W} (for constraining the width) or {@code H}
 *     (for constraining the height) in front of the ratio, separated
 *     by a comma:
 *     <pre>
 *         {@code
 *           <Button android:layout_width="0dp"
 *                   android:layout_height="0dp"
 *                   app:layout_constraintDimensionRatio="H,16:9"
 *                   app:layout_constraintBottom_toBottomOf="parent"
 *                   app:layout_constraintTop_toTopOf="parent"/>
 *         }
 *     </pre>
 *     will set the height of the button following a 16:9 ratio, while the width of the button will match the constraints
 *     to parent.
 *
 * </p>
 *
 * <h4 id="Chains">Chains</h4>
 * <p>Chains provide group-like behavior in a single axis (horizontally or vertically). The other axis can be constrained independently.</p>
 * <h5>Creating a chain</h5>
 * <p>
 *     A set of widgets are considered a chain if they a linked together via a bi-directional connection (see Fig. 8, showing a minimal chain, with two widgets).
 * </p>
 *     <p><div align="center" >
 *       <img width="325px" src="resources/images/chains.png">
 *           <br><b><i>Fig. 8 - Chain</i></b>
 *     </div>
 * <p>
 * <h5>Chain heads</h5>
 * <p>
 *     Chains are controlled by attributes set on the first element of the chain (the "head" of the chain):
 * </p>
 *     <p><div align="center" >
 *       <img width="400px" src="resources/images/chains-head.png">
 *           <br><b><i>Fig. 9 - Chain Head</i></b>
 *     </div>
 *     <p>The head is the left-most widget for horizontal chains, and the top-most widget for vertical chains.</p>
 * <h5>Margins in chains</h5>
 * <p>If margins are specified on connections, they will be taken in account. In the case of spread chains, margins will be deducted from the allocated space.</p>
 * <h5>Chain Style</h5>
 * <p>When setting the attribute {@code layout_constraintHorizontal_chainStyle} or {@code layout_constraintVertical_chainStyle} on the first element of a chain,
 * the behavior of the chain will change according to the specified style (default is {@code CHAIN_SPREAD}).
 * <ul>
 *     <li>{@code CHAIN_SPREAD} -- the elements will be spread out (default style)</li>
 *     <li>Weighted chain -- in {@code CHAIN_SPREAD} mode, if some widgets are set to {@code MATCH_CONSTRAINT}, they will split the available space</li>
 *     <li>{@code CHAIN_SPREAD_INSIDE} -- similar, but the endpoints of the chain will not be spread out</li>
 *     <li>{@code CHAIN_PACKED} -- the elements of the chain will be packed together. The horizontal or vertical
 *          bias attribute of the child will then affect the positioning of the packed elements</li>
 * </ul>
 *     <p><div align="center" >
 *       <img width="600px" src="resources/images/chains-styles.png">
 *           <br><b><i>Fig. 10 - Chains Styles</i></b>
 *     </div>
 * </p>
 * <h5>Weighted chains</h5>
 * <p>The default behavior of a chain is to spread the elements equally in the available space. If one or more elements are using {@code MATCH_CONSTRAINT}, they
 * will use the available empty space (equally divided among themselves). The attribute {@code layout_constraintHorizontal_weight} and {@code layout_constraintVertical_weight}
 * will control how the space will be distributed among the elements using {@code MATCH_CONSTRAINT}. For exemple, on a chain containing two elements using {@code MATCH_CONSTRAINT},
 * with the first element using a weight of 2 and the second a weight of 1, the space occupied by the first element will be twice that of the second element.</p>
 *
 * <h4 id="VirtualHelpers"> Virtual Helper objects </h4>
 * <p>In addition to the intrinsic capabilities detailed previously, you can also use special helper objects
 * in {@code ConstraintLayout} to help you with your layout. Currently, the {@code Guideline}{@see Guideline} object allows you to create
 * Horizontal and Vertical guidelines which are positioned relative to the {@code ConstraintLayout} container. Widgets can
 * then be positioned by constraining them to such guidelines.</p>
 * </div>
 */
public class ConstraintLayout extends ViewGroup {
    // For now, disallow embedded (single-layer resolution) situations.
    // While it works, the constraints of the layout have the same importance as any other
    // constraint of the overall layout, which can cause issues. Let's revisit this
    // after implementing priorities/hierarchy of constraints.
    static final boolean ALLOWS_EMBEDDED = false;

    /** @hide */
    public static final String VERSION="ConstraintLayout-1.0.0";
    private static final String TAG = "ConstraintLayout";
    private static final boolean SIMPLE_LAYOUT = true;

    SparseArray<View> mChildrenByIds = new SparseArray<>();

    // This array will keep a list of the widget with one or two dimensions that are
    // set to MATCH_CONSTRAINT (i.e. they depend on the solver result, not from
    // WRAP_CONTENT or a fixed dimension)
    private final ArrayList<ConstraintWidget> mVariableDimensionsWidgets = new ArrayList<>(100);

    ConstraintWidgetContainer mLayoutWidget = new ConstraintWidgetContainer();

    private int mMinWidth = 0;
    private int mMinHeight = 0;
    private int mMaxWidth = Integer.MAX_VALUE;
    private int mMaxHeight = Integer.MAX_VALUE;

    private boolean mDirtyHierarchy = true;
    private int mOptimizationLevel = 2; // all
    private ConstraintSet mConstraintSet = null;

    public ConstraintLayout(Context context) {
        super(context);
        init(null);
    }

    public ConstraintLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public ConstraintLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    @Override
    public void setId(int id) {
        mChildrenByIds.remove(getId());
        super.setId(id);
        mChildrenByIds.put(getId(), this);
    }

    private void init(AttributeSet attrs) {
        mLayoutWidget.setCompanionWidget(this);
        mChildrenByIds.put(getId(), this);
        mConstraintSet = null;
        if (attrs != null) {
            TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.ConstraintLayout_Layout);
            final int N = a.getIndexCount();
            for (int i = 0; i < N; i++) {
                int attr = a.getIndex(i);
                if (attr == R.styleable.ConstraintLayout_Layout_android_minWidth) {
                    mMinWidth = a.getDimensionPixelOffset(attr, mMinWidth);
                } else if (attr == R.styleable.ConstraintLayout_Layout_android_minHeight) {
                    mMinHeight = a.getDimensionPixelOffset(attr, mMinHeight);
                } else if (attr == R.styleable.ConstraintLayout_Layout_android_maxWidth) {
                    mMaxWidth = a.getDimensionPixelOffset(attr, mMaxWidth);
                } else if (attr == R.styleable.ConstraintLayout_Layout_android_maxHeight) {
                    mMaxHeight = a.getDimensionPixelOffset(attr, mMaxHeight);
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_optimizationLevel) {
                    mOptimizationLevel = a.getInt(attr, mOptimizationLevel);
                } else if (attr == R.styleable.ConstraintLayout_Layout_constraintSet) {
                    int id = a.getResourceId(attr, 0);
                    mConstraintSet = new ConstraintSet();
                    mConstraintSet.load(getContext(),id);
                }
            }
            a.recycle();
        }
        mLayoutWidget.setOptimizationLevel(mOptimizationLevel);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        super.addView(child, index, params);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            onViewAdded(child);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeView(View view) {
        super.removeView(view);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            onViewRemoved(view);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onViewAdded(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            super.onViewAdded(view);
        }
        ConstraintWidget widget = getViewWidget(view);
        if (view instanceof android.support.constraint.Guideline) {
            if (!(widget instanceof Guideline)) {
                LayoutParams layoutParams = (LayoutParams) view.getLayoutParams();
                layoutParams.widget = new Guideline();
                layoutParams.isGuideline = true;
                ((Guideline) layoutParams.widget).setOrientation(layoutParams.orientation);
                widget = layoutParams.widget;
            }
        }
        mChildrenByIds.put(view.getId(), view);
        mDirtyHierarchy = true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onViewRemoved(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            super.onViewRemoved(view);
        }
        mChildrenByIds.remove(view.getId());
        mLayoutWidget.remove(getViewWidget(view));
        mDirtyHierarchy = true;
    }

    /**
     * Set the min width for this view
     *
     * @param value
     */
    public void setMinWidth(int value) {
        if (value == mMinWidth) {
            return;
        }
        mMinWidth = value;
        requestLayout();
    }

    /**
     * Set the min height for this view
     *
     * @param value
     */
    public void setMinHeight(int value) {
        if (value == mMinHeight) {
            return;
        }
        mMinHeight = value;
        requestLayout();
    }

    /*
     * The minimum width of this view.
     *
     * @return The minimum width of this view
     *
     * @see #setMinWidth(int)
     */
    public int getMinWidth() {
        return mMinWidth;
    }

    /**
     * The minimum height of this view.
     *
     * @return The minimum height of this view
     *
     * @see #setMinHeight(int)
     */
    public int getMinHeight() {
        return mMinHeight;
    }

    /**
     * Set the max width for this view
     *
     * @param value
     */
    public void setMaxWidth(int value) {
        if (value == mMaxWidth) {
            return;
        }
        mMaxWidth = value;
        requestLayout();
    }

    /**
     * Set the max height for this view
     *
     * @param value
     */
    public void setMaxHeight(int value) {
        if (value == mMaxHeight) {
            return;
        }
        mMaxHeight = value;
        requestLayout();
    }

    /*
     * The maximum width of this view.
     *
     * @return The maximum width of this view
     *
     * @see #setMaxWidth(int)
     */
    public int getMaxWidth() {
        return mMaxWidth;
    }

    /**
     * The maximum height of this view.
     *
     * @return The maximum height of this view
     *
     * @see #setMaxHeight(int)
     */
    public int getMaxHeight() {
        return mMaxHeight;
    }

    private void updateHierarchy() {
        final int count = getChildCount();

        boolean recompute = false;
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.isLayoutRequested()) {
                recompute = true;
                break;
            }
        }
        if (recompute) {
            mVariableDimensionsWidgets.clear();
            setChildrenConstraints();
        }
    }

    private void setChildrenConstraints() {
        if (mConstraintSet != null) {
            mConstraintSet.applyToInternal(this);
        }
        final int count = getChildCount();
        mLayoutWidget.removeAllChildren();
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            ConstraintWidget widget = getViewWidget(child);
            if (widget == null) {
                continue;
            }

            final LayoutParams layoutParams = (LayoutParams) child.getLayoutParams();
            widget.reset();
            widget.setVisibility(child.getVisibility());
            widget.setCompanionWidget(child);
            mLayoutWidget.add(widget);

            if (!layoutParams.verticalDimensionFixed || !layoutParams.horizontalDimensionFixed) {
                mVariableDimensionsWidgets.add(widget);
            }

            if (layoutParams.isGuideline) {
                Guideline guideline = (Guideline) widget;
                if (layoutParams.guideBegin != -1) {
                    guideline.setGuideBegin(layoutParams.guideBegin);
                }
                if (layoutParams.guideEnd != -1) {
                    guideline.setGuideEnd(layoutParams.guideEnd);
                }
                if (layoutParams.guidePercent != -1) {
                    guideline.setGuidePercent(layoutParams.guidePercent);
                }
            } else if ((layoutParams.resolvedLeftToLeft != UNSET)
                    || (layoutParams.resolvedLeftToRight != UNSET)
                    || (layoutParams.resolvedRightToLeft != UNSET)
                    || (layoutParams.resolvedRightToRight != UNSET)
                    || (layoutParams.topToTop != UNSET)
                    || (layoutParams.topToBottom != UNSET)
                    || (layoutParams.bottomToTop != UNSET)
                    || (layoutParams.bottomToBottom != UNSET)
                    || (layoutParams.baselineToBaseline != UNSET)
                    || (layoutParams.editorAbsoluteX != UNSET)
                    || (layoutParams.editorAbsoluteY != UNSET)
                    || (layoutParams.width == MATCH_PARENT)
                    || (layoutParams.height == MATCH_PARENT)) {

                // Get the left/right constraints resolved for RTL
                int resolvedLeftToLeft = layoutParams.resolvedLeftToLeft;
                int resolvedLeftToRight = layoutParams.resolvedLeftToRight;
                int resolvedRightToLeft = layoutParams.resolvedRightToLeft;
                int resolvedRightToRight = layoutParams.resolvedRightToRight;
                int resolveGoneLeftMargin = layoutParams.resolveGoneLeftMargin;
                int resolveGoneRightMargin = layoutParams.resolveGoneRightMargin;
                float resolvedHorizontalBias = layoutParams.resolvedHorizontalBias;

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    // Pre JB MR1, left/right should take precedence, unless they are
                    // not defined and somehow a corresponding start/end constraint exists
                    resolvedLeftToLeft = layoutParams.leftToLeft;
                    resolvedLeftToRight = layoutParams.leftToRight;
                    resolvedRightToLeft = layoutParams.rightToLeft;
                    resolvedRightToRight = layoutParams.rightToRight;
                    resolveGoneLeftMargin = layoutParams.goneLeftMargin;
                    resolveGoneRightMargin = layoutParams.goneRightMargin;
                    resolvedHorizontalBias = layoutParams.horizontalBias;

                    if (resolvedLeftToLeft == UNSET && resolvedLeftToRight == UNSET) {
                        if (layoutParams.startToStart != UNSET) {
                            resolvedLeftToLeft = layoutParams.startToStart;
                        } else if (layoutParams.startToEnd != UNSET) {
                            resolvedLeftToRight = layoutParams.startToEnd;
                        }
                    }
                    if (resolvedRightToLeft == UNSET && resolvedRightToRight == UNSET) {
                        if (layoutParams.endToStart != UNSET) {
                            resolvedRightToLeft = layoutParams.endToStart;
                        } else if (layoutParams.endToEnd != UNSET) {
                            resolvedRightToRight = layoutParams.endToEnd;
                        }
                    }
                }

                // Left constraint
                if (resolvedLeftToLeft != UNSET) {
                    ConstraintWidget target = getTargetWidget(resolvedLeftToLeft);
                    if (target != null) {
                        widget.immediateConnect(ConstraintAnchor.Type.LEFT, target,
                                ConstraintAnchor.Type.LEFT, layoutParams.leftMargin,
                                resolveGoneLeftMargin);
                    }
                } else if (resolvedLeftToRight != UNSET) {
                    ConstraintWidget target = getTargetWidget(resolvedLeftToRight);
                    if (target != null) {
                        widget.immediateConnect(ConstraintAnchor.Type.LEFT, target,
                                ConstraintAnchor.Type.RIGHT, layoutParams.leftMargin,
                                resolveGoneLeftMargin);
                    }
                }

                // Right constraint
                if (resolvedRightToLeft != UNSET) {
                    ConstraintWidget target = getTargetWidget(resolvedRightToLeft);
                    if (target != null) {
                        widget.immediateConnect(ConstraintAnchor.Type.RIGHT, target,
                                ConstraintAnchor.Type.LEFT, layoutParams.rightMargin,
                                resolveGoneRightMargin);
                    }
                } else if (resolvedRightToRight != UNSET) {
                    ConstraintWidget target = getTargetWidget(resolvedRightToRight);
                    if (target != null) {
                        widget.immediateConnect(ConstraintAnchor.Type.RIGHT, target,
                                ConstraintAnchor.Type.RIGHT, layoutParams.rightMargin,
                                resolveGoneRightMargin);
                    }
                }

                // Top constraint
                if (layoutParams.topToTop != UNSET) {
                    ConstraintWidget target = getTargetWidget(layoutParams.topToTop);
                    if (target != null) {
                        widget.immediateConnect(ConstraintAnchor.Type.TOP, target,
                                ConstraintAnchor.Type.TOP, layoutParams.topMargin,
                                layoutParams.goneTopMargin);
                    }
                } else if (layoutParams.topToBottom != UNSET) {
                    ConstraintWidget target = getTargetWidget(layoutParams.topToBottom);
                    if (target != null) {
                        widget.immediateConnect(ConstraintAnchor.Type.TOP, target,
                                ConstraintAnchor.Type.BOTTOM, layoutParams.topMargin,
                                layoutParams.goneTopMargin);
                    }
                }

                // Bottom constraint
                if (layoutParams.bottomToTop != UNSET) {
                    ConstraintWidget target = getTargetWidget(layoutParams.bottomToTop);
                    if (target != null) {
                        widget.immediateConnect(ConstraintAnchor.Type.BOTTOM, target,
                                ConstraintAnchor.Type.TOP, layoutParams.bottomMargin,
                                layoutParams.goneBottomMargin);
                    }
                } else if (layoutParams.bottomToBottom != UNSET) {
                    ConstraintWidget target = getTargetWidget(layoutParams.bottomToBottom);
                    if (target != null) {
                        widget.immediateConnect(ConstraintAnchor.Type.BOTTOM, target,
                                ConstraintAnchor.Type.BOTTOM, layoutParams.bottomMargin,
                                layoutParams.goneBottomMargin);
                    }
                }

                // Baseline constraint
                if (layoutParams.baselineToBaseline != UNSET) {
                    View view = mChildrenByIds.get(layoutParams.baselineToBaseline);
                    ConstraintWidget target = getTargetWidget(layoutParams.baselineToBaseline);
                    if (target != null && view != null && view.getLayoutParams() instanceof LayoutParams) {
                        LayoutParams targetParams = (LayoutParams) view.getLayoutParams();
                        layoutParams.needsBaseline = true;
                        targetParams.needsBaseline = true;
                        ConstraintAnchor baseline = widget.getAnchor(ConstraintAnchor.Type.BASELINE);
                        ConstraintAnchor targetBaseline =
                                target.getAnchor(ConstraintAnchor.Type.BASELINE);
                        baseline.connect(targetBaseline, 0, -1, ConstraintAnchor.Strength.STRONG,
                                ConstraintAnchor.USER_CREATOR, true);

                        widget.getAnchor(ConstraintAnchor.Type.TOP).reset();
                        widget.getAnchor(ConstraintAnchor.Type.BOTTOM).reset();
                    }
                }

                if (resolvedHorizontalBias >= 0 && resolvedHorizontalBias != 0.5f) {
                    widget.setHorizontalBiasPercent(resolvedHorizontalBias);
                }
                if (layoutParams.verticalBias >= 0 && layoutParams.verticalBias != 0.5f) {
                    widget.setVerticalBiasPercent(layoutParams.verticalBias);
                }

                if (isInEditMode() && ((layoutParams.editorAbsoluteX != UNSET)
                        || (layoutParams.editorAbsoluteY != UNSET))) {
                    widget.setOrigin(layoutParams.editorAbsoluteX, layoutParams.editorAbsoluteY);
                }

                // FIXME: need to agree on the correct magic value for this rather than simply using zero.
                if (!layoutParams.horizontalDimensionFixed) {
                    if (layoutParams.width == LayoutParams.MATCH_PARENT) {
                        widget.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_PARENT);
                        widget.getAnchor(ConstraintAnchor.Type.LEFT).mMargin = layoutParams.leftMargin;
                        widget.getAnchor(ConstraintAnchor.Type.RIGHT).mMargin = layoutParams.rightMargin;
                    } else {
                        widget.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
                        widget.setWidth(0);
                    }
                } else {
                    widget.setHorizontalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
                    widget.setWidth(layoutParams.width);
                }
                if (!layoutParams.verticalDimensionFixed) {
                    if (layoutParams.height == LayoutParams.MATCH_PARENT) {
                        widget.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_PARENT);
                        widget.getAnchor(ConstraintAnchor.Type.TOP).mMargin = layoutParams.topMargin;
                        widget.getAnchor(ConstraintAnchor.Type.BOTTOM).mMargin = layoutParams.bottomMargin;
                    } else {
                        widget.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
                        widget.setHeight(0);
                    }
                } else {
                    widget.setVerticalDimensionBehaviour(ConstraintWidget.DimensionBehaviour.FIXED);
                    widget.setHeight(layoutParams.height);
                }

                if (layoutParams.dimensionRatio != null) {
                    widget.setDimensionRatio(layoutParams.dimensionRatio);
                }
                widget.setHorizontalWeight(layoutParams.horizontalWeight);
                widget.setVerticalWeight(layoutParams.verticalWeight);
                widget.setHorizontalChainStyle(layoutParams.horizontalChainStyle);
                widget.setVerticalChainStyle(layoutParams.verticalChainStyle);
                widget.setHorizontalMatchStyle(layoutParams.matchConstraintDefaultWidth,
                        layoutParams.matchConstraintMinWidth, layoutParams.matchConstraintMaxWidth);
                widget.setVerticalMatchStyle(layoutParams.matchConstraintDefaultHeight,
                        layoutParams.matchConstraintMinHeight, layoutParams.matchConstraintMaxHeight);
            }
        }
    }

    private final ConstraintWidget getTargetWidget(int id) {
        if (id == LayoutParams.PARENT_ID) {
            return mLayoutWidget;
        } else {
            View view = mChildrenByIds.get(id);
            if (view == this) {
                return mLayoutWidget;
            }
            return view == null ? null : ((LayoutParams) view.getLayoutParams()).widget;
        }
    }

    private final ConstraintWidget getViewWidget(View view) {
        if (view == this) {
            return mLayoutWidget;
        }
        return view == null ? null : ((LayoutParams) view.getLayoutParams()).widget;
    }

    private void internalMeasureChildren(int parentWidthSpec, int parentHeightSpec) {
        int heightPadding = getPaddingTop() + getPaddingBottom();
        int widthPadding = getPaddingLeft() + getPaddingRight();

        final int widgetsCount = getChildCount();
        for (int i = 0; i < widgetsCount; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() == GONE) {
                continue;
            }
            LayoutParams params = (LayoutParams) child.getLayoutParams();
            ConstraintWidget widget = params.widget;
            if (params.isGuideline) {
                continue;
            }

            int width = params.width;
            int height = params.height;

            // Don't need to measure widgets that are MATCH_CONSTRAINT on both dimensions,
            // unless they are marked as MATCH_CONSTRAINT_WRAP
            boolean doMeasure =
                    (params.horizontalDimensionFixed
                    || params.verticalDimensionFixed)
                    || (!params.horizontalDimensionFixed
                            && (params.matchConstraintDefaultWidth == MATCH_CONSTRAINT_WRAP)
                                || params.width == MATCH_PARENT)
                    || (!params.verticalDimensionFixed
                            && (params.matchConstraintDefaultHeight == MATCH_CONSTRAINT_WRAP
                                || params.height == MATCH_PARENT));

            boolean didWrapMeasureWidth = false;
            boolean didWrapMeasureHeight = false;

            if (doMeasure) {
                final int childWidthMeasureSpec;
                final int childHeightMeasureSpec;

                if (width == MATCH_CONSTRAINT || width == MATCH_PARENT) {
                    childWidthMeasureSpec = getChildMeasureSpec(parentWidthSpec,
                            widthPadding, LayoutParams.WRAP_CONTENT);
                    didWrapMeasureWidth = true;
                } else {
                    childWidthMeasureSpec = getChildMeasureSpec(parentWidthSpec,
                            widthPadding, width);
                }
                if (height == MATCH_CONSTRAINT || height == MATCH_PARENT) {
                    childHeightMeasureSpec = getChildMeasureSpec(parentHeightSpec,
                            heightPadding, LayoutParams.WRAP_CONTENT);
                    didWrapMeasureHeight = true;
                } else {
                    childHeightMeasureSpec = getChildMeasureSpec(parentHeightSpec,
                            heightPadding, height);
                }
                child.measure(childWidthMeasureSpec, childHeightMeasureSpec);

                width = child.getMeasuredWidth();
                height = child.getMeasuredHeight();
            }

            widget.setWidth(width);
            widget.setHeight(height);
            if (didWrapMeasureWidth) {
                widget.setWrapWidth(width);
            }
            if (didWrapMeasureHeight) {
                widget.setWrapHeight(height);
            }

            if (params.needsBaseline) {
                int baseline = child.getBaseline();
                if (baseline != -1) {
                    widget.setBaselineDistance(baseline);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();

        mLayoutWidget.setX(paddingLeft);
        mLayoutWidget.setY(paddingTop);
        setSelfDimensionBehaviour(widthMeasureSpec, heightMeasureSpec);
        if (mDirtyHierarchy) {
            mDirtyHierarchy = false;
            updateHierarchy();
        }
        internalMeasureChildren(widthMeasureSpec, heightMeasureSpec);

        //noinspection PointlessBooleanExpression
        if (ALLOWS_EMBEDDED && mLayoutWidget.getParent() != null) {
            setVisibility(INVISIBLE);
            return;
        }

        // let's solve the linear system.
        if (getChildCount() > 0) {
            solveLinearSystem(); // first pass
        }
        int childState = 0;

        // let's update the size dependent widgets if any...
        final int sizeDependentWidgetsCount = mVariableDimensionsWidgets.size();

        int heightPadding = paddingTop + getPaddingBottom();
        int widthPadding = paddingLeft + getPaddingRight();

        if (sizeDependentWidgetsCount > 0) {
            boolean needSolverPass = false;
            boolean containerWrapWidth = mLayoutWidget.getHorizontalDimensionBehaviour()
                    == ConstraintWidget.DimensionBehaviour.WRAP_CONTENT;
            boolean containerWrapHeight = mLayoutWidget.getVerticalDimensionBehaviour()
                    == ConstraintWidget.DimensionBehaviour.WRAP_CONTENT;
            for (int i = 0; i < sizeDependentWidgetsCount; i++) {
                ConstraintWidget widget = mVariableDimensionsWidgets.get(i);
                if (widget instanceof Guideline) {
                    continue;
                }
                View child = (View) widget.getCompanionWidget();
                if (child == null) {
                    continue;
                }
                if (child.getVisibility() == View.GONE) {
                    continue;
                }

                int widthSpec = 0;
                int heightSpec = 0;

                ConstraintLayout.LayoutParams params = (LayoutParams) child.getLayoutParams();
                if (params.width == WRAP_CONTENT) {
                    widthSpec = getChildMeasureSpec(widthMeasureSpec, widthPadding, params.width);
                } else {
                    widthSpec = MeasureSpec.makeMeasureSpec(widget.getWidth(), MeasureSpec.EXACTLY);
                }
                if (params.height == WRAP_CONTENT) {
                    heightSpec = getChildMeasureSpec(heightMeasureSpec, heightPadding, params.height);
                } else {
                    heightSpec = MeasureSpec.makeMeasureSpec(widget.getHeight(), MeasureSpec.EXACTLY);
                }

                // we need to re-measure the child...
                child.measure(widthSpec, heightSpec);

                int measuredWidth = child.getMeasuredWidth();
                int measuredHeight = child.getMeasuredHeight();
                if (measuredWidth != widget.getWidth()) {
                    widget.setWidth(measuredWidth);
                    if (containerWrapWidth && widget.getRight() > mLayoutWidget.getWidth()) {
                        int w = widget.getRight()
                                + widget.getAnchor(ConstraintAnchor.Type.RIGHT).getMargin();
                        mLayoutWidget.setWidth(Math.max(mMinWidth, w));
                    }
                    needSolverPass = true;
                }
                if (measuredHeight != widget.getHeight()) {
                    widget.setHeight(measuredHeight);
                    if (containerWrapHeight && widget.getBottom() > mLayoutWidget.getHeight()) {
                        int h = widget.getBottom()
                                + widget.getAnchor(ConstraintAnchor.Type.BOTTOM).getMargin();
                        mLayoutWidget.setHeight(Math.max(mMinHeight, h));
                    }
                    needSolverPass = true;
                }
                if (params.needsBaseline) {
                    int baseline = child.getBaseline();
                    if (baseline != -1 && baseline != widget.getBaselineDistance()) {
                        widget.setBaselineDistance(baseline);
                        needSolverPass = true;
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    childState = combineMeasuredStates(childState, child.getMeasuredState());
                }
            }
            if (needSolverPass) {
                solveLinearSystem();
            }
        }

        int androidLayoutWidth = mLayoutWidget.getWidth() + widthPadding;
        int androidLayoutHeight = mLayoutWidget.getHeight() + heightPadding;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            int resolvedWidthSize = resolveSizeAndState(androidLayoutWidth, widthMeasureSpec, childState);
            int resolvedHeightSize = resolveSizeAndState(androidLayoutHeight, heightMeasureSpec,
                    childState << MEASURED_HEIGHT_STATE_SHIFT);
            resolvedWidthSize = Math.min(mMaxWidth, resolvedWidthSize);
            resolvedHeightSize = Math.min(mMaxHeight, resolvedHeightSize);
            resolvedWidthSize &= MEASURED_SIZE_MASK;
            resolvedHeightSize &= MEASURED_SIZE_MASK;
            if (mLayoutWidget.isWidthMeasuredTooSmall()) {
                resolvedWidthSize |= MEASURED_STATE_TOO_SMALL;
            }
            if (mLayoutWidget.isHeightMeasuredTooSmall()) {
                resolvedHeightSize |= MEASURED_STATE_TOO_SMALL;
            }
            setMeasuredDimension(resolvedWidthSize, resolvedHeightSize);
        } else {
            setMeasuredDimension(androidLayoutWidth, androidLayoutHeight);
        }
    }

    private void setSelfDimensionBehaviour(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int heightPadding = getPaddingTop() + getPaddingBottom();
        int widthPadding = getPaddingLeft() + getPaddingRight();

        ConstraintWidget.DimensionBehaviour widthBehaviour = ConstraintWidget.DimensionBehaviour.FIXED;
        ConstraintWidget.DimensionBehaviour heightBehaviour = ConstraintWidget.DimensionBehaviour.FIXED;
        int desiredWidth = 0;
        int desiredHeight = 0;

        ViewGroup.LayoutParams params = getLayoutParams();
        switch (widthMode) {
            case MeasureSpec.AT_MOST: {
                widthBehaviour = ConstraintWidget.DimensionBehaviour.WRAP_CONTENT;
                desiredWidth = widthSize;
            }
            break;
            case MeasureSpec.UNSPECIFIED: {
                widthBehaviour = ConstraintWidget.DimensionBehaviour.WRAP_CONTENT;
            }
            break;
            case MeasureSpec.EXACTLY: {
                desiredWidth = Math.min(mMaxWidth, widthSize) - widthPadding;
            }
        }
        switch (heightMode) {
            case MeasureSpec.AT_MOST: {
                heightBehaviour = ConstraintWidget.DimensionBehaviour.WRAP_CONTENT;
                desiredHeight = heightSize;
            }
            break;
            case MeasureSpec.UNSPECIFIED: {
                heightBehaviour = ConstraintWidget.DimensionBehaviour.WRAP_CONTENT;
            }
            break;
            case MeasureSpec.EXACTLY: {
                desiredHeight = Math.min(mMaxHeight, heightSize) - heightPadding;
            }
        }

        mLayoutWidget.setMinWidth(0);
        mLayoutWidget.setMinHeight(0);
        mLayoutWidget.setHorizontalDimensionBehaviour(widthBehaviour);
        mLayoutWidget.setWidth(desiredWidth);
        mLayoutWidget.setVerticalDimensionBehaviour(heightBehaviour);
        mLayoutWidget.setHeight(desiredHeight);
        mLayoutWidget.setMinWidth(mMinWidth - getPaddingLeft() - getPaddingRight());
        mLayoutWidget.setMinHeight(mMinHeight - getPaddingTop() - getPaddingBottom());
    }

    /**
     * @hide
     *
     * Solve the linear system
     */
    protected void solveLinearSystem() {
        if (SIMPLE_LAYOUT) {
            mLayoutWidget.layout();
        } else {
            int groups = mLayoutWidget.layoutFindGroupsSimple();
            mLayoutWidget.layoutWithGroup(groups);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        final int widgetsCount = getChildCount();
        final boolean isInEditMode = isInEditMode();
        for (int i = 0; i < widgetsCount; i++) {
            final View child = getChildAt(i);
            LayoutParams params = (LayoutParams) child.getLayoutParams();
            if (child.getVisibility() == GONE && !params.isGuideline && !isInEditMode) {
                // If we are in edit mode, let's layout the widget so that they are at "the right place"
                // visually in the editor (as we get our positions from layoutlib)
                continue;
            }

            ConstraintWidget widget = params.widget;

            int l = widget.getDrawX();
            int t = widget.getDrawY();
            int r = l + widget.getWidth();
            int b = t + widget.getHeight();

            if (ALLOWS_EMBEDDED) {
                if (getParent() instanceof ConstraintLayout) {
                    int dx = 0;
                    int dy = 0;
                    ConstraintWidget item = mLayoutWidget; // start with ourselves
                    while (item != null) {
                        dx += item.getDrawX();
                        dy += item.getDrawY();
                        item = item.getParent();
                    }
                    l -= dx;
                    t -= dy;
                    r -= dx;
                    b -= dy;
                }
            }
            child.layout(l, t, r, b);
        }
    }

    /**
     * @hide
     *
     * Set the optimization level for the layout resolution
     * The level can be one of:
     * <ul>
     * <li>ConstraintWidgetContainer.OPTIMIZATION_NONE</li>
     * <li>ConstraintWidgetContainer.OPTIMIZATION_ALL</li>
     * <li>ConstraintWidgetContainer.OPTIMIZATION_BASIC</li>
     * <li>ConstraintWidgetContainer.OPTIMIZATION_CHAIN  </li>
     * </ul>
     * @param level optimization level
     */
    public void setOptimizationLevel(int level) {
        mLayoutWidget.setOptimizationLevel(level);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new LayoutParams(p);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }

    /**
     * Sets a ConstraintSet object to manage constraints. The ConstraintSet overrides LayoutParams of child views.
     * @param set Layout children using ConstraintSet
     */
    public void setConstraintSet(ConstraintSet set) {
        mConstraintSet = set;
    }

    /**
     *  This class contains the different attributes specifying how a view want to be laid out inside
     *  a {@link ConstraintLayout}. For building up constraints at run time, using {@link ConstraintSet} is recommended.
     */
    public static class LayoutParams extends ViewGroup.MarginLayoutParams {
        /**
         * Dimension will be controlled by constraints.
         */
        public static final int MATCH_CONSTRAINT = 0;

        /**
         * References the id of the parent.
         */
        public static final int PARENT_ID = 0;

        /**
         * Defines an id that is not set.
         */
        public static final int UNSET = -1;

        /**
         * The horizontal orientation.
         */
        public static final int HORIZONTAL = ConstraintWidget.HORIZONTAL;

        /**
         * The vertical orientation.
         */
        public static final int VERTICAL = ConstraintWidget.VERTICAL;

        /**
         * The left side of a view.
         */
        public static final int LEFT = 1;

        /**
         * The right side of a view.
         */
        public static final int RIGHT = 2;

        /**
         * The top of a view.
         */
        public static final int TOP = 3;

        /**
         * The bottom side of a view.
         */
        public static final int BOTTOM = 4;

        /**
         * The baseline of the text in a view.
         */
        public static final int BASELINE = 5;

        /**
         * The left side of a view in left to right languages.
         * In right to left languages it corresponds to the right side of the view
         */
        public static final int START = 6;

        /**
         * The right side of a view in right to left languages.
         * In right to left languages it corresponds to the left side of the view
         */
        public static final int END =  7;

        /**
         * Set matchConstraintDefault* default to the wrap content size.
         * Use to set the matchConstraintDefaultWidth and matchConstraintDefaultHeight
         */
        public static final int MATCH_CONSTRAINT_WRAP = ConstraintWidget.MATCH_CONSTRAINT_WRAP;

        /**
         * Set matchConstraintDefault* spread as much as possible within its constraints.
         * Use to set the matchConstraintDefaultWidth and matchConstraintDefaultHeight
         */
        public static final int MATCH_CONSTRAINT_SPREAD = ConstraintWidget.MATCH_CONSTRAINT_SPREAD;

        /**
         * Chain spread style
         */
        public static final int CHAIN_SPREAD = ConstraintWidget.CHAIN_SPREAD;

        /**
         * Chain spread inside style
         */
        public static final int CHAIN_SPREAD_INSIDE = ConstraintWidget.CHAIN_SPREAD_INSIDE;

        /**
         * Chain packed style
         */
        public static final int CHAIN_PACKED = ConstraintWidget.CHAIN_PACKED;

        /**
         * The distance of child (guideline) to the top or left edge of its parent.
         */
        public int guideBegin = UNSET;

        /**
         * The distance of child (guideline) to the top or left edge of its parent.
         */
        public int guideEnd = UNSET;

        /**
         * The ratio of the distance to the parent's sides
         */
        public float guidePercent = UNSET;

        /**
         * Constrains the left side of a child to the left side of a target child (contains the target child id).
         */
        public int leftToLeft = UNSET;

        /**
         * Constrains the left side of a child to the right side of a target child (contains the target child id).
         */
       public int leftToRight = UNSET;

        /**
         * Constrains the right side of a child to the left side of a target child (contains the target child id).
         */
        public int rightToLeft = UNSET;

        /**
         * Constrains the right side of a child to the right side of a target child (contains the target child id).
         */
        public int rightToRight = UNSET;

        /**
         * Constrains the top side of a child to the top side of a target child (contains the target child id).
         */
        public int topToTop = UNSET;

        /**
         * Constrains the top side of a child to the bottom side of a target child (contains the target child id).
         */
        public int topToBottom = UNSET;

        /**
         * Constrains the bottom side of a child to the top side of a target child (contains the target child id).
         */
        public int bottomToTop = UNSET;

        /**
         * Constrains the bottom side of a child to the bottom side of a target child (contains the target child id).
         */
        public int bottomToBottom = UNSET;

        /**
         * Constrains the baseline of a child to the baseline of a target child (contains the target child id).
         */
        public int baselineToBaseline = UNSET;

        /**
         * Constrains the start side of a child to the end side of a target child (contains the target child id).
         */
        public int startToEnd = UNSET;

        /**
         * Constrains the start side of a child to the start side of a target child (contains the target child id).
         */
        public int startToStart = UNSET;

        /**
         * Constrains the end side of a child to the start side of a target child (contains the target child id).
         */
        public int endToStart = UNSET;

        /**
         * Constrains the end side of a child to the end side of a target child (contains the target child id).
         */
        public int endToEnd = UNSET;

        /**
         * The left margin to use when the target is gone.
         */
        public int goneLeftMargin = UNSET;

        /**
         * The top margin to use when the target is gone.
         */
        public int goneTopMargin = UNSET;

        /**
         * The right margin to use when the target is gone
         */
        public int goneRightMargin = UNSET;

        /**
         * The bottom margin to use when the target is gone.
         */
        public int goneBottomMargin = UNSET;

        /**
         * The start margin to use when the target is gone.
         */
        public int goneStartMargin = UNSET;

        /**
         * The end margin to use when the target is gone.
         */
        public int goneEndMargin = UNSET;

        /**
         * The ratio between two connections when the left and right (or start and end) sides are constrained.
         */
        public float horizontalBias = 0.5f;

        /**
         * The ratio between two connections when the top and bottom sides are constrained.
         */
        public float verticalBias = 0.5f;

        /**
         * The ratio information.
         */
        public String dimensionRatio = null;

        /**
         * The ratio between the width and height of the child.
         */
        float dimensionRatioValue = 0;

        /**
         * The child's side to constrain using dimensRatio.
         */
        int dimensionRatioSide = VERTICAL;

        /**
         * The child's weight that we can use to distribute the available horizontal space
         * in a chain, if the dimension behaviour is set to MATCH_CONSTRAINT
         */
        public float horizontalWeight = 0;

        /**
         * The child's weight that we can use to distribute the available vertical space
         * in a chain, if the dimension behaviour is set to MATCH_CONSTRAINT
         */
        public float verticalWeight = 0;

        /**
         * If the child is the start of a horizontal chain, this attribute will drive how
         * the elements of the chain will be positioned. The possible values are:
         * <ul>
         *     <li>{@see CHAIN_SPREAD} -- the elements will be spread out</li>
         *     <li>{@see CHAIN_SPREAD_INSIDE} -- similar, but the endpoints of the chain will not be spread out</li>
         *     <li>{@see CHAIN_PACKED} -- the elements of the chain will be packed together. The horizontal
         *          bias attribute of the child will then affect the positioning of the packed elements</li>
         * </ul>
         */
        public int horizontalChainStyle = CHAIN_SPREAD;

        /**
         * If the child is the start of a vertical chain, this attribute will drive how
         * the elements of the chain will be positioned. The possible values are:
         * <ul>
         *     <li>{@see CHAIN_SPREAD} -- the elements will be spread out</li>
         *     <li>{@see CHAIN_SPREAD_INSIDE} -- similar, but the endpoints of the chain will not be spread out</li>
         *     <li>{@see CHAIN_PACKED} -- the elements of the chain will be packed together. The vertical
         *          bias attribute of the child will then affect the positioning of the packed elements</li>
         * </ul>
         */
        public int verticalChainStyle = CHAIN_SPREAD;

        /**
         * Define how the widget horizontal dimension is handled when set to MATCH_CONSTRAINT
         * <ul>
         *     <li>{@see MATCH_CONSTRAINT_SPREAD} -- the default. The dimension will expand up to the constraints, minus margins</li>
         *     <li>{@see MATCH_CONSTRAINT_WRAP} -- The dimension will be the same as WRAP_CONTENT, unless the size ends
         *     up too large for the constraints; in that case the dimension will expand up to the constraints, minus margins</li>
         *     This attribute may not be applied if the widget is part of a chain in that dimension.
         * </ul>
         */
        public int matchConstraintDefaultWidth = MATCH_CONSTRAINT_SPREAD;

        /**
         * Define how the widget vertical dimension is handled when set to MATCH_CONSTRAINT
         * <ul>
         *     <li>{@see MATCH_CONSTRAINT_SPREAD} -- the default. The dimension will expand up to the constraints, minus margins</li>
         *     <li>{@see MATCH_CONSTRAINT_WRAP} -- The dimension will be the same as WRAP_CONTENT, unless the size ends
         *     up too large for the constraints; in that case the dimension will expand up to the constraints, minus margins</li>
         *     This attribute may not be applied if the widget is part of a chain in that dimension.
         * </ul>
         */
        public int matchConstraintDefaultHeight = MATCH_CONSTRAINT_SPREAD;

        /**
         * Specify a minimum width size for the widget. It will only apply if the size of the widget
         * is set to MATCH_CONSTRAINT. Don't apply if the widget is part of an horizontal chain.
         */
        public int matchConstraintMinWidth = 0;

        /**
         * Specify a minimum height size for the widget. It will only apply if the size of the widget
         * is set to MATCH_CONSTRAINT. Don't apply if the widget is part of an vertical chain.
         */
        public int matchConstraintMinHeight = 0;

        /**
         * Specify a maximum width size for the widget. It will only apply if the size of the widget
         * is set to MATCH_CONSTRAINT. Don't apply if the widget is part of an horizontal chain.
         */
        public int matchConstraintMaxWidth = 0;

        /**
         * Specify a maximum height size for the widget. It will only apply if the size of the widget
         * is set to MATCH_CONSTRAINT. Don't apply if the widget is part of an vertical chain.
         */
        public int matchConstraintMaxHeight = 0;

        /**
         * The design time location of the left side of the child.
         * Used at design time for a horizontally unconstrained child.
         */
        public int editorAbsoluteX = UNSET;

        /**
         * The design time location of the right side of the child.
         *  Used at design time for a vertically unconstrained child.
         */
        public int editorAbsoluteY = UNSET;

        public int orientation = UNSET;

        // Internal use only
        boolean horizontalDimensionFixed = true;
        boolean verticalDimensionFixed = true;

        boolean needsBaseline = false;
        boolean isGuideline = false;

        int resolvedLeftToLeft = UNSET;
        int resolvedLeftToRight = UNSET;
        int resolvedRightToLeft = UNSET;
        int resolvedRightToRight = UNSET;
        int resolveGoneLeftMargin = UNSET;
        int resolveGoneRightMargin = UNSET;
        float resolvedHorizontalBias = 0.5f;

        ConstraintWidget widget = new ConstraintWidget();

        /**
         * Create a LayoutParams base on an existing layout Params
         *
         * @param source the Layout Params to be copied
         */
        public LayoutParams(LayoutParams source) {
            super(source);
            this.guideBegin = source.guideBegin;
            this.guideEnd = source.guideEnd;
            this.guidePercent = source.guidePercent;
            this.leftToLeft = source.leftToLeft;
            this.leftToRight = source.leftToRight;
            this.rightToLeft = source.rightToLeft;
            this.rightToRight = source.rightToRight;
            this.topToTop = source.topToTop;
            this.topToBottom = source.topToBottom;
            this.bottomToTop = source.bottomToTop;
            this.bottomToBottom = source.bottomToBottom;
            this.baselineToBaseline = source.baselineToBaseline;
            this.startToEnd = source.startToEnd;
            this.startToStart = source.startToStart;
            this.endToStart = source.endToStart;
            this.endToEnd = source.endToEnd;
            this.goneLeftMargin = source.goneLeftMargin;
            this.goneTopMargin = source.goneTopMargin;
            this.goneRightMargin = source.goneRightMargin;
            this.goneBottomMargin = source.goneBottomMargin;
            this.goneStartMargin = source.goneStartMargin;
            this.goneEndMargin = source.goneEndMargin;
            this.horizontalBias = source.horizontalBias;
            this.verticalBias = source.verticalBias;
            this.dimensionRatio = source.dimensionRatio;
            this.dimensionRatioValue = source.dimensionRatioValue;
            this.dimensionRatioSide = source.dimensionRatioSide;
            this.horizontalWeight = source.horizontalWeight;
            this.verticalWeight = source.verticalWeight;
            this.horizontalChainStyle = source.horizontalChainStyle;
            this.verticalChainStyle = source.verticalChainStyle;
            this.matchConstraintDefaultWidth = source.matchConstraintDefaultWidth;
            this.matchConstraintDefaultHeight = source.matchConstraintDefaultHeight;
            this.matchConstraintMinWidth = source.matchConstraintMinWidth;
            this.matchConstraintMaxWidth = source.matchConstraintMaxWidth;
            this.matchConstraintMinHeight = source.matchConstraintMinHeight;
            this.matchConstraintMaxHeight = source.matchConstraintMaxHeight;
            this.editorAbsoluteX = source.editorAbsoluteX;
            this.editorAbsoluteY = source.editorAbsoluteY;
            this.orientation = source.orientation;
            this.horizontalDimensionFixed = source.horizontalDimensionFixed;
            this.verticalDimensionFixed = source.verticalDimensionFixed;
            this.needsBaseline = source.needsBaseline;
            this.isGuideline = source.isGuideline;
            this.resolvedLeftToLeft = source.resolvedLeftToLeft;
            this.resolvedLeftToRight = source.resolvedLeftToRight;
            this.resolvedRightToLeft = source.resolvedRightToLeft;
            this.resolvedRightToRight = source.resolvedRightToRight;
            this.resolveGoneLeftMargin = source.resolveGoneLeftMargin;
            this.resolveGoneRightMargin = source.resolveGoneRightMargin;
            this.resolvedHorizontalBias = source.resolvedHorizontalBias;
            this.widget = source.widget;
        }

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);

            TypedArray a = c.obtainStyledAttributes(attrs, R.styleable.ConstraintLayout_Layout);
            final int N = a.getIndexCount();
            for (int i = 0; i < N; i++) {
                int attr = a.getIndex(i);
                if (attr == R.styleable.ConstraintLayout_Layout_layout_constraintLeft_toLeftOf) {
                    leftToLeft = a.getResourceId(attr, leftToLeft);
                    if (leftToLeft == UNSET) {
                        leftToLeft = a.getInt(attr, UNSET);
                    }
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_constraintLeft_toRightOf) {
                    leftToRight = a.getResourceId(attr, leftToRight);
                    if (leftToRight == UNSET) {
                        leftToRight = a.getInt(attr, UNSET);
                    }
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_constraintRight_toLeftOf) {
                    rightToLeft = a.getResourceId(attr, rightToLeft);
                    if (rightToLeft == UNSET) {
                        rightToLeft = a.getInt(attr, UNSET);
                    }
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_constraintRight_toRightOf) {
                    rightToRight = a.getResourceId(attr, rightToRight);
                    if (rightToRight == UNSET) {
                        rightToRight = a.getInt(attr, UNSET);
                    }
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_constraintTop_toTopOf) {
                    topToTop = a.getResourceId(attr, topToTop);
                    if (topToTop == UNSET) {
                        topToTop = a.getInt(attr, UNSET);
                    }
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_constraintTop_toBottomOf) {
                    topToBottom = a.getResourceId(attr, topToBottom);
                    if (topToBottom == UNSET) {
                        topToBottom = a.getInt(attr, UNSET);
                    }
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_constraintBottom_toTopOf) {
                    bottomToTop = a.getResourceId(attr, bottomToTop);
                    if (bottomToTop == UNSET) {
                        bottomToTop = a.getInt(attr, UNSET);
                    }
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_constraintBottom_toBottomOf) {
                    bottomToBottom = a.getResourceId(attr, bottomToBottom);
                    if (bottomToBottom == UNSET) {
                        bottomToBottom = a.getInt(attr, UNSET);
                    }
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_constraintBaseline_toBaselineOf) {
                    baselineToBaseline = a.getResourceId(attr, baselineToBaseline);
                    if (baselineToBaseline == UNSET) {
                        baselineToBaseline = a.getInt(attr, UNSET);
                    }
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_editor_absoluteX) {
                    editorAbsoluteX = a.getDimensionPixelOffset(attr, editorAbsoluteX);
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_editor_absoluteY) {
                    editorAbsoluteY = a.getDimensionPixelOffset(attr, editorAbsoluteY);
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_constraintGuide_begin) {
                    guideBegin = a.getDimensionPixelOffset(attr, guideBegin);
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_constraintGuide_end) {
                    guideEnd = a.getDimensionPixelOffset(attr, guideEnd);
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_constraintGuide_percent) {
                    guidePercent = a.getFloat(attr, guidePercent);
                } else if (attr == R.styleable.ConstraintLayout_Layout_android_orientation) {
                    orientation = a.getInt(attr, orientation);
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_constraintStart_toEndOf) {
                    startToEnd = a.getResourceId(attr, startToEnd);
                    if (startToEnd == UNSET) {
                        startToEnd = a.getInt(attr, UNSET);
                    }
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_constraintStart_toStartOf) {
                    startToStart = a.getResourceId(attr, startToStart);
                    if (startToStart == UNSET) {
                        startToStart = a.getInt(attr, UNSET);
                    }
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_constraintEnd_toStartOf) {
                    endToStart = a.getResourceId(attr, endToStart);
                    if (endToStart == UNSET) {
                        endToStart = a.getInt(attr, UNSET);
                    }
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_constraintEnd_toEndOf) {
                    endToEnd = a.getResourceId(attr, endToEnd);
                    if (endToEnd == UNSET) {
                        endToEnd = a.getInt(attr, UNSET);
                    }
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_goneMarginLeft) {
                    goneLeftMargin = a.getDimensionPixelSize(attr, goneLeftMargin);
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_goneMarginTop) {
                    goneTopMargin = a.getDimensionPixelSize(attr, goneTopMargin);
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_goneMarginRight) {
                    goneRightMargin = a.getDimensionPixelSize(attr, goneRightMargin);
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_goneMarginBottom) {
                    goneBottomMargin = a.getDimensionPixelSize(attr, goneBottomMargin);
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_goneMarginStart) {
                    goneStartMargin = a.getDimensionPixelSize(attr, goneStartMargin);
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_goneMarginEnd) {
                    goneEndMargin = a.getDimensionPixelSize(attr, goneEndMargin);
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_constraintHorizontal_bias) {
                    horizontalBias = a.getFloat(attr, horizontalBias);
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_constraintVertical_bias) {
                    verticalBias = a.getFloat(attr, verticalBias);
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_constraintDimensionRatio) {
                    dimensionRatio = a.getString(attr);
                    dimensionRatioValue = Float.NaN;
                    dimensionRatioSide = UNSET;
                    if (dimensionRatio != null) {
                        int len = dimensionRatio.length();
                        int commaIndex = dimensionRatio.indexOf(',');
                        if (commaIndex > 0 && commaIndex < len - 1) {
                            String dimension = dimensionRatio.substring(0, commaIndex);
                            if (dimension.equalsIgnoreCase("W")) {
                                dimensionRatioSide = HORIZONTAL;
                            } else if (dimension.equalsIgnoreCase("H")) {
                                dimensionRatioSide = VERTICAL;
                            }
                            commaIndex++;
                        } else {
                            commaIndex = 0;
                        }
                        int colonIndex = dimensionRatio.indexOf(':');
                        if (colonIndex >= 0 && colonIndex < len - 1) {
                            String nominator = dimensionRatio.substring(commaIndex, colonIndex);
                            String denominator = dimensionRatio.substring(colonIndex + 1);
                            if (nominator.length() > 0 && denominator.length() > 0) {
                                try {
                                    float nominatorValue = Float.parseFloat(nominator);
                                    float denominatorValue = Float.parseFloat(denominator);
                                    if (nominatorValue > 0 && denominatorValue > 0) {
                                        if (dimensionRatioSide == VERTICAL) {
                                            dimensionRatioValue = Math.abs(denominatorValue / nominatorValue);
                                        } else {
                                            dimensionRatioValue = Math.abs(nominatorValue / denominatorValue);
                                        }
                                    }
                                } catch (NumberFormatException e) {
                                    // Ignore
                                }
                            }
                        } else {
                            String r = dimensionRatio.substring(commaIndex);
                            if (r.length() > 0) {
                                try {
                                    dimensionRatioValue = Float.parseFloat(r);
                                } catch (NumberFormatException e) {
                                    // Ignore
                                }
                            }
                        }
                    }
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_constraintHorizontal_weight) {
                    horizontalWeight = a.getFloat(attr, 0);
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_constraintVertical_weight) {
                    verticalWeight = a.getFloat(attr, 0);
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_constraintHorizontal_chainStyle) {
                    horizontalChainStyle = a.getInt(attr, CHAIN_SPREAD);
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_constraintVertical_chainStyle) {
                    verticalChainStyle = a.getInt(attr, CHAIN_SPREAD);
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_constraintWidth_default) {
                    matchConstraintDefaultWidth = a.getInt(attr, MATCH_CONSTRAINT_SPREAD);
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_constraintHeight_default) {
                    matchConstraintDefaultHeight = a.getInt(attr, MATCH_CONSTRAINT_SPREAD);
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_constraintWidth_min) {
                    matchConstraintMinWidth = a.getDimensionPixelSize(attr, matchConstraintMinWidth);
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_constraintWidth_max) {
                    matchConstraintMaxWidth = a.getDimensionPixelSize(attr, matchConstraintMaxWidth);
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_constraintHeight_min) {
                    matchConstraintMinHeight = a.getDimensionPixelSize(attr, matchConstraintMinHeight);
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_constraintHeight_max) {
                    matchConstraintMaxHeight = a.getDimensionPixelSize(attr, matchConstraintMaxHeight);
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_constraintLeft_creator) {
                    // Skip
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_constraintTop_creator) {
                    // Skip
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_constraintRight_creator) {
                    // Skip
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_constraintBottom_creator) {
                    // Skip
                } else if (attr == R.styleable.ConstraintLayout_Layout_layout_constraintBaseline_creator) {
                    // Skip
                } else {
                    // Skip
                }
            }
            a.recycle();
            validate();
        }

        public void validate() {
            isGuideline = false;
            horizontalDimensionFixed = true;
            verticalDimensionFixed = true;
            if (width == MATCH_CONSTRAINT || width == MATCH_PARENT) {
                horizontalDimensionFixed = false;
            }
            if (height == MATCH_CONSTRAINT || height == MATCH_PARENT) {
                verticalDimensionFixed = false;
            }
            if (guidePercent != UNSET || guideBegin != UNSET || guideEnd != UNSET) {
                isGuideline = true;
                horizontalDimensionFixed = true;
                verticalDimensionFixed = true;
                if (!(widget instanceof Guideline)) {
                    widget = new Guideline();
                }
                ((Guideline) widget).setOrientation(orientation);
            }
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
        public void resolveLayoutDirection(int layoutDirection) {
            super.resolveLayoutDirection(layoutDirection);

            resolvedRightToLeft = UNSET;
            resolvedRightToRight = UNSET;
            resolvedLeftToLeft = UNSET;
            resolvedLeftToRight = UNSET;

            resolveGoneLeftMargin = UNSET;
            resolveGoneRightMargin = UNSET;
            resolveGoneLeftMargin = goneLeftMargin;
            resolveGoneRightMargin = goneRightMargin;
            resolvedHorizontalBias = horizontalBias;

            boolean isRtl = (View.LAYOUT_DIRECTION_RTL == getLayoutDirection());
            // Post JB MR1, if start/end are defined, they take precedence over left/right
            if (isRtl) {
                if (startToEnd != UNSET) {
                    resolvedRightToLeft = startToEnd;
                } else if (startToStart != UNSET) {
                    resolvedRightToRight = startToStart;
                }
                if (endToStart != UNSET) {
                    resolvedLeftToRight = endToStart;
                }
                if (endToEnd != UNSET) {
                    resolvedLeftToLeft = endToEnd;
                }
                if (goneStartMargin != UNSET) {
                    resolveGoneRightMargin = goneStartMargin;
                }
                if (goneEndMargin != UNSET) {
                    resolveGoneLeftMargin = goneEndMargin;
                }
                resolvedHorizontalBias = 1 - horizontalBias;
            } else {
                if (startToEnd != UNSET) {
                    resolvedLeftToRight = startToEnd;
                }
                if (startToStart != UNSET) {
                    resolvedLeftToLeft = startToStart;
                }
                if (endToStart != UNSET) {
                    resolvedRightToLeft = endToStart;
                }
                if (endToEnd != UNSET) {
                    resolvedRightToRight = endToEnd;
                }
                if (goneStartMargin != UNSET) {
                    resolveGoneLeftMargin = goneStartMargin;
                }
                if (goneEndMargin != UNSET) {
                    resolveGoneRightMargin = goneEndMargin;
                }
            }
            // if no constraint is defined via RTL attributes, use left/right if present
            if (endToStart == UNSET && endToEnd == UNSET) {
                if (rightToLeft != UNSET) {
                    resolvedRightToLeft = rightToLeft;
                } else if (rightToRight != UNSET) {
                    resolvedRightToRight = rightToRight;
                }
            }
            if (startToStart == UNSET && startToEnd == UNSET) {
                if (leftToLeft != UNSET) {
                    resolvedLeftToLeft = leftToLeft;
                } else if (leftToRight != UNSET) {
                    resolvedLeftToRight = leftToRight;
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void requestLayout() {
        super.requestLayout();
        mDirtyHierarchy = true;
    }
}
