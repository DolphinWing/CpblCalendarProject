package dolphin.android.apps.CpblCalendar

import dolphin.android.apps.CpblCalendar.provider.CpblCalendarHelper
import dolphin.android.apps.CpblCalendar.provider.Game
import java.util.*

/**
 * Created by dolphin on 2015/02/07.
 *
 *
 * Query action callback
 */
interface OnQueryCallback {
    fun onQueryStart()

    fun onQueryStateChange(msg: String?)

    fun onQuerySuccess(helper: CpblCalendarHelper?, gameArrayList: ArrayList<Game>?, year: Int, month: Int)

    fun onQueryError(year: Int, month: Int)
}
