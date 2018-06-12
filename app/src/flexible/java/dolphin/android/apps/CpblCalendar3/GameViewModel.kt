package dolphin.android.apps.CpblCalendar3

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import android.util.SparseArray
import dolphin.android.apps.CpblCalendar.provider.CpblCalendarHelper

internal class GameViewModel(application: Application) : AndroidViewModel(application) {
    val helper = CpblCalendarHelper(application)
    var debugMode: Boolean = false
    private val mAllGames = (application as CpblApplication).cacheList
    //SparseArray<GameListLiveData>()

    private fun gkey(year: Int, monthOfJava: Int) = year * 12 + monthOfJava

    fun fetch(year: Int, monthOfJava: Int, fetchFromWeb: Boolean = true,
              clearCached: Boolean = false): GameListLiveData? {
        val key = gkey(year, monthOfJava)
        if (clearCached) {
            mAllGames.remove(key)
        }
        if (fetchFromWeb && mAllGames[key] == null) {
            mAllGames.put(key, GameListLiveData(helper, year, monthOfJava, debugMode))
        }
        return mAllGames[key]
    }

    fun query(year: Int, monthOfJava: Int): GameListLiveData? = mAllGames[gkey(year, monthOfJava)]
}