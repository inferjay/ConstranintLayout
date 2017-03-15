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
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import java.util.Arrays;

/**
 * A barrier is positioned relative to multiple widgets
 */
public class Barrier extends View implements Helper {

    public static final int LEFT = android.support.constraint.solver.widgets.Barrier.LEFT;
    public static final int RIGHT = android.support.constraint.solver.widgets.Barrier.RIGHT;
    public static final int TOP = android.support.constraint.solver.widgets.Barrier.TOP;
    public static final int BOTTOM = android.support.constraint.solver.widgets.Barrier.BOTTOM;
    public static final int START = BOTTOM + 2;
    public static final int END = START + 1;

    private int mIndicatedType = LEFT;
    private int mResolvedType = LEFT;

    private int[] mIds = new int[32];
    private int mCount = 0;
    private android.support.constraint.solver.widgets.Barrier mBarrier = new android.support.constraint.solver.widgets.Barrier();

    public Barrier(Context context) {
        super(context);
        super.setVisibility(View.GONE);
        init(null);
    }

    public Barrier(Context context, AttributeSet attrs) {
        super(context, attrs);
        super.setVisibility(View.GONE);
        init(attrs);
    }

    public Barrier(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        super.setVisibility(View.GONE);
        init(attrs);
    }

    public Barrier(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr);
        super.setVisibility(View.GONE);
        init(attrs);
    }

    /**
     * Set the barrier type
     * @param type
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

    private void init(AttributeSet attrs) {
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
        validateParams();
    }

    public void validateParams() {
        ViewGroup.LayoutParams params = getLayoutParams();
        if (params instanceof ConstraintLayout.LayoutParams) {
            ConstraintLayout.LayoutParams layoutParams = (ConstraintLayout.LayoutParams) params;
            layoutParams.widget = mBarrier;
        }
    }

    @Override
    public void setTag(int tag, Object value) {
        if (value != null && value instanceof String && ((String) value).equalsIgnoreCase("true")) {
            if (mCount + 1 > mIds.length) {
                mIds = Arrays.copyOf(mIds, mIds.length * 2);
            }
            mIds[mCount] = tag;
            mCount++;
        }
    }

    /**
     * {@hide
     */
    @Override
    public void setVisibility(int visibility) {
        // Nothing
    }

    /**
     * {@hide
     */
    @Override
    public void draw(Canvas canvas) {
        // Nothing
    }

    /**
     * {@hide
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(0, 0);
    }

    public void update(ConstraintLayout container) {
        mBarrier.removeAllIds();
        for (int i = 0; i < mCount; i++) {
            int id = mIds[i];
            View view = container.findViewById(id);
            if (view != null) {
                mBarrier.add(container.getViewWidget(view));
            }
        }
    }

}
