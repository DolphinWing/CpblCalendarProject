package dolphin.android.apps.CpblCalendar3

import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import dolphin.android.apps.CpblCalendar.Utils
import dolphin.android.apps.CpblCalendar.provider.CpblCalendarHelper
import dolphin.android.apps.CpblCalendar.provider.Game
import dolphin.android.util.PackageUtils
import eu.davidea.flexibleadapter.common.FlexibleItemAnimator
import eu.davidea.flexibleadapter.common.SmoothScrollLinearLayoutManager
import java.util.*

class HighlightViewFragment : Fragment() {
    companion object {
        private const val TAG = "HighlightViewFragment"

        const val MSG_LAUNCH_URL = 65101
    }

    private lateinit var viewModel: GameViewModel
    private lateinit var config: FirebaseRemoteConfig
    private var mSwipeRefreshLayout: SwipeRefreshLayout? = null
    private var mRecyclerView: FixedRecyclerView? = null
    private var year: Int = 2018
    private var monthOfJava: Int = Calendar.JUNE

    override fun setArguments(args: Bundle?) {
        super.setArguments(args)
        if (args?.containsKey("refresh") == true) {
            val enabled = args.getBoolean("refresh", false)
            mSwipeRefreshLayout?.isRefreshing = enabled
            mSwipeRefreshLayout?.isEnabled = enabled
        }
        if (args?.containsKey("year") == true && args.containsKey("month")) {
            year = args.getInt("year", 2018)
            monthOfJava = args.getInt("month", Calendar.JUNE)
            val cached = args.getBoolean("cache", false)
            Log.d(TAG, "prepare $year/${monthOfJava + 1} cache=$cached")
            //may not yet attach to activity
            Handler().postDelayed(Runnable { prepareData(year, monthOfJava, cached) },
                    if (activity == null) 800 else 500)
        }
    }

    private fun showSnackBar(message: String? = null, visible: Boolean = true) {
        (activity as? ListActivity)?.showSnackBar(message, visible)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(activity!!,
                ViewModelProvider.AndroidViewModelFactory(activity!!.application))
                .get(GameViewModel::class.java)
        config = FirebaseRemoteConfig.getInstance()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val contentView = inflater.inflate(R.layout.fragment_highlight, container, false)
        mSwipeRefreshLayout = contentView.findViewById(R.id.bottom_sheet_option1)
        mRecyclerView = contentView.findViewById(R.id.bottom_sheet)
        mSwipeRefreshLayout?.isRefreshing = true
        mRecyclerView?.apply {
            layoutManager = SmoothScrollLinearLayoutManager(activity)
            setHasFixedSize(true)
        }
        return contentView
    }

    val isRefreshing: Boolean
        get() = mSwipeRefreshLayout?.isRefreshing ?: false

    private fun prepareData(year: Int, monthOfJava: Int, clearCached: Boolean = false) {
        showSnackBar(getString(R.string.title_download_from_cpbl, year, monthOfJava + 1))

        val list = ArrayList<Game>()
        if (config.getBoolean("enable_highlight_no_games")) {
            prepareExtraCards(list) //directly load extra cards
            return
        }

        val now = CpblCalendarHelper.getNowTime()
        viewModel.fetch(year, monthOfJava, clearCached = clearCached)?.observe(this,
                androidx.lifecycle.Observer { resources ->
                    Log.d(TAG, "progress = ${resources.progress}")
                    when {
                        resources.progress == 98 ->
                            showSnackBar(getString(R.string.title_download_complete))
                        resources.progress < 90 ->
                            showSnackBar(resources.messages +
                                    getString(R.string.title_download_from_cpbl, year,
                                            monthOfJava + 1))
                        else -> {
                            resources.dataList?.let { dataList ->
                                Log.d(TAG, "data list size: ${dataList.size}")
                                if (dataList.isNotEmpty()) {//we have data
                                    list.addAll(dataList)
                                    //mSpinnerMonth.setSelection(monthOfJava - 1)
                                    //mPager.currentItem = monthOfJava - 1

                                    if (dataList.first().StartTime.after(now) &&
                                            monthOfJava > Calendar.JANUARY) {
                                        Log.d(TAG, "load previous month")//get previous month
                                        preparePreviousMonth(list, year, monthOfJava - 1)
                                        return@Observer
                                    } else if (dataList.last().StartTime.before(now)) {//started
                                        //check if it is final
                                        var more = dataList.last().IsFinal
                                        if (dataList.size > 1 &&
                                                dataList.last().StartTime ==
                                                dataList.dropLast(1).last().StartTime) {
                                            more = more or dataList.dropLast(1).last().IsFinal
                                        }
                                        if (more && monthOfJava < Calendar.DECEMBER) {
                                            Log.d(TAG, "load next month")
                                            prepareNextMonth(list, year, monthOfJava + 1)
                                            return@Observer
                                        }
                                    }
                                } //maybe we have some extra messages
                            }
                            prepareExtraCards(list)
                        }
                    }
                })
    }

    private fun preparePreviousMonth(list: ArrayList<Game>, year: Int, previousMonth: Int) {
        showSnackBar(getString(R.string.title_download_from_cpbl, year, previousMonth + 1))
        viewModel.fetch(year, previousMonth)
                ?.observe(this, androidx.lifecycle.Observer { resources ->
                    if (resources.progress < 100) {
                        Log.d(TAG, "download $year/${previousMonth + 1} ${resources.messages}")
                    } else {
                        resources.dataList?.let { dataList ->
                            if (dataList.isNotEmpty()) {
                                list.addAll(0, dataList)
                            }
                        }
                        prepareExtraCards(list)
                    }
                })
    }

    private fun prepareNextMonth(list: ArrayList<Game>, year: Int, nextMonth: Int) {
        showSnackBar(getString(R.string.title_download_from_cpbl, year, nextMonth + 1))
        viewModel.fetch(year, nextMonth)?.observe(this, androidx.lifecycle.Observer { resources ->
            if (resources.progress < 100) {
                Log.d(TAG, "download $year/${nextMonth + 1} ${resources.messages}")
            } else {
                resources.dataList?.let { dataList ->
                    if (dataList.isNotEmpty()) {
                        list.addAll(dataList)
                    }
                }
                prepareExtraCards(list)
            }
        })
    }

    private fun prepareExtraCards(list: ArrayList<Game>) {
        showSnackBar(getString(R.string.title_download_complete))
        val gameList = CpblCalendarHelper.getHighlightGameList(list)
        if (viewModel.debugMode) {
            gameList.add(0, Game(-2).apply { LiveMessage = "debug announcement 2" })
            gameList.add(0, Game(-2).apply { LiveMessage = "debug announcement 1" })
            gameList.add(0, Game(-3).apply { LiveMessage = "update card" })
        } else {
            gameList.addAll(0, getAnnouncementCards(config))
            getNewVersionCard(config)?.let { gameList.add(0, it) }
        }
        updateHighlightList(gameList)
    }

    private fun getAnnouncementCards(config: FirebaseRemoteConfig): List<Game> {
        val list = ArrayList<Game>()
        val now = CpblCalendarHelper.getNowTime()
        val keys = config.getString("add_highlight_card")
        if (keys != null && keys.isNotEmpty()) {
            for (id in keys.split(";".toRegex()).toTypedArray()) {
                val msg = config.getString("add_highlight_card_$id")
                if (msg != null && id.length >= 6) {
                    val cal = CpblCalendarHelper.getNowTime()
                    cal.set(Calendar.YEAR, Integer.parseInt(id.substring(0, 4)))
                    cal.set(Calendar.MONTH, Integer.parseInt(id.substring(4, 5)))
                    cal.set(Calendar.DAY_OF_MONTH, Integer.parseInt(id.substring(5, 6)))
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    if (cal.before(now)) {
                        continue//check if the announcement is expired
                    }

                    list.add(0, Game(HighlightCardAdapter.TYPE_ANNOUNCEMENT, cal).apply {
                        LiveMessage = msg
                    })
                }
            }
        }
        return list
    }

    private fun getNewVersionCard(config: FirebaseRemoteConfig): Game? {
        val info = PackageUtils.getPackageInfo(activity!!, ListActivity::class.java)
        val versionCode: Long = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info?.longVersionCode //see
        } else {
            info?.versionCode?.toLong()
        } ?: Long.MAX_VALUE
        val latestCode = config.getLong("latest_version_code")
        Log.v("CpblCalendar3", String.format("versionCode: %d, play: %d", versionCode, latestCode))
        if (versionCode < latestCode) {
            val summary: String? = config.getString("latest_version_summary")
            return Game(HighlightCardAdapter.TYPE_UPDATE_CARD).apply {
                LiveMessage = if (summary != null && summary.isNotEmpty())
                    summary
                else
                    getString(R.string.new_version_available_message)
            }
        }
        return null
    }

    private fun updateHighlightList(list: ArrayList<Game>) {
        hasHighlights = list.isNotEmpty()
        val adapter = HighlightCardAdapter(activity!!.application as CpblApplication, list,
                object : HighlightCardAdapter.OnCardClickListener {
                    override fun onOptionClick(view: View, game: Game, position: Int) {
                        Log.d(TAG, "on option click $position")
                        onOptionSelected(view.id, game)
                    }
                })

        mRecyclerView?.apply {
            this.adapter = adapter

            itemAnimator = object : FlexibleItemAnimator() {
                override fun preAnimateRemoveImpl(holder: RecyclerView.ViewHolder?): Boolean {
                    Log.d(TAG, "before removal animation")
                    holder?.itemView?.alpha = 1f
                    return true
                }

                override fun animateRemoveImpl(holder: RecyclerView.ViewHolder?, index: Int) {
                    Log.d(TAG, "animate removal $index")
                    ViewCompat.animate(holder!!.itemView)
                            .alpha(0f)
                            .setDuration(removeDuration)
                            .setInterpolator(DecelerateInterpolator())
                            .setListener(DefaultRemoveVpaListener(holder))
                            .withEndAction { adapter.notifyDataSetChanged() }
                            .start()
                }

//                override fun preAnimateAddImpl(holder: RecyclerView.ViewHolder?): Boolean {
//                    Log.d(TAG, "before add animation")
//                    holder?.itemView?.alpha = 0f
//                    return true
//                }
//
//                override fun animateAddImpl(holder: RecyclerView.ViewHolder?, index: Int) {
//                    Log.d(TAG, "animate add $index")
//                    ViewCompat.animate(holder!!.itemView)
//                            .alpha(1f)
//                            .setDuration(addDuration)
//                            .setInterpolator(DecelerateInterpolator())
//                            .setListener(DefaultAddVpaListener(holder))
//                            .start()
//                }
            }
//            itemAnimator!!.addDuration = 300
            itemAnimator!!.removeDuration = 300
        }
//        (activity as? ListActivity)?.updateHighlightList(list)
        showSnackBar(visible = false)
        mSwipeRefreshLayout?.isRefreshing = false
        mSwipeRefreshLayout?.isEnabled = false
        activity?.invalidateOptionsMenu()
        if (hasHighlights.not()) {//no data, directly show full list
            sendMessageToActivity(HighlightCardAdapter.TYPE_MORE_CARD)
        }
    }

    var hasHighlights: Boolean = false
        private set

    private fun onOptionSelected(viewId: Int, game: Game) {
//        (activity as? ListActivity)?.onOptionSelected(viewId, game)
        when (viewId) {
            R.id.card_option1 ->
                if (game.Id != HighlightCardAdapter.TYPE_MORE_CARD) {
                    Log.d(TAG, "start game ${game.Id} from ${game.Url}")
                    //Utils.startGameActivity(this@ListActivity, game)
                    sendMessageToActivity(MSG_LAUNCH_URL, game)
                } else {
                    sendMessageToActivity(HighlightCardAdapter.TYPE_MORE_CARD)
                }
            R.id.card_option2 ->
                if (game.Id != HighlightCardAdapter.TYPE_UPDATE_CARD) {
                    if (game.IsLive) {//refresh cards
                        prepareData(year, monthOfJava, true)
                    } else if (activity != null) {//register in calendar app
                        val calIntent = Utils.createAddToCalendarIntent(activity, game)
                        if (PackageUtils.isCallable(activity!!, calIntent)) {
                            startActivity(calIntent)
                        }
                    }
                } else {
                    sendMessageToActivity(HighlightCardAdapter.TYPE_MORE_CARD)
                }
            R.id.card_option3 -> {
                Log.d(TAG, "dismiss card, maybe we should remember it")
            }
        }
    }

    private fun sendMessageToActivity(what: Int, game: Game? = null) {
        (activity as? ListActivity)?.onHighlightFragmentUpdate(what, game)
    }
}