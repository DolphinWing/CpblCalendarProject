package dolphin.android.apps.CpblCalendar3

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import dolphin.android.apps.CpblCalendar.provider.CpblCalendarHelper
import java.util.concurrent.Executors

internal class GameViewModel(application: Application) : AndroidViewModel(application) {
    val helper = CpblCalendarHelper(application)
    var debugMode: Boolean = false
    private val mAllGames = (application as CpblApplication).cacheList
    private val executor = Executors.newFixedThreadPool(3)

    //SparseArray<GameListLiveData>()

    private fun gkey(year: Int, monthOfJava: Int) = year * 12 + monthOfJava

    fun fetch(year: Int, monthOfJava: Int, fetchFromWeb: Boolean = true,
              clearCached: Boolean = false): GameListLiveData? {
        val key = gkey(year, monthOfJava)
        if (clearCached) {
            mAllGames.remove(key)
        }
        if (fetchFromWeb && mAllGames[key] == null) {
            mAllGames.put(key, GameListLiveData(executor, helper, year, monthOfJava, debugMode))
        }
        return mAllGames[key]
    }

    fun query(year: Int, monthOfJava: Int): GameListLiveData? = mAllGames[gkey(year, monthOfJava)]

    override fun onCleared() {
        super.onCleared()
        executor.shutdown()
    }
}