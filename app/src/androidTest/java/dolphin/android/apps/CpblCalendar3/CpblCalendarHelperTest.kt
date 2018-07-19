package dolphin.android.apps.CpblCalendar3

import android.util.Log
import androidx.test.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import dolphin.android.apps.CpblCalendar.provider.CpblCalendarHelper
import dolphin.android.apps.CpblCalendar.provider.Game
import dolphin.android.apps.CpblCalendar.provider.Team
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*

@RunWith(AndroidJUnit4::class)
class CpblCalendarHelperTest {
    companion object {
        private const val TAG = "CpblCalendarHelperTest"
    }

    private lateinit var helper: CpblCalendarHelper
    private lateinit var testData: ArrayList<Game>

    @Before
    fun setup() {
        val appContext = InstrumentationRegistry.getTargetContext()
        helper = CpblCalendarHelper(appContext)

        val now = CpblCalendarHelper.getNowTime()
        val year = now.get(Calendar.YEAR)
        val monthOfJava = now.get(Calendar.MONTH)
        val dayOfMonth = now.get(Calendar.DAY_OF_MONTH)
        val id = monthOfJava * 100
        val random = Random(System.currentTimeMillis())
        val time = CpblCalendarHelper.getGameTime(year, monthOfJava + 1, dayOfMonth,
                18, 35)
        val fields = appContext.resources.getStringArray(R.array.cpbl_game_field_name)
        testData = arrayListOf(
                Game(id - 3, time).apply {
                    StartTime.add(Calendar.MONTH, -1)
                    StartTime.add(Calendar.DAY_OF_MONTH, -2)
                    IsFinal = true
                    DelayMessage = "delayed"
                    AwayTeam = Team(appContext, Team.ID_SINON_BULLS, false)
                    HomeTeam = Team(appContext, Team.ID_EDA_RHINOS, true)
                    FieldId = fields[random.nextInt(fields.size)]
                    HomeScore = random.nextInt(20)
                    AwayScore = random.nextInt(20)
                },
                Game(id - 2, time).apply {
                    StartTime.add(Calendar.MONTH, -1)
                    StartTime.add(Calendar.DAY_OF_MONTH, -1)
                    IsFinal = true
                    AwayTeam = Team(appContext, Team.ID_MKT_COBRAS, false)
                    HomeTeam = Team(appContext, Team.ID_MKT_SUNS, true)
                    FieldId = fields[random.nextInt(fields.size)]
                    HomeScore = random.nextInt(20)
                    AwayScore = random.nextInt(20)
                },
                Game(id - 1, time).apply {
                    StartTime.add(Calendar.MONTH, -1)
                    StartTime.add(Calendar.DAY_OF_MONTH, -1)
                    IsFinal = true
                    AwayTeam = Team(appContext, Team.ID_FIRST_KINGO)
                    HomeTeam = Team(appContext, Team.ID_CT_WHALES, true)
                    FieldId = fields[random.nextInt(fields.size)]
                    HomeScore = random.nextInt(20)
                    AwayScore = random.nextInt(20)
                },
                Game(id + 1, time).apply {
                    IsFinal = true
                    StartTime.add(Calendar.DAY_OF_MONTH, -1)
                    AwayTeam = Team(appContext, Team.ID_FUBON_GUARDIANS, false)
                    HomeTeam = Team(appContext, Team.ID_LAMIGO_MONKEYS, true)
                    AwayScore = random.nextInt(20)
                    HomeScore = random.nextInt(20)
                    FieldId = fields[random.nextInt(fields.size)]
                },
                Game(id + 2, time).apply {
                    IsFinal = true
                    StartTime.add(Calendar.DAY_OF_MONTH, -1)
                    AwayTeam = Team(appContext, Team.ID_CT_ELEPHANTS, false)
                    HomeTeam = Team(appContext, Team.ID_UNI_711_LIONS, true)
                    HomeScore = random.nextInt(20)
                    AwayScore = random.nextInt(20)
                    FieldId = fields[random.nextInt(fields.size)]
                },
                Game(id + 3, time).apply {
                    StartTime.timeInMillis = now.timeInMillis //reset start time
                    StartTime.add(Calendar.HOUR_OF_DAY, -1) //the game should be started
                    IsLive = true
                    //IsFinal = true
                    LiveMessage = "LIVE!!"
                    AwayTeam = Team(appContext, Team.ID_UNKNOWN_AWAY, false)
                    HomeTeam = Team(appContext, Team.ID_UNKNOWN_HOME, true)
                    HomeScore = random.nextInt(20)
                    AwayScore = random.nextInt(20)
                    FieldId = fields[random.nextInt(fields.size)]
                },
                Game(id + 4, time).apply {
                    StartTime.timeInMillis = now.timeInMillis //reset start time
                    StartTime.add(Calendar.HOUR_OF_DAY, -1) //the game should be started
                    IsLive = true
                    //IsFinal = true
                    LiveMessage = "LIVE!!"
                    AwayTeam = Team(appContext, Team.ID_UNKNOWN_AWAY, false)
                    HomeTeam = Team(appContext, Team.ID_UNKNOWN_HOME, true)
                    HomeScore = random.nextInt(20)
                    AwayScore = random.nextInt(20)
                    FieldId = fields[random.nextInt(fields.size)]
                },
                Game(id + 5, time).apply {
                    StartTime.add(Calendar.DAY_OF_MONTH, 1)
                    AwayTeam = Team(appContext, Team.ID_CHINESE_TAIPEI, false)
                    HomeTeam = Team(appContext, Team.ID_SOUTH_KOREA, true)
                    FieldId = fields[random.nextInt(fields.size)]
                },
                Game(id + 11, time).apply {
                    StartTime.add(Calendar.MONTH, 1)
                    DelayMessage = "original $year/$monthOfJava/$dayOfMonth"
                    AwayTeam = Team(appContext, Team.ID_SS_TIGERS, false)
                    HomeTeam = Team(appContext, Team.ID_W_DRAGONS, true)
                    FieldId = fields[random.nextInt(fields.size)]
                },
                Game(id + 12, time).apply {
                    StartTime.add(Calendar.MONTH, 1)
                    Channel = "CPBL TV"
                    AwayTeam = Team(appContext, Team.ID_JUNGO_BEARS)
                    HomeTeam = Team(appContext, Team.ID_ELEPHANTS, true)
                    FieldId = fields[random.nextInt(fields.size)]
                },
                Game(id + 13, time).apply {
                    StartTime.add(Calendar.MONTH, 1)
                    StartTime.add(Calendar.DAY_OF_MONTH, 1)
                    AwayTeam = Team(appContext, Team.ID_TIME_EAGLES)
                    HomeTeam = Team(appContext, Team.ID_LANEW_BEARS, true)
                    FieldId = fields[random.nextInt(fields.size)]
                }
        )
    }

    @Test
    fun highlightListTest() {
        //2 games are live, show also yesterday game result
        val gameList1 = CpblCalendarHelper.getHighlightGameList(testData)
        Assert.assertNotNull(gameList1)
        gameList1?.forEachIndexed { index, game ->
            Log.d(TAG, "$index: ${game.Id} final=${game.IsFinal} live=${game.IsLive} ${game.displayDate}")
        }
        Assert.assertEquals(4, gameList1.size)
        Assert.assertEquals(503, gameList1[0].Id)
        Assert.assertEquals(502, gameList1[2].Id)

        //1 game today is complete, another game is still playing
        //first game complete
        testData[5].IsLive = false
        testData[5].IsFinal = true
        val gameList2 = CpblCalendarHelper.getHighlightGameList(testData)
        Assert.assertNotNull(gameList2)
        Assert.assertEquals(4, gameList2.size)
        Assert.assertEquals(504, gameList2[1].Id)
        Assert.assertEquals(501, gameList2[3].Id)

        //1 game today is complete, another game is still playing
        //second game complete
        testData[5].IsLive = true
        testData[5].IsFinal = false
        testData[6].IsLive = false
        testData[6].IsFinal = true
        val gameList3 = CpblCalendarHelper.getHighlightGameList(testData)
        Assert.assertNotNull(gameList3)
        Assert.assertEquals(4, gameList3.size)
        Assert.assertEquals(501, gameList3[3].Id)

        //no games live
        testData[5].IsLive = false
        testData[5].IsFinal = true
        val gameList4 = CpblCalendarHelper.getHighlightGameList(testData)
        Assert.assertNotNull(gameList4)
        gameList4?.forEach {
            Log.d(TAG, "${it.Id} final=${it.IsFinal} live=${it.IsLive} ${it.displayDate}")
        }
        Assert.assertEquals(3, gameList4.size)
        Assert.assertEquals(505, gameList4[2].Id)

        //remove tomorrow 1 game, it should load next month 2 games
        testData.removeAt(7)
        val gameList5 = CpblCalendarHelper.getHighlightGameList(testData)
        Assert.assertNotNull(gameList5)
        Assert.assertEquals(4, gameList5.size)
        Assert.assertEquals(511, gameList5[2].Id)
        Assert.assertEquals(512, gameList5[3].Id)

        //today has one game
        testData.removeAt(5)
        val gameList6 = CpblCalendarHelper.getHighlightGameList(testData)
        Assert.assertNotNull(gameList6)
        Assert.assertEquals(3, gameList6.size)
        Assert.assertEquals(511, gameList6[1].Id)
        Assert.assertEquals(512, gameList6[2].Id)
    }
}