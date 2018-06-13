package dolphin.android.apps.CpblCalendar3;

import android.content.Context;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import android.util.AttributeSet;

/**
 * Created by dolphin on 2017/05/20.
 * <p>
 * http://stackoverflow.com/a/25227797
 */

public class FixedRecyclerView extends RecyclerView {
    public FixedRecyclerView(Context context) {
        super(context);
    }

    public FixedRecyclerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public FixedRecyclerView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public boolean canScrollVertically(int direction) {
        // check if scrolling up
        if (direction < 1) {
            boolean original = super.canScrollVertically(direction);
            return !original && getChildAt(0) != null && getChildAt(0).getTop() < getPaddingTop() || original;
        }
        return super.canScrollVertically(direction);

    }
}
