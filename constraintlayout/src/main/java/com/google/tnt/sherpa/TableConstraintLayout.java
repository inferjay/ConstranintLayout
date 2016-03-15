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
import android.util.AttributeSet;

import com.google.tnt.solver.widgets.*;

public class TableConstraintLayout extends ConstraintLayout {

    @Override
    protected ConstraintWidgetContainer getLayoutWidget() {
        if (mLayoutWidget == null) {
            mLayoutWidget = new ConstraintTableLayout();
        }
        return mLayoutWidget;
    }

    public TableConstraintLayout(Context context) {
        super(context);
    }

    public TableConstraintLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TableConstraintLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        final LayoutParams layoutParams = (LayoutParams) getLayoutParams();
        if (layoutParams == null) {
            return;
        }
        ConstraintTableLayout layout = (ConstraintTableLayout) getLayoutWidget();
        layout.setVerticalGrowth(layoutParams.orientation == LayoutParams.VERTICAL);
        layout.setNumCols(layoutParams.numColumns);
        layout.setNumRows(layoutParams.numRows);
        layout.setPadding(layoutParams.padding);
        String alignment = layoutParams.columnsAlignment;
        if (alignment != null) {
            for (int i = 0, n = alignment.length(); i < n; i++) {
                char c = alignment.charAt(i);
                if (c == 'L') {
                    layout.setColumnAlignment(i, 1);
                } else if (c == 'C') {
                    layout.setColumnAlignment(i, 0);
                } else if (c == 'R') {
                    layout.setColumnAlignment(i, 2);
                }
            }
        }
    }

    @Override
    protected void setChildrenConstraints() {
        // Do nothing here, it's already handled by the ConstraintTableLayout object
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (!ALLOWS_EMBEDDED || mLayoutWidget.getParent() == null) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        } else {
            setSelfDimensionBehaviour(widthMeasureSpec, heightMeasureSpec);
            int widthSize = MeasureSpec.getSize(widthMeasureSpec);
            int heightSize = MeasureSpec.getSize(heightMeasureSpec);
            internalMeasureChildren(widthMeasureSpec, heightMeasureSpec);
            setMeasuredDimension(widthSize, heightSize);
        }
    }
}
