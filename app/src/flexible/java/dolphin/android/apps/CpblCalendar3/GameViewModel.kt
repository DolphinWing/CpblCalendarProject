package dolphin.android.apps.CpblCalendar3

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import dolphin.android.apps.CpblCalendar.provider.CpblCalendarHelper
import java.util.concurrent.Executors

class GameViewModel(application: Application) : AndroidViewModel(application) {
    val helper = CpblCalendarHelper(application)
    var debugMode: Boolean = false
    private val mAllGames
        get () = getApplication<CpblApplication>().cacheList
    private val executor = Executors.newFixedThreadPool(3)
    private val context: Context?
        get() = getApplication<CpblApplication>().applicationContext

    //SparseArray<GameListLiveData>()

    init {
        executor.submit { helper.warmup() }
    }

    private fun gkey(year: Int, monthOfJava: Int) = year * 12 + monthOfJava

    fun fetch(year: Int, monthOfJava: Int, fetchFromWeb: Boolean = true,
              clearCached: Boolean = false): GameListLiveData? {
        val key = gkey(year, monthOfJava)
        if (clearCached) {
            mAllGames.remove(key)
        }
        if (fetchFromWeb && mAllGames[key] == null) {
            mAllGames.put(key, GameListLiveData(executor, helper, year, monthOfJava, debugMode, context))
        }
        return mAllGames[key]
    }

    fun query(year: Int, monthOfJava: Int): GameListLiveData? = mAllGames[gkey(year, monthOfJava)]

//    override fun onCleared() {
//        super.onCleared()
//        executor.shutdown()
//    }
}