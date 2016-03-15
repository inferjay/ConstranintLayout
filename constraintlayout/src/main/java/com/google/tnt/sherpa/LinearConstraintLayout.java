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

package com.google.tnt.sherpa;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;

import com.google.tnt.solver.widgets.ConstraintAnchor;
import com.google.tnt.solver.widgets.ConstraintWidget;

public class LinearConstraintLayout extends ConstraintLayout {
    boolean mIsHorizontal = true;

    public LinearConstraintLayout(Context context) {
        super(context);
    }

    public LinearConstraintLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        setAttributes(context, attrs);
    }

    private void setAttributes(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ConstraintLayout_LayoutParams);
        int orientation = -1;
        orientation = a.getInt(R.styleable.ConstraintLayout_LayoutParams_orientation, orientation);
        if (orientation == 1) {
            mIsHorizontal = false;
        }
    }

    public LinearConstraintLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setAttributes(context, attrs);
    }

    @Override
    protected void setChildrenConstraints() {
        final int count = getChildCount();
        ConstraintWidget previous = mLayoutWidget;
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() == GONE) {
                continue;
            }
            ConstraintWidget widget = mConstrainedViews.get(child);
            widget.setParent(mLayoutWidget);
            if (mIsHorizontal) {
                widget.connect(ConstraintAnchor.Type.TOP, mLayoutWidget, ConstraintAnchor.Type.TOP);
                widget.connect(ConstraintAnchor.Type.BOTTOM, mLayoutWidget, ConstraintAnchor.Type.BOTTOM);
                if (previous == mLayoutWidget) {
                    widget.connect(ConstraintAnchor.Type.LEFT, previous, ConstraintAnchor.Type.LEFT);
                } else {
                    widget.connect(ConstraintAnchor.Type.LEFT, previous, ConstraintAnchor.Type.RIGHT);
                    previous.connect(ConstraintAnchor.Type.RIGHT, widget, ConstraintAnchor.Type.LEFT);
                }
            } else {
                widget.connect(ConstraintAnchor.Type.LEFT, mLayoutWidget, ConstraintAnchor.Type.LEFT);
                widget.connect(ConstraintAnchor.Type.RIGHT, mLayoutWidget, ConstraintAnchor.Type.RIGHT);
                if (previous == mLayoutWidget) {
                    widget.connect(ConstraintAnchor.Type.TOP, previous, ConstraintAnchor.Type.TOP);
                } else {
                    widget.connect(ConstraintAnchor.Type.TOP, previous, ConstraintAnchor.Type.BOTTOM);
                    previous.connect(ConstraintAnchor.Type.BOTTOM, widget, ConstraintAnchor.Type.TOP);
                }
            }
            previous = widget;
        }
        if (previous != mLayoutWidget) {
            if (mIsHorizontal) {
                previous.connect(ConstraintAnchor.Type.RIGHT, mLayoutWidget, ConstraintAnchor.Type.RIGHT);
            } else {
                previous.connect(ConstraintAnchor.Type.BOTTOM, mLayoutWidget, ConstraintAnchor.Type.BOTTOM);
            }
        }
    }

}
