package dolphin.android.apps.CpblCalendar3

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.RecyclerView

/**
 * For RecyclerView + SwipeRefreshLayout
 *
 * See http://stackoverflow.com/a/25227797
 */

class FixedRecyclerView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null,
                                                  defStyleAttr: Int = 0)
    : RecyclerView(context, attrs, defStyleAttr) {

    override fun canScrollVertically(direction: Int): Boolean {
        // check if scrolling up
        if (direction < 1) {
            val original = super.canScrollVertically(direction)
            return !original && getChildAt(0) != null && getChildAt(0).top < paddingTop || original
        }
        return super.canScrollVertically(direction)
    }
}
