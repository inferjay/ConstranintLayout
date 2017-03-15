package android.support.constraint;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import java.util.Arrays;

/**
 * Base ConstraintHelper
 */
public abstract class ConstraintHelper extends View {

    protected int[] mIds = new int[32];
    protected int mCount = 0;

    protected android.support.constraint.solver.widgets.Helper mHelperWidget = null;
    protected boolean mUseViewMeasure = false;

    public ConstraintHelper(Context context) {
        super(context);
        if (!isInEditMode()) {
            super.setVisibility(View.GONE);
        }
    }

    public ConstraintHelper(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (!isInEditMode()) {
            super.setVisibility(View.GONE);
        }
    }

    public ConstraintHelper(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (!isInEditMode()) {
            super.setVisibility(View.GONE);
        }
    }

    @Override
    public void setTag(int tag, Object value) {
        if (mCount + 1 > mIds.length) {
            mIds = Arrays.copyOf(mIds, mIds.length * 2);
        }
        mIds[mCount] = tag;
        mCount++;
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
        if (mUseViewMeasure) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        } else {
            setMeasuredDimension(0, 0);
        }
    }

    /**
     * Allows a helper to replace the default ConstraintWidget in LayoutParams by its own subclass
     */
    public void validateParams() {
        if (mHelperWidget == null) {
            return;
        }
        ViewGroup.LayoutParams params = getLayoutParams();
        if (params instanceof ConstraintLayout.LayoutParams) {
            ConstraintLayout.LayoutParams layoutParams = (ConstraintLayout.LayoutParams) params;
            layoutParams.widget = mHelperWidget;
        }
    }

    /**
     * Allows a helper a chance to updatePreLayout its internal object or set up connections for the pointed elements
     *
     * @param container
     */
    public void updatePreLayout(ConstraintLayout container) {
        if (mHelperWidget == null) {
            return;
        }
        mHelperWidget.removeAllIds();
        for (int i = 0; i < mCount; i++) {
            int id = mIds[i];
            View view = container.findViewById(id);
            if (view != null) {
                mHelperWidget.add(container.getViewWidget(view));
            }
        }
    }

    public void updatePostLayout(ConstraintLayout container) {
        // Do nothing
    }
}
