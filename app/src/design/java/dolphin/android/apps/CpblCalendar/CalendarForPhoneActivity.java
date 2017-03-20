package dolphin.android.apps.CpblCalendar;

import android.Manifest;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
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
import com.google.firebase.analytics.FirebaseAnalytics;

import java.util.ArrayList;
import java.util.Calendar;

import dolphin.android.apps.CpblCalendar.preference.PreferenceUtils;
import dolphin.android.apps.CpblCalendar.provider.CpblCalendarHelper;
import dolphin.android.apps.CpblCalendar.provider.Game;

/**
 * Created by dolphin on 2013/6/3.
 * <p/>
 * CalendarActivity for phone version, with a ActionBarDrawer pane.
 */
public class CalendarForPhoneActivity extends CalendarActivity implements OnQueryCallback,
        ActivityCompat.OnRequestPermissionsResultCallback, AbsListView.OnScrollListener,
        AdapterView.OnItemClickListener {

    private DrawerLayout mDrawerLayout;

    private View mDrawerList;

    private TextView mFavTeams;
    //private ArrayList<Game> mGameList = null;
    private FloatingActionButton mFab;
    private BottomSheetBehavior mBottomSheetBehavior;
    private View mBottomSheetBackground;

    private AdView mAdView;
    private FirebaseAnalytics mFirebaseAnalytics;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar_phone);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = findViewById(R.id.left_drawer);
        if (mDrawerList != null) {
            mDrawerList.setOnTouchListener(new View.OnTouchListener() {
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

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            //if (getResources().getBoolean(R.bool.config_tablet)) {
            //    toolbar.setLogo(R.mipmap.ic_launcher);
            //}
        }

        initQueryPane();
        View favTeamLayout = findViewById(R.id.layout3);
        if (favTeamLayout != null) {
            favTeamLayout.setVisibility(View.VISIBLE);
            mFavTeams = (TextView) findViewById(R.id.textView6);
            mFavTeams.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showFavTeamsDialog();
                }
            });
            updateFavTeamsSummary();
        }

        mAdView = (AdView) findViewById(R.id.adView);
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        GameListFragment gameListFragment = (GameListFragment) getFragmentManager()
                .findFragmentById(R.id.main_content_frame);
        ListView listView = gameListFragment.getListView();
        if (listView != null) {
            listView.setOnItemClickListener(this);
        }

        mFab = (FloatingActionButton) findViewById(R.id.button_floating_action);
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

        View bottomSheet = findViewById(R.id.bottom_sheet);
        mBottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        mBottomSheetBehavior.setPeekHeight(0);
        mBottomSheetBehavior.setHideable(true);
        //override parent class
        mBottomSheetBackground = findViewById(R.id.bottom_sheet_background);
        if (mBottomSheetBackground != null) {
            mBottomSheetBackground.setOnTouchListener(new View.OnTouchListener() {
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

        new Handler().post(new Runnable() {
            @Override
            public void run() {
                loadAds();//load ads in the background
            }
        });

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            autoLoadGames(savedInstanceState);
        } else {//ask user to grant permission
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                showRequestStorageRationale();
            } else {
                requestStoragePermission();
            }
        }
    }

    private void autoLoadGames(Bundle savedInstanceState) {
        //[39]dolphin++ for rotation
        final int kind = (savedInstanceState != null)
                ? savedInstanceState.getInt(KEY_GAME_KIND)
                : CpblCalendarHelper.getSuggestedGameKind(this);//[66]++
        final int field = (savedInstanceState != null)
                ? savedInstanceState.getInt(KEY_GAME_FIELD) : 0;
        final int year = (savedInstanceState != null)
                ? savedInstanceState.getInt(KEY_GAME_YEAR) : 0;
        final int month = (savedInstanceState != null)
                ? savedInstanceState.getInt(KEY_GAME_MONTH)
                : Calendar.getInstance().get(Calendar.MONTH);
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
                if (mButtonQuery != null) {
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
            item2.setVisible(visible);
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
                ((CpblApplication) getApplication()).setPrefrenceChanged(false);
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

    private Snackbar mSnackbar;

    @Override
    public void onQueryStateChange(String msg) {
        //Log.d(TAG, "onQueryUpdate: " + msg);
        if (getProgressText() != null) {
            getProgressText().setText(msg);
        }

        if (mSnackbar != null && mSnackbar.isShown()) {
            mSnackbar.setText(msg);
        } else {
            mSnackbar = Snackbar.make(findViewById(R.id.main_content_frame), msg, Snackbar.LENGTH_INDEFINITE);
            mSnackbar.show();
        }
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
            String y = mSpinnerYear != null ? mSpinnerYear.getSelectedItem().toString() : null;
            String m = mSpinnerMonth != null ? mSpinnerMonth.getSelectedItem().toString() : null;
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

//        if (mBottomSheetBackground != null) {
//            mBottomSheetBackground.setVisibility(View.GONE);
//        }
        if (mFab != null) {//enable search
            mFab.show();
        }
        if (mSnackbar != null) {//hide loading text
            mSnackbar.dismiss();
            mSnackbar = null;
        }
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

//        if (mBottomSheetBackground != null) {
//            mBottomSheetBackground.setVisibility(is_load ? View.VISIBLE : View.GONE);
//        }
        if (mFab != null) {
            if (is_load) {
                mFab.hide();
            } else {
                mFab.show();
            }
        }
        if (getProgressText() != null) {
            getProgressText().setVisibility(View.GONE);
        }
        if (mSnackbar != null && !is_load) {
            mSnackbar.dismiss();
            mSnackbar = null;
        }
    }

    private void sendTrackerException() {
        sendTrackerException("commitAllowingStateLoss", "phone", 0);
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

        application.setPrefrenceChanged(false);//reset to default
    }

    @Override
    public void OnFragmentAttached() {
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
        if (mAdView != null) {
            mAdView.destroy();
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (mDrawerLayout != null && mDrawerList != null) {
            if (mDrawerLayout.isDrawerOpen(mDrawerList)) {
                mDrawerLayout.closeDrawer(mDrawerList);
                return;//[146]++
            }
        }
        super.onBackPressed();
    }

    private void requestStoragePermission() {
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        boolean result;
        if (permissions.length > 0) {
            for (int i = 0; i < permissions.length; i++) {
                result = (grantResults[i] == PackageManager.PERMISSION_GRANTED);
                Log.v(TAG, "permission " + permissions[i] + (result ? " granted" : " denied"));
            }
        }
        autoLoadGames(null);
    }

    private void showRequestStorageRationale() {
        //show dialog to ask user to give permission
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(R.string.title_ask_permission)
                .setMessage(R.string.message_ask_permission)
                .setCancelable(false)
                .setPositiveButton(R.string.ok_ask_permission, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        requestStoragePermission();
                    }
                })
                .setNegativeButton(R.string.cancel_ask_permission, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        autoLoadGames(null);
                    }
                });
        builder.show();
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

    @Override
    public void onScrollStateChanged(AbsListView absListView, int scrollState) {
        if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE) {
            if (mFab != null) {
                mFab.show();
            }
        } else if (mFab != null && mFab.isShown()) {
            mFab.hide();
        }
    }

    @Override
    public void onScroll(AbsListView absListView, int firstVisibleItem, int visibleItemCount,
                         int totalItemCount) {
        //TODO: auto generated section
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
//        if (view != null) {
//            Game game = (Game) view.getTag();
//            if (mFirebaseAnalytics != null) {
//                Bundle bundle = new Bundle();
//                bundle.putString(FirebaseAnalytics.Param.ITEM_ID, String.format(Locale.US,
//                        "%d-%d", game.StartTime.get(Calendar.YEAR), game.Id));
//                bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, String.format(Locale.TAIWAN,
//                        "%s vs %s", game.AwayTeam.getShortName(), game.HomeTeam.getShortName()));
//                bundle.putString(FirebaseAnalytics.Param.ITEM_CATEGORY, game.Kind);
//                bundle.putString(FirebaseAnalytics.Param.ITEM_LOCATION_ID, game.Field);
//                mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM, bundle);
//            }
//            Utils.startGameActivity(getActivity(), game);
//        }
        setBottomSheetVisibility(true);
    }

    private void setBottomSheetVisibility(boolean visible) {
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
    }
}
