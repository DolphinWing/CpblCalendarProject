package dolphin.android.apps.CpblCalendar3

import android.arch.lifecycle.LiveData
import android.util.Log
import dolphin.android.apps.CpblCalendar.provider.CpblCalendarHelper
import dolphin.android.apps.CpblCalendar.provider.Game
import dolphin.android.apps.CpblCalendar.provider.Team
import dolphin.android.apps.CpblCalendar.provider.TeamHelper
import java.util.*
import java.util.concurrent.Executors

internal class GameListLiveData(//application: CpblApplication,
        private val helper: CpblCalendarHelper, private val year: Int, private val monthOfJava: Int,
        private val debugMode: Boolean = false) : LiveData<List<Game>>() {
    companion object {
        private const val TAG = "GameListLiveData"
        private const val TIMEOUT = 3 * 60 * 60 * 1000
    }

    //private val helper = CpblCalendarHelper(application)
    //private val teamHelper = TeamHelper(application)
    private val executor = Executors.newSingleThreadExecutor()
    private var updateTime: Long = 0

    override fun onInactive() {
        super.onInactive()
        Log.d(TAG, "onInactive $year/${monthOfJava + 1}")
    }

    override fun onActive() {
        super.onActive()
        Log.d(TAG, "onActive $year/${monthOfJava + 1}")

        executor.submit { if (expired() || value == null) fetch() }
    }

    private fun expired(): Boolean = updateTime <= 0 ||
            (System.currentTimeMillis() - updateTime) > TIMEOUT

    private fun fetch() {
        if (debugMode) {
            genData()
            return //just create fake data
        }

        updateTime = System.currentTimeMillis()

        Log.v(TAG, "fetch $year/${monthOfJava + 1}")
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

    private fun genData() {
        Thread.sleep(500)
        val now = CpblCalendarHelper.getNowTime()
        val monthOfJava = now.get(Calendar.MONTH)
        val dayOfMonth = now.get(Calendar.DAY_OF_MONTH)
        val id = monthOfJava * 100
        when {
            this.monthOfJava == monthOfJava -> {
                val time = CpblCalendarHelper.getGameTime(year, this.monthOfJava + 1, dayOfMonth,
                        18, 35)
                postValue(arrayListOf(
                        Game(id + 1, time).apply {
                            IsFinal = true
                            StartTime.add(Calendar.DAY_OF_MONTH, -1)
                            AwayTeam = Team(Team.ID_FUBON_GUARDIANS)
                            HomeTeam = Team(Team.ID_LAMIGO_MONKEYS)
                        },
                        Game(id + 2, time).apply {
                            IsFinal = true
                            StartTime.add(Calendar.DAY_OF_MONTH, -1)
                            AwayTeam = Team(Team.ID_CT_ELEPHANTS)
                            HomeTeam = Team(Team.ID_UNI_711_LIONS)
                        },
                        Game(id + 3, time).apply {
                            IsLive = true
                            LiveMessage = "LIVE!!"
                            AwayTeam = Team(Team.ID_UNKNOWN_AWAY)
                            HomeTeam = Team(Team.ID_UNKNOWN_HOME)
                        },
                        Game(id + 4, time).apply {
                            StartTime.add(Calendar.DAY_OF_MONTH, 1)
                            AwayTeam = Team(Team.ID_CHINESE_TAIPEI)
                            HomeTeam = Team(Team.ID_SOUTH_KOREA)
                        }))
            }
            this.monthOfJava > monthOfJava -> {
                val time = CpblCalendarHelper.getGameTime(year, this.monthOfJava + 1, 28,
                        18, 35)
                postValue(arrayListOf(
                        Game(id + 1, time).apply {
                            StartTime.add(Calendar.DAY_OF_MONTH, -1)
                            IsFinal = true
                            DelayMessage = "delayed"
                            AwayTeam = Team(Team.ID_SINON_BULLS)
                            HomeTeam = Team(Team.ID_EDA_RHINOS)
                        },
                        Game(id + 2, time).apply {
                            IsFinal = true
                            AwayTeam = Team(Team.ID_MKT_COBRAS)
                            HomeTeam = Team(Team.ID_MKT_SUNS)
                        },
                        Game(id + 3, time).apply {
                            IsFinal = true
                            AwayTeam = Team(Team.ID_FIRST_KINGO)
                            HomeTeam = Team(Team.ID_CT_WHALES)
                        }))
            }
            this.monthOfJava < monthOfJava -> {
                val time = CpblCalendarHelper.getGameTime(year, this.monthOfJava + 1, 1,
                        18, 35)
                postValue(arrayListOf(
                        Game(id + 1, time).apply {
                            DelayMessage = "original $year/$monthOfJava/$dayOfMonth"
                            AwayTeam = Team(Team.ID_SS_TIGERS)
                            HomeTeam = Team(Team.ID_W_DRAGONS)
                        },
                        Game(id + 2, time).apply {
                            Channel = "CPBL TV"
                            AwayTeam = Team(Team.ID_JUNGO_BEARS)
                            HomeTeam = Team(Team.ID_ELEPHANTS)
                        },
                        Game(id + 3, time).apply {
                            StartTime.add(Calendar.DAY_OF_MONTH, 1)
                            AwayTeam = Team(Team.ID_TIME_EAGLES)
                            HomeTeam = Team(Team.ID_LANEW_BEARS)
                        }))
            }
        }
    }
}