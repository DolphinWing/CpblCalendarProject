package dolphin.android.apps.CpblCalendar;

import android.annotation.SuppressLint;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import dolphin.android.apps.CpblCalendar.preference.PreferenceUtils;
import dolphin.android.apps.CpblCalendar.provider.CpblCalendarHelper;
import dolphin.android.apps.CpblCalendar.provider.Game;
import dolphin.android.apps.CpblCalendar3.CpblApplication;
import dolphin.android.apps.CpblCalendar3.R;
import dolphin.android.util.PackageUtils;

/**
 * Created by dolphin on 2013/6/3.
 * <p/>
 * CalendarActivity for phone version, with a ActionBarDrawer pane.
 */
public class CalendarForPhoneActivity extends CalendarActivity implements OnQueryCallback,
        ActivityCompat.OnRequestPermissionsResultCallback, AbsListView.OnScrollListener,
        AdapterView.OnItemClickListener, View.OnClickListener {
    private boolean ENABLE_BOTTOM_SHEET = false;

    private DrawerLayout mDrawerLayout;

    private View mDrawerList;

    private TextView mFavTeams;
    //private ArrayList<Game> mGameList = null;
    private FloatingActionButton mFab;
    private BottomSheetBehavior mBottomSheetBehavior;
    private TextView mBottomSheetTitle;
    private View mBottomSheetBackground;
    private View mBottomSheetOption1, mBottomSheetOption2;
    @SuppressWarnings("unused")
    private View mBottomSheetOption3, mBottomSheetOption4;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    private AdView mAdView;
    private FirebaseAnalytics mFirebaseAnalytics;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar_phone);

        ENABLE_BOTTOM_SHEET = FirebaseRemoteConfig.getInstance()
                .getBoolean("enable_bottom_sheet_options");
        //Log.d(TAG, "ENABLE_BOTTOM_SHEET: " + ENABLE_BOTTOM_SHEET);

        mDrawerLayout = findViewById(R.id.drawer_layout);
        mDrawerList = findViewById(R.id.left_drawer);
        if (mDrawerList != null) {
            mDrawerList.setOnTouchListener(new View.OnTouchListener() {
                @SuppressLint("ClickableViewAccessibility")
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    return true;//used to avoid clicking objects behind drawer
                }
            });
        }

        if (mDrawerLayout != null) {
            // set a custom shadow that overlays the main content when the drawer opens
            mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            //if (getResources().getBoolean(R.bool.config_tablet)) {
            //    toolbar.setLogo(R.mipmap.ic_launcher);
            //}
        }
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            //actionBar.setDisplayUseLogoEnabled(true);
            //actionBar.setLogo(R.mipmap.ic_launcher);
            actionBar.setDisplayHomeAsUpEnabled(false);
        }

        initQueryPane();
        View favTeamLayout = findViewById(R.id.layout3);
        if (favTeamLayout != null) {
            favTeamLayout.setVisibility(View.VISIBLE);
            mFavTeams = findViewById(R.id.textView6);
            mFavTeams.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showFavTeamsDialog();
                }
            });
            updateFavTeamsSummary();
        }

        mAdView = findViewById(R.id.adView);
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        mSwipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        if (mSwipeRefreshLayout != null) {
            mSwipeRefreshLayout.setEnabled(false);
        }

        GameListFragment gameListFragment = (GameListFragment) getFragmentManager()
                .findFragmentById(R.id.main_content_frame);
        ListView listView = null;
        if (gameListFragment != null) {
            listView = gameListFragment.getListView();
            if (listView != null) {
                if (ENABLE_BOTTOM_SHEET) {
                    //Log.d(TAG, "use bottom sheet");
                    gameListFragment.setOnOptionClickListener(new GameAdapter.OnOptionClickListener() {
                        @Override
                        public void onOptionClicked(View view, Game game) {
                            setBottomSheetVisibility(true, game, view);
                        }
                    });
                }// else {
                //Log.d(TAG, "use list.onclick");
                listView.setOnItemClickListener(this);
                //}
            }
        }

        mFab = findViewById(R.id.button_floating_action);
        if (mFab != null) {
            mFab.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (mDrawerLayout != null) {
                                mDrawerLayout.openDrawer(mDrawerList);
                            }
                        }
                    }
            );

            //add FAB show/hide control
            if (listView != null) {
                listView.setOnScrollListener(this);
            }
        }

        prepareBottomSheet();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                loadAds();//load ads in the background
            }
        }, 500);

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
        autoLoadGames(savedInstanceState, true);
//        }
    }

    private void prepareBottomSheet() {
        View bottomSheet = findViewById(R.id.bottom_sheet);
        if (ENABLE_BOTTOM_SHEET && bottomSheet != null) {
            mBottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
            mBottomSheetBehavior.setPeekHeight(0);
            mBottomSheetBehavior.setHideable(true);
            //override parent class
            mBottomSheetTitle = findViewById(R.id.bottom_sheet_title);
            mBottomSheetBackground = findViewById(R.id.bottom_sheet_background);
            if (mBottomSheetBackground != null) {
                mBottomSheetBackground.setOnTouchListener(new View.OnTouchListener() {
                    @SuppressLint("ClickableViewAccessibility")
                    @Override
                    public boolean onTouch(View view, MotionEvent motionEvent) {
                        if (mBottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED
                                && motionEvent.getAction() == MotionEvent.ACTION_UP) {
                            setBottomSheetVisibility(false);
                        }
                        return true;//do nothing
                    }
                });
            }
            mBottomSheetOption1 = bottomSheet.findViewById(R.id.bottom_sheet_option1);
            if (mBottomSheetOption1 != null) {
                mBottomSheetOption1.setOnClickListener(this);
            }
            mBottomSheetOption2 = bottomSheet.findViewById(R.id.bottom_sheet_option2);
            if (mBottomSheetOption2 != null) {
                mBottomSheetOption2.setOnClickListener(this);
            }
//            mBottomSheetOption3 = bottomSheet.findViewById(R.id.bottom_sheet_option3);
//            if (mBottomSheetOption3 != null) {
//                mBottomSheetOption3.setOnClickListener(this);
//            }
            mBottomSheetOption4 = bottomSheet.findViewById(R.id.bottom_sheet_option4);
            if (mBottomSheetOption4 != null) {
                mBottomSheetOption4.setOnClickListener(this);
            }
        } else if (bottomSheet != null) {//don't show
            bottomSheet.setVisibility(View.GONE);
        }
    }

    private void autoLoadGames(Bundle savedInstanceState, final boolean performQuery) {
        Calendar now = CpblCalendarHelper.getNowTime();
        FirebaseRemoteConfig config = FirebaseRemoteConfig.getInstance();
        boolean overrideValues = config.getBoolean("override_start_enabled");
        int y = overrideValues ? Integer.parseInt(config.getString("override_start_year")) : 0;
        int m = overrideValues ? Integer.parseInt(config.getString("override_start_month")) : 0;
        //[39]dolphin++ for rotation
        final int kind = (savedInstanceState != null)
                ? savedInstanceState.getInt(KEY_GAME_KIND)
                : CpblCalendarHelper.getSuggestedGameKind(this);//[66]++
        final int field = (savedInstanceState != null)
                ? savedInstanceState.getInt(KEY_GAME_FIELD) : 0;
        final int year = (savedInstanceState != null)
                ? savedInstanceState.getInt(KEY_GAME_YEAR)
                : overrideValues ? now.get(Calendar.YEAR) - y : 0;
        final int month = (savedInstanceState != null)
                ? savedInstanceState.getInt(KEY_GAME_MONTH)
                : overrideValues ? m - 1 : now.get(Calendar.MONTH);
        final boolean debugMode = getResources().getBoolean(R.bool.pref_engineer_mode);
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                if (mSpinnerField != null) {
                    mSpinnerField.setSelection(field);
                }
                if (mSpinnerKind != null) {
                    mSpinnerKind.setSelection(kind);
                }
                if (mSpinnerYear != null) {
                    mSpinnerYear.setSelection(debugMode ? 1 : year);
                }
                if (mSpinnerMonth != null) {
                    mSpinnerMonth.setSelection(debugMode ? 5 : month);
                }
                if (performQuery && mButtonQuery != null) {
                    mButtonQuery.performClick();//load at beginning
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.splash, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // If the nav drawer is open, hide action items related to the content view
        boolean drawerOpen = mDrawerLayout != null && mDrawerList != null
                && mDrawerLayout.isDrawerOpen(mDrawerList);
        boolean visible = !drawerOpen & !IsQuery();
        MenuItem item1 = menu.findItem(R.id.action_settings);
        if (item1 != null) {
            item1.setVisible(visible);
        }
        MenuItem item2 = menu.findItem(R.id.action_refresh);
        if (item2 != null) {
            item2.setVisible(false);
        }
        MenuItem item3 = menu.findItem(R.id.action_leader_board);
        if (item3 != null) {
            item3.setVisible(true);
            item3.setEnabled(visible);//keep the menu order
        }
        MenuItem item4 = menu.findItem(R.id.action_search);
        if (item4 != null) {
            item4.setVisible(mDrawerLayout != null);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_search://[133]dolphin++
                if (mDrawerLayout != null && mDrawerList != null) {
                    mDrawerLayout.openDrawer(mDrawerList);
                }
                return true;
            case R.id.action_settings://set default values
                ((CpblApplication) getApplication()).setPreferenceChanged(false);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected AppCompatActivity getActivity() {
        return this;
    }

    @Override
    public OnQueryCallback getOnQueryCallback() {
        return this;
    }

    @Override
    public void onQueryStart() {
        if (mDrawerLayout != null && mDrawerList != null) {
            mDrawerLayout.closeDrawer(mDrawerList);
        }

        Bundle bundle = new Bundle();
        if (mSpinnerField != null) {
            String field = mSpinnerField.getSelectedItem().toString();
            bundle.putString("field", field);
        }
        if (mSpinnerKind != null) {
            String kind = mSpinnerKind.getSelectedItem().toString();
            bundle.putString(FirebaseAnalytics.Param.SEARCH_TERM, kind);
        }
        if (mSpinnerYear != null) {
            String year = mSpinnerYear.getSelectedItem().toString();
            bundle.putString("year", year);
        }
        if (mSpinnerMonth != null) {
            String month = mSpinnerMonth.getSelectedItem().toString();
            bundle.putString("month", month);
        }
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SEARCH, bundle);

        invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
        onLoading(true);//[30]dolphin++
    }

    @Override
    public void onQueryStateChange(String msg) {
        //Log.d(TAG, "onQueryUpdate: " + msg);
        if (getProgressText() != null) {
            getProgressText().setText(msg);
        }

//        if (mSnackbar != null && mSnackbar.isShown()) {
//            mSnackbar.setText(msg);
//        } else {
//            mSnackbar = Snackbar.make(findViewById(R.id.main_content_frame), msg, Snackbar.LENGTH_INDEFINITE);
//            mSnackbar.show();
//        }
        showSnackbar(msg);
    }

    @Override
    public void onQuerySuccess(CpblCalendarHelper helper, ArrayList<Game> gameArrayList, int year,
                               int month) {
        if (mDrawerLayout != null && mDrawerList != null) {
            mDrawerLayout.closeDrawer(mDrawerList);//[138]++ close it//[87]dolphin++ put to success?
        }
        //Log.d(TAG, "onSuccess");
        updateGameListFragment(gameArrayList, year, month);
        invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
    }

    private void updateGameListFragment(ArrayList<Game> gameArrayList, int year, int month) {
        //mGameList = gameArrayList;//as a temp storage

        FragmentManager fmgr = getFragmentManager();
        //if (fmgr != null) {//update the fragment
        FragmentTransaction trans = fmgr.beginTransaction();
        GameListFragment frag1 = (GameListFragment) fmgr.findFragmentById(R.id.main_content_frame);
        if (frag1 != null) {
            String y = mSpinnerYear != null ? mSpinnerYear.getSelectedItem().toString() : String.valueOf(year);
            String m = mSpinnerMonth != null ? mSpinnerMonth.getSelectedItem().toString() : String.valueOf(month);
            frag1.updateAdapter(gameArrayList, y, m);
            try {
                trans.commitAllowingStateLoss();//[30]dolphin++
            } catch (IllegalStateException e) {
                Log.e(TAG, "updateGameListFragment: " + e.getMessage());
                sendTrackerException();
            }
        }
        //}

        super.onLoading(false);
        doOnLoading2017(false);
    }

    @Override
    public void onQueryError(int year, int month) {
        //Log.e(TAG, "onError");
        onLoading(false);//[30]dolphin++
        updateGameListFragment(new ArrayList<Game>(), year, month);//[59]++ no data
        Toast.makeText(getActivity(), R.string.query_error,
                Toast.LENGTH_SHORT).show();//[31]dolphin++
        invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
    }

    @Override
    public void onLoading(boolean is_load) {
        FragmentManager fmgr = getFragmentManager();
        //if (fmgr != null) {
        FragmentTransaction trans = fmgr.beginTransaction();
        GameListFragment frag1 =
                (GameListFragment) fmgr.findFragmentById(R.id.main_content_frame);
        if (frag1 != null) {
            frag1.setListShown(is_load);
        }
        try {
            trans.commitAllowingStateLoss();//[30]dolphin++
        } catch (IllegalStateException e) {
            Log.e(TAG, "onLoading: " + e.getMessage());
            sendTrackerException();
        }
        //}
//        View fab = findViewById(R.id.button_floating_action);
//        if (fab != null) {
//            fab.setEnabled(!is_load);
//        }
//        if (mFavTeams != null) {
//            mFavTeams.setEnabled(!is_load);
//        }
        super.onLoading(is_load);
        if (getProgressText() != null) {
            getProgressText().setVisibility(View.GONE);
        }
        doOnLoading2017(is_load);
    }

    private void doOnLoading2017(boolean visible) {
//        if (mBottomSheetBackground != null) {
//            mBottomSheetBackground.setVisibility(is_load ? View.VISIBLE : View.GONE);
//        }
        if (mFab != null) {
            if (visible) {
                mFab.hide();
            } else {
                mFab.show();
            }
        }

        if (mSwipeRefreshLayout != null) {
            mSwipeRefreshLayout.setRefreshing(visible);
        }
//        //show offline mode indicator
//        if (!visible && PreferenceUtils.isCacheMode(getBaseContext())) {
//            showSnackbar(getString(R.string.action_cache_mode));
//        } else
        if (mSnackbar != null && !visible) {
            mSnackbar.dismiss();
            mSnackbar = null;
        }
    }

    private void sendTrackerException() {
//        sendTrackerException("commitAllowingStateLoss", "phone", 0);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //[22]dolphin++ need get the data from server again!
        CpblApplication application = (CpblApplication) getApplication();

        // because the favorite team preference may remove some teams
        //onQuerySuccess(mGameList);//reload the fragment when possible preferences change
        if (application.isUpdateRequired() && mButtonQuery != null) {
            mButtonQuery.performClick();//[22]dolphin++
        } else {
            quick_update();
        }
        //query_to_update(true);//[126]dolphin++ quick refresh

        application.setPreferenceChanged(false);//reset to default
    }

    @Override
    public void onFragmentAttached() {
        //[33]dolphin++
    }

    private void loadAds() {
        if (mAdView == null) {
            return;
        }
        // Create an ad request. Check logcat output for the hashed device ID to
        // get test ads on a physical device. e.g.
        // "Use AdRequest.Builder.addTestDevice("ABCDEF012345") to get test ads on this device."
        final AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                .build();
        mAdView.loadAd(adRequest);
    }

    @Override
    protected void onPause() {
        if (mAdView != null/* && mAdView.getVisibility() == View.VISIBLE*/) {
            mAdView.pause();
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();

        //https://developers.google.com/android/guides/setup
        GoogleApiAvailability availability = GoogleApiAvailability.getInstance();
        int result = availability.isGooglePlayServicesAvailable(this);
        if (result != ConnectionResult.SUCCESS) {
            if (availability.isUserResolvableError(result)) {
                availability.getErrorDialog(this, result, 0, new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialogInterface) {
                        CalendarForPhoneActivity.this.finish();
                    }
                });
            } else {
                Toast.makeText(this, "Google API unavailable", Toast.LENGTH_LONG).show();
                Log.e(TAG, "This device is not supported.");
                finish();
                return;
            }
        }

        if (mAdView != null/* && mAdView.getVisibility() == View.VISIBLE*/) {
            mAdView.resume();
        }
    }

    /**
     * Called before the activity is destroyed
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mAdView != null) {
            mAdView.destroy();
        }
    }

    @Override
    public void onBackPressed() {
        if (mDrawerLayout != null && mDrawerList != null) {
            if (mDrawerLayout.isDrawerOpen(mDrawerList)) {
                mDrawerLayout.closeDrawer(mDrawerList);
                return;//[146]++
            }
        }
        if (ENABLE_BOTTOM_SHEET && mBottomSheetBehavior != null
                && mBottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
            setBottomSheetVisibility(false);
            return;
        }
        super.onBackPressed();
    }

    private void showFavTeamsDialog() {
        //Log.d(TAG, "show fav teams dialog");
        new MultiChoiceListDialogFragment(getActivity(),
                new MultiChoiceListDialogFragment.OnClickListener() {
                    @Override
                    public void onOkay() {
                        updateFavTeamsSummary();
                    }

                    @Override
                    public void onCancel() {

                    }
                }).show(getSupportFragmentManager(), "CpblFavTeams");
    }

    private void updateFavTeamsSummary() {
        if (mFavTeams != null) {
            mFavTeams.setText(PreferenceUtils.getFavoriteTeamSummary(getActivity()));
        }
    }

    //http://stackoverflow.com/a/5123903
    private boolean mAlreadyBottom = false;

    @Override
    public void onScrollStateChanged(AbsListView absListView, int scrollState) {
        if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE) {
            if (!mAlreadyBottom && mFab != null) {
                mFab.show();
            }
        } else if (mFab != null && mFab.isShown()) {
            mFab.hide();
        }
    }

    @Override
    public void onScroll(AbsListView absListView, int firstVisibleItem, int visibleItemCount,
                         int totalItemCount) {
        //http://stackoverflow.com/a/5123903
        mAlreadyBottom = firstVisibleItem + visibleItemCount >= totalItemCount;
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        //if (ENABLE_BOTTOM_SHEET) {//more options
        //    setBottomSheetVisibility(true, (Game) view.getTag(), view);
        //}//else
        if (view != null) {//old method
            showGameActivity((Game) view.getTag());
        }
    }

    @SuppressWarnings("SameParameterValue")
    private void setBottomSheetVisibility(boolean visible) {
        setBottomSheetVisibility(visible, null);
    }

    @SuppressWarnings("SameParameterValue")
    private void setBottomSheetVisibility(boolean visible, Game game) {
        setBottomSheetVisibility(visible, game, null);
    }

    private void setBottomSheetVisibility(boolean visible, Game game, View view) {
        if (mBottomSheetBackground != null) {//use progress background
            final boolean isVisible = visible;
            float from = visible ? 0.0f : 1.0f;
            float to = visible ? 1.0f : 0.0f;
            //http://stackoverflow.com/a/20629036
            //https://developer.android.com/reference/android/view/animation/AlphaAnimation.html
            AlphaAnimation animation1 = new AlphaAnimation(from, to);
            animation1.setDuration(200);
            //animation1.setStartOffset(5000);
            animation1.setFillAfter(true);
            animation1.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    //Log.d(TAG, "onAnimationStart");
                    if (isVisible) {
                        mBottomSheetBackground.setVisibility(View.VISIBLE);
                    }
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    //Log.d(TAG, "onAnimationEnd");
                    if (!isVisible) {
                        mBottomSheetBackground.setVisibility(View.GONE);
                        //Log.d(TAG, "hide background");
                    }
                    //http://tomkuo139.blogspot.tw/2009/11/android-alphaanimation.html
                    mBottomSheetBackground.setAnimation(null);//clear animation
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            mBottomSheetBackground.startAnimation(animation1);
        }
        if (mBottomSheetBehavior != null) {
            mBottomSheetBehavior.setState(visible ? BottomSheetBehavior.STATE_EXPANDED
                    : BottomSheetBehavior.STATE_COLLAPSED);
        }
        if (mFab != null) {
            if (visible) {
                mFab.hide();
            } else {
                mFab.show();
            }
        }

//        int accountHeight = 0;//accountTextView.getHeight();
        if (game != null) {//check option visibility
            if (mBottomSheetTitle != null) {
                mBottomSheetTitle.setText(String.format(Locale.US, "G%d %s", game.Id,
                        getString(R.string.msg_content_text, game.AwayTeam.getShortName(),
                                game.HomeTeam.getShortName())));
            }
            if (mBottomSheetOption1 != null) {
                if (game.canOpenUrl()) {
                    mBottomSheetOption1.setVisibility(View.VISIBLE);
                    mBottomSheetOption1.setEnabled(true);
                } else {
//                    accountHeight += mBottomSheetOption1.getHeight();
//                    mBottomSheetOption1.setVisibility(View.GONE);
                    mBottomSheetOption1.setEnabled(false);
                }
                mBottomSheetOption1.setTag(game);
            }
            if (mBottomSheetOption2 != null) {
                if (game.IsLive || game.IsFinal) {
//                    accountHeight += mBottomSheetOption2.getHeight();
//                    mBottomSheetOption2.setVisibility(View.GONE);
                    mBottomSheetOption2.setEnabled(false);
                } else {
                    mBottomSheetOption2.setVisibility(View.VISIBLE);
                    mBottomSheetOption2.setEnabled(true);
                }
                mBottomSheetOption2.setTag(game);
            }
            if (mBottomSheetOption4 != null) {
                mBottomSheetOption4.setTag(game);
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

    private void showGameActivity(Game game) {
        if (!game.canOpenUrl()) {
            return;//unable to open url
        }
        if (mFirebaseAnalytics != null) {//log to Firebase Analytics
            Bundle bundle = new Bundle();
            bundle.putString(FirebaseAnalytics.Param.ITEM_ID, String.format(Locale.US,
                    "%d-%d", game.StartTime.get(Calendar.YEAR), game.Id));
            bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, String.format(Locale.TAIWAN,
                    "%s vs %s", game.AwayTeam.getShortName(), game.HomeTeam.getShortName()));
            bundle.putString(FirebaseAnalytics.Param.ITEM_CATEGORY, game.Kind);
            bundle.putString(FirebaseAnalytics.Param.ITEM_LOCATION_ID, game.Field);
            mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM, bundle);
        }
        Utils.startGameActivity(getActivity(), game);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.bottom_sheet_option1:
                showGameActivity((Game) view.getTag());
                break;
            case R.id.bottom_sheet_option2:
                addToCalendar((Game) view.getTag());
                break;
            case R.id.bottom_sheet_option4:
                showFieldInfoActivity((Game) view.getTag());
                break;
        }
        setBottomSheetVisibility(false);
    }

    //Android Essentials: Adding Events to the User’s Calendar
    //http://goo.gl/jyT75l
    private void addToCalendar(Game game) {
        Intent calIntent = Utils.createAddToCalendarIntent(this, game);
        if (PackageUtils.isCallable(getActivity(), calIntent)) {
            startActivity(calIntent);
        }
    }

    private void showFieldInfoActivity(Game game) {
        game.getFieldId(getBaseContext());//update game field id
        String url = CpblCalendarHelper.URL_FIELD_2017.replace("@field", game.FieldId);
        //Log.d(TAG, "url = " + url);
        Utils.startBrowserActivity(getActivity(), url);
    }
}