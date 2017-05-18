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
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

/**
 * Placeholder provides a virtual object which can render an existing object
 */
public class Placeholder extends View {

  private int mContentId = -1;
  private View mContent = null;
  private int mDefaultVisibility = View.INVISIBLE;

  public Placeholder(Context context) {
    super(context);
    init(null);
  }

  public Placeholder(Context context, AttributeSet attrs) {
    super(context, attrs);
    init(attrs);
  }

  public Placeholder(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init(attrs);
  }

  public Placeholder(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr);
    init(attrs);
  }

  private void init(AttributeSet attrs) {
    super.setVisibility(mDefaultVisibility);
    mContentId = -1;
    if (attrs != null) {
      TypedArray a = getContext()
          .obtainStyledAttributes(attrs, R.styleable.ConstraintLayout_Layout);
      final int N = a.getIndexCount();
      for (int i = 0; i < N; i++) {
        int attr = a.getIndex(i);
        if (attr == R.styleable.ConstraintLayout_Layout_content) {
          mContentId = a.getResourceId(attr, mContentId);
        }
      }
    }
  }

  public void setDefaultVisibility(int visibility) {
    mDefaultVisibility = visibility;
  }

  public View getContent() {
    return mContent;
  }

  public void onDraw(Canvas canvas) {
    if (isInEditMode()) {
      canvas.drawRGB(223, 223, 223);
      Paint paint = new Paint();
      paint.setARGB(255, 210, 210, 210);
      paint.setTextAlign(Paint.Align.CENTER);
      paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));

      Rect r = new Rect();
      canvas.getClipBounds(r);
      paint.setTextSize(r.height());
      int cHeight = r.height();
      int cWidth = r.width();
      paint.setTextAlign(Paint.Align.LEFT);
      String text = "?";
      paint.getTextBounds(text, 0, text.length(), r);
      float x = cWidth / 2f - r.width() / 2f - r.left;
      float y = cHeight / 2f + r.height() / 2f - r.bottom;
      canvas.drawText(text, x, y, paint);
    }
  }

  public void updatePreLayout(ConstraintLayout container) {
    if (mContentId == -1) {
      ConstraintLayout.LayoutParams layoutParams = (ConstraintLayout.LayoutParams) getLayoutParams();
      if (!isInEditMode()) {
        setVisibility(mDefaultVisibility);
      }

    }

    mContent = container.findViewById(mContentId);
    if (mContent != null) {
      ConstraintLayout.LayoutParams layoutParamsContent = (ConstraintLayout.LayoutParams) mContent
          .getLayoutParams();
      layoutParamsContent.isInPlaceholder = true;
      mContent.setVisibility(View.VISIBLE);
      setVisibility(View.VISIBLE);
    }
  }

  public void setContentId(int id) {
    if (mContentId == id) {
      return;
    }
    if (mContent != null) {
      mContent.setVisibility(VISIBLE); // ???
      ConstraintLayout.LayoutParams layoutParamsContent = (ConstraintLayout.LayoutParams) mContent
          .getLayoutParams();
      layoutParamsContent.isInPlaceholder = false;
      mContent = null;
    }

    mContentId = id;
    if (id != ConstraintLayout.LayoutParams.UNSET) {
      View v = ((View) getParent()).findViewById(id);
      if (v != null) {
        v.setVisibility(GONE);
      }
    }
  }

  public void updatePostMeasure(ConstraintLayout container) {
    if (mContent == null) {
      return;
    }
    ConstraintLayout.LayoutParams layoutParams = (ConstraintLayout.LayoutParams) getLayoutParams();
    ConstraintLayout.LayoutParams layoutParamsContent = (ConstraintLayout.LayoutParams) mContent
        .getLayoutParams();
    layoutParamsContent.widget.setVisibility(View.VISIBLE);
    layoutParams.widget.setWidth(layoutParamsContent.widget.getWidth());
    layoutParams.widget.setHeight(layoutParamsContent.widget.getHeight());
    layoutParamsContent.widget.setVisibility(View.GONE);
  }

}
