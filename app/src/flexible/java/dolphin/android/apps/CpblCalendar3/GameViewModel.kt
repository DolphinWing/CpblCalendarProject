package dolphin.android.apps.CpblCalendar3

import android.arch.lifecycle.ViewModel
import android.util.SparseArray
import dolphin.android.apps.CpblCalendar.provider.CpblCalendarHelper

internal class GameViewModel : ViewModel() {
    //var helper: CpblCalendarHelper
    private val mAllGames = SparseArray<GameListLiveData>()

    private fun gkey(year: Int, monthOfJava: Int) = year * 12 + monthOfJava

    fun fetch(helper: CpblCalendarHelper, year: Int, monthOfJava: Int,
              fetchFromWeb: Boolean = true, clearCached: Boolean = false): GameListLiveData? {
        val key = gkey(year, monthOfJava)
        if (clearCached) {
            mAllGames.remove(key)
        }
        if (fetchFromWeb && mAllGames[key] == null) {
            mAllGames.put(key, GameListLiveData(helper, year, monthOfJava))
        }
        return mAllGames[key]
    }

    fun query(year: Int, monthOfJava: Int): GameListLiveData? = mAllGames[gkey(year, monthOfJava)]
}