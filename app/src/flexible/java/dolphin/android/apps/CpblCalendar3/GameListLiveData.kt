package dolphin.android.apps.CpblCalendar3

import android.arch.lifecycle.LiveData
import android.util.Log
import dolphin.android.apps.CpblCalendar.provider.CpblCalendarHelper
import dolphin.android.apps.CpblCalendar.provider.Game
import kotlin.concurrent.thread

internal class GameListLiveData(private val helper: CpblCalendarHelper,
                                private val year: Int,
                                private val monthOfJava: Int) : LiveData<List<Game>>() {
    companion object {
        private const val TAG = "GameListLiveData"
    }

    override fun onInactive() {
        super.onInactive()
        Log.d(TAG, "onInactive $year/${monthOfJava + 1}")
    }

    override fun onActive() {
        super.onActive()
        Log.d(TAG, "onActive $year/${monthOfJava + 1}")
        thread { fetch() }
    }

    private fun fetch() {
        val list = helper.query2018(year, monthOfJava, "01")
        //check if we have warm up games here
        if (helper.isWarmUpMonth(year, monthOfJava)) {
            Log.v(TAG, "find warm up games")
            list.addAll(helper.query2018(year, monthOfJava, "07"))
        }
        //check if we have all star games here
        if (helper.isAllStarMonth(year, monthOfJava)) {
            Log.v(TAG, "find all-star games")
            list.addAll(helper.query2018(year, monthOfJava, "02"))
        }
        //check if we have challenger games here
        if (helper.isChallengeMonth(year, monthOfJava)) {
            Log.v(TAG, "find challenger games")
            list.addAll(helper.query2018(year, monthOfJava, "05"))
        }
        //check if we have championship games here
        if (helper.isChampionMonth(year, monthOfJava)) {
            Log.v(TAG, "find championship games")
            list.addAll(helper.query2018(year, monthOfJava, "03"))
        }
        //sort games by time and id
        val sortedList = list.sortedWith(compareBy(Game::StartTime, Game::Id))
        Log.d(TAG, "list size = ${sortedList.size}")
        //mAllGamesCache.put(year * 12 + monthOfJava, sortedList)

        postValue(sortedList)
    }
}