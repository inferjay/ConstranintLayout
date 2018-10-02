package android.support.constraint;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import java.lang.reflect.Field;
import java.util.Arrays;

/**
 * @hide
 * <b>Added in 1.1</b>
 * <p>
 *     This class manages a set of referenced widgets. Helper objects can be created to act upon the set
 *     of referenced widgets. The difference between {@code ConstraintHelper} and {@code ViewGroup} is that
 *     multiple {@code ConstraintHelper} can reference the same widgets.
 * <p>
 *     Widgets are referenced by being added to a comma separated list of ids, e.g:
 *     <pre>
 *     {@code
 *         <android.support.constraint.Barrier
 *              android:id="@+id/barrier"
 *              android:layout_width="wrap_content"
 *              android:layout_height="wrap_content"
 *              app:barrierDirection="start"
 *              app:constraint_referenced_ids="button1,button2" />
 *     }
 *     </pre>
 * </p>
 */
public abstract class ConstraintHelper extends View {

    /**
     * @hide
     */
    protected int[] mIds = new int[32];
    /**
     * @hide
     */
    protected int mCount;

    /**
     * @hide
     */
    protected Context myContext;
    /**
     * @hide
     */
    protected android.support.constraint.solver.widgets.Helper mHelperWidget;
    /**
     * @hide
     */
    protected boolean mUseViewMeasure = false;
    /**
     * @hide
     */
    private String mReferenceIds;

    public ConstraintHelper(Context context) {
        super(context);
        myContext = context;
        init(null);
    }

    public ConstraintHelper(Context context, AttributeSet attrs) {
        super(context, attrs);
        myContext = context;
        init(attrs);
    }

    public ConstraintHelper(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        myContext = context;
        init(attrs);
    }

    /**
     * @hide
     */
    protected void init(AttributeSet attrs) {
        if (attrs != null) {
            TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.ConstraintLayout_Layout);
            final int N = a.getIndexCount();
            for (int i = 0; i < N; i++) {
                int attr = a.getIndex(i);
                if (attr == R.styleable.ConstraintLayout_Layout_constraint_referenced_ids) {
                    mReferenceIds = a.getString(attr);
                    setIds(mReferenceIds);
                }
            }
        }
    }

    /**
     * Helpers typically reference a collection of ids
     * @return ids referenced
     */
    public int[] getReferencedIds() {
        return Arrays.copyOf(mIds, mCount);
    }

    /**
     * Helpers typically reference a collection of ids
     * @return ids referenced
     */
    public void setReferencedIds(int[] ids) {
        mCount = 0;
        for (int i = 0; i < ids.length; i++) {
            setTag(ids[i], null);
        }
    }

    /**
     * @hide
     */
    @Override
    public void setTag(int tag, Object value) {
        if (mCount + 1 > mIds.length) {
            mIds = Arrays.copyOf(mIds, mIds.length * 2);
        }
        mIds[mCount] = tag;
        mCount++;
    }

    /**
     * @hide
     */
    @Override
    public void onDraw(Canvas canvas) {
        // Nothing
    }

    /**
     * @hide
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
     * @hide
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
     * @hide
     */
    private void addID(String idString) {
        if (idString == null) {
            return;
        }
        if (myContext == null) {
            return;
        }
        idString = idString.trim();
        int tag = 0;
        try {
            Class res = R.id.class;
            Field field = res.getField(idString);
            tag = field.getInt(null);
        }
        catch (Exception e) {
            // Do nothing
        }
        if (tag == 0) {
            tag = myContext.getResources().getIdentifier(idString, "id",
                    myContext.getPackageName());
        }
        if (tag == 0 && isInEditMode() && getParent() instanceof ConstraintLayout) {
            ConstraintLayout constraintLayout = (ConstraintLayout) getParent();
            Object value = constraintLayout.getDesignInformation(0, idString);
            if (value != null && value instanceof Integer) {
                tag = (Integer) value;
            }
        }

        if (tag != 0) {
            setTag(tag, null);
        } else {
            Log.w("ConstraintHelper", "Could not find id of \""+idString+"\"");
        }
    }

    /**
     * @hide
     */
    private void setIds(String idList) {
        if (idList == null) {
            return;
        }
        int begin = 0;
        while (true) {
            int end = idList.indexOf(',', begin);
            if (end == -1) {
                addID(idList.substring(begin));
                break;
            }
            addID(idList.substring(begin, end));
            begin = end + 1;
        }
    }

    /**
     * @hide
     * Allows a helper a chance to update its internal object pre layout or set up connections for the pointed elements
     *
     * @param container
     */
    public void updatePreLayout(ConstraintLayout container) {
        if (isInEditMode()) {
            setIds(mReferenceIds);
        }
        if (mHelperWidget == null) {
            return;
        }
        mHelperWidget.removeAllIds();
        for (int i = 0; i < mCount; i++) {
            int id = mIds[i];
            View view = container.getViewById(id);
            if (view != null) {
                mHelperWidget.add(container.getViewWidget(view));
            }
        }
    }

    /**
     * @hide
     * Allows a helper a chance to update its internal object post layout or set up connections for the pointed elements
     *
     * @param container
     */
    public void updatePostLayout(ConstraintLayout container) {
        // Do nothing
    }

    /**
     * @hide
     * @param container
     */
    public void updatePostMeasure(ConstraintLayout container) {
        // Do nothing
    }

}
