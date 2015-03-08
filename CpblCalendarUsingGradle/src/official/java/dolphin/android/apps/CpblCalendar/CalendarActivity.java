package dolphin.android.apps.CpblCalendar;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;

import java.util.ArrayList;
import java.util.Calendar;

import dolphin.android.app.ABSFragmentActivity;
import dolphin.android.apps.CpblCalendar.preference.AlarmHelper;
import dolphin.android.apps.CpblCalendar.preference.GBPreferenceActivity;
import dolphin.android.apps.CpblCalendar.preference.PreferenceActivity;
import dolphin.android.apps.CpblCalendar.preference.PreferenceUtils;
import dolphin.android.apps.CpblCalendar.provider.CpblCalendarHelper;
import dolphin.android.apps.CpblCalendar.provider.Game;
import dolphin.android.apps.CpblCalendar.provider.Stand;
import dolphin.android.net.HttpHelper;

/**
 * Created by dolphin on 2013/6/3.
 * <p/>
 * Base implementation of CalendarActivity for different style
 */
public abstract class CalendarActivity extends ABSFragmentActivity
        implements EmptyFragmentWithCallbackOnResume.OnFragmentAttachedListener {

    protected final static String TAG = "CalendarActivity";

    private final static boolean OFFLINE_DEBUG = false;

    private String[] mGameField;

    private String[] mGameKind;

    public final static String KEY_GAME_KIND = "kind";

    public final static String KEY_GAME_FIELD = "field";

    public final static String KEY_GAME_YEAR = "year";

    public final static String KEY_GAME_MONTH = "month";

    public Spinner mSpinnerKind;

    public Spinner mSpinnerField;

    public Spinner mSpinnerYear;

    public Spinner mSpinnerMonth;

    public Button mButtonQuery;

    private View mProgressView;

    private TextView mProgressText;//[84]dolphin++

    private GoogleAnalyticsHelper mAnalytics;

    private SparseArray<SparseArray<Game>> mDelayGames2014;//[118]dolphin++

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_Holo_green);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        super.onCreate(savedInstanceState);

        if (PreferenceUtils.isEngineerMode(this))//[28]dolphin
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        }

        //only after GB needs to set this
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            Utils.enableStrictMode();
        }

//        //[72]dolphin++ https://code.google.com/p/android-query/wiki/Service
//        MarketService ms = new MarketService(this);
//        ms.level(MarketService.REVISION).checkVersion();

        NotificationManager mNotifyMgr =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotifyMgr.cancelAll();//[51]dolphin++ clear all notifications

        mGameField = getResources().getStringArray(R.array.cpbl_game_field_id);
        mGameKind = getResources().getStringArray(R.array.cpbl_game_kind_id_2014);
        mCacheMode = PreferenceUtils.isCacheMode(this);//[83]dolphin++
        //Log.d(TAG, "mCacheMode = " + mCacheMode);

        //[33]dolphin++ java.lang.IllegalStateException
        // android.support.v4.app.FragmentManagerImpl.enqueueAction
        // http://stackoverflow.com/a/12681526
        if (savedInstanceState == null) {
            final FragmentManager fm = this.getSupportFragmentManager();
            final FragmentTransaction ft = fm.beginTransaction();
            final Fragment emptyFragmentWithCallback = new EmptyFragmentWithCallbackOnResume();
            ft.add(emptyFragmentWithCallback, EmptyFragmentWithCallbackOnResume.TAG);
            ft.commit();
        }

        //[56]++ refresh the alarm when open the activity
        if (PreferenceUtils.isEnableNotification(this)) {
            NotifyReceiver.setNextAlarm(this);
        }
        mActivity = getSFActivity();//[89]dolphin++ fix 1.2.0 java.lang.NullPointerException

        mAnalytics = new GoogleAnalyticsHelper((CpblApplication) getApplication(), getScreenName());
        mDelayGames2014 = new SparseArray<>();
    }

    @Override
    protected void onDestroy() {
        mActivity = null;//[91]dolphin++
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();

        registerReceiver(mRefreshListReceiver,
                new IntentFilter(NotifyReceiver.ACTION_DELETE_NOTIFICATION));
    }

    @Override
    protected void onPause() {
        unregisterReceiver(mRefreshListReceiver);

        super.onPause();
    }

    /**
     * initial the query pane
     */
    public void initQueryPane() {
        mSpinnerField = (Spinner) findViewById(R.id.spinner1);
        mSpinnerKind = (Spinner) findViewById(R.id.spinner2);
        mSpinnerYear = (Spinner) findViewById(R.id.spinner3);
        mSpinnerMonth = (Spinner) findViewById(R.id.spinner4);

        mProgressView = findViewById(android.R.id.progress);
        mProgressView.setOnTouchListener(new View.OnTouchListener() {//[123]++ capture all events
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return true;//do nothing
            }
        });
        mProgressText = (TextView) findViewById(android.R.id.message);

        final Calendar now = CpblCalendarHelper.getNowTime();
        mYear = now.get(Calendar.YEAR);//[78]dolphin++ add initial value
        mYear = (mYear > 2013) ? mYear : 2014;//[89]dolphin++ set initial value
        mMonth = now.get(Calendar.MONTH) + 1;//[78]dolphin++ add initial value

        mSpinnerYear.setAdapter(CpblCalendarHelper.buildYearAdapter(getBaseContext(), mYear));

//        //[87]dolphin++ hide spinner when not applicable
//        final View layout1 = findViewById(R.id.layout1);
//        final View layout2 = findViewById(R.id.layout2);
//        mSpinnerYear.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
//            @Override
//            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
//                //layout1.setVisibility(i == 0 ? View.INVISIBLE : View.VISIBLE);
//                layout2.setVisibility(i < (now.get(Calendar.YEAR) - 2013) ? View.INVISIBLE : View.VISIBLE);
//            }
//
//            @Override
//            public void onNothingSelected(AdapterView<?> adapterView) {
//
//            }
//        });

        mSpinnerYear.setEnabled(!mCacheMode);

        mSpinnerMonth.setAdapter(CpblCalendarHelper.buildMonthAdapter(getBaseContext()));

        mButtonQuery = (Button) findViewById(android.R.id.button1);
        mButtonQuery.setOnClickListener(onQueryClick);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (outState != null) {//create a new object for save state
            if (mSpinnerKind != null) {
                outState.putInt(KEY_GAME_KIND, mSpinnerKind.getSelectedItemPosition());
            }
            if (mSpinnerField != null) {
                outState.putInt(KEY_GAME_FIELD, mSpinnerField.getSelectedItemPosition());
            }
            if (mSpinnerYear != null) {
                outState.putInt(KEY_GAME_YEAR, mSpinnerYear.getSelectedItemPosition());
            }
            if (mSpinnerMonth != null) {
                outState.putInt(KEY_GAME_MONTH, mSpinnerMonth.getSelectedItemPosition());
            }

            super.onSaveInstanceState(outState);
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(com.actionbarsherlock.view.Menu menu) {
        mCacheMode = PreferenceUtils.isCacheMode(this);
        //Log.d(TAG, String.format("onPrepareOptionsMenu mCacheMode=%s", mCacheMode));
        MenuItem item = menu.findItem(R.id.action_cache_mode);
        if (item != null) {
            item.setIcon(mCacheMode ? R.drawable.holo_green_btn_check_on_holo_dark
                    : R.drawable.holo_green_btn_check_off_holo_dark);
            //item.setCheckable(mCacheMode);
            item.setVisible(!mIsQuery && getResources().getBoolean(R.bool.feature_cache_mode));
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(com.actionbarsherlock.view.MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings: {
                Intent i = new Intent();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    i.setClass(this, PreferenceActivity.class);
                } else {
                    i.setClass(this, GBPreferenceActivity.class);
                }
                startActivityForResult(i, 0);
            }
            return true;
            case R.id.action_refresh://[13]dolphin++
                if (PreferenceUtils.isCacheMode(this)) {
                    runDownloadCache();
                } else {
                    mButtonQuery.performClick();
                }
                return true;//break;
            case R.id.action_leader_board://[26]dolphin++
                //[87]dolphin-- showLeaderBoard(mHelper.getScoreBoardHtml());
                item.setVisible(false);
                showLeaderBoard2014();//[87]dolphin++
                return true;//break;
            case R.id.action_go_to_cpbl:
                mAnalytics.sendGmsGoogleAnalyticsReport("UI", "go_to_website", null);
                CpblCalendarHelper.startActivityToCpblSchedule(getBaseContext());
                return true;
            case R.id.action_cache_mode:
                if (mCacheMode) {//cancel
                    mCacheMode = false;
                    PreferenceUtils.setCacheMode(getBaseContext(), false);
                    item.setIcon(R.drawable.holo_green_btn_check_off_holo_dark);
                    //item.setCheckable(mCacheMode);
                    mButtonQuery.performClick();//refresh a again
                } else {//show confirm dialog
                    showCacheModeEnableDialog(item);
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private Button.OnClickListener onQueryClick = new Button.OnClickListener() {
        @Override
        public void onClick(View view) {
            onLoading(true);
            mIsQuery = true;//Log.d(TAG, "onQueryClick");
            setSupportProgressBarIndeterminateVisibility(true);

            String kind = mSpinnerKind.getSelectedItem().toString();
            getSActionBar().setTitle(kind);

            String gameYear = mSpinnerYear.getSelectedItem().toString();
            int year = Integer.parseInt(gameYear.split(" ")[0]);
            //Log.d(TAG, String.format("  mSpinnerYear: %d", year));
            int month = mSpinnerMonth.getSelectedItemPosition() + 1;
            //Log.d(TAG, String.format(" mSpinnerMonth: %d", month));
            int fieldIndex = mSpinnerField.getSelectedItemPosition();
            String fieldId = mGameField[fieldIndex];
            if (fieldIndex > 0) {
                getSActionBar().setTitle(String.format("%s%s%s", kind,
                        getString(R.string.title_at),
                        mSpinnerField.getSelectedItem().toString()));
            }

            getSActionBar().setSubtitle(String.format("%s %s",
                    gameYear, mSpinnerMonth.getSelectedItem().toString()));

            final boolean bDemoCache = getResources().getBoolean(R.bool.demo_cache);
            final SherlockFragmentActivity activity = getSFActivity();
            if (PreferenceUtils.isCacheMode(activity) || bDemoCache) {//do cache mode query
                doCacheModeQuery(activity, year, month, getOnQueryCallback());
            } else if (HttpHelper.checkNetworkAvailable(activity)/* && !bDemoCache*/) {
                doWebQuery(activity, mSpinnerKind.getSelectedItemPosition(),
                        year, month, fieldId, getOnQueryCallback());
            } else {//[35]dolphin++ check network
                Toast.makeText(activity, R.string.no_available_network,
                        Toast.LENGTH_LONG).show();//[47] change SHORT to LONG
                doQueryCallback(null);//[47]++ do callback to return
            }
        }
    };

    //public abstract void doQuery(int kind, int year, int month);

    /**
     * get Activity instance
     */
    public abstract SherlockFragmentActivity getSFActivity();

    public String getScreenName() {
        return GoogleAnalyticsHelper.SCREEN_CALENDAR_ACTIVITY_BASE;
    }

    /**
     * get OnQueryCallback interface implementation
     */
    public abstract OnQueryCallback getOnQueryCallback();

    public String getGameKind(int index) {
        return mGameKind[index];
    }

    private Activity mActivity;

    private OnQueryCallback mQueryCallback;

    private boolean mIsQuery = false;

    private int mKind = 0;//[78]dolphin++ add initial value

    private int mYear;

    private int mMonth;

    private String mField = "F00";

    private CpblCalendarHelper mHelper = null;

    private boolean mCacheMode = false;

    public boolean IsQuery() {
        return mIsQuery;
    }

    public TextView getProgressText() {
        return mProgressText;
    }

    /**
     * add this method not to be override by child activity
     */
    private void internalLoading(boolean is_load) {
        mButtonQuery.setEnabled(!is_load);//disable the button when loading
        mSpinnerField.setEnabled(!is_load);
        mSpinnerKind.setEnabled(!is_load);
        mSpinnerYear.setEnabled((!mCacheMode & !is_load));
        //Log.d(TAG, "mSpinnerYear isEnabled = " + mSpinnerYear.isEnabled());
        mSpinnerMonth.setEnabled(!is_load);

        if (mProgressView != null) {
            mProgressView.setVisibility(is_load ? View.VISIBLE : View.GONE);
        }
        if (mProgressText != null) {
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH)
//                mProgressText.animate().alpha(is_load ? 0 : 1).setDuration(500).start();
//            else
            mProgressText.setVisibility(is_load ? View.VISIBLE : View.GONE);
            mProgressText.setText(is_load ? getString(R.string.title_download) : "");
        }
        setSupportProgressBarIndeterminateVisibility(is_load);
    }

    public void onLoading(boolean is_load) {
        internalLoading(is_load);
    }

    /**
     * do query action to the web
     */
    public void doWebQuery(SherlockFragmentActivity activity,
                           int kind, int year, int month, String field,
                           OnQueryCallback callback) {
        mActivity = activity;
        mQueryCallback = callback;
        if (mQueryCallback != null)//call before start
        {
            mQueryCallback.onQueryStart();
        }
        mKind = kind;//[22]dolphin++
        mYear = year;//[22]dolphin++
        mMonth = month;//[22]dolphin++
        mField = field;//[44]dolphin++

        mHelper = new CpblCalendarHelper(mActivity);
        final String gameKind = getGameKind(mKind);
        final Resources resources = mActivity.getResources();
        new Thread(new Runnable() {
            @Override
            public void run() {
                //[118]dolphin++ add check delay games
                if (mDelayGames2014.get(mYear) == null) {
                    doQueryStateUpdateCallback(R.string.title_download_delay_games);
                    mDelayGames2014.put(mYear, mHelper.queryDelayGames2014(mActivity, mYear));
                }

                ArrayList<Game> gameList = null;
                try {
                    Calendar now = CpblCalendarHelper.getNowTime();
                    if (OFFLINE_DEBUG) {//offline debug
                        gameList = Utils.get_debug_list(getSFActivity(), mYear, mMonth);
                    } else if (resources.getBoolean(R.bool.demo_no_data)) {
                        gameList = new ArrayList<Game>();//null;//[74]++
                    } else {//try local cache
                        //query from Internet
//                        if (now.get(Calendar.YEAR) < 2014)
                        if (mYear < 2014) {
                            gameList = mHelper.query(gameKind, mYear, mMonth, mField);
                        } else if (resources.getBoolean(R.bool.demo_zxc22)) {
                            doQueryStateUpdateCallback(getString(R.string.title_download_from_zxc22,
                                    mYear, mMonth));
                            gameList = mHelper.query2014zxc(mMonth);
                        } else {//do real job
                            doQueryStateUpdateCallback(getString(R.string.title_download_from_cpbl,
                                    mYear, mMonth));
                            gameList = mHelper.query2014(mYear, mMonth, gameKind, mDelayGames2014.get(mYear));
//                            ArrayList<Game> tmpList = mHelper.query2014();
                            //.query(gameKind, mYear, mMonth, mField);
//                            if (gameList == null || gameList.size() <= 0) {//backup plan
                            //doQueryStateUpdateCallback(getString(R.string.title_download_from_zxc22,
                            //        mYear, mMonth));
//                            gameList = mHelper.query2014zxc(mMonth);
                            //ArrayList<Game> tmpList = mHelper.query2014zxc(mMonth);
//                            }
//                            try {//update the data from CPBL website
                            doQueryStateUpdateCallback(R.string.title_download_complete);
//                                //mergeGameList(gameList, tmpList, mHelper.getDelayGameList());
//                            } catch (Exception e) {
//                            }
                        }
                    }
                    doQueryStateUpdateCallback(R.string.title_download_complete);

                    if (resources.getBoolean(R.bool.feature_auto_load_next)) {
                        //auto load next month games
                        if (mMonth >= 3 && mMonth < 10 && now.get(Calendar.DAY_OF_MONTH) > 15) {
                            doQueryStateUpdateCallback(getString(R.string.title_download_from_zxc22,
                                    mYear, mMonth + 1));
                            ArrayList<Game> list = mHelper.query2014zxc(mMonth + 1);
                            for (Game g : list) {
                                if (g.StartTime.get(Calendar.DAY_OF_MONTH) > 15) {
                                    break;
                                }
                                gameList.add(g);
                            }

                            doQueryStateUpdateCallback(R.string.title_download_complete);
                        }
                    }

                    if (gameList != null) {
                        doQueryCallback(gameList);
                    } else {
                        throw new Exception("no data");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "doWebQuery: " + e.getMessage());
                    mAnalytics.sendTrackerException("doWebQuery", e.getMessage(), 0);

                    //can use cache, so try to read from cache
                    if (mHelper.canUseCache()) {
                        doQueryStateUpdateCallback(getString(R.string.title_download_from_cache,
                                mYear, mMonth));
                        gameList = mHelper.getCache(mYear, mMonth);//try to read from cache
                    } else {//try to get from web again
                        doQueryStateUpdateCallback(getString(R.string.title_download_from_zxc22,
                                mYear, mMonth));
                        gameList = mHelper.query2014zxc(mMonth);
                    }
                    doQueryCallback(gameList);
                }
            }
        }).start();
    }

    private ArrayList<Game> mergeGameList(ArrayList<Game> mainList, ArrayList<Game> refList,
                                          SparseArray<Game> delayList) {
        if (refList != null) {
            if (mainList != null) {
                for (Game g : mainList) {
                    //[95]dolphin++ check delay game first
                    Game d = delayList.get(g.Id);
                    if (g.IsDelay && d != null) {
                        //g.StartTime = delayList.get(g.Id).StartTime;
                        g.StartTime.set(Calendar.HOUR_OF_DAY,
                                d.StartTime.get(Calendar.HOUR_OF_DAY));
                        g.StartTime.set(Calendar.MINUTE,
                                d.StartTime.get(Calendar.MINUTE));
                    }
                    //[93]dolphin--
                    //if (g.IsFinal) {//no need to check time
                    //    continue;
                    //}
                    //Log.d(TAG, String.format("g=%d @ %d", g.Id,
                    //        g.StartTime.get(Calendar.DAY_OF_MONTH)));
                    for (Game t : refList) {
                        if (g.Id == t.Id) {
                            g.StartTime = t.StartTime;
                            g.Field = t.Field;
                            //Log.d(TAG, String.format("===> %02d:%02d",
                            //        g.StartTime.get(Calendar.HOUR_OF_DAY),
                            //        g.StartTime.get(Calendar.MINUTE)));
                            break;
                        }
                    }
                }
            } else {
                return refList;//gameList = tmpList;
            }
        }
        return mainList;
    }

    private void doQueryStateUpdateCallback(int resId) {
        doQueryStateUpdateCallback(getString(resId));
    }

    private void doQueryStateUpdateCallback(final String message) {
        if (mActivity != null && mQueryCallback != null) {
            CalendarActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mQueryCallback.onQueryStateChange(message);
                }
            });
        }
    }

    /**
     * callback of Query action
     */
    private void doQueryCallback(ArrayList<Game> list) {
        mIsQuery = false;

        mAnalytics.sendGmsGoogleAnalyticsReport("UI", "doQueryCallback",
                String.format("%04d/%02d:%s", mYear, mMonth,
                        GoogleAnalyticsHelper.getExtraMessage(list, mCacheMode)));

        if (list != null && mHelper != null && mHelper.canUseCache()
                && !PreferenceUtils.isCacheMode(this)) {
            //Log.d(TAG, String.format("write to cache %04d-%02d.json", mYear, mMonth));
            boolean r = mHelper.putCache(mYear, mMonth, list);
            Log.v(TAG, String.format("%04d-%02d.json result: %s", mYear, mMonth,
                    (r ? "success" : "failed")));
        }

        final ArrayList<Game> gameList = Utils.cleanUpGameList(mActivity, list, mYear,
                mSpinnerField.getSelectedItemPosition());
        //check if we still need to update the screen
        if (mActivity != null && mQueryCallback != null) {
            mActivity.runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    if (gameList != null) {
                        //update subtitle if needed
                        if (gameList.size() > 0 && gameList.get(0).Source == Game.SOURCE_ZXC22) {
                            getSFActivity().getSupportActionBar()
                                    .setSubtitle(String.format("%s: %s",
                                            getString(R.string.title_data_source),
                                            getString(R.string.summary_zxc22)));
                        }

                        //show offline mode indicator
                        if (PreferenceUtils.isCacheMode(getBaseContext())) {
                            getSFActivity().getSupportActionBar()
                                    .setSubtitle(R.string.action_cache_mode);
                        }

                        mQueryCallback.onQuerySuccess(mHelper, gameList);
                    } else {
                        mQueryCallback.onQueryError();
                    }

                    //internalLoading(false);//[87]dolphin++ but no need
                }
            });
        } else {
            Log.e(TAG, "what happened?");
        }
    }

    /**
     * check if the tutorial is done
     */
    public boolean isTutorialDone() {
        if (CpblCalendarHelper.getNowTime().get(Calendar.YEAR) < 2014) {
            if (PreferenceUtils.isEngineerMode(getBaseContext())) {
                return false;//[39]dolphin++ move to global method
            }
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
            return pref.getBoolean(PreferenceUtils.KEY_SHOWCASE_PHONE, false);
        }
        return true;//[70]jimmy++ new website don't use GET method
    }

    /**
     * set tutorial is done
     */
    public void setTutorialDone() {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean(PreferenceUtils.KEY_SHOWCASE_PHONE, true);
        editor.commit();
    }

    /**
     * show leader team board dialog
     */
    public void showLeaderBoard(String html) {
        try {//[42]dolphin++ add a try-catch //[43]catch all dialog
            //[42]dolphin++ WindowManager$BadTokenException reported @ 2013-07-23
            Utils.buildLeaderBoardDialog(CalendarActivity.this, html,
                    mSpinnerKind.getItemAtPosition(1).toString());
        } catch (Exception e) {
            Log.e(TAG, "showLeaderBoard: " + e.getMessage());
        }

        mAnalytics.sendGmsGoogleAnalyticsReport("UI", "showLeaderBoard", null);
    }

    ArrayList<Stand> mStanding = null;

    public void showLeaderBoard2014() {
        if (mStanding != null) {
            doShowLeaderBoard2014();
            return;
        }

        mIsQuery = true;//indicate that now it is downloading
        invalidateOptionsMenu();
        internalLoading(true);//download from website
        new Thread(new Runnable() {
            @Override
            public void run() {
                mStanding = mHelper.query2014LeaderBoard();
                if (mActivity != null) {//[91]dolphin++
                    CalendarActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            doShowLeaderBoard2014();
                        }
                    });
                }
            }
        }).start();
    }

    private void doShowLeaderBoard2014() {
        showLeaderBoard(Utils.prepareLeaderBoard2014(getSFActivity(), mStanding));
        internalLoading(false);
        mIsQuery = false;
        invalidateOptionsMenu();
    }

    private void showCacheModeEnableDialog(final MenuItem item) {
        AlertDialog dialog = Utils.buildEnableCacheModeDialog(CalendarActivity.this,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        mCacheMode = true;
                        PreferenceUtils.setCacheMode(getBaseContext(), true);
                        item.setIcon(R.drawable.holo_green_btn_check_on_holo_dark);
                        //item.setCheckable(mCacheMode);
                        //mButtonQuery.performClick();
                        mSpinnerYear.setSelection(0);//[87]dolphin++
                        mSpinnerField.setSelection(0);//[87]dolphin++
                        runDownloadCache();
                    }
                });
        dialog.show();
    }

    private void runDownloadCache() {
        onLoading(true);
        mIsQuery = true;//[88]dolphin++ indicate now is loading
        invalidateOptionsMenu();

        mHelper = new CpblCalendarHelper(mActivity);
        new Thread(new Runnable() {
            @Override
            public void run() {
                //[98]dolphin++ add check delay list
                SparseArray<Game> delayList = mHelper.getDelayGameList();
                for (int m = 3; m <= 10; m++) {
                    doQueryStateUpdateCallback(getString(R.string.title_download_from_cpbl,
                            mYear, m));
                    ArrayList<Game> list = mHelper.query("01", mYear, m, "F00");
//                    if (list == null) {
                    //doQueryStateUpdateCallback(getString(R.string.title_download_from_zxc22,
                    //        mYear, m));
//                        list = mHelper.query2014zxc(m);
//                    }
                    //ArrayList<Game> list2 = mHelper.query2014zxc(m);

//                    Log.v(TAG, "delayList.size() = " + delayList.size());
//                    for (int i = 0; i < delayList.size(); i++) {
//                        //check if the game is in this month
//                        Game d = delayList.get(delayList.keyAt(i));
//                        Log.v(TAG, " ID = " + d.Id + " " + d.StartTime.getTime().toString());
//                        if (d.StartTime.get(Calendar.MONTH) == m - 1) {
//                            boolean alreadyIn = false;
//                            //check if the game is already in the list
//                            for (Game g : list) {//[98]dolphin++ add check delay list
//                                if (g.Id == d.Id /*&& g.IsDelay*/) {
//                                    //g.StartTime = delayList.get(g.Id).StartTime;
//                                    g.StartTime.set(Calendar.HOUR_OF_DAY,
//                                            d.StartTime.get(Calendar.HOUR_OF_DAY));
//                                    g.StartTime.set(Calendar.MINUTE,
//                                            d.StartTime.get(Calendar.MINUTE));
//                                    alreadyIn = true;
//                                }
//                            }
//
//                            if (!alreadyIn) {//add the game to the list
//                                list.add(d);
//                            }
//                        }
//                    }
                    //list = mergeGameList2(list, list2, delayList);

                    boolean r = mHelper.putCache(mYear, m, list);
                    Log.v(TAG, String.format("write %04d/%02d result: %s", mYear, m,
                            (r ? "success" : "failed")));
                }
                doQueryStateUpdateCallback(R.string.title_download_complete);

                if (mActivity != null) {//[91]dolphin++
                    CalendarActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mButtonQuery.performClick();
                        }
                    });
                }
            }
        }).start();
    }

    private void doCacheModeQuery(SherlockFragmentActivity activity,
                                  int year, int month, OnQueryCallback callback) {
        mActivity = activity;
        mQueryCallback = callback;
        if (mQueryCallback != null) {//call before start
            mQueryCallback.onQueryStart();
        }
        mYear = year;
        mMonth = month;

        final Resources resources = mActivity.getResources();
        mHelper = new CpblCalendarHelper(mActivity);
        new Thread(new Runnable() {
            @Override
            public void run() {
                ArrayList<Game> gameList = mHelper.getCache(mYear, mMonth);
                Calendar now = CpblCalendarHelper.getNowTime();
                if (resources.getBoolean(R.bool.feature_auto_load_next)) {
                    //auto load next month games
                    if (mMonth >= 3 && mMonth < 10 && now.get(Calendar.DAY_OF_MONTH) > 15) {
                        doQueryStateUpdateCallback(getString(R.string.title_download_from_cache,
                                mYear, mMonth + 1));
                        ArrayList<Game> list = mHelper.query2014zxc(mMonth + 1);
                        for (Game g : list) {
                            if (g.StartTime.get(Calendar.DAY_OF_MONTH) > 15) {
                                break;
                            }
                            gameList.add(g);
                        }

                        doQueryStateUpdateCallback(getString(R.string.title_download_complete));
                    }
                }
                doQueryCallback(gameList);
            }
        }).start();
    }

    private ArrayList<Game> mergeGameList2(ArrayList<Game> mainList, ArrayList<Game> refList,
                                           SparseArray<Game> delayList) {
        //assume main list is refList as zxc22.idv.tw
        if (refList != null) {
            //assume refList is cpbl old website
            if (mainList != null) {
                for (Game game : mainList) {
                    if (refList.contains(game)) {//ok, the game stays
                        for (Game rg : refList) {
                            if (rg.Id == game.Id
                                    && rg.StartTime.get(Calendar.DAY_OF_YEAR) == game.StartTime
                                    .get(Calendar.DAY_OF_YEAR)) {
                                game.StartTime = rg.StartTime;
                                game.Field = rg.Field;
                            }
                        }
                    }//no in refList
                    Game d = delayList.get(game.Id);
                    if (game.IsDelay && d != null) {
                        //g.StartTime = delayList.get(g.Id).StartTime;
                        game.StartTime.set(Calendar.HOUR_OF_DAY,
                                d.StartTime.get(Calendar.HOUR_OF_DAY));
                        game.StartTime.set(Calendar.MINUTE,
                                d.StartTime.get(Calendar.MINUTE));
                    }
                }
            } else {//no main list
                return refList;
            }
        }
        return mainList;
    }

    protected void sendTrackerException(String action, String label, long evtValue) {
        mAnalytics.sendTrackerException(action, label, evtValue);
    }

    protected BroadcastReceiver mRefreshListReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null && action.equals(NotifyReceiver.ACTION_DELETE_NOTIFICATION)) {
                AlarmHelper helper = new AlarmHelper(context);
                helper.getAlarmList();
                mButtonQuery.performClick();
            }
        }
    };
}
