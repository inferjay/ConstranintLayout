package android.support.constraint;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

/**
 * This provides a basic Reference object to be placed in a Constraints virtual viewGroup
 */
public class Reference extends View {
  public Reference(Context context) {
    super(context);
    setVisibility(GONE);
  }

  public Reference(Context context, AttributeSet attrs) {
    super(context, attrs);
    setVisibility(GONE);
  }

  public Reference(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    setVisibility(GONE);
  }
}
