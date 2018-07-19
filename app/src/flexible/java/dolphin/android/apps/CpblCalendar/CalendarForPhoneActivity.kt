package dolphin.android.apps.CpblCalendar

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.AbsListView
import android.widget.AdapterView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import dolphin.android.apps.CpblCalendar.preference.PrefsHelper
import dolphin.android.apps.CpblCalendar.provider.CpblCalendarHelper
import dolphin.android.apps.CpblCalendar.provider.Game
import dolphin.android.apps.CpblCalendar3.CpblApplication
import dolphin.android.apps.CpblCalendar3.R
import dolphin.android.util.PackageUtils
import java.util.*

/**
 * Created by dolphin on 2013/6/3.
 *
 *
 * CalendarActivity for phone version, with a ActionBarDrawer pane.
 */
class CalendarForPhoneActivity : CalendarActivity(), OnQueryCallback, ActivityCompat.OnRequestPermissionsResultCallback, AbsListView.OnScrollListener, AdapterView.OnItemClickListener, View.OnClickListener {
    @Suppress("PrivatePropertyName")
    private var ENABLE_BOTTOM_SHEET = false

    private lateinit var mDrawerLayout: DrawerLayout
    private lateinit var mDrawerList: View

    private lateinit var mFavTeams: TextView
    //private ArrayList<Game> mGameList = null;
    private lateinit var mFab: FloatingActionButton
    private var mBottomSheetBehavior: BottomSheetBehavior<*>? = null
    private var mBottomSheetTitle: TextView? = null
    private var mBottomSheetBackground: View? = null
    private var mBottomSheetOption1: View? = null
    private var mBottomSheetOption2: View? = null
    private var mBottomSheetOption4: View? = null
    private var mSwipeRefreshLayout: SwipeRefreshLayout? = null

    private var mAdView: AdView? = null
    private lateinit var mFirebaseAnalytics: FirebaseAnalytics
    private lateinit var mPrefsHelper: PrefsHelper

    //http://stackoverflow.com/a/5123903
    private var mAlreadyBottom = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendar_phone)

        ENABLE_BOTTOM_SHEET = FirebaseRemoteConfig.getInstance()
                .getBoolean("enable_bottom_sheet_options")
        //Log.d(TAG, "ENABLE_BOTTOM_SHEET: " + ENABLE_BOTTOM_SHEET);
        mPrefsHelper = PrefsHelper(this)

        mDrawerLayout = findViewById(R.id.drawer_layout)
        mDrawerList = findViewById(R.id.left_drawer)
        //used to avoid clicking objects behind drawer
        mDrawerList.setOnTouchListener { _, _ -> true }
        // set a custom shadow that overlays the main content when the drawer opens
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START)

        findViewById<Toolbar>(R.id.toolbar)?.apply { setSupportActionBar(this) }
        supportActionBar?.setDisplayHomeAsUpEnabled(false)

        initQueryPane()

        mFavTeams = findViewById(R.id.textView6)
        mFavTeams.setOnClickListener { showFavTeamsDialog() }
        updateFavTeamsSummary()
        findViewById<View>(R.id.layout3)?.visibility = View.VISIBLE

        mAdView = findViewById(R.id.adView)
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this)
        mSwipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        if (mSwipeRefreshLayout != null) {
            mSwipeRefreshLayout!!.isEnabled = false
        }

        mFab = findViewById(R.id.button_floating_action)
        mFab.setOnClickListener { mDrawerLayout.openDrawer(mDrawerList) }

        //var listView: ListView? = null
        (fragmentManager.findFragmentById(R.id.main_content_frame) as? GameListFragment)?.let {
            it.listView?.apply {
                if (ENABLE_BOTTOM_SHEET) {
                    it.onOptionClickListener = object : GameAdapter.OnOptionClickListener {
                        override fun onOptionClicked(view: View, game: Game) {
                            setBottomSheetVisibility(true, game, view)
                        }
                    }
                }
                onItemClickListener = this@CalendarForPhoneActivity
                setOnScrollListener(this@CalendarForPhoneActivity)
            }
        }


        prepareBottomSheet()

        Handler().postDelayed({
            loadAds()//load ads in the background
        }, 500)

        //        final ArrayList<Game> list = (getIntent() != null && getIntent().hasExtra(HighlightActivity.KEY_CACHE))
        //                ? getIntent().<Game>getParcelableArrayListExtra(HighlightActivity.KEY_CACHE) : null;
        //        //dolphin++@2017.05.19, check if we have cached data from HighlightActivity
        //        //http://stackoverflow.com/a/19314677
        //        if (list != null) {//from HighlightActivity
        //            autoLoadGames(savedInstanceState, false);
        //            //Log.d(TAG, String.format("list %d", list.size()));
        //            new Handler().postDelayed(new Runnable() {
        //                @Override
        //                public void run() {
        //                    doHighlightCacheUpdate(list);
        //                }
        //            }, 100);
        //        } else {//don't ask ever again
        autoLoadGames(savedInstanceState, true)
        //        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun prepareBottomSheet() {
        findViewById<View>(R.id.bottom_sheet)?.apply {
            if (ENABLE_BOTTOM_SHEET) {
                mBottomSheetBehavior = BottomSheetBehavior.from(this)
                mBottomSheetBehavior!!.peekHeight = 0
                mBottomSheetBehavior!!.isHideable = true
                //override parent class
                mBottomSheetTitle = findViewById(R.id.bottom_sheet_title)
                mBottomSheetBackground = findViewById(R.id.bottom_sheet_background)
                mBottomSheetBackground?.setOnTouchListener { _, motionEvent ->
                    if (mBottomSheetBehavior!!.state == BottomSheetBehavior.STATE_EXPANDED
                            && motionEvent.action == MotionEvent.ACTION_UP) {
                        setBottomSheetVisibility(false)
                    }
                    true//do nothing
                }
                mBottomSheetOption1 = this.findViewById(R.id.bottom_sheet_option1)
                mBottomSheetOption1?.setOnClickListener(this@CalendarForPhoneActivity)
                mBottomSheetOption2 = this.findViewById(R.id.bottom_sheet_option2)
                mBottomSheetOption2?.setOnClickListener(this@CalendarForPhoneActivity)
                //            mBottomSheetOption3 = bottomSheet.findViewById(R.id.bottom_sheet_option3);
                //            if (mBottomSheetOption3 != null) {
                //                mBottomSheetOption3.setOnClickListener(this);
                //            }
                mBottomSheetOption4 = this.findViewById(R.id.bottom_sheet_option4)
                mBottomSheetOption4?.setOnClickListener(this@CalendarForPhoneActivity)
            } else {//don't show
                visibility = View.GONE
            }
        }
    }

    private fun autoLoadGames(savedInstanceState: Bundle?, performQuery: Boolean) {
        val now = CpblCalendarHelper.getNowTime()
        val config = FirebaseRemoteConfig.getInstance()
        val overrideValues = config.getBoolean("override_start_enabled")
        val y = if (overrideValues) Integer.parseInt(config.getString("override_start_year")) else 0
        val m = if (overrideValues) Integer.parseInt(config.getString("override_start_month")) else 0
        //[39]dolphin++ for rotation
        val kind = savedInstanceState?.getInt(CalendarActivity.KEY_GAME_KIND)
                ?: CpblCalendarHelper.getSuggestedGameKind(this)//[66]++
        val field = savedInstanceState?.getInt(CalendarActivity.KEY_GAME_FIELD) ?: 0
        val year = savedInstanceState?.getInt(CalendarActivity.KEY_GAME_YEAR)
                ?: if (overrideValues) now.get(Calendar.YEAR) - y else 0
        val month = savedInstanceState?.getInt(CalendarActivity.KEY_GAME_MONTH)
                ?: if (overrideValues) m - 1 else now.get(Calendar.MONTH)
        val debugMode = resources.getBoolean(R.bool.pref_engineer_mode)
        Handler().post {
            if (mSpinnerField != null) {
                mSpinnerField.setSelection(field)
            }
            if (mSpinnerKind != null) {
                mSpinnerKind.setSelection(kind)
            }
            if (mSpinnerYear != null) {
                mSpinnerYear.setSelection(if (debugMode) 1 else year)
            }
            if (mSpinnerMonth != null) {
                mSpinnerMonth.setSelection(if (debugMode) 5 else month)
            }
            if (performQuery && mButtonQuery != null) {
                mButtonQuery.performClick()//load at beginning
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.splash, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        // If the nav drawer is open, hide action items related to the content view
        val drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerList)
        val visible = !drawerOpen and !IsQuery()
        menu.findItem(R.id.action_settings)?.isVisible = visible
        menu.findItem(R.id.action_refresh)?.isVisible = false
        menu.findItem(R.id.action_leader_board)?.apply {
            isVisible = true
            isEnabled = visible//keep the menu order
        }
        menu.findItem(R.id.action_search)?.isVisible = true
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_search -> {
                mDrawerLayout.openDrawer(mDrawerList)
                return true
            }
            R.id.action_settings -> (application as CpblApplication).isUpdateRequired = false
        }
        return super.onOptionsItemSelected(item)
    }

    override fun getActivity(): AppCompatActivity = this

    public override fun getOnQueryCallback(): OnQueryCallback = this

    override fun onQueryStart() {
        mDrawerLayout.closeDrawer(mDrawerList)

        val bundle = Bundle()
        mSpinnerField?.let { bundle.putString("field", it.selectedItem.toString()) }
        mSpinnerKind?.let {
            bundle.putString(FirebaseAnalytics.Param.SEARCH_TERM, it.selectedItem.toString())
        }
        mSpinnerYear?.let { bundle.putString("year", it.selectedItem.toString()) }
        mSpinnerMonth?.let { bundle.putString("month", it.selectedItem.toString()) }
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SEARCH, bundle)

        invalidateOptionsMenu() // creates call to onPrepareOptionsMenu()
        onLoading(true)//[30]dolphin++
    }

    override fun onQueryStateChange(msg: String?) {
        //Log.d(TAG, "onQueryUpdate: " + msg);
        if (progressText != null) {
            progressText.text = msg
        }

        //if (mSnackbar != null && mSnackbar.isShown()) {
        //    mSnackbar.setText(msg);
        //} else {
        //    mSnackbar = Snackbar.make(findViewById(R.id.main_content_frame), msg, Snackbar.LENGTH_INDEFINITE);
        //    mSnackbar.show();
        //}
        showSnackbar(msg)
    }

    override fun onQuerySuccess(helper: CpblCalendarHelper?, gameArrayList: ArrayList<Game>?, year: Int,
                                month: Int) {
        mDrawerLayout.closeDrawer(mDrawerList)
        //Log.d(TAG, "onSuccess");
        updateGameListFragment(gameArrayList, year, month)
        invalidateOptionsMenu() // creates call to onPrepareOptionsMenu()
    }

    private fun updateGameListFragment(gameArrayList: ArrayList<Game>?, year: Int, month: Int) {
        //mGameList = gameArrayList;//as a temp storage
        Log.d(TAG, "update list: ${gameArrayList?.size} for $year/$month")

        //if (fmgr != null) {//update the fragment
        val trans = fragmentManager.beginTransaction()
        val frag1 = fragmentManager.findFragmentById(R.id.main_content_frame) as GameListFragment
        val y = if (mSpinnerYear != null) mSpinnerYear.selectedItem.toString() else year.toString()
        val m = if (mSpinnerMonth != null) mSpinnerMonth.selectedItem.toString() else month.toString()
        frag1.updateAdapter(gameArrayList, y, m)
        try {
            trans.commitAllowingStateLoss()//[30]dolphin++
        } catch (e: IllegalStateException) {
            Log.e(CalendarActivity.TAG, "updateGameListFragment: " + e.message)
            sendTrackerException()
        }
        //}

        super.onLoading(false)
        doOnLoading2017(false)
    }

    override fun onQueryError(year: Int, month: Int) {
        //Log.e(TAG, "onError");
        onLoading(false)//[30]dolphin++
        updateGameListFragment(ArrayList(), year, month)//[59]++ no data
        Toast.makeText(activity, R.string.query_error,
                Toast.LENGTH_SHORT).show()//[31]dolphin++
        invalidateOptionsMenu() // creates call to onPrepareOptionsMenu()
    }

    public override fun onLoading(is_load: Boolean) {
        //if (fmgr != null) {
        val trans = fragmentManager.beginTransaction()
        val frag1 = fragmentManager.findFragmentById(R.id.main_content_frame) as GameListFragment
        frag1.setListShown(is_load)
        try {
            trans.commitAllowingStateLoss()//[30]dolphin++
        } catch (e: IllegalStateException) {
            Log.e(CalendarActivity.TAG, "onLoading: " + e.message)
            sendTrackerException()
        }

        //}
        //        View fab = findViewById(R.id.button_floating_action);
        //        if (fab != null) {
        //            fab.setEnabled(!is_load);
        //        }
        //        if (mFavTeams != null) {
        //            mFavTeams.setEnabled(!is_load);
        //        }
        super.onLoading(is_load)
        if (progressText != null) {
            progressText.visibility = View.GONE
        }
        doOnLoading2017(is_load)
    }

    private fun doOnLoading2017(visible: Boolean) {
        //        if (mBottomSheetBackground != null) {
        //            mBottomSheetBackground.setVisibility(is_load ? View.VISIBLE : View.GONE);
        //        }
        if (visible) {
            mFab.hide()
        } else {
            mFab.show()
        }

        if (mSwipeRefreshLayout != null) {
            mSwipeRefreshLayout!!.isRefreshing = visible
        }
        //        //show offline mode indicator
        //        if (!visible && PreferenceUtils.isCacheMode(getBaseContext())) {
        //            showSnackbar(getString(R.string.action_cache_mode));
        //        } else
        if (mSnackbar != null && !visible) {
            mSnackbar.dismiss()
            mSnackbar = null
        }
    }

    private fun sendTrackerException() {
        //        sendTrackerException("commitAllowingStateLoss", "phone", 0);
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        //[22]dolphin++ need get the data from server again!
        val application = application as CpblApplication

        // because the favorite team preference may remove some teams
        //onQuerySuccess(mGameList);//reload the fragment when possible preferences change
        if (application.isUpdateRequired && mButtonQuery != null) {
            mButtonQuery.performClick()//[22]dolphin++
        } else {
            quick_update()
        }
        //query_to_update(true);//[126]dolphin++ quick refresh

        application.isUpdateRequired = false//reset to default
    }

    override fun onFragmentAttached() {
        //[33]dolphin++
    }

    private fun loadAds() {
        if (mAdView == null) {
            return
        }
        // Create an ad request. Check logcat output for the hashed device ID to
        // get test ads on a physical device. e.g.
        // "Use AdRequest.Builder.addTestDevice("ABCDEF012345") to get test ads on this device."
        val adRequest = AdRequest.Builder()
                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                .build()
        mAdView!!.loadAd(adRequest)
    }

    override fun onPause() {
        if (mAdView != null/* && mAdView.getVisibility() == View.VISIBLE*/) {
            mAdView!!.pause()
        }
        super.onPause()
    }

    override fun onResume() {
        super.onResume()

        //https://developers.google.com/android/guides/setup
        val availability = GoogleApiAvailability.getInstance()
        val result = availability.isGooglePlayServicesAvailable(this)
        if (result != ConnectionResult.SUCCESS) {
            if (availability.isUserResolvableError(result)) {
                availability.getErrorDialog(this, result, 0) { this@CalendarForPhoneActivity.finish() }
            } else {
                Toast.makeText(this, "Google API unavailable", Toast.LENGTH_LONG).show()
                Log.e(CalendarActivity.TAG, "This device is not supported.")
                finish()
                return
            }
        }

        if (mAdView != null/* && mAdView.getVisibility() == View.VISIBLE*/) {
            mAdView!!.resume()
        }
    }

    /**
     * Called before the activity is destroyed
     */
    public override fun onDestroy() {
        super.onDestroy()
        if (mAdView != null) {
            mAdView!!.destroy()
        }
    }

    override fun onBackPressed() {
        if (mDrawerLayout.isDrawerOpen(mDrawerList)) {
            mDrawerLayout.closeDrawer(mDrawerList)
            return //[146]++
        }
        if (ENABLE_BOTTOM_SHEET && mBottomSheetBehavior != null
                && mBottomSheetBehavior!!.state == BottomSheetBehavior.STATE_EXPANDED) {
            setBottomSheetVisibility(false)
            return
        }
        super.onBackPressed()
    }

    private fun showFavTeamsDialog() {
        //Log.d(TAG, "show fav teams dialog");
        MultiChoiceListDialogFragment(activity,
                object : MultiChoiceListDialogFragment.OnClickListener {
                    override fun onOkay() {
                        updateFavTeamsSummary()
                    }

                    override fun onCancel() {

                    }
                }).show(supportFragmentManager, "CpblFavTeams")
    }

    private fun updateFavTeamsSummary() {
        var summary = ""
        mPrefsHelper.favoriteTeams.let {
            if (it.size() > 0) {
                if (it.size() == resources.getStringArray(R.array.cpbl_team_id).size) {
                    summary = getString(R.string.title_favorite_teams_all)
                } else {
                    for (i in 0 until it.size()) {
                        summary += it.valueAt(i).shortName + " "
                    }
                    summary = summary.trim { it <= ' ' }
                }
            } else {
                summary = getString(R.string.no_favorite_teams)
            }
        }
        mFavTeams.text = summary
    }

    override fun onScrollStateChanged(absListView: AbsListView, scrollState: Int) {
        if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE) {
            if (!mAlreadyBottom) {
                mFab.show()
            }
        } else if (mFab.isShown) {
            mFab.hide()
        }
    }

    override fun onScroll(absListView: AbsListView, firstVisibleItem: Int, visibleItemCount: Int,
                          totalItemCount: Int) {
        //http://stackoverflow.com/a/5123903
        mAlreadyBottom = firstVisibleItem + visibleItemCount >= totalItemCount
    }

    override fun onItemClick(adapterView: AdapterView<*>, view: View?, position: Int, id: Long) {
        //if (ENABLE_BOTTOM_SHEET) {//more options
        //    setBottomSheetVisibility(true, (Game) view.getTag(), view);
        //}//else
        if (view != null) {//old method
            showGameActivity(view.tag as Game)
        }
    }

    private fun setBottomSheetVisibility(visible: Boolean, game: Game? = null) {
        setBottomSheetVisibility(visible, game, null)
    }

    private fun setBottomSheetVisibility(visible: Boolean, game: Game?, view: View?) {
        if (mBottomSheetBackground != null) {//use progress background
            val from = if (visible) 0.0f else 1.0f
            val to = if (visible) 1.0f else 0.0f
            //http://stackoverflow.com/a/20629036
            //https://developer.android.com/reference/android/view/animation/AlphaAnimation.html
            val animation1 = AlphaAnimation(from, to)
            animation1.duration = 200
            //animation1.setStartOffset(5000);
            animation1.fillAfter = true
            animation1.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation) {
                    //Log.d(TAG, "onAnimationStart");
                    if (visible) {
                        mBottomSheetBackground!!.visibility = View.VISIBLE
                    }
                }

                override fun onAnimationEnd(animation: Animation) {
                    //Log.d(TAG, "onAnimationEnd");
                    if (!visible) {
                        mBottomSheetBackground!!.visibility = View.GONE
                        //Log.d(TAG, "hide background");
                    }
                    //http://tomkuo139.blogspot.tw/2009/11/android-alphaanimation.html
                    mBottomSheetBackground!!.animation = null//clear animation
                }

                override fun onAnimationRepeat(animation: Animation) {

                }
            })
            mBottomSheetBackground!!.startAnimation(animation1)
        }
        if (mBottomSheetBehavior != null) {
            mBottomSheetBehavior!!.state = if (visible)
                BottomSheetBehavior.STATE_EXPANDED
            else
                BottomSheetBehavior.STATE_COLLAPSED
        }
        if (visible) {
            mFab.hide()
        } else {
            mFab.show()
        }

        //        int accountHeight = 0;//accountTextView.getHeight();
        if (game != null) {//check option visibility
            if (mBottomSheetTitle != null) {
                mBottomSheetTitle!!.text = String.format(Locale.US, "G%d %s", game.Id,
                        getString(R.string.msg_content_text, game.AwayTeam.shortName,
                                game.HomeTeam.shortName))
            }
            if (mBottomSheetOption1 != null) {
                if (game.canOpenUrl()) {
                    mBottomSheetOption1!!.visibility = View.VISIBLE
                    mBottomSheetOption1!!.isEnabled = true
                } else {
                    //                    accountHeight += mBottomSheetOption1.getHeight();
                    //                    mBottomSheetOption1.setVisibility(View.GONE);
                    mBottomSheetOption1!!.isEnabled = false
                }
                mBottomSheetOption1!!.tag = game
            }
            if (mBottomSheetOption2 != null) {
                if (game.IsLive || game.IsFinal) {
                    //                    accountHeight += mBottomSheetOption2.getHeight();
                    //                    mBottomSheetOption2.setVisibility(View.GONE);
                    mBottomSheetOption2!!.isEnabled = false
                } else {
                    mBottomSheetOption2!!.visibility = View.VISIBLE
                    mBottomSheetOption2!!.isEnabled = true
                }
                mBottomSheetOption2!!.tag = game
            }
            if (mBottomSheetOption4 != null) {
                mBottomSheetOption4!!.tag = game
            }
        }
        //        if (visible) {
        //            //http://stackoverflow.com/a/35804525
        //            CoordinatorLayout coordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinator_layout);
        //            View bottomSheet = findViewById(R.id.bottom_sheet);
        //            bottomSheet.getLayoutParams().height = bottomSheet.getHeight() - accountHeight;
        //            bottomSheet.requestLayout();
        //            mBottomSheetBehavior.onLayoutChild(coordinatorLayout, bottomSheet,
        //                    ViewCompat.LAYOUT_DIRECTION_LTR);
        //        }
    }

    private fun showGameActivity(game: Game) {
        if (!game.canOpenUrl()) {
            return //unable to open url
        }
        //log to Firebase Analytics
        val bundle = Bundle()
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, String.format(Locale.US,
                "%d-%d", game.StartTime.get(Calendar.YEAR), game.Id))
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, String.format(Locale.TAIWAN,
                "%s vs %s", game.AwayTeam.shortName, game.HomeTeam.shortName))
        bundle.putString(FirebaseAnalytics.Param.ITEM_CATEGORY, game.Kind)
        bundle.putString(FirebaseAnalytics.Param.ITEM_LOCATION_ID, game.Field)
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM, bundle)
        Utils.startGameActivity(activity, game)
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.bottom_sheet_option1 -> showGameActivity(view.tag as Game)
            R.id.bottom_sheet_option2 -> addToCalendar(view.tag as Game)
            R.id.bottom_sheet_option4 -> showFieldInfoActivity(view.tag as Game)
        }
        setBottomSheetVisibility(false)
    }

    //Android Essentials: Adding Events to the User’s Calendar
    //http://goo.gl/jyT75l
    private fun addToCalendar(game: Game) {
        val calIntent = Utils.createAddToCalendarIntent(this, game)
        if (PackageUtils.isCallable(activity, calIntent)) {
            startActivity(calIntent)
        }
    }

    private fun showFieldInfoActivity(game: Game) {
        game.getFieldId(baseContext)//update game field id
        val url = CpblCalendarHelper.URL_FIELD_2017.replace("@field", game.FieldId)
        //Log.d(TAG, "url = " + url);
        Utils.startBrowserActivity(activity, url)
    }
}
