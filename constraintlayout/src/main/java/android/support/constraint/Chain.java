package android.support.constraint;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.support.constraint.solver.widgets.ConstraintWidget;
import android.util.AttributeSet;
import android.view.View;

/**
 * Maintain a chain
 */
public class Chain extends ConstraintHelper {

    private boolean mUseRtl = true;

    public Chain(Context context) {
        super(context);
        init(null);
    }

    public Chain(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public Chain(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        if (attrs != null) {
            TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.ConstraintLayout_Layout);
            final int N = a.getIndexCount();
            for (int i = 0; i < N; i++) {
                int attr = a.getIndex(i);
                if (attr == R.styleable.ConstraintLayout_Layout_chainUseRtl) {
                    mUseRtl = a.getBoolean(attr, mUseRtl);
                }
            }
        }
        mUseViewMeasure = false;
    }

    public void updatePreLayout(ConstraintLayout container) {
        View previous = null;
        ConstraintLayout.LayoutParams chainParams = (ConstraintLayout.LayoutParams) getLayoutParams();
        ConstraintLayout.LayoutParams params = null;
        ConstraintLayout.LayoutParams previousParams = null;

        if (chainParams.orientation == ConstraintLayout.LayoutParams.VERTICAL) {
            chainParams.width = 1;
            if (chainParams.height == ConstraintLayout.LayoutParams.WRAP_CONTENT) {
                chainParams.height = ConstraintLayout.LayoutParams.MATCH_PARENT;
            }
        } else {
            if (chainParams.width == ConstraintLayout.LayoutParams.WRAP_CONTENT) {
                chainParams.width = ConstraintLayout.LayoutParams.MATCH_PARENT;
            }
            chainParams.height = 1;
        }
        chainParams.validate();

        int chainId = getId();
        if (chainParams.orientation == ConstraintLayout.LayoutParams.VERTICAL) {
            chainParams.width = ConstraintLayout.LayoutParams.MATCH_PARENT;
            for (int i = 0; i < mCount; i++) {
                int id = mIds[i];
                View view = container.getViewById(id);
                if (view != null) {
                    params = (ConstraintLayout.LayoutParams) view.getLayoutParams();
                    params.widget.reset();
                    params.helped = true;
                    params.topToTop = ConstraintLayout.LayoutParams.UNSET;
                    params.topToBottom = ConstraintLayout.LayoutParams.UNSET;
                    params.bottomToTop = ConstraintLayout.LayoutParams.UNSET;
                    params.bottomToBottom = ConstraintLayout.LayoutParams.UNSET;
                    if (previousParams == null) {
                        params.topToTop = chainId;
                    } else {
                        params.topToBottom = previous.getId();
                        previousParams.bottomToTop = view.getId();
                    }
                    if (i == mCount - 1) {
                        params.bottomToBottom = chainId;
                    }
                    previous = view;
                    previousParams = params;
                }
            }
        } else {
            chainParams.height = ConstraintLayout.LayoutParams.MATCH_PARENT;
            int layoutDirection = 0;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                layoutDirection = getLayoutDirection();
            }
            for (int i = 0; i < mCount; i++) {
                int id = mIds[i];
                View view = container.getViewById(id);
                if (view != null) {
                    params = (ConstraintLayout.LayoutParams) view.getLayoutParams();
                    params.widget.reset();
                    params.helped = true;
                    params.leftToLeft = ConstraintLayout.LayoutParams.UNSET;
                    params.leftToRight = ConstraintLayout.LayoutParams.UNSET;
                    params.rightToLeft = ConstraintLayout.LayoutParams.UNSET;
                    params.rightToRight = ConstraintLayout.LayoutParams.UNSET;
                    params.startToStart = ConstraintLayout.LayoutParams.UNSET;
                    params.startToEnd = ConstraintLayout.LayoutParams.UNSET;
                    params.endToStart = ConstraintLayout.LayoutParams.UNSET;
                    params.endToEnd = ConstraintLayout.LayoutParams.UNSET;
                    if (mUseRtl) {
                        if (previousParams == null) {
                            params.startToStart = chainId;
                        } else {
                            params.startToEnd = previous.getId();
                            previousParams.endToStart = view.getId();
                        }
                        if (i == mCount - 1) {
                            params.endToEnd = chainId;
                        }
                    } else {
                        if (previousParams == null) {
                            params.leftToLeft = chainId;
                        } else {
                            params.leftToRight = previous.getId();
                            previousParams.rightToLeft = view.getId();
                        }
                        if (i == mCount - 1) {
                            params.rightToRight = chainId;
                        }
                    }
                    previous = view;
                    previousParams = params;
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                for (int i = 0; i < mCount; i++) {
                    int id = mIds[i];
                    View view = container.getViewById(id);
                    if (view != null) {
                        params = (ConstraintLayout.LayoutParams) view.getLayoutParams();
                        params.resolveLayoutDirection(layoutDirection);
                    }
                }
            }
        }
    }

    public void updatePostLayout(ConstraintLayout container) {
        ConstraintLayout.LayoutParams chainParams = (ConstraintLayout.LayoutParams) getLayoutParams();
        int min = Integer.MAX_VALUE;
        int max = 0;
        if (chainParams.orientation == ConstraintLayout.LayoutParams.VERTICAL) {
            for (int i = 0; i < mCount; i++) {
                int id = mIds[i];
                View view = container.getViewById(id);
                ConstraintWidget widget = container.getViewWidget(view);
                int x1 = widget.getDrawX();
                int x2 = widget.getDrawX() + widget.getDrawWidth();
                if (x1 < min) {
                    min = x1;
                }
                if (x2 > max) {
                    max = x2;
                }
            }
            chainParams.widget.setDrawX(min);
            chainParams.widget.setWidth(max - min);
        } else {
            for (int i = 0; i < mCount; i++) {
                int id = mIds[i];
                View view = container.getViewById(id);
                ConstraintWidget widget = container.getViewWidget(view);
                int y1 = widget.getDrawY();
                int y2 = widget.getDrawY() + widget.getDrawHeight();
                if (y1 < min) {
                    min = y1;
                }
                if (y2 > max) {
                    max = y2;
                }
            }
            chainParams.widget.setDrawY(min);
            chainParams.widget.setHeight(max - min);
        }
    }

}
