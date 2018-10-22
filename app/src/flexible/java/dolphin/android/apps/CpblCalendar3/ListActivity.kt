@file:Suppress("PackageName")

package dolphin.android.apps.CpblCalendar3

import android.animation.ValueAnimator
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
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
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import cn.carbswang.android.numberpickerview.library.NumberPickerView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import dolphin.android.apps.CpblCalendar.Utils
import dolphin.android.apps.CpblCalendar.preference.PrefsHelper
import dolphin.android.apps.CpblCalendar.provider.CpblCalendarHelper
import dolphin.android.apps.CpblCalendar.provider.Game
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

    //private lateinit var mSwipeRefreshLayout: SwipeRefreshLayout
    private lateinit var mHighlightFragment: HighlightViewFragment
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
        val hint = findViewById<View>(R.id.picker_team_hint)

        mPickerField.setFriction(ViewConfiguration.getScrollFriction() * 2)
        mPickerYear.apply {
            mYear = now.get(Calendar.YEAR)
            Log.d(TAG, "year = $mYear")
            displayedValues = Array(mYear - 1989) { y ->
                getString(R.string.title_cpbl_year, y + 1) + " (${1990 + y})"
            }
            minValue = 1
            maxValue = mYear - 1989
            Log.d(TAG, "max = $maxValue ")
            setFriction(ViewConfiguration.getScrollFriction() * 2)

            setOnScrollListener { view, scrollState ->
                if (scrollState == NumberPickerView.OnScrollListener.SCROLL_STATE_IDLE) {
                    mPickerTeam.smoothScrollToValue(-1)
                    mPickerTeam.isEnabled = view.value == maxValue
                    hint?.visibility = if (mPickerTeam.isEnabled) View.GONE else View.VISIBLE
                } else {
                    mPickerTeam.isEnabled = false
                    hint?.visibility = View.GONE
                }
            }

            value = maxValue
        }
        Log.d(TAG, "picker year = ${mPickerYear.value}")

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
        //months.remove(months.last()) //no December games
        months.removeAt(0) //no January games
        //months.removeAt(0)
        months.forEach { mTabLayout.addTab(mTabLayout.newTab().setText(it)) }
        mAdapter = SimplePageAdapter(this, months)
        mPager.adapter = mAdapter

        mPickerMonth.apply {
            displayedValues = Array(months.size) { months[it] }
            minValue = Calendar.FEBRUARY
            maxValue = Calendar.DECEMBER //Calendar.NOVEMBER

            mMonth = now.get(Calendar.MONTH)
            mPager.currentItem = mMonth - 1 //select page

            value = mMonth
        }

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

        //Handler().postDelayed({ pager.currentItem = mMonth - 1 }, 500)
        //mSwipeRefreshLayout = findViewById(R.id.bottom_sheet_option1)
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
        if (prefs.showHighlightOnLoadEnabled) {
            mBottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            //HighlightFragment().show(supportFragmentManager, "highlight")
        } else {
            mBottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        }
        bottomSheet.setOnTouchListener { _, event ->
            event.y > supportActionBar?.height ?: 56
        }
//        findViewById<RecyclerView>(R.id.bottom_sheet)?.apply {
//            layoutManager = SmoothScrollLinearLayoutManager(this@ListActivity)
//            setHasFixedSize(true)
//        }

        //load highlight fragment
        mHighlightFragment = HighlightViewFragment()
        supportFragmentManager.beginTransaction()
                .replace(R.id.bottom_sheet_background, mHighlightFragment)
                .commitNow()

        if (prefs.showHighlightOnLoadEnabled) {
            prepareHighlightCards()
        } else { //auto load current year/month
            Handler().postDelayed({
                Log.d(TAG, "this year = $mYear")
                mPickerYear.value = mYear - 1989
                restoreFilter()
                filterPaneVisible = false
                doQueryAction()
                invalidateOptionsMenu()
            }, 200)
        }
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
        mCustomTabsConnection?.let { connection ->
            unbindService(connection)
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
                    mHighlightFragment.isRefreshing == true -> false
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
            android.R.id.home ->
                when {
                    mHighlightFragment.isRefreshing -> Log.w(TAG, "still refresh...")
                    mBottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED -> {
                        restoreFilter() //hide highlight from hamburger
                        filterPaneVisible = false //hide highlight from hamburger, make sure hidden
                        doQueryAction(false) //hide highlight, load fragment again
                        mBottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                    }
                    else -> {
                        if (!filterPaneVisible) {
                            restoreFilter() //hide filter pane from hamburger
                            doQueryAction(false) //hide filter, load fragment again
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
                    prepareHighlightCards()
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
        Log.d(TAG, "restore $mYear/${mMonth + 1}")
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
            for (i in 0 until mAdapter.count) {//clear all fragments
                mAdapter.getChildFragment(i)?.arguments = Bundle().apply {
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
            restoreFilter()
            filterPaneVisible = false
            doQueryAction(false)
            return
        }
        super.onBackPressed()
    }

    private fun prepareHighlightCards(refresh: Boolean = false) {
        val now = CpblCalendarHelper.getNowTime()
        val year = now.get(Calendar.YEAR)
        val monthOfJava = now.get(Calendar.MONTH)
        mHighlightFragment.arguments = Bundle().apply {
            putBoolean("refresh", true)
            putInt("year", year)
            putInt("month", monthOfJava)
            putBoolean("cache", refresh)
        }
        invalidateOptionsMenu()
    }

//    internal fun updateHighlightList(list: ArrayList<Game>) {
//        //adapter.isSwipeEnabled = true
//        showSnackBar(visible = false)
//        mHighlightFragment.arguments = Bundle().apply { putBoolean("refresh", false) }
//        invalidateOptionsMenu()
//    }

//    internal fun onOptionSelected(viewId: Int, game: Game) {
//        when (viewId) {
//            R.id.card_option1 -> if (game.Id != HighlightCardAdapter.TYPE_MORE_CARD) {
//                Log.d(TAG, "start game ${game.Id} from ${game.Url}")
//                //Utils.startGameActivity(this@ListActivity, game)
//                CustomTabsHelper.launchUrl(this, mCustomTabsSession, game)
//            } else {
//                doQueryAction(false) //get new data from ViewModel
//                mBottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
//                invalidateOptionsMenu()
//            }
//            R.id.card_option2 -> if (game.Id != HighlightCardAdapter.TYPE_UPDATE_CARD) {
//                if (game.IsLive) {//refresh cards
//                    prepareHighlightCards(true)
//                } else {//register in calendar app
//                    val calIntent = Utils.createAddToCalendarIntent(this, game)
//                    if (PackageUtils.isCallable(this, calIntent)) {
//                        startActivity(calIntent)
//                    }
//                }
//            } else {
//                try {
//                    startActivity(Intent(Intent.ACTION_VIEW,
//                            Uri.parse("market://details?id=$packageName")))
//                } catch (anfe: android.content.ActivityNotFoundException) {
//                    startActivity(Intent(Intent.ACTION_VIEW,
//                            Uri.parse(
//                                    "https://play.google.com/store/apps/details?id=$packageName")))
//                }
//                finish() //update new app, so we can close now
//            }
//            R.id.card_option3 -> {
//                Log.d(TAG, "dismiss card, maybe we should remember it")
//            }
//        }
//    }

    internal fun onHighlightFragmentUpdate(what: Int, game: Game? = null) {
        when (what) {
            HighlightViewFragment.MSG_LAUNCH_URL ->
                CustomTabsHelper.launchUrl(this, mCustomTabsSession, game)
            HighlightCardAdapter.TYPE_MORE_CARD -> {
                doQueryAction(false) //get new data from ViewModel
                mBottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                invalidateOptionsMenu()
            }
            HighlightCardAdapter.TYPE_UPDATE_CARD -> {
                val url = "https://play.google.com/store/apps/details?id=$packageName"
                try {
                    startActivity(Intent(Intent.ACTION_VIEW,
                            Uri.parse("market://details?id=$packageName")))
                } catch (anfe: ActivityNotFoundException) {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                }
                finish() //update new app, so we can close now
            }
        }
    }
}
