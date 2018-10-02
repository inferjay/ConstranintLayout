/*
 * Copyright (C) 2018 The Android Open Source Project
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

import android.support.constraint.solver.widgets.ConstraintWidget.DimensionBehaviour;

import java.util.ArrayList;

import static android.support.constraint.solver.widgets.ConstraintWidget.MATCH_CONSTRAINT_PERCENT;
import static android.support.constraint.solver.widgets.ConstraintWidget.MATCH_CONSTRAINT_RATIO;
import static android.support.constraint.solver.widgets.ConstraintWidget.MATCH_CONSTRAINT_SPREAD;

/**
 * Class to represent a chain by its main elements.
 */
public class ChainHead {

    protected ConstraintWidget mFirst;
    protected ConstraintWidget mFirstVisibleWidget;
    protected ConstraintWidget mLast;
    protected ConstraintWidget mLastVisibleWidget;
    protected ConstraintWidget mHead;
    protected ConstraintWidget mFirstMatchConstraintWidget;
    protected ConstraintWidget mLastMatchConstraintWidget;
    protected ArrayList<ConstraintWidget> mWeightedMatchConstraintsWidgets;
    protected int mWidgetsCount;
    protected int mWidgetsMatchCount;
    protected float mTotalWeight = 0f;
    private int mOrientation;
    private boolean mIsRtl = false;
    protected boolean mHasUndefinedWeights;
    protected boolean mHasDefinedWeights;
    protected boolean mHasComplexMatchWeights;
    private boolean mDefined;

    /**
     * Initialize variables, then determine visible widgets, the head of chain and
     * matched constraint widgets.
     *
     * @param first first widget in a chain
     * @param orientation orientation of the chain (either Horizontal or Vertical)
     * @param isRtl Right-to-left layout flag to determine the actual head of the chain
     */
    public ChainHead(ConstraintWidget first, int orientation, boolean isRtl){
        mFirst = first;
        mOrientation = orientation;
        mIsRtl = isRtl;
    }

    /**
     * Returns true if the widget should be part of the match equality rules in the chain
     *
     * @param widget      the widget to test
     * @param orientation current orientation, HORIZONTAL or VERTICAL
     * @return
     */
    static private boolean isMatchConstraintEqualityCandidate(ConstraintWidget widget, int orientation) {
        return widget.getVisibility() != ConstraintWidget.GONE
                && widget.mListDimensionBehaviors[orientation] == ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT
                && (widget.mResolvedMatchConstraintDefault[orientation] == MATCH_CONSTRAINT_SPREAD
                || widget.mResolvedMatchConstraintDefault[orientation] == MATCH_CONSTRAINT_RATIO);
    }

    private void defineChainProperties(){
        int offset = mOrientation * 2;
        ConstraintWidget lastVisited = mFirst;

        // TraverseChain
        ConstraintWidget widget = mFirst;
        ConstraintWidget next = mFirst;
        boolean done = false;
        while (!done) {
            mWidgetsCount++;
            widget.mNextChainWidget[mOrientation] = null;
            widget.mListNextMatchConstraintsWidget[mOrientation] = null;
            if (widget.getVisibility() != ConstraintWidget.GONE) {
                // Visible widgets linked list.
                if (mFirstVisibleWidget == null) {
                    mFirstVisibleWidget = widget;
                }
                mLastVisibleWidget = widget;

                // Match constraint linked list.
                if (widget.mListDimensionBehaviors[mOrientation] == DimensionBehaviour.MATCH_CONSTRAINT
                    && (widget.mResolvedMatchConstraintDefault[mOrientation] == MATCH_CONSTRAINT_SPREAD
                    || widget.mResolvedMatchConstraintDefault[mOrientation] == MATCH_CONSTRAINT_RATIO
                    || widget.mResolvedMatchConstraintDefault[mOrientation] == MATCH_CONSTRAINT_PERCENT)) {
                    mWidgetsMatchCount++;
                    float weight = widget.mWeight[mOrientation];
                    if (weight > 0) {
                        mTotalWeight += widget.mWeight[mOrientation];
                    }

                    if (isMatchConstraintEqualityCandidate(widget, mOrientation)) {
                        if (weight < 0) {
                            mHasUndefinedWeights = true;
                        } else {
                            mHasDefinedWeights = true;
                        }
                        if (mWeightedMatchConstraintsWidgets == null) {
                            mWeightedMatchConstraintsWidgets = new ArrayList<>();
                        }
                        mWeightedMatchConstraintsWidgets.add(widget);
                    }

                    if (mFirstMatchConstraintWidget == null) {
                        mFirstMatchConstraintWidget = widget;
                    }
                    if (mLastMatchConstraintWidget != null) {
                        mLastMatchConstraintWidget.mListNextMatchConstraintsWidget[mOrientation] = widget;
                    }
                    mLastMatchConstraintWidget = widget;
                }
            }
            if (lastVisited != widget) {
                lastVisited.mNextChainWidget[mOrientation] = widget;
            }
            lastVisited = widget;

            // go to the next widget
            ConstraintAnchor nextAnchor = widget.mListAnchors[offset + 1].mTarget;
            if (nextAnchor != null) {
                next = nextAnchor.mOwner;
                if (next.mListAnchors[offset].mTarget == null
                    || next.mListAnchors[offset].mTarget.mOwner != widget) {
                    next = null;
                }
            } else {
                next = null;
            }
            if (next != null) {
                widget = next;
            } else {
                done = true;
            }
        }
        mLast = widget;

        if (mOrientation == ConstraintWidget.HORIZONTAL && mIsRtl) {
            mHead = mLast;
        } else {
            mHead = mFirst;
        }

        mHasComplexMatchWeights = mHasDefinedWeights && mHasUndefinedWeights;
    }

    public ConstraintWidget getFirst() {
        return mFirst;
    }

    public ConstraintWidget getFirstVisibleWidget() {
        return mFirstVisibleWidget;
    }

    public ConstraintWidget getLast() {
        return mLast;
    }

    public ConstraintWidget getLastVisibleWidget() {
        return mLastVisibleWidget;
    }

    public ConstraintWidget getHead() {
        return mHead;
    }

    public ConstraintWidget getFirstMatchConstraintWidget() {
        return mFirstMatchConstraintWidget;
    }

    public ConstraintWidget getLastMatchConstraintWidget() {
        return mLastMatchConstraintWidget;
    }

    public float getTotalWeight() {
        return mTotalWeight;
    }

    public void define() {
        if (!mDefined) {
            defineChainProperties();
        }
        mDefined = true;
    }
}
