package android.support.constraint;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

/**
 * Control the visibility and elevation of the referenced views
 */
/**
 * <b>Added in 1.1</b>
 * <p>
 *     This class controls the visibility of a set of referenced widgets.
 *     Widgets are referenced by being added to a comma separated list of ids, e.g:
 *     <pre>
 *     {@code
 *          <android.support.constraint.Group
 *              android:id="@+id/group"
 *              android:layout_width="wrap_content"
 *              android:layout_height="wrap_content"
 *              android:visibility="visible"
 *              app:constraint_referenced_ids="button4,button9" />
 *     }
 *     </pre>
 *     <p>
 *         The visibility of the group will be applied to the referenced widgets.
 *         It's a convenient way to easily hide/show a set of widgets without having to maintain this set
 *         programmatically.
 *     <p>
 *     <h2>Multiple groups</h2>
 *     <p>
 *         Multiple groups can reference the same widgets -- in that case, the XML declaration order will
 *         define the final visibility state (the group declared last will have the last word).
 * </p>
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

    /**
     * @hide
     * @param attrs
     */
    protected void init(AttributeSet attrs) {
        super.init(attrs);
        mUseViewMeasure = false;
    }

    /**
     * @hide
     * @param container
     */
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

    /**
     * @hide
     * @param container
     */
    @Override
    public void updatePostLayout(ConstraintLayout container) {
        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) getLayoutParams();
        params.widget.setWidth(0);
        params.widget.setHeight(0);
    }
}
