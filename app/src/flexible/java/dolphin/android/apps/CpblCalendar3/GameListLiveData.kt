package dolphin.android.apps.CpblCalendar3

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import dolphin.android.apps.CpblCalendar.provider.CpblCalendarHelper
import dolphin.android.apps.CpblCalendar.provider.Game
import dolphin.android.apps.CpblCalendar.provider.Team
import java.util.*
import java.util.concurrent.ExecutorService

internal class GameListLiveData(private val executor: ExecutorService, //application: CpblApplication,
                                private val helper: CpblCalendarHelper, private val year: Int,
                                private val monthOfJava: Int,
                                private val debugMode: Boolean = false,
                                private val context: Context? = null)
    : LiveData<List<Game>>() {
    companion object {
        private const val TAG = "GameListLiveData"
        private const val TIMEOUT = 3 * 60 * 60 * 1000
    }

    //private val helper = CpblCalendarHelper(application)
    //private val teamHelper = TeamHelper(application)
    //private val executor = Executors.newSingleThreadExecutor()
    private var updateTime: Long = 0

    override fun onInactive() {
        super.onInactive()
        if (debugMode) Log.d(TAG, "onInactive $year/${monthOfJava + 1}")
    }

    override fun onActive() {
        super.onActive()
        if (debugMode) Log.d(TAG, "onActive $year/${monthOfJava + 1}")

        executor.submit { if (expired() || value == null) fetch() }
    }

    private fun expired(): Boolean = updateTime <= 0 ||
            (System.currentTimeMillis() - updateTime) > TIMEOUT

    private fun fetch() {
        //Log.d(TAG, "debug mode = $debugMode")
        updateTime = System.currentTimeMillis()
        if (debugMode) {
            genData()
            return //just create fake data
        }

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
        if (debugMode) Log.d(TAG, "list size = ${sortedList.size}")
        //mAllGamesCache.put(year * 12 + monthOfJava, sortedList)

        postValue(sortedList)
    }

    private fun genData() {
        Thread.sleep(500)
        val now = CpblCalendarHelper.getNowTime()
        val monthOfJava = now.get(Calendar.MONTH)
        val dayOfMonth = now.get(Calendar.DAY_OF_MONTH)
        val id = monthOfJava * 100
        val random = Random(System.currentTimeMillis())
        when {
            this.monthOfJava == monthOfJava -> {
                val time = Game.getGameTime(year, this.monthOfJava + 1, dayOfMonth,
                        18, 35)
                postValue(arrayListOf(
                        Game(id + 1, time).apply {
                            IsFinal = true
                            StartTime.add(Calendar.DAY_OF_MONTH, -1)
                            AwayTeam = Team(context, Team.ID_FUBON_GUARDIANS, false)
                            HomeTeam = Team(context, Team.ID_LAMIGO_MONKEYS, true)
                            AwayScore = random.nextInt(20)
                            HomeScore = random.nextInt(20)
                            FieldId = "F04"
                        },
                        Game(id + 2, time).apply {
                            IsFinal = true
                            StartTime.add(Calendar.DAY_OF_MONTH, -1)
                            AwayTeam = Team(context, Team.ID_CT_ELEPHANTS, false)
                            HomeTeam = Team(context, Team.ID_UNI_711_LIONS, true)
                            HomeScore = random.nextInt(20)
                            AwayScore = random.nextInt(20)
                            FieldId = "F02"
                        },
                        Game(id + 3, time).apply {
                            StartTime.timeInMillis = now.timeInMillis //reset start time
                            StartTime.add(Calendar.HOUR_OF_DAY, -1) //the game should be started
                            IsLive = true
                            LiveMessage = "LIVE!! TOP 3"
                            AwayTeam = Team(context, Team.ID_UNKNOWN_AWAY, false)
                            HomeTeam = Team(context, Team.ID_UNKNOWN_HOME, true)
                            HomeScore = random.nextInt(20)
                            AwayScore = random.nextInt(20)
                            FieldId = "F26"
                            Field = "Tainan"
                        },
                        Game(id + 4, time).apply {
                            StartTime.timeInMillis = now.timeInMillis //reset start time
                            StartTime.add(Calendar.HOUR_OF_DAY, -1) //the game should be started
                            IsLive = true
                            LiveMessage = "LIVE!! BOT 7"
                            AwayTeam = Team(context, Team.ID_FUBON_GUARDIANS, false)
                            HomeTeam = Team(context, Team.ID_LAMIGO_MONKEYS, true)
                            AwayScore = random.nextInt(20)
                            HomeScore = random.nextInt(20)
                            FieldId = "F04"
                            Field = "Taipei"
                        },
                        Game(id + 5, time).apply {
                            StartTime.add(Calendar.DAY_OF_MONTH, 1)
                            AwayTeam = Team(context, Team.ID_CHINESE_TAIPEI, false)
                            HomeTeam = Team(context, Team.ID_SOUTH_KOREA, true)
                            FieldId = "F08"
                        }))
            }
            this.monthOfJava > monthOfJava -> {
                val time = Game.getGameTime(year, this.monthOfJava + 1, 28,
                        18, 35)
                postValue(arrayListOf(
                        Game(id + 1, time).apply {
                            StartTime.add(Calendar.DAY_OF_MONTH, -1)
                            IsFinal = true
                            DelayMessage = "delayed"
                            AwayTeam = Team(context, Team.ID_SINON_BULLS, false)
                            HomeTeam = Team(context, Team.ID_EDA_RHINOS, true)
                            FieldId = "F09"
                            HomeScore = random.nextInt(20)
                            AwayScore = random.nextInt(20)
                        },
                        Game(id + 2, time).apply {
                            IsFinal = true
                            AwayTeam = Team(context, Team.ID_MKT_COBRAS, false)
                            HomeTeam = Team(context, Team.ID_MKT_SUNS, true)
                            FieldId = "F11"
                            HomeScore = random.nextInt(20)
                            AwayScore = random.nextInt(20)
                        },
                        Game(id + 3, time).apply {
                            IsFinal = true
                            AwayTeam = Team(context, Team.ID_FIRST_KINGO)
                            HomeTeam = Team(context, Team.ID_CT_WHALES, true)
                            FieldId = "F26"
                            HomeScore = random.nextInt(20)
                            AwayScore = random.nextInt(20)
                        }))
            }
            this.monthOfJava < monthOfJava -> {
                val time = Game.getGameTime(year, this.monthOfJava + 1, 1,
                        18, 35)
                postValue(arrayListOf(
                        Game(id + 1, time).apply {
                            DelayMessage = "original $year/$monthOfJava/$dayOfMonth"
                            AwayTeam = Team(context, Team.ID_SS_TIGERS, false)
                            HomeTeam = Team(context, Team.ID_W_DRAGONS, true)
                            FieldId = "F19"
                        },
                        Game(id + 2, time).apply {
                            Channel = "CPBL TV"
                            AwayTeam = Team(context, Team.ID_JUNGO_BEARS)
                            HomeTeam = Team(context, Team.ID_ELEPHANTS, true)
                            FieldId = "F05"
                        },
                        Game(id + 3, time).apply {
                            StartTime.add(Calendar.DAY_OF_MONTH, 1)
                            AwayTeam = Team(context, Team.ID_TIME_EAGLES)
                            HomeTeam = Team(context, Team.ID_LANEW_BEARS, true)
                            FieldId = "F03"
                        }))
            }
        }
    }
}