package dolphin.android.apps.CpblCalendar;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.MapBuilder;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

import java.util.ArrayList;
import java.util.Calendar;

import dolphin.android.apps.CpblCalendar.preference.PreferenceUtils;
import dolphin.android.apps.CpblCalendar.provider.CpblCalendarHelper;
import dolphin.android.apps.CpblCalendar.provider.Game;

//import com.espian.showcaseview.OnShowcaseEventListener;
//import com.espian.showcaseview.ShowcaseView;

/**
 * Created by dolphin on 2013/6/3.
 * <p/>
 * CalendarActivity for tablet version, with a static query pane.
 */
public class CalendarForTabletActivity extends CalendarActivity
//        implements OnShowcaseEventListener
{
    private ArrayList<Game> mGameList = null;
    //    private ShowcaseView mShowcaseView;
    //private boolean mIsTutorialQuery = false;//[20]dolphin++
    private TextView mLeaderBoardTitle = null;
    private WebView mLeaderBoardContent = null;

    private boolean mEnableAdMob = false;
    private AdView adView;//[37]++ add AdMob Ads to screen

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar_tablet);

        initQueryPane();

//        //[20]dolphin++ add tablet tutorial
//        if (!isTutorialDone()) {
//            ShowcaseView.ConfigOptions co = new ShowcaseView.ConfigOptions();
//            co.hideOnClickOutside = false;
//            co.block = true;
//            mShowcaseView = ShowcaseView.insertShowcaseView(android.R.id.button1,
//                    this, getString(R.string.phone_activity_showcase_title),
//                    getString(R.string.tablet_activity_showcase_detail), co);
//
//            try {//in case that will have exception, directly set tutorial flag true
//                mShowcaseView.setOnShowcaseEventListener(this);
//                //mIsTutorialQuery = true;//mark that button click will be tutorial query
//            } catch (Exception e) {
//                Log.e(TAG, "show tutorial exception: " + e.getMessage());
//                setTutorialDone();
//            }
//        }

        View leaderBoard = findViewById(R.id.team_board);
        if (leaderBoard != null) {//[27]dolphin++ for xlarge screen
            mLeaderBoardTitle = (TextView) findViewById(android.R.id.title);
            mLeaderBoardTitle.setText(mSpinnerKind.getItemAtPosition(1).toString());
            mLeaderBoardContent = (WebView) findViewById(R.id.webView);

            //[29]dolphin++
            if (PreferenceUtils.isIncludeLeaderBoard(this)) {
                leaderBoard.setVisibility(View.VISIBLE);
            } else {
                //leaderBoard.setVisibility(View.INVISIBLE);
                leaderBoard.setVisibility(View.GONE);
            }
            leaderBoard.setVisibility(View.GONE);//[70]dolphin++ hide this due to new layout
        }

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
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                mSpinnerField.setSelection(field);
                mSpinnerKind.setSelection(kind);
                mSpinnerYear.setSelection(year);
                mSpinnerMonth.setSelection(month);

                ////[20]dolphin++ auto load when no tutorial required
                //if (isTutorialDone()) {
                mButtonQuery.performClick();//load at beginning
                //}
            }
        });

        //[70]dolphin++ hide left drawer since we can't change month or year now
        //[87]dolphin++ use new flag to control it
        if (!getResources().getBoolean(R.bool.feature_query_panel)) {
            View leftDrawer = findViewById(R.id.left_drawer);
            if (leftDrawer != null)
                leftDrawer.setVisibility(View.GONE);
            leftDrawer = findViewById(R.id.left_drawer_sw720dp);
            if (leftDrawer != null)//only sw720dp and larger
                leftDrawer.setVisibility(View.GONE);
        }

        mEnableAdMob = getResources().getBoolean(R.bool.feature_admob);
        if (mEnableAdMob) {
//        //[37]dolphin++ http://goo.gl/YOfr9
//        adView = new AdView(this, AdSize.BANNER, "a151e5722a9a626");
//        if (adView != null) {
//            LinearLayout layout = (LinearLayout) findViewById(R.id.adLayout);
//            if (layout != null) {
//                layout.addView(adView);
//            }
//
//            AdRequest request = new AdRequest();
//            request.addTestDevice(AdRequest.TEST_EMULATOR);
//            adView.loadAd(request);
//        }
            try {
                adView = new AdView(this);
                adView.setAdUnitId(getString(R.string.ad_trackingId));
                adView.setAdSize(AdSize.BANNER);

                LinearLayout layout = (LinearLayout) findViewById(R.id.adLayout);
                layout.addView(adView);

                AdRequest adRequest = new AdRequest.Builder()
                        .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                        .build();
                adView.loadAd(adRequest);
            } catch (Exception e) {
                adView = null;
            }
        } else {
            LinearLayout layout = (LinearLayout) findViewById(R.id.adLayout);
            layout.setVisibility(View.GONE);//[79]dolphin++ remove AdView for now
        }
    }

    @Override
    public SherlockFragmentActivity getSFActivity() {
        return this;
    }

    @Override
    protected void onPause() {
        //[88]dolphin++ AdMob service in Google Play Services
        if (adView != null) adView.pause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();

        //[88]dolphin++ AdMob service in Google Play Services
        //https://developer.android.com/google/play-services/setup.html?hl=zh-tw#ensure
        int errorCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        switch (errorCode) {
            case ConnectionResult.SUCCESS:
                break;
            case ConnectionResult.SERVICE_MISSING:
            case ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED:
            case ConnectionResult.SERVICE_DISABLED:
                showGooglePlayServiceErrorDialog(errorCode);
                return;
            default:
                break;
        }

        if (adView != null) adView.resume();
    }

    private void showGooglePlayServiceErrorDialog(int errorCode) {
//        Log.w(TAG, String.format("showGooglePlayServiceErrorDialog %d", errorCode));
//        GooglePlayServicesUtil.getErrorDialog(errorCode, CalendarForTabletActivity.this, 0,
//                new DialogInterface.OnCancelListener() {
//                    @Override
//                    public void onCancel(DialogInterface dialogInterface) {
//                        Log.w(TAG, "showGooglePlayServiceErrorDialog onCancel");
        adView = null;
        mEnableAdMob = false;
        LinearLayout layout = (LinearLayout) findViewById(R.id.adLayout);
        if (layout != null) layout.setVisibility(View.GONE);
//                    }
//                }
//        ).show();
    }

    @Override
    protected void onDestroy() {
//        if (adView != null)//[37]++ add AdMob Ads to screen
//            adView.destroy();
        if (adView != null) adView.destroy();//[88]dolphin++ now in Google Play Services
        super.onDestroy();
    }

    @Override
    public OnQueryCallback getOnQueryCallback() {
        return new OnQueryCallback() {
            @Override
            public void onQueryStart() {
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
                onLoading(true);//[30]dolphin++
            }

            @Override
            public void onQueryStateChange(String msg) {
                //Log.d(TAG, "onQueryUpdate: " + msg);
                if (getProgressText() != null)
                    getProgressText().setText(msg);
            }

            @Override
            public void onQuerySuccess(CpblCalendarHelper helper,
                                       ArrayList<Game> gameArrayList) {
                //if (mShowcaseView != null && mShowcaseView.isShown()) {//[20]dolphin++
                //    mShowcaseView.hide();
                //    //mShowcaseView = null;
                //}
                if (mLeaderBoardContent != null) {//[27]dolphin++
                    //Log.d(TAG, html);
                    // Encoding issue with WebView's loadData
                    // http://stackoverflow.com/a/9402988
                    mLeaderBoardContent.loadData(helper.getScoreBoardHtml(),
                            "text/html; charset=" + CpblCalendarHelper.ENCODE_UTF8, null);
                }

                updateGameListFragment(gameArrayList);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            @Override
            public void onQueryError() {
                onLoading(false);//[47]dolphin++
                updateGameListFragment(new ArrayList<Game>());//[59]++ no data
                Toast.makeText(getSFActivity(), R.string.query_error,
                        Toast.LENGTH_SHORT).show();//[31]dolphin++
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };
    }

    private void updateGameListFragment(ArrayList<Game> gameArrayList) {
        mGameList = gameArrayList;//as a temp storage

        FragmentManager fmgr = getSupportFragmentManager();
        if (fmgr != null) {//update the fragment
            FragmentTransaction trans = fmgr.beginTransaction();
            GameListFragment frag1 =
                    (GameListFragment) fmgr.findFragmentById(R.id.content_frame);
            if (frag1 != null) {
                frag1.updateAdapter(gameArrayList);
                trans.commitAllowingStateLoss();//[30]dolphin++
            }
        }

        super.onLoading(false);
    }

    @Override
    public void onLoading(boolean is_load) {
        FragmentManager fmgr = getSupportFragmentManager();
        if (fmgr != null) {
            FragmentTransaction trans = fmgr.beginTransaction();
            GameListFragment frag1 =
                    (GameListFragment) fmgr.findFragmentById(R.id.content_frame);
            if (frag1 != null)
                frag1.setListShown(is_load);
            try {
                trans.commitAllowingStateLoss();//[30]dolphin++
            } catch (IllegalStateException e) {
                Log.e(TAG, "onLoading: " + e.getMessage());
                EasyTracker easyTracker = EasyTracker.getInstance(this);
                if (easyTracker != null) {
                    easyTracker.send(MapBuilder.createEvent("Exception",
                            "commitAllowingStateLoss", "tablet", null).build());
                }
            }
        }

        super.onLoading(is_load);
    }

    @Override
    public boolean onCreateOptionsMenu(com.actionbarsherlock.view.Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getSupportMenuInflater().inflate(R.menu.splash, menu);
        //[29]dolphin++ check leader board show status
        MenuItem item = menu.findItem(R.id.action_leader_board);
        if (item != null &&
                (PreferenceUtils.isIncludeLeaderBoard(this)
                        || Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB))
            menu.removeItem(R.id.action_leader_board);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(com.actionbarsherlock.view.MenuItem item) {
        //do common actions in CalendarActivity
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPrepareOptionsMenu(com.actionbarsherlock.view.Menu menu) {
        //check if the ActionBar MenuItem is required
        MenuItem item = menu.findItem(R.id.menu_action_more);
        boolean visible = !IsQuery();
//        boolean cacheMode = PreferenceUtils.isCacheMode(this);//[87]dolphin++
        if (item != null) {//[25]dolphin++
            item.setVisible(visible);
        } else {
            menu.findItem(R.id.action_settings).setVisible(visible);
            menu.findItem(R.id.action_refresh).setVisible(visible);//[13]dolphin++
        }

        //if (Calendar.getInstance().get(Calendar.YEAR) < 2014) {//[70]dolphin++
        item = menu.findItem(R.id.action_leader_board);//[26]dolphin++
        if (item != null)
            item.setVisible(/*cacheMode ? false : */visible);//[87]dolphin++
        //}
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        View leaderBoard = findViewById(R.id.team_board);
        if (leaderBoard != null) {//[29]++ update the layout
            leaderBoard.setVisibility(PreferenceUtils.isIncludeLeaderBoard(this)
                    ? View.VISIBLE : View.GONE);
        }

        //reload the fragment when possible preferences change
        //[22]dolphin++ need get the data from server again!
        // because the favorite team preference may remove some teams
        //[22]dolphin-- updateGameListFragment(mGameList);
        mButtonQuery.performClick();//[22]dolphin++
    }

//    @Override
//    public void onShowcaseViewHide(ShowcaseView showcaseView) {
//        setTutorialDone();
//        //if (mIsTutorialQuery) {
//        //    mButtonQuery.performClick();
//        //    mIsTutorialQuery = false;
//        //}
//        mShowcaseView = null;
//    }
//
//    @Override
//    public void onShowcaseViewDidHide(ShowcaseView showcaseView) {
//        setTutorialDone();
//        mShowcaseView = null;
//    }
//
//    @Override
//    public void onShowcaseViewShow(ShowcaseView showcaseView) {
//
//    }

    @Override
    public void OnFragmentAttached() {
        //[33]dolphin++
    }
}