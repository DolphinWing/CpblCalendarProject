@file:Suppress("PackageName")

package dolphin.android.apps.CpblCalendar3

import android.animation.ValueAnimator
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.browser.customtabs.CustomTabsIntent
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import androidx.fragment.app.Fragment
import androidx.core.content.ContextCompat
import androidx.viewpager.widget.ViewPager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.graphics.drawable.DrawerArrowDrawable
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.appcompat.widget.Toolbar
import android.text.Html
import android.util.Log
import android.view.*
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.*
import cn.carbswang.android.numberpickerview.library.NumberPickerView
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
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
    private lateinit var mPager: androidx.viewpager.widget.ViewPager
    private lateinit var mAdapter: SimplePageAdapter
    private lateinit var mTabLayout: com.google.android.material.tabs.TabLayout

    private lateinit var mPickerYear: NumberPickerView
    private lateinit var mPickerMonth: NumberPickerView
    private lateinit var mPickerField: NumberPickerView
    private lateinit var mPickerTeam: NumberPickerView
    private lateinit var mTextViewYear: TextView
    private lateinit var mTextViewField: TextView
    private lateinit var mTextViewTeam: TextView

    private lateinit var mSwipeRefreshLayout: androidx.swiperefreshlayout.widget.SwipeRefreshLayout
    private lateinit var mBottomSheetBehavior: com.google.android.material.bottomsheet.BottomSheetBehavior<View>
    private lateinit var mHomeIcon: DrawerArrowDrawable

    private var mYear: Int = 2018
    private var mMonth: Int = Calendar.MAY
    private lateinit var viewModel: GameViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list)

        val now = CpblCalendarHelper.getNowTime()
        viewModel = ViewModelProviders.of(this).get(GameViewModel::class.java)
        viewModel.debugMode = false

        mHomeIcon = DrawerArrowDrawable(this).apply { color = Color.WHITE }
        findViewById<Toolbar>(R.id.toolbar)?.apply {
            setSupportActionBar(this)
            navigationIcon = mHomeIcon
        }
//        supportActionBar?.apply {
//            setDisplayHomeAsUpEnabled(true)
//            setDisplayShowHomeEnabled(true)
//            setHomeAsUpIndicator(R.drawable.ic_action_filter_list)
//        }

        mTabLayout = findViewById(R.id.tab_layout)
        mPager = findViewById(R.id.viewpager)

        //prepare filter pane
        mTextViewYear = findViewById(android.R.id.button1)
        mTextViewYear.setOnClickListener { filterPaneVisible = true }
        mTextViewField = findViewById(android.R.id.button2)
        mTextViewField.setOnClickListener { filterPaneVisible = true }
        mTextViewTeam = findViewById(android.R.id.button3)
        mTextViewTeam.setOnClickListener { filterPaneVisible = true }

        mPickerYear = findViewById(android.R.id.text1)
        mPickerMonth = findViewById(android.R.id.text2)
        mPickerField = findViewById(android.R.id.icon1)
        mPickerTeam = findViewById(android.R.id.icon2)

        mPickerYear.apply {
            val year = now.get(Calendar.YEAR)
            displayedValues = Array(year - 1989, {
                getString(R.string.title_cpbl_year, it + 1) + " (${1990 + it})"
            })
            minValue = 1
            maxValue = year - 1989
            value = maxValue
        }
        mPickerTeam.apply {
            displayedValues = arrayOf(getString(R.string.title_favorite_teams_all),
                    getString(R.string.team_ct_elephants_short2),
                    getString(R.string.team_711_lions_short2),
                    getString(R.string.team_fubon_guardians_short),
                    getString(R.string.team_lamigo_monkeys_short))
            //*resources.getStringArray(R.array.cpbl_team_name))
            minValue = -1
            maxValue = 3
        }
        findViewById<View>(android.R.id.icon)?.setOnClickListener { restoreFilter() }

        mFilterListPane = findViewById(R.id.filter_list_pane)
        //mFilterListPane.setOnClickListener { filterPaneVisible = !filterPaneVisible }
        mFilterControlBg = findViewById(R.id.filter_control_background)
//        mFilterControlBg.setOnClickListener { filterPaneVisible = false }
        mFilterControlBg.setOnTouchListener { _, _ -> true }
        mFilterControlPane = findViewById(R.id.filter_control_pane)
//        findViewById<View>(R.id.filter_view_pane)?.setOnClickListener {
//            restoreFilter()
//            filterPaneVisible = !filterPaneVisible
//        }
        //filterPaneVisible = false //mFilterControlPane.visibility == View.VISIBLE
        mFilterControlPane.setOnTouchListener { _, _ -> true }
        findViewById<View>(android.R.id.custom)?.setOnClickListener {
            //hide filter panel
            filterPaneVisible = false
            //start fetch, page change will cause fetch actions
            if (mPager.currentItem != mPickerMonth.value - 1) {
                mPager.currentItem = mPickerMonth.value - 1
            } else {
                doQueryAction() //page is the same, request the data from view model
            }
        }

        //prepare month list
        val months = ArrayList(Arrays.asList(*DateFormatSymbols(Locale.TAIWAN).months))
        months.dropLast(1) //no December games
        months.removeAt(0) //no January games
        //months.removeAt(0)
        months.forEach { mTabLayout.addTab(mTabLayout.newTab().setText(it)) }
        mAdapter = SimplePageAdapter(this, months)
        mPager.adapter = mAdapter
        mPager.addOnPageChangeListener(com.google.android.material.tabs.TabLayout.TabLayoutOnPageChangeListener(mTabLayout))
        mPager.addOnPageChangeListener(object : androidx.viewpager.widget.ViewPager.SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                mMonth = position + 1
                runOnUiThread {
                    //                    mSpinnerMonth.setSelection(position)
                    mPickerMonth.value = mMonth
                }
                Log.d(TAG, "selected month = ${mMonth + 1}")
                doQueryAction()
            }
        })
        //pager.currentItem = mMonth - 1
        mTabLayout.addOnTabSelectedListener(com.google.android.material.tabs.TabLayout.ViewPagerOnTabSelectedListener(mPager))

        mPickerMonth.apply {
            displayedValues = Array(months.size, { months[it] })
            minValue = Calendar.FEBRUARY
            maxValue = Calendar.NOVEMBER
            value = mMonth
        }

        //Handler().postDelayed({ pager.currentItem = mMonth - 1 }, 500)
        mSwipeRefreshLayout = findViewById(R.id.bottom_sheet_option1)
        val bottomSheet: View = findViewById(R.id.bottom_sheet_background)
        mBottomSheetBehavior = com.google.android.material.bottomsheet.BottomSheetBehavior.from(bottomSheet)
        mBottomSheetBehavior.setBottomSheetCallback(object : com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                //do nothing
            }

            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_DRAGGING) {
                    mBottomSheetBehavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
                }
            }
        })
        mBottomSheetBehavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
        //HighlightFragment().show(supportFragmentManager, "highlight")
        bottomSheet.setOnTouchListener { _, event ->
            event.y > supportActionBar?.height ?: 56
        }
        prepareHighlightCards()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_list, menu)
        return true //super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.findItem(R.id.action_highlight)?.isVisible = //false
                mBottomSheetBehavior.state != com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED && !filterPaneVisible
        menu?.findItem(R.id.action_refresh)?.isVisible = when {
            mBottomSheetBehavior.state == com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED &&
                    mSwipeRefreshLayout.isRefreshing == true -> false
            filterPaneVisible -> false
            else -> true
        }
        //menu?.findItem(R.id.action_go_to_cpbl)?.isVisible = !filterPaneVisible
        //menu?.findItem(R.id.action_settings)?.isVisible = !filterPaneVisible
        menu?.findItem(R.id.action_cache_mode)?.isVisible =
                mBottomSheetBehavior.state != com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED && !filterPaneVisible
        menu?.findItem(R.id.action_leader_board)?.isVisible = !filterPaneVisible
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            android.R.id.home -> when {
                mSwipeRefreshLayout.isRefreshing -> Log.w(TAG, "still refresh...")
                mBottomSheetBehavior.state == com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED ->
                    mBottomSheetBehavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_HIDDEN
                else -> {
                    if (!filterPaneVisible) {
                        restoreFilter()
                    }
                    filterPaneVisible = !filterPaneVisible
                }
            }
            R.id.action_refresh -> {
                if (mBottomSheetBehavior.state == com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED) {
                    prepareHighlightCards(refresh = true)
                } else {
                    doWebQuery()
                }
                return true
            }
            R.id.action_leader_board -> {
                //https://goo.gl/GtBKgp
                val builder = CustomTabsIntent.Builder()
                        .setToolbarColor(ContextCompat.getColor(this, R.color.holo_green_dark))
                val customTabsIntent = builder.build()
                customTabsIntent.launchUrl(this, Utils.LEADER_BOARD_URI)
                return true
            }
            R.id.action_highlight -> {
                if (mBottomSheetBehavior.state != com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED) {
                    findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.bottom_sheet)?.scrollToPosition(0)
                    mBottomSheetBehavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
                } else {//should not be here
                    mBottomSheetBehavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_HIDDEN
                }
                invalidateOptionsMenu()
                return true
            }
            R.id.action_cache_mode -> {
                AlertDialog.Builder(this@ListActivity)
                        .setTitle(R.string.title_cache_mode_enable_title)
                        .setMessage(R.string.title_cache_mode_enable_message)
                        .setPositiveButton(R.string.title_cache_mode_start) { _, _ ->
                            PreferenceUtils.setCacheMode(this@ListActivity, true)
                            startActivity(Intent(this@ListActivity,
                                    CacheModeListActivity::class.java).apply {
                                putExtra("cache_init", true)
                            })
                            finish()
                        }
                        .setNegativeButton(R.string.title_cache_mode_cancel) { _, _ -> }
                        .show()
                return true
            }
            R.id.action_go_to_cpbl -> {
                CpblCalendarHelper.startActivityToCpblSchedule(this@ListActivity, mYear,
                        mMonth + 1, "01", "F00")
                filterPaneVisible = false
                return true
            }
            R.id.action_settings -> {
                startActivityForResult(Intent(this, SettingsActivity3::class.java), 0)
                filterPaneVisible = false
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private var filterPaneVisible: Boolean = false
        set(value) {
            if (field != value) {
                runOnUiThread { showFilterPane(value) }
                field = value
            }
            runOnUiThread { invalidateOptionsMenu() }
        }

    private fun restoreFilter() {
        runOnUiThread {
            mPickerYear.value = mYear - 1989
            mPickerMonth.value = mMonth
            mPickerTeam.value = -1
            mPickerField.value = 0
        }
    }

    private fun showFilterPane(visible: Boolean) {
        findViewById<View>(R.id.filter_view_pane)?.apply {
            if (visible) {
                val bottom: Float = mFilterControlPane.bottom.toFloat()
                this.animate()
                        .translationY(bottom - resources.getDimension(R.dimen.padding_large))
                        .setInterpolator(AccelerateInterpolator())
                        //.withStartAction { mFilterListPane.visibility = View.GONE }
                        .withEndAction { mFilterControlBg.visibility = View.VISIBLE }
                        .start()
                mFilterListPane.animate()
                        .translationY(bottom)
                        .setInterpolator(AccelerateInterpolator())
                        .start()
                mTabLayout.animate()
                        .translationY(bottom - this.height)
                        .setInterpolator(AccelerateInterpolator())
                        .start()
                mPager.animate()
                        .translationY(bottom - this.height - mTabLayout.height)
                        .setInterpolator(AccelerateInterpolator())
                        .withStartAction { mPager.isEnabled = false }
                        .start()
            } else {
                this.animate()
                        .translationY(0f)
                        .setInterpolator(DecelerateInterpolator())
                        //.withStartAction { mFilterListPane.visibility = View.VISIBLE }
                        .withStartAction { mFilterControlBg.visibility = View.GONE }
                        .start()
                mFilterListPane.animate()
                        .translationY(0f)
                        .setInterpolator(DecelerateInterpolator())
                        .start()
                mTabLayout.animate()
                        .translationY(0f)
                        .setInterpolator(DecelerateInterpolator())
                        .start()
                mPager.animate()
                        .translationY(0f)
                        .setInterpolator(DecelerateInterpolator())
                        .withEndAction { mPager.isEnabled = true }
                        .start()
            }
        }

        //https://stackoverflow.com/a/42024138
        val animation = ValueAnimator.ofFloat(if (visible) 0f else 1f, if (visible) 1f else 0f)
        animation.apply {
            addUpdateListener { mHomeIcon.progress = it.animatedValue as Float }
            interpolator = DecelerateInterpolator()
            duration = 400
        }
        animation.start()
    }

    private var selectedFieldId: String = "F00"
        get() {
            val f = mPickerField.value
            return resources.getStringArray(R.array.cpbl_game_field_id)[f]
        }

    private var selectedTeamId: Int = 0
        get() {
            val t = mPickerTeam.value
            val team = if (t < 0) "0" else resources.getStringArray(R.array.cpbl_team_id)[t]
            return team.toInt()
        }

    private fun doQueryAction() {
        val year = mPickerYear.value + 1989
        val month = mPickerMonth.value
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

        Log.d(TAG, "team = ${mPickerTeam.value} field = ${mPickerField.value}")
        mTextViewYear.text = //mSpinnerYear.selectedItem.toString()
                getString(R.string.title_cpbl_year, mPickerYear.value)
        mTextViewField.text = //mSpinnerField.selectedItem.toString()
                resources.getStringArray(R.array.cpbl_game_field_name)[mPickerField.value]
        mTextViewTeam.text = //mSpinnerTeam.selectedItem.toString()
                if (mPickerTeam.value < 0) {
                    getString(R.string.title_favorite_teams_all)
                } else {
                    resources.getStringArray(R.array.cpbl_team_name)[mPickerTeam.value]
                }
    }

    private var snackbar: com.google.android.material.snackbar.Snackbar? = null

    private fun doWebQuery(newYear: Int = mYear, newMonth: Int = mMonth) {
        Log.d(TAG, "start fetch $newYear/${newMonth + 1}")
        viewModel.fetch(newYear, newMonth, clearCached = true)
        mAdapter.getChildFragment(newMonth - 1)?.arguments = Bundle().apply {
            putInt("year", newYear)
            putInt("month", newMonth)
            putBoolean("refresh", true)
            putString("field_id", selectedFieldId)
            putInt("team_id", selectedTeamId)
        }
    }

    private fun showSnackBar(text: String? = null, visible: Boolean = !text.isNullOrEmpty()) {
        if (visible && text != null) {
            if (snackbar != null) {
                snackbar!!.setText(text)
            } else {
                snackbar = com.google.android.material.snackbar.Snackbar.make(findViewById<View>(R.id.main_content_frame), text,
                        com.google.android.material.snackbar.Snackbar.LENGTH_INDEFINITE)
            }
            snackbar!!.show()
        } else {
            snackbar?.dismiss()
            snackbar = null
        }
    }

    class SimplePageAdapter(a: AppCompatActivity, private var months: ArrayList<String>) :
            WizardPager.PagerAdapter(a.supportFragmentManager) {
        override fun getItem(position: Int): androidx.fragment.app.Fragment {
            return MonthViewFragment().apply {
                arguments = Bundle().apply {
                    putInt("index", position)
                    putString("title", months[position])
                }
            }
        }

        override fun getCount() = months.size
    }

    internal class MonthViewFragment : androidx.fragment.app.Fragment(), FlexibleAdapter.OnItemClickListener,
            FlexibleAdapter.OnItemLongClickListener {

        private var container: androidx.swiperefreshlayout.widget.SwipeRefreshLayout? = null
        private lateinit var list: androidx.recyclerview.widget.RecyclerView
        private lateinit var emptyView: View
        private var index = -1

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
                viewModel.fetch(year, month)?.observe(this,
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
            //cpblHelper = CpblCalendarHelper(activity!!)
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
            this.emptyView = view.findViewById(R.id.empty_pane)
            return view //super.onCreateView(inflater, container, savedInstanceState)
        }

        private fun startRefreshing(enabled: Boolean, year: Int = 2018, monthOfJava: Int = 0) {
            if (enabled) {
                this.container?.isEnabled = true
                this.container?.isRefreshing = true
                if (activity is ListActivity) {
                    (activity as ListActivity).showSnackBar(
                            getString(R.string.title_download_from_cpbl, year, monthOfJava + 1))
                }
            } else {
                if (activity is ListActivity) {
                    (activity as ListActivity).showSnackBar(visible = false)
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
            for (i in 0 until adapterList.size) {
                if (DateUtils.isToday(adapterList[i].game.StartTime)) {
                    this.list.scrollToPosition(i)
                    break//break when the upcoming game if found
                } else if (!adapterList[i].game.IsFinal) {
                    this.list.scrollToPosition(i)
                    break//break when the upcoming game if found
                }
            }
            emptyView.visibility = if (adapterList.isEmpty()) View.VISIBLE else View.GONE
            this.list.visibility = if (adapterList.isEmpty()) View.GONE else View.VISIBLE
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
        override fun bindViewHolder(adapter: FlexibleAdapter<IFlexible<androidx.recyclerview.widget.RecyclerView.ViewHolder>>?,
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
                        game.Field?.contains(context.getString(R.string.title_at)) == false) {
                    String.format("%s%s", context.getString(R.string.title_at),
                            game.getFieldFullName(context))
                } else {
                    game.getFieldFullName(context)
                }
                teamAwayName?.text = game.AwayTeam.name
                teamAwayScore?.text = if (game.IsFinal || game.IsLive) {
                    game.AwayScore.toString()
                } else "-"
                teamAwayLogo?.apply {
                    setImageResource(R.drawable.ic_baseball)
                    setColorFilter(helper.getLogoColorFilter(game.AwayTeam, year),
                            PorterDuff.Mode.SRC_IN)
                }
                teamHomeName?.text = game.HomeTeam.name
                teamHomeScore?.text = if (game.IsFinal || game.IsLive) {
                    game.HomeScore.toString()
                } else "-"
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
                                      adapter: FlexibleAdapter<IFlexible<androidx.recyclerview.widget.RecyclerView.ViewHolder>>?): ViewHolder {
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
        : FlexibleAdapter<MyItemView>(items, listener) {
        init {
            //isOnlyEntryAnimation = true
            setAnimationOnForwardScrolling(true)
        }
    }

    override fun onBackPressed() {
        Log.d(TAG, "onBackPressed: ${mBottomSheetBehavior.state}")
        if (mBottomSheetBehavior.state == com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED) {
            //mBottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            finish()
            return
        } else if (filterPaneVisible) {//close the filter pane
            filterPaneVisible = false
            restoreFilter()
            return
        }
        super.onBackPressed()
    }

    private fun prepareHighlightCards(refresh: Boolean = false) {
        mSwipeRefreshLayout.apply {
            isEnabled = true
            isRefreshing = true
        }
        invalidateOptionsMenu()
        val list = ArrayList<Game>()
        val now = CpblCalendarHelper.getNowTime()
        val year = now.get(Calendar.YEAR)
        val monthOfJava = now.get(Calendar.MONTH)
        showSnackBar(getString(R.string.title_download_from_cpbl, year, monthOfJava + 1))
        viewModel.fetch(year, monthOfJava, clearCached = refresh)
                ?.observe(this@ListActivity, Observer {
                    if (it?.isNotEmpty() == true) {//we have data
                        list.addAll(it)
                        //mSpinnerMonth.setSelection(monthOfJava - 1)
                        mPager.currentItem = monthOfJava - 1

                        if (it.first().StartTime.after(now) && monthOfJava > Calendar.JANUARY) {
                            //get previous month data
                            preparePreviousMonth(list, year, monthOfJava - 1)
                            return@Observer
                        } else if (it.last().StartTime.before(now)) {//already started
                            //check if it is final
                            var more = it.last().IsFinal
                            if (it.size > 1 && it.last().StartTime == it.dropLast(1).last().StartTime) {
                                more = more or it.dropLast(1).last().IsFinal
                            }
                            if (more && monthOfJava < Calendar.DECEMBER) {
                                prepareNextMonth(list, year, monthOfJava + 1)
                                return@Observer
                            }
                        }
                    }
                    prepareExtraCards(list)
                })
    }

    private fun preparePreviousMonth(list: ArrayList<Game>, year: Int, previousMonth: Int) {
        showSnackBar(getString(R.string.title_download_from_cpbl, year, previousMonth + 1))
        viewModel.fetch(year, previousMonth)?.observe(this@ListActivity,
                Observer {
                    if (it?.isNotEmpty() == true) list.addAll(0, it)
                    prepareExtraCards(list)
                })
    }

    private fun prepareNextMonth(list: ArrayList<Game>, year: Int, nextMonth: Int) {
        showSnackBar(getString(R.string.title_download_from_cpbl, year, nextMonth + 1))
        viewModel.fetch(year, nextMonth)?.observe(this@ListActivity,
                Observer {
                    if (it?.isNotEmpty() == true) list.addAll(it)
                    prepareExtraCards(list)
                })
    }

    private fun prepareExtraCards(list: ArrayList<Game>) {
        showSnackBar(getString(R.string.title_download_complete))
        val config = FirebaseRemoteConfig.getInstance()
        val gameList = CpblCalendarHelper.getHighlightGameList(list)
        gameList.addAll(0, GameCardAdapter.getAnnouncementCards(this, config))
        GameCardAdapter.getNewVersionCard(this, config)?.let { gameList.add(0, it) }
        updateHighlightList(gameList)
    }

    private fun updateHighlightList(list: ArrayList<Game>) {
        list.add(GameCardAdapter.createMoreCard())//add a more button to last row

        val adapter = GameCardAdapter(this, list, TeamHelper(application as CpblApplication))
        adapter.setOnClickListener { view, game ->
            when {
                GameCardAdapter.isMoreCard(game) -> {
                    //view.id == R.id.card_option1 -> {
                    //mPager.currentItem = mMonth - 1
                    doQueryAction() //get new data from ViewModel
                    mBottomSheetBehavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_HIDDEN
                    invalidateOptionsMenu()
                }
                view.id == R.id.card_option2 -> if (GameCardAdapter.isUpdateCard(game)) {
                    try {
                        startActivity(Intent(Intent.ACTION_VIEW,
                                Uri.parse("market://details?id=$packageName")))
                    } catch (anfe: android.content.ActivityNotFoundException) {
                        startActivity(Intent(Intent.ACTION_VIEW,
                                Uri.parse("https://play.google.com/store/apps/details?id=$packageName")))
                    }
                    finish() //update new app, so we can close now
                } else {
                    val calIntent = Utils.createAddToCalendarIntent(this@ListActivity, game)
                    if (PackageUtils.isCallable(this@ListActivity, calIntent)) {
                        startActivity(calIntent)
                    }
                }
                else -> Utils.startGameActivity(this@ListActivity, game)
            }
        }
        findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.bottom_sheet)?.apply {
            this.adapter = adapter
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this@ListActivity)
            setHasFixedSize(true)
        }
        showSnackBar(visible = false)
        mSwipeRefreshLayout.apply {
            isRefreshing = false
            isEnabled = false
        }
        invalidateOptionsMenu()
    }
}
