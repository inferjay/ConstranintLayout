/*
 * Copyright (C) 2017 The Android Open Source Project
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
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.os.Build;
import android.support.constraint.solver.widgets.ConstraintWidget;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import java.lang.reflect.Field;
import java.util.Arrays;

/**
 * A Barrier is a Line on a Constraint Layout that takes multiple widgets and aligns
 * to the most extreme widget on the specified side.
 * e.g. A left barrier will align to the left of all the contained views.
 */
public class Barrier extends ConstraintHelper {

    public static final int LEFT = android.support.constraint.solver.widgets.Barrier.LEFT;
    public static final int RIGHT = android.support.constraint.solver.widgets.Barrier.RIGHT;
    public static final int TOP = android.support.constraint.solver.widgets.Barrier.TOP;
    public static final int BOTTOM = android.support.constraint.solver.widgets.Barrier.BOTTOM;
    public static final int START = BOTTOM + 2;
    public static final int END = START + 1;

    private int mIndicatedType = LEFT;
    private int mResolvedType = LEFT;
    private android.support.constraint.solver.widgets.Barrier mBarrier;

    public Barrier(Context context) {
        super(context);
        super.setVisibility(View.GONE);
    }

    public Barrier(Context context, AttributeSet attrs) {
        super(context, attrs);
        super.setVisibility(View.GONE);
    }

    public Barrier(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        super.setVisibility(View.GONE);
    }

    public int getType() {
        return mIndicatedType;
    }

    /**
     * Set the barrier type
     */
    public void setType(int type) {
        mIndicatedType = type;
        mResolvedType = type;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            // Pre JB MR1, left/right should take precedence, unless they are
            // not defined and somehow a corresponding start/end constraint exists
            if (mIndicatedType == START) {
                mResolvedType = LEFT;
            } else if (mIndicatedType == END) {
                mResolvedType = RIGHT;
            }
        } else {
            // Post JB MR1, if start/end are defined, they take precedence over left/right
            Configuration config = getResources().getConfiguration();
            boolean isRtl = (View.LAYOUT_DIRECTION_RTL == config.getLayoutDirection());
            if (isRtl) {
                if (mIndicatedType == START) {
                    mResolvedType = RIGHT;
                } else if (mIndicatedType == END) {
                    mResolvedType = LEFT;
                }
            } else {
                if (mIndicatedType == START) {
                    mResolvedType = LEFT;
                } else if (mIndicatedType == END) {
                    mResolvedType = RIGHT;
                }
            }
        }
        mBarrier.setBarrierType(mResolvedType);
    }

    @Override
    protected void init(AttributeSet attrs) {
        super.init(attrs);
        mBarrier = new android.support.constraint.solver.widgets.Barrier();
        if (attrs != null) {
            TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.ConstraintLayout_Layout);
            final int N = a.getIndexCount();
            for (int i = 0; i < N; i++) {
                int attr = a.getIndex(i);
                if (attr == R.styleable.ConstraintLayout_Layout_barrierDirection) {
                    setType(a.getInt(attr, LEFT));
                }
            }
        }
        mHelperWidget = mBarrier;
        validateParams();
    }

}
