package dolphin.android.apps.CpblCalendar3

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import cn.carbswang.android.numberpickerview.library.NumberPickerView

class FixedNumberPickerView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : NumberPickerView(context, attrs, defStyleAttr) {
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return if (isEnabled) super.onTouchEvent(event) else true
    }
}