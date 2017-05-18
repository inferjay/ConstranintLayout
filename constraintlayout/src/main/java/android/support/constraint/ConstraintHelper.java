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
 * Base ConstraintHelper
 */
public abstract class ConstraintHelper extends View {

    protected int[] mIds = new int[32];
    protected int mCount = 0;

    protected Context myContext;
    protected android.support.constraint.solver.widgets.Helper mHelperWidget = null;
    protected boolean mUseViewMeasure = false;
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
        for (int i = 0; i < ids.length; i++) {
            setTag(ids[i], null);
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
     * Allows a helper a chance to updatePreLayout its internal object or set up connections for the pointed elements
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
            View view = container.findViewById(id);
            if (view != null) {
                mHelperWidget.add(container.getViewWidget(view));
            }
        }
    }

    public void updatePostMeasure(ConstraintLayout container) {
        // Do nothing
    }
    public void updatePostLayout(ConstraintLayout container) {
        // Do nothing
    }
}
