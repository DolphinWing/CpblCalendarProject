@file:Suppress("PackageName")

package dolphin.android.apps.CpblCalendar3

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.PorterDuff
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.support.customtabs.CustomTabsIntent
import android.support.design.widget.Snackbar
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v4.view.ViewPager
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.text.Html
import android.util.Log
import android.view.*
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.*
import dolphin.android.apps.CpblCalendar.Utils
import dolphin.android.apps.CpblCalendar.preference.PreferenceUtils
import dolphin.android.apps.CpblCalendar.provider.CpblCalendarHelper
import dolphin.android.apps.CpblCalendar.provider.Game
import dolphin.android.apps.CpblCalendar.provider.TeamHelper
import dolphin.android.util.DateUtils
import dolphin.android.util.PackageUtils
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.common.SmoothScrollLinearLayoutManager
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem
import eu.davidea.flexibleadapter.items.IFlexible
import eu.davidea.viewholders.FlexibleViewHolder
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class ListActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "ListActivity"
    }

    private lateinit var mFilterListPane: View
    private lateinit var mFilterControlPane: View
    private lateinit var mFilterControlBg: View
    private lateinit var mAdapter: SimplePageAdapter

    private lateinit var mSpinnerYear: Spinner
    private lateinit var mSpinnerMonth: Spinner
    private lateinit var mSpinnerField: Spinner
    private lateinit var mSpinnerTeam: Spinner
    private lateinit var mTextViewYear: TextView
    private lateinit var mTextViewField: TextView
    private lateinit var mTextViewTeam: TextView

    private lateinit var helper: CpblCalendarHelper
    private lateinit var teamHelper: TeamHelper
    //    private val mAllGamesCache = SparseArray<List<Game>>()
    private var mYear: Int = 2018
    private var mMonth: Int = Calendar.MAY
    private lateinit var viewModel: GameViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list)

        helper = CpblCalendarHelper(this)
        teamHelper = TeamHelper(application as CpblApplication)
        viewModel = ViewModelProviders.of(this).get(GameViewModel::class.java)

        findViewById<Toolbar>(R.id.toolbar)?.apply { setSupportActionBar(this) }

        val tabLayout: TabLayout = findViewById(R.id.tab_layout)
        val pager: ViewPager = findViewById(R.id.viewpager)

        //prepare filter pane
        mSpinnerYear = findViewById(R.id.spinner3)
        mSpinnerYear.adapter = CpblCalendarHelper.buildYearAdapter(this, mYear)
        mSpinnerYear.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) {
                //won't happen
            }

            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                if (p2 == 0) {
                    mSpinnerTeam.isEnabled = true
                } else {
                    mSpinnerTeam.isEnabled = false
                    mSpinnerTeam.setSelection(0)
                }
            }
        }
        mSpinnerMonth = findViewById(R.id.spinner4)
        mSpinnerField = findViewById(R.id.spinner1)
        mSpinnerTeam = findViewById(R.id.spinner2)
        mSpinnerTeam.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item,
                arrayOf(getString(R.string.title_favorite_teams_all),
                        *resources.getStringArray(R.array.cpbl_team_name))).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        mTextViewYear = findViewById(android.R.id.button1)
        mTextViewYear.setOnClickListener { filterPaneVisible = true }
        mTextViewField = findViewById(android.R.id.button2)
        mTextViewField.setOnClickListener { filterPaneVisible = true }
        mTextViewTeam = findViewById(android.R.id.button3)
        mTextViewTeam.setOnClickListener { filterPaneVisible = true }

        mFilterListPane = findViewById(R.id.filter_list_pane)
        //mFilterListPane.setOnClickListener { filterPaneVisible = !filterPaneVisible }
        mFilterControlBg = findViewById(R.id.filter_control_background)
        mFilterControlBg.setOnClickListener { filterPaneVisible = false }
        mFilterControlPane = findViewById(R.id.filter_control_pane)
        findViewById<View>(R.id.filter_view_pane)?.setOnClickListener {
            restoreFilter()
            filterPaneVisible = !filterPaneVisible
        }
        filterPaneVisible = false //mFilterControlPane.visibility == View.VISIBLE

        findViewById<View>(android.R.id.custom)?.setOnClickListener {
            //hide filter panel
            filterPaneVisible = false
            //start fetch, page change will cause fetch actions
            if (pager.currentItem != mSpinnerMonth.selectedItemPosition) {
                pager.currentItem = mSpinnerMonth.selectedItemPosition
            } else {
                doQueryAction()
            }
        }

        //prepare month list
        val months = ArrayList(Arrays.asList(*DateFormatSymbols(Locale.TAIWAN).months))
        months.removeAt(months.size - 1) //no December games
        months.removeAt(0) //no January games
        //months.removeAt(0)
        months.forEach { tabLayout.addTab(tabLayout.newTab().setText(it)) }
        mAdapter = SimplePageAdapter(this, months)
        pager.adapter = mAdapter
        pager.addOnPageChangeListener(TabLayout.TabLayoutOnPageChangeListener(tabLayout))
        pager.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                mMonth = position + 1
                runOnUiThread { mSpinnerMonth.setSelection(position) }
                Log.d(TAG, "selected month = ${mMonth + 1}")
                doQueryAction()
            }
        })
        //pager.currentItem = mMonth - 1
        tabLayout.addOnTabSelectedListener(TabLayout.ViewPagerOnTabSelectedListener(pager))
        mSpinnerMonth.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item,
                months).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        mSpinnerMonth.setSelection(pager.currentItem)
        Handler().postDelayed({ pager.currentItem = mMonth - 1 }, 500)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_list, menu)
        return true //super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.action_leader_board -> {
                //https://goo.gl/GtBKgp
                val builder = CustomTabsIntent.Builder()
                        .setToolbarColor(ContextCompat.getColor(this, R.color.holo_green_dark))
                val customTabsIntent = builder.build()
                customTabsIntent.launchUrl(this, Utils.LEADER_BOARD_URI)
                return true
            }
            R.id.action_cache_mode -> {
                PreferenceUtils.setCacheMode(this@ListActivity, true)
                startActivity(Intent(this@ListActivity, CacheModeListActivity::class.java).apply {
                    putExtra("cache_init", true)
                })
                finish()
                return true
            }
            R.id.action_go_to_cpbl -> {
                CpblCalendarHelper.startActivityToCpblSchedule(this@ListActivity, mYear,
                        mMonth + 1, "01", "F00")
                return true
            }
            R.id.action_settings -> {
                startActivityForResult(Intent(this, SettingsActivity3::class.java), 0)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private var filterPaneVisible: Boolean = false
        set(value) {
            runOnUiThread { showFilterPane(value) }
            field = value
        }

    private fun restoreFilter() {
        runOnUiThread {
            mSpinnerYear.setSelection(CpblCalendarHelper.getNowTime().get(Calendar.YEAR) - mYear)
            mSpinnerMonth.setSelection(mMonth - 1)
        }
    }

    private fun showFilterPane(visible: Boolean) {
        mFilterControlPane.apply {
            if (visible) {
                this.animate()
                        .translationY(0f)
                        .withStartAction {
                            //visibility = View.VISIBLE
                            mFilterControlBg.visibility = View.VISIBLE
                            mFilterListPane.visibility = View.INVISIBLE
                        }
                        //.withEndAction { }
                        .setInterpolator(AccelerateInterpolator())
                        .start()

            } else {
                this.animate()
                        .translationY(-bottom.toFloat())
                        .setInterpolator(DecelerateInterpolator())
                        //.withStartAction { }
                        .withEndAction {
                            //visibility = View.GONE
                            mFilterListPane.visibility = View.VISIBLE
                            mFilterControlBg.visibility = View.GONE
                        }
                        .start()
            }
        }
    }

    private var selectedFieldId: String = "F00"
        get() {
            val f = mSpinnerField.selectedItemPosition
            return resources.getStringArray(R.array.cpbl_game_field_id)[f]
        }

    private var selectedTeamId: Int = 0
        get() {
            val t = mSpinnerTeam.selectedItemPosition
            val team = if (t == 0) "0" else resources.getStringArray(R.array.cpbl_team_id)[t - 1]
            return team.toInt()
        }

    private fun doQueryAction() {
        val y = mSpinnerYear.selectedItemPosition
        val year = CpblCalendarHelper.getNowTime().get(Calendar.YEAR) - y
        val month = mSpinnerMonth.selectedItemPosition + 1
        Log.d(TAG, "do query action to $year/${month + 1}")
        if (year != mYear) {//clear all months
            Log.d(TAG, "clear $mYear data")
            for (i in 1..10) {//Feb. to Nov.
                mAdapter.getChildFragment(i - 1)?.arguments = Bundle().apply {
                    //putInt("year", mYear)
                    //putInt("month", i)
                    putBoolean("clear", true)
                }
            }
        }

        mYear = year
        //show loading
        mAdapter.getChildFragment(month - 1)?.let {
            it.arguments = Bundle().apply {
                Log.d(TAG, "notify fragment to update $year/${month + 1} $selectedFieldId")
                putBoolean("refresh", true)
                putInt("year", year)
                putInt("month", month)
                putString("field_id", selectedFieldId)
                putInt("team_id", selectedTeamId)
            }
        }

        mTextViewYear.text = mSpinnerYear.selectedItem.toString()
        mTextViewField.text = mSpinnerField.selectedItem.toString()
        mTextViewTeam.text = mSpinnerTeam.selectedItem.toString()
    }

    private var snackbar: Snackbar? = null

    private fun doWebQuery(newYear: Int = mYear, newMonth: Int = mMonth) {
        Log.d(TAG, "start fetch $newYear/${newMonth + 1}")
        viewModel.fetch(helper, newYear, newMonth, clearCached = true)
        mAdapter.getChildFragment(newMonth - 1)?.arguments = Bundle().apply {
            putInt("year", newYear)
            putInt("month", newMonth)
            putBoolean("refresh", true)
            putString("field_id", selectedFieldId)
            putInt("team_id", selectedTeamId)
        }
    }

    private fun showSnackBar(visible: Boolean, text: String? = null) {
        if (visible && text != null) {
            if (snackbar != null) {
                snackbar!!.setText(text)
            } else {
                snackbar = Snackbar.make(findViewById<View>(R.id.main_content_frame), text,
                        Snackbar.LENGTH_INDEFINITE)
            }
            snackbar!!.show()
        } else {
            snackbar?.dismiss()
            snackbar = null
        }
    }

    class SimplePageAdapter(a: AppCompatActivity, private var months: ArrayList<String>) :
            WizardPager.PagerAdapter(a.supportFragmentManager) {
        override fun getItem(position: Int): Fragment {
            return MonthViewFragment().apply {
                arguments = Bundle().apply {
                    putInt("index", position)
                    putString("title", months[position])
                }
            }
        }

        override fun getCount() = months.size
    }

    internal class MonthViewFragment : Fragment(), FlexibleAdapter.OnItemClickListener,
            FlexibleAdapter.OnItemLongClickListener {

        private var container: SwipeRefreshLayout? = null
        private lateinit var list: RecyclerView
        private var index = -1
        private lateinit var cpblHelper: CpblCalendarHelper
        private lateinit var helper: TeamHelper
        private lateinit var viewModel: GameViewModel
        private var year = 2018
        private var month = Calendar.MAY

        override fun setArguments(args: Bundle?) {
            super.setArguments(args)
            if (args?.containsKey("index") == true) {
                index = args.getInt("index", -1)
                Log.d(TAG, "fragment index = $index")
            }
            if (args?.containsKey("title") == true) {
                Log.d(TAG, "title: ${args.getString("title")}")
            }
            if (args?.getBoolean("refresh", false) == true) {
                year = args.getInt("year", 2018)
                month = args.getInt("month", 0)
                val fieldId = args.getString("field_id", "F00")
                val teamId = args.getInt("team_id", 0)
                startRefreshing(true, year, month)
                Log.d(TAG, "refreshing $year/${month + 1} @$fieldId $teamId")
                viewModel.fetch(cpblHelper, year, month)?.observe(this,
                        Observer<List<Game>> { updateAdapter(it, helper, fieldId, teamId) })
                //} else {
                //    startRefreshing(false)
            }
            if (args?.getBoolean("clear", false) == true) {
                Log.d(TAG, "clear adapter")
                updateAdapter(null, helper)
            }
        }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            //Log.d(TAG, "Fragment onCreate")
            viewModel = ViewModelProviders.of(activity!!).get(GameViewModel::class.java)
            cpblHelper = CpblCalendarHelper(activity!!)
            helper = TeamHelper(activity!!.application as CpblApplication)
        }

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            val view = inflater.inflate(R.layout.fragment_recycler_view, container, false)
            this.container = view.findViewById(R.id.swipeRefreshLayout)
            this.container!!.isRefreshing = true
            this.container!!.setOnRefreshListener {
                Log.d(TAG, "pull to refresh")
                if (activity is ListActivity) {
                    (activity as ListActivity).doWebQuery()
                }
            }
            this.list = view.findViewById(android.R.id.list)
            this.list.layoutManager = SmoothScrollLinearLayoutManager(activity)
            return view //super.onCreateView(inflater, container, savedInstanceState)
        }

        private fun startRefreshing(enabled: Boolean, year: Int = 2018, monthOfJava: Int = 0) {
            if (enabled) {
                this.container?.isEnabled = true
                this.container?.isRefreshing = true
                if (activity is ListActivity) {
                    (activity as ListActivity).showSnackBar(true,
                            getString(R.string.title_download_from_cpbl, year, monthOfJava + 1))
                }
            } else {
                if (activity is ListActivity) {
                    (activity as ListActivity).showSnackBar(false)
                }
                this.container?.isRefreshing = false
                this.container?.isEnabled = PreferenceUtils.isPullToRefreshEnabled(activity)
            }
        }

        private val adapterList = ArrayList<MyItemView>()
        private fun updateAdapter(list: List<Game>? = null, helper: TeamHelper,
                                  field: String = "F00", team: Int = 0) {
            Log.d(TAG, "we have ${list?.size} games in $month (page=$index)")
            if (activity == null) return
            //val adapterList = ArrayList<MyItemView>()
            adapterList.clear()
            list?.forEach {
                if ((field == "F00" || it.FieldId == field) &&
                        (team == 0 || it.HomeTeam.id == team || it.AwayTeam.id == team)) {
                    adapterList.add(MyItemView(activity!!, it, helper))
                }
            }
            Log.d(TAG, "field = $field, ${adapterList.size} games")
            this.list.adapter = ItemAdapter(adapterList, this)
            this.list.setHasFixedSize(true)
            if (list != null)
                for (i in 0 until list.size) {
                    if (DateUtils.isToday(list[i].StartTime)) {
                        this.list.scrollToPosition(i)
                        break//break when the upcoming game if found
                    } else if (!list[i].IsFinal) {
                        this.list.scrollToPosition(i)
                        break//break when the upcoming game if found
                    }
                }
            startRefreshing(false)
        }

        override fun onItemClick(view: View?, position: Int): Boolean {
            val game = adapterList[position].game
            Log.d(TAG, "onItemClick $position ${game.Id}")
            if (game.Url != null && activity != null) {
                Utils.startGameActivity(activity, game)
                return true
            }
            return false
        }

        override fun onItemLongClick(position: Int) {
            val game = adapterList[position].game
            Log.d(TAG, "onItemLongClick $position ${game.Id}")
            if (!game.IsFinal && !game.IsLive && activity != null) {
                AlertDialog.Builder(activity!!)
                        .setItems(arrayOf(getString(R.string.action_add_to_calendar),
                                getString(R.string.action_show_field_info)), { _, i ->
                            Log.d(TAG, "selected $i")
                            when (i) {
                                0 -> {
                                    val calIntent = Utils.createAddToCalendarIntent(activity, game)
                                    if (PackageUtils.isCallable(activity, calIntent)) {
                                        startActivity(calIntent)
                                    }
                                }
                                1 -> {
                                    Utils.startBrowserActivity(activity,
                                            CpblCalendarHelper.URL_FIELD_2017.replace("@field", game.FieldId))
                                }
                            }
                        }).show()
            }
        }
    }

    internal class MyItemView(private val context: Context, val game: Game,
                              private val helper: TeamHelper) :
            AbstractFlexibleItem<MyItemView.ViewHolder>() {
        override fun bindViewHolder(adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>?,
                                    holder: ViewHolder?, position: Int, payloads: MutableList<Any>?) {
            val dateStr = SimpleDateFormat("MMM d (E) ", Locale.TAIWAN)
                    .format(game.StartTime.time)
            val timeStr = SimpleDateFormat("HH:mm", Locale.TAIWAN)
                    .format(game.StartTime.time)
            val year = game.StartTime.get(Calendar.YEAR)

            holder?.apply {
                gameId?.text = when (game.Kind) {
                    "01" -> game.Id.toString()
                    "02" -> context.getString(R.string.id_prefix_all_star, game.Id)
                    "03" -> context.getString(R.string.id_prefix_champion, game.Id)
                    "05" -> context.getString(R.string.id_prefix_challenge, game.Id)
                    "07" -> context.getString(R.string.id_prefix_warm_up, game.Id)
                    else -> game.Id.toString()
                }
                val displayTime = when {
                    game.IsFinal -> dateStr
                    game.IsLive -> "$dateStr&nbsp;&nbsp;${game.LiveMessage}"
                    else -> dateStr + timeStr
                }
                gameTime?.text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    Html.fromHtml(displayTime, Html.TO_HTML_PARAGRAPH_LINES_INDIVIDUAL)
                } else {
                    Html.fromHtml(displayTime)
                }
                gameField?.text = if (game.Source == Game.SOURCE_CPBL ||
                        !game.Field.contains(context.getString(R.string.title_at))) {
                    String.format("%s%s", context.getString(R.string.title_at),
                            game.getFieldFullName(context))
                } else {
                    game.getFieldFullName(context)
                }
                teamAwayName?.text = game.AwayTeam.name
                teamAwayScore?.text = if (game.IsFinal) game.AwayScore.toString() else "-"
                teamAwayLogo?.apply {
                    setImageResource(R.drawable.ic_baseball)
                    setColorFilter(helper.getLogoColorFilter(game.AwayTeam, year),
                            PorterDuff.Mode.SRC_IN)
                }
                teamHomeName?.text = game.HomeTeam.name
                teamHomeScore?.text = if (game.IsFinal) game.HomeScore.toString() else "-"
                teamHomeLogo?.apply {
                    setImageResource(R.drawable.ic_baseball)
                    setColorFilter(helper.getLogoColorFilter(game.HomeTeam, year),
                            PorterDuff.Mode.SRC_IN)
                }
                extraMessage?.apply {
                    if (game.DelayMessage.isNullOrEmpty()) {
                        visibility = View.GONE
                    } else {
                        visibility = View.VISIBLE
                        text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            Html.fromHtml(game.DelayMessage, Html.TO_HTML_PARAGRAPH_LINES_INDIVIDUAL)
                        } else {
                            Html.fromHtml(game.DelayMessage)
                        }
                    }

                }
                liveChannel?.apply {
                    visibility = if (game.IsFinal || game.Channel.isNullOrEmpty())
                        View.GONE else View.VISIBLE
                    text = game.Channel
                }
            }
        }

        override fun hashCode(): Int {
            return game.StartTime.timeInMillis.hashCode() + game.Id.hashCode()
        }

        override fun getLayoutRes() = R.layout.recyclerview_item_matchup_card

        override fun equals(other: Any?) = if (other is MyItemView) other == this else false

        override fun createViewHolder(view: View?,
                                      adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>?): ViewHolder {
            return ViewHolder(view, adapter)
        }

        class ViewHolder(view: View?, adapter: FlexibleAdapter<out IFlexible<*>>?) :
                FlexibleViewHolder(view, adapter) {
            val gameId: TextView? = view?.findViewById(R.id.textView9)
            val gameTime: TextView? = view?.findViewById(R.id.textView1)
            val gameField: TextView? = view?.findViewById(R.id.textView7)
            val teamAwayLogo: ImageView? = view?.findViewById(android.R.id.icon1)
            val teamAwayName: TextView? = view?.findViewById(R.id.textView2)
            val teamAwayScore: TextView? = view?.findViewById(R.id.textView3)
            val teamHomeLogo: ImageView? = view?.findViewById(android.R.id.icon2)
            val teamHomeName: TextView? = view?.findViewById(R.id.textView5)
            val teamHomeScore: TextView? = view?.findViewById(R.id.textView4)
            val extraMessage: TextView? = view?.findViewById(R.id.textView8)
            val liveChannel: TextView? = view?.findViewById(R.id.textView6)

            init {
                view?.findViewById<View>(R.id.item_control_pane)?.visibility = View.INVISIBLE
            }
        }
    }

    internal class ItemAdapter(items: MutableList<MyItemView>?, listener: Any? = null)
        : FlexibleAdapter<MyItemView>(items, listener)
}
