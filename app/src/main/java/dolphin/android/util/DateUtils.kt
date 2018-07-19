package dolphin.android.util

import java.util.*

/**
 * Quick access to some comparisons.
 */

class DateUtils {
    companion object {
        @JvmStatic
        fun sameDay(cal1: Calendar, cal2: Calendar): Boolean {
            return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                    cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
        }

        @JvmStatic
        fun isToday(calendar: Calendar): Boolean {
            return sameDay(Calendar.getInstance(), calendar)
        }
    }
}
