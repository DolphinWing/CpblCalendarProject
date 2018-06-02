package dolphin.android.apps.CpblCalendar3

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import dolphin.android.apps.CpblCalendar.provider.CpblCalendarHelper
import dolphin.android.apps.CpblCalendar.provider.Game
import java.util.*
import kotlin.collections.ArrayList

class HighlightActivity3 : HighlightActivity() {
    companion object {
        private const val TAG = "HighlightActivity"
        private const val DEBUG_DATA = false
    }

    private lateinit var helper: CpblCalendarHelper
    private lateinit var viewModel: GameViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        //download data will be called in HighlightActivity#onCreate, so we have to init first
        helper = CpblCalendarHelper(this)
        viewModel = ViewModelProviders.of(this).get(GameViewModel::class.java)
        viewModel.debugMode = DEBUG_DATA
        super.onCreate(savedInstanceState)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.action_settings -> {
                startActivityForResult(Intent(this@HighlightActivity3,
                        SettingsActivity3::class.java), 0)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    @Suppress("ConstantConditionIf")
    override fun downloadCalendar() {
        val now = CpblCalendarHelper.getNowTime()
        val year = if (DEBUG_DATA) 2018 else now.get(Calendar.YEAR)
        val monthOfJava = if (DEBUG_DATA) 5 else now.get(Calendar.MONTH)
        Log.d(TAG, "download calendar")
        val list = ArrayList<Game>()
        tryShowSnackbar(getString(R.string.title_download_from_cpbl, year, monthOfJava + 1))
        viewModel.fetch(year, monthOfJava)?.observe(this,
                Observer<List<Game>> {
                    //check today and if we have games before and after
                    if (it?.isNotEmpty() == true) {
                        list.addAll(it)
                        if (it.first().StartTime.after(now) && monthOfJava > Calendar.JANUARY) {
                            //get last month data
                            tryShowSnackbar(getString(R.string.title_download_from_cpbl, year, monthOfJava))
                            fetchDataForPreviousMonth(year, monthOfJava - 1, list)
                            return@Observer
                        } else if (it.last().StartTime.before(now)) {//already started
                            //check if it is final
                            var more = it.last().IsFinal
                            if (it.size > 1 && it.last().StartTime == it.dropLast(1).last().StartTime) {
                                more = more or it.dropLast(1).last().IsFinal
                            }
                            if (more && monthOfJava < Calendar.DECEMBER) {
                                tryShowSnackbar(getString(R.string.title_download_from_cpbl, year, monthOfJava + 2))
                                fetchDataForNextMonth(year, monthOfJava + 1, list)
                                return@Observer
                            }
                        }
                    }
                    downloadExtraCards(list)
                })
    }

    private fun downloadExtraCards(list: ArrayList<Game>) {
        tryShowSnackbar(getString(R.string.title_download_complete))
        val config = FirebaseRemoteConfig.getInstance()
        val gameList = CpblCalendarHelper.getHighlightGameList(list)
        gameList.addAll(0, GameCardAdapter.getAnnouncementCards(this, config))
        GameCardAdapter.getNewVersionCard(this, config)?.let { gameList.add(0, it) }
        updateViews(gameList)
    }

    private fun fetchDataForPreviousMonth(year: Int, monthOfJava: Int, list: ArrayList<Game>) {
        viewModel.fetch(year, monthOfJava)?.observe(this@HighlightActivity3,
                Observer<List<Game>> {
                    if (it?.isNotEmpty() == true) {
                        list.addAll(0, it)
                    }
                    downloadExtraCards(list)
                })
    }

    private fun fetchDataForNextMonth(year: Int, monthOfJava: Int, list: ArrayList<Game>) {
        viewModel.fetch(year, monthOfJava)?.observe(this@HighlightActivity3,
                Observer<List<Game>> {
                    if (it?.isNotEmpty() == true) {
                        list.addAll(it)
                    }
                    downloadExtraCards(list)
                })
    }

    override fun startGameListActivity(list: ArrayList<Game>?) {
        //super.startGameListActivity(list)
        val intent = Intent(this, ListActivity::class.java)
        intent.putParcelableArrayListExtra(KEY_CACHE, list)
        //Log.d(TAG, String.format("list %d", mCacheGames.size()));
        startActivity(intent)
    }
}