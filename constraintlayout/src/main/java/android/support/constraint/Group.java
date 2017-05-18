package android.support.constraint;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;

/**
 * Control the visibility and elevation of the referenced views
 */
public class Group extends ConstraintHelper {

    public Group(Context context) {
        super(context);
    }

    public Group(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public Group(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    protected void init(AttributeSet attrs) {
        super.init(attrs);
        mUseViewMeasure = false;
    }

    @Override
    public void updatePreLayout(ConstraintLayout container) {
        int visibility = getVisibility();
        float elevation = 0;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            elevation = getElevation();
        }
        for (int i = 0; i < mCount; i++) {
            int id = mIds[i];
            View view = container.getViewById(id);
            if (view != null) {
                view.setVisibility(visibility);
                if (elevation > 0 && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    view.setElevation(elevation);
                }
            }
        }
    }

    @Override
    public void updatePostLayout(ConstraintLayout container) {
        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) getLayoutParams();
        params.widget.setWidth(0);
        params.widget.setHeight(0);
    }
}
