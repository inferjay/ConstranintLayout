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
    protected float mTotalWeight = 0f;
    private int mOrientation;
    private boolean mIsRtl = false;

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
        defineChainProperties();
    }

    private void defineChainProperties(){
        int offset = mOrientation * 2;

        // TraverseChain
        ConstraintWidget widget = mFirst;
        ConstraintWidget next = mFirst;
        boolean done = false;
        while (!done) {
            widget.mListNextVisibleWidget[mOrientation] = null;
            widget.mListNextMatchConstraintsWidget[mOrientation] = null;
            if(widget.getVisibility() != ConstraintWidget.GONE) {
                // Visible widgets linked list.
                if (mFirstVisibleWidget == null) {
                    mFirstVisibleWidget = widget;
                }
                if(mLastVisibleWidget != null){
                    mLastVisibleWidget.mListNextVisibleWidget[mOrientation] = widget;
                }
                mLastVisibleWidget = widget;

                // Match constraint linked list.
                if(widget.mListDimensionBehaviors[mOrientation] == DimensionBehaviour.MATCH_CONSTRAINT){
                    mTotalWeight += widget.mWeight[mOrientation];
                    if(mFirstMatchConstraintWidget == null){
                        mFirstMatchConstraintWidget = widget;
                    }
                    if(mLastMatchConstraintWidget != null){
                        mLastMatchConstraintWidget.mListNextMatchConstraintsWidget[mOrientation] = widget;
                    }
                    mLastMatchConstraintWidget = widget;
                }
            }

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

        if(mOrientation == ConstraintWidget.HORIZONTAL && mIsRtl) {
            mHead = mLast;
        }else{
            mHead = mFirst;
        }
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
}
