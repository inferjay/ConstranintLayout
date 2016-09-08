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

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;

/**
 * Utility class representing a Guideline helper object for {@link ConstraintLayout}.
 */
public class Guideline extends View {
    public Guideline(Context context) {
        super(context);
        super.setVisibility(View.GONE);
    }

    public Guideline(Context context, AttributeSet attrs) {
        super(context, attrs);
        super.setVisibility(View.GONE);
    }

    public Guideline(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        super.setVisibility(View.GONE);
    }

    public Guideline(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        super.setVisibility(View.GONE);
    }

    /**
     * {@hide
     */
    @Override
    public void setVisibility(int visibility) {
    }

    /**
     * {@hide
     */
    @Override
    public void draw(Canvas canvas) {
    }

    /**
     * {@hide
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(0, 0);
    }
}
