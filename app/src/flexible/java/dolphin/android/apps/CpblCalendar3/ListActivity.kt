@file:Suppress("PackageName")

package dolphin.android.apps.CpblCalendar3

import android.animation.ValueAnimator
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.graphics.drawable.DrawerArrowDrawable
import androidx.appcompat.widget.Toolbar
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsServiceConnection
import androidx.browser.customtabs.CustomTabsSession
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.viewpager.widget.ViewPager
import cn.carbswang.android.numberpickerview.library.NumberPickerView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import dolphin.android.apps.CpblCalendar.Utils
import dolphin.android.apps.CpblCalendar.preference.PrefsHelper
import dolphin.android.apps.CpblCalendar.provider.CpblCalendarHelper
import dolphin.android.apps.CpblCalendar.provider.Game
import dolphin.android.util.PackageUtils
import eu.davidea.flexibleadapter.common.FlexibleItemAnimator
import eu.davidea.flexibleadapter.common.SmoothScrollLinearLayoutManager
import kotlinx.android.synthetic.flexible.activity_splash.view.*
import java.text.DateFormatSymbols
import java.util.*
import kotlin.collections.ArrayList

class ListActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "ListActivity"
    }

    private lateinit var mDrawerList: DrawerLayout

    private lateinit var mFilterListPane: View
    private lateinit var mFilterControlPane: View
    private lateinit var mFilterControlBg: View
    private lateinit var mPager: ViewPager
    private lateinit var mAdapter: SimplePageAdapter
    private lateinit var mTabLayout: TabLayout

    private lateinit var mPickerYear: NumberPickerView
    private lateinit var mPickerMonth: NumberPickerView
    private lateinit var mPickerField: NumberPickerView
    private lateinit var mPickerTeam: NumberPickerView
    private lateinit var mChipYear: Chip
    private lateinit var mChipField: Chip
    private lateinit var mChipTeam: Chip

    private lateinit var mSwipeRefreshLayout: SwipeRefreshLayout
    private lateinit var mBottomSheetBehavior: BottomSheetBehavior<View>
    private lateinit var mHomeIcon: DrawerArrowDrawable

    private var mAdView: AdView? = null

    private var mYear: Int = 2018
    private var mMonth: Int = Calendar.MAY
    private lateinit var viewModel: GameViewModel
    private lateinit var prefs: PrefsHelper

    private var mCustomTabsClient: CustomTabsClient? = null
    internal var mCustomTabsSession: CustomTabsSession? = null
    private var mCustomTabsConnection: CustomTabsServiceConnection? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list)

        val now = CpblCalendarHelper.getNowTime()
        viewModel = ViewModelProviders.of(this,
                ViewModelProvider.AndroidViewModelFactory(application))
                .get(GameViewModel::class.java)
        viewModel.debugMode = false
        prefs = PrefsHelper(this)

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

        mDrawerList = findViewById(R.id.drawer_layout)
        mDrawerList.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        supportFragmentManager.beginTransaction()
                .replace(R.id.drawer_list_right, SettingsFragment())
                .commit()
        mDrawerList.addDrawerListener(object : DrawerLayout.SimpleDrawerListener() {
            override fun onDrawerClosed(drawerView: View) {
                Log.d(TAG, "apply the settings")
                doQueryAction(false) //try to reset pull to refresh and adapter style
                mDrawerList.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            }
        })

        mTabLayout = findViewById(R.id.tab_layout)
        mPager = findViewById(R.id.viewpager)
        mAdView = findViewById(R.id.adView)

        //prepare filter pane
        mChipYear = findViewById(android.R.id.button1)
        mChipYear.setOnClickListener { filterPaneVisible = true }
        mChipField = findViewById(android.R.id.button2)
        mChipField.setOnClickListener { filterPaneVisible = true }
        mChipTeam = findViewById(android.R.id.button3)
        mChipTeam.setOnClickListener { filterPaneVisible = true }

        //Log.d(TAG, "friction: ${ViewConfiguration.getScrollFriction()}")
        mPickerYear = findViewById(android.R.id.text1)
        mPickerMonth = findViewById(android.R.id.text2)
        mPickerField = findViewById(android.R.id.icon1)
        mPickerTeam = findViewById(android.R.id.icon2)

        mPickerField.setFriction(ViewConfiguration.getScrollFriction() * 2)
        mPickerYear.apply {
            val year = now.get(Calendar.YEAR)
            displayedValues = Array(year - 1989) { y ->
                getString(R.string.title_cpbl_year, y + 1) + " (${1990 + y})"
            }
            minValue = 1
            maxValue = year - 1989
            value = maxValue
            setFriction(ViewConfiguration.getScrollFriction() * 2)
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
        mFilterControlBg.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {//hide filter pane
                filterPaneVisible = false
            }
            true
        }
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
        //mTabLayout.setupWithViewPager(mPager)
        mPager.addOnPageChangeListener(TabLayout.TabLayoutOnPageChangeListener(mTabLayout))
        mPager.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                mMonth = position + 1
                runOnUiThread {
                    mPickerMonth.value = mMonth
                }
                Log.d(TAG, "selected month = ${mMonth + 1}")
                doQueryAction() //query when page change
            }
        })
        mTabLayout.addOnTabSelectedListener(TabLayout.ViewPagerOnTabSelectedListener(mPager))
        //pager.currentItem = mMonth - 1

        mPickerMonth.apply {
            displayedValues = Array(months.size) { months[it] }
            minValue = Calendar.FEBRUARY
            maxValue = Calendar.NOVEMBER
            value = mMonth
        }

        //Handler().postDelayed({ pager.currentItem = mMonth - 1 }, 500)
        mSwipeRefreshLayout = findViewById(R.id.bottom_sheet_option1)
        val bottomSheet: View = findViewById(R.id.bottom_sheet_background)
        mBottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        mBottomSheetBehavior.setBottomSheetCallback(object :
                BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                //do nothing
            }

            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_DRAGGING) {
                    mBottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                }
            }
        })
        mBottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        //HighlightFragment().show(supportFragmentManager, "highlight")
        bottomSheet.setOnTouchListener { _, event ->
            event.y > supportActionBar?.height ?: 56
        }
        findViewById<RecyclerView>(R.id.bottom_sheet)?.apply {
            layoutManager = SmoothScrollLinearLayoutManager(this@ListActivity)
            setHasFixedSize(true)
        }
        prepareHighlightCards()
        loadAds()
    }

    private fun loadAds() {
//        Handler().postDelayed({
        mAdView?.loadAd(AdRequest.Builder()
                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                .build())
//        }, 500)
    }

    override fun onStart() {
        super.onStart()

        CustomTabsHelper.getPackageNameToUse(this)?.let { packageName ->
            mCustomTabsConnection = object : CustomTabsServiceConnection() {
                override fun onCustomTabsServiceConnected(name: ComponentName?,
                                                          client: CustomTabsClient?) {
                    mCustomTabsClient = client
                    mCustomTabsClient?.warmup(0L)
                    mCustomTabsSession = mCustomTabsClient?.newSession(null)
                    mCustomTabsSession?.mayLaunchUrl(Utils.LEADER_BOARD_URI, null, null)
                }

                override fun onServiceDisconnected(p0: ComponentName?) {
                    mCustomTabsClient = null
                    mCustomTabsSession = null
                }
            }
            CustomTabsClient.bindCustomTabsService(this, packageName, mCustomTabsConnection)
        }
    }

    override fun onResume() {
        super.onResume()
        mAdView?.resume()
    }

    override fun onPause() {
        super.onPause()
        mAdView?.pause()
    }

    override fun onStop() {
        super.onStop()
        if (mCustomTabsConnection != null) {
            unbindService(mCustomTabsConnection!!)
            mCustomTabsClient = null
            mCustomTabsSession = null
            mCustomTabsConnection = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mAdView?.destroy()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_list, menu)
        return true //super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.findItem(R.id.action_highlight)?.isVisible = //false
                mBottomSheetBehavior.state != BottomSheetBehavior.STATE_EXPANDED && !filterPaneVisible
        menu?.findItem(R.id.action_refresh)?.isVisible = when {
            mBottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED &&
                    mSwipeRefreshLayout.isRefreshing == true -> false
            filterPaneVisible -> false
            else -> true
        }
        //menu?.findItem(R.id.action_go_to_cpbl)?.isVisible = !filterPaneVisible
        //menu?.findItem(R.id.action_settings)?.isVisible = !filterPaneVisible
        menu?.findItem(R.id.action_cache_mode)?.isVisible =
                mBottomSheetBehavior.state != BottomSheetBehavior.STATE_EXPANDED && !filterPaneVisible
        menu?.findItem(R.id.action_leader_board)?.isVisible = !filterPaneVisible
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            android.R.id.home -> when {
                mSwipeRefreshLayout.isRefreshing -> Log.w(TAG, "still refresh...")
                mBottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED ->
                    mBottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                else -> {
                    if (!filterPaneVisible) {
                        restoreFilter()
                    }
                    filterPaneVisible = !filterPaneVisible
                }
            }
            R.id.action_refresh -> {
                if (mBottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
                    prepareHighlightCards(refresh = true)
                } else {
                    doWebQuery()
                }
                return true
            }
            R.id.action_leader_board -> {
                CustomTabsHelper.launchUrl(this, mCustomTabsSession, Utils.LEADER_BOARD_URI)
                return true
            }
            R.id.action_highlight -> {
                if (mBottomSheetBehavior.state != BottomSheetBehavior.STATE_EXPANDED) {
                    findViewById<RecyclerView>(R.id.bottom_sheet)?.scrollToPosition(0)
                    mBottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                } else {//should not be here
                    mBottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                }
                invalidateOptionsMenu()
                return true
            }
            R.id.action_cache_mode -> {
                AlertDialog.Builder(this@ListActivity)
                        .setTitle(R.string.title_cache_mode_enable_title)
                        .setMessage(R.string.title_cache_mode_enable_message)
                        .setPositiveButton(R.string.title_cache_mode_start) { _, _ ->
                            prefs.cacheModeEnabled = true
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
                //Utils.startActivityToCpblSchedule(this@ListActivity, mYear,
                //        mMonth + 1, "01", "F00")
                val url = CpblCalendarHelper.URL_SCHEDULE_2016.replace("@year",
                        mYear.toString()).replace("@month",
                        (mMonth + 1).toString()).replace("@kind", "01")
                        .replace("@field", "F00")
                CustomTabsHelper.launchUrl(this, mCustomTabsSession, Uri.parse(url))
                filterPaneVisible = false
                return true
            }
            R.id.action_settings -> {
                //startActivityForResult(Intent(this, SettingsActivity3::class.java), 0)
                mDrawerList.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
                mDrawerList.openDrawer(GravityCompat.END)
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
            //runOnUiThread { invalidateOptionsMenu() }
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
                        .withStartAction { invalidateOptionsMenu() }
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
                        //.withEndAction { loadAds() }
                        .start()
            } else {
                this.animate()
                        .translationY(0f)
                        .setInterpolator(DecelerateInterpolator())
                        .withEndAction { invalidateOptionsMenu() }
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

    private fun doQueryAction(showSnackbar: Boolean = true) {
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
                putBoolean("snackbar", showSnackbar)
                putInt("year", year)
                putInt("month", month)
                putString("field_id", selectedFieldId)
                putInt("team_id", selectedTeamId)
            }
        }

        Log.d(TAG, "team = ${mPickerTeam.value} field = ${mPickerField.value}")
        mChipYear.text = //mSpinnerYear.selectedItem.toString()
                getString(R.string.title_cpbl_year, mPickerYear.value)
        mChipField.text = //mSpinnerField.selectedItem.toString()
                resources.getStringArray(R.array.cpbl_game_field_name)[mPickerField.value]
        mChipTeam.text = //mSpinnerTeam.selectedItem.toString()
                if (mPickerTeam.value < 0) {
                    getString(R.string.title_favorite_teams_all)
                } else {
                    resources.getStringArray(R.array.cpbl_team_name)[mPickerTeam.value]
                }
    }

    private var snackbar: Snackbar? = null

    internal fun doWebQuery(newYear: Int = mYear, newMonth: Int = mMonth) {
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

    internal fun showSnackBar(text: String? = null, visible: Boolean = !text.isNullOrEmpty()) {
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

    override fun onBackPressed() {
        Log.d(TAG, "onBackPressed: ${mBottomSheetBehavior.state}")
        if (mBottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
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
                ?.observe(this@ListActivity, Observer { resources ->
                    when {
                        resources.progress == 98 ->
                            showSnackBar(getString(R.string.title_download_complete))
                        resources.progress < 90 ->
                            showSnackBar(resources.messages +
                                    getString(R.string.title_download_from_cpbl, year,
                                            monthOfJava + 1))
                        else -> resources.dataList?.let { dataList ->
                            if (dataList.isNotEmpty()) {//we have data
                                list.addAll(dataList)
                                //mSpinnerMonth.setSelection(monthOfJava - 1)
                                mPager.currentItem = monthOfJava - 1

                                if (dataList.first().StartTime.after(now) &&
                                        monthOfJava > Calendar.JANUARY) {
                                    //get previous month data
                                    preparePreviousMonth(list, year, monthOfJava - 1)
                                    return@Observer
                                } else if (dataList.last().StartTime.before(now)) {//already started
                                    //check if it is final
                                    var more = dataList.last().IsFinal
                                    if (dataList.size > 1 &&
                                            dataList.last().StartTime ==
                                            dataList.dropLast(1).last().StartTime) {
                                        more = more or dataList.dropLast(1).last().IsFinal
                                    }
                                    if (more && monthOfJava < Calendar.DECEMBER) {
                                        prepareNextMonth(list, year, monthOfJava + 1)
                                        return@Observer
                                    }
                                }
                                prepareExtraCards(list)
                            } else {
                                Log.e(TAG, "no data list, just show full list directly")
                                doQueryAction(false) //get new data from ViewModel
                                mBottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                                invalidateOptionsMenu()
                            }
                        }
                    }
                })
    }

    private fun preparePreviousMonth(list: ArrayList<Game>, year: Int, previousMonth: Int) {
        showSnackBar(getString(R.string.title_download_from_cpbl, year, previousMonth + 1))
        viewModel.fetch(year, previousMonth)?.observe(this@ListActivity, Observer { resources ->
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
        viewModel.fetch(year, nextMonth)?.observe(this@ListActivity, Observer { resources ->
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
            val config = FirebaseRemoteConfig.getInstance()
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
        val info = PackageUtils.getPackageInfo(this, ListActivity::class.java)
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
        val adapter = HighlightCardAdapter(application as CpblApplication, list,
                object : HighlightCardAdapter.OnCardClickListener {
                    override fun onOptionClick(view: View, game: Game, position: Int) {
                        Log.d(TAG, "on option click $position")
                        onOptionSelected(view.id, game)
                    }
                })

        findViewById<RecyclerView>(R.id.bottom_sheet)?.apply {
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
        //adapter.isSwipeEnabled = true
        showSnackBar(visible = false)
        mSwipeRefreshLayout.apply {
            isRefreshing = false
            isEnabled = false
        }
        invalidateOptionsMenu()
    }

    private fun onOptionSelected(viewId: Int, game: Game) {
        when (viewId) {
            R.id.card_option1 -> if (game.Id != HighlightCardAdapter.TYPE_MORE_CARD) {
                Log.d(TAG, "start game ${game.Id} from ${game.Url}")
                //Utils.startGameActivity(this@ListActivity, game)
                CustomTabsHelper.launchUrl(this, mCustomTabsSession, game)
            } else {
                doQueryAction(false) //get new data from ViewModel
                mBottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                invalidateOptionsMenu()
            }
            R.id.card_option2 -> if (game.Id != HighlightCardAdapter.TYPE_UPDATE_CARD) {
                if (game.IsLive) {//refresh cards
                    prepareHighlightCards(true)
                } else {//register in calendar app
                    val calIntent = Utils.createAddToCalendarIntent(this, game)
                    if (PackageUtils.isCallable(this, calIntent)) {
                        startActivity(calIntent)
                    }
                }
            } else {
                try {
                    startActivity(Intent(Intent.ACTION_VIEW,
                            Uri.parse("market://details?id=$packageName")))
                } catch (anfe: android.content.ActivityNotFoundException) {
                    startActivity(Intent(Intent.ACTION_VIEW,
                            Uri.parse(
                                    "https://play.google.com/store/apps/details?id=$packageName")))
                }
                finish() //update new app, so we can close now
            }
            R.id.card_option3 -> {
                Log.d(TAG, "dismiss card, maybe we should remember it")
            }
        }
    }
}
