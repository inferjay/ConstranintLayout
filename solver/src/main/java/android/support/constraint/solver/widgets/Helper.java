package android.support.constraint.solver.widgets;

import java.util.Arrays;

/**
 * Helper class
 */
public class Helper extends ConstraintWidget {
    protected ConstraintWidget[] mWidgets = new ConstraintWidget[4];
    protected int mWidgetsCount = 0;

    /**
     * Add a widget to the barrier
     *
     * @param widget a widget
     */
    public void add(ConstraintWidget widget) {
        if (mWidgetsCount + 1 > mWidgets.length) {
            mWidgets = Arrays.copyOf(mWidgets, mWidgets.length * 2);
        }
        mWidgets[mWidgetsCount] = widget;
        mWidgetsCount++;
    }

    /**
     * Reset the widgets list contained by this helper
     */
    public void removeAllIds() {
        mWidgetsCount = 0;
    }
}
