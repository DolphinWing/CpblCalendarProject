package dolphin.android.apps.CpblCalendar3

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import android.util.AttributeSet

/**
 * Created by dolphin on 2017/05/20.
 *
 *
 * http://stackoverflow.com/a/25227797
 */

class FixedRecyclerView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null,
                                                  defStyleAttr: Int = 0)
    : RecyclerView(context, attrs, defStyleAttr) {

    override fun canScrollVertically(direction: Int): Boolean {
        if (direction < 1) {// check if scrolling up
            val original = super.canScrollVertically(direction)
            return !original && getChildAt(0) != null && getChildAt(0).top < paddingTop || original
        }
        return super.canScrollVertically(direction)
    }
}
