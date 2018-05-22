package dolphin.android.apps.CpblCalendar3

import android.arch.lifecycle.ViewModel
import android.util.SparseArray
import dolphin.android.apps.CpblCalendar.provider.CpblCalendarHelper

internal class GameViewModel : ViewModel() {
    //var helper: CpblCalendarHelper
    private val mAllGames = SparseArray<GameListLiveData>()

    //fun hasData(year: Int, monthOfJava: Int) = mAllGames[year * 12 + monthOfJava] != null

    fun query(helper: CpblCalendarHelper, year: Int, monthOfJava: Int, fetch: Boolean = true): GameListLiveData? {
        val key = year * 12 + monthOfJava
        if (fetch && mAllGames[key] == null) {
            mAllGames.put(key, GameListLiveData(helper, year, monthOfJava))
        }
        return mAllGames[key]
    }
}