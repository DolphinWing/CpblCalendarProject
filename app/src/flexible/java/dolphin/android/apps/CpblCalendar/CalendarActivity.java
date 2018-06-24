package dolphin.android.apps.CpblCalendar;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.content.ContextCompat;
import dolphin.android.apps.CpblCalendar.preference.PrefsHelper;
import dolphin.android.apps.CpblCalendar.provider.CpblCalendarHelper;
import dolphin.android.apps.CpblCalendar.provider.Game;
import dolphin.android.apps.CpblCalendar3.R;
import dolphin.android.apps.CpblCalendar3.SettingsActivity3;
import dolphin.android.net.HttpHelper;

/**
 * Created by dolphin on 2013/6/3.
 * <p/>
 * Base implementation of CalendarActivity for different style
 */
@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public abstract class CalendarActivity extends AppCompatActivity//ActionBarActivity//Activity
        implements EmptyFragmentWithCallbackOnResume.OnFragmentAttachedListener {

    final static String TAG = "CalendarActivity";

    private final static boolean OFFLINE_DEBUG = false;
    private final static int KIND_ALLSTAR_INDEX = 3;

    private String[] mGameField;
    private String[] mGameKind;

    final static String KEY_GAME_KIND = "kind";
    final static String KEY_GAME_FIELD = "field";
    final static String KEY_GAME_YEAR = "year";
    final static String KEY_GAME_MONTH = "month";

    protected Spinner mSpinnerKind;
    protected Spinner mSpinnerField;
    protected Spinner mSpinnerYear;
    protected Spinner mSpinnerMonth;
    protected Button mButtonQuery;

    private View mProgressView;
    private TextView mProgressText;//[84]dolphin++

    private FirebaseAnalytics mFirebaseAnalytics;//[198]++
    private FirebaseRemoteConfig mRemoteConfig;//[199]++
    private PrefsHelper mPrefsHelper;

    private final SparseArray<SparseArray<Game>> mDelayGames2014 = new SparseArray<>();//[118]dolphin++
    private final SparseArray<ArrayList<Game>> mAllGamesCache = new SparseArray<>();//[146]++
    private final SparseIntArray mAllStarSpecialMonth = new SparseIntArray();

    protected Snackbar mSnackbar;

    //http://gunhansancar.com/change-language-programmatically-in-android/
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(Utils.onAttach(newBase));
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        //supportRequestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        super.onCreate(savedInstanceState);

        mPrefsHelper = new PrefsHelper(this);

        if (mPrefsHelper.getEngineerMode()) {//[28]dolphin
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        }

        Utils.enableStrictMode();

        NotificationManager mNotifyMgr =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (mNotifyMgr != null) {
            mNotifyMgr.cancelAll();//[51]dolphin++ clear all notifications
        }

        mGameField = getResources().getStringArray(R.array.cpbl_game_field_id);
        mGameKind = getResources().getStringArray(R.array.cpbl_game_kind_id_2014);
        mCacheMode = mPrefsHelper.getCacheModeEnabled();//[83]dolphin++
        //Log.d(TAG, "mCacheMode = " + mCacheMode);

//        //[56]++ refresh the alarm when open the activity
//        if (PreferenceUtils.isEnableNotification(this)) {
//            AlarmProvider.setNextAlarm((CpblApplication) getApplication());
//        }
        mActivity = getActivity();//[89]dolphin++ fix 1.2.0 java.lang.NullPointerException

//        mAnalytics = new GoogleAnalyticsHelper((CpblApplication) getApplication(),
//                GoogleAnalyticsHelper.SCREEN_CALENDAR_ACTIVITY_BASE);
        mRemoteConfig = FirebaseRemoteConfig.getInstance();
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
//        mDelayGames2014 = new SparseArray<>();
//        mAllGamesCache = new SparseArray<>();//[146]++

        //create a allstar year/month mapping
        for (String allstar : getResources().getString(R.string.allstar_month_override).split(";")) {
            String[] data = allstar.split("/");
            if (data.length >= 2) {
                mAllStarSpecialMonth.put(Integer.parseInt(data[0]), Integer.parseInt(data[1]));
            }
        }
    }

    @Override
    protected void onDestroy() {
        mActivity = null;//[91]dolphin++
        super.onDestroy();
    }

    protected abstract AppCompatActivity getActivity();

    /**
     * initial the query pane
     */
    protected void initQueryPane() {
        mSpinnerField = findViewById(R.id.spinner1);
        mSpinnerKind = findViewById(R.id.spinner2);
        mSpinnerYear = findViewById(R.id.spinner3);
        mSpinnerMonth = findViewById(R.id.spinner4);

        mProgressView = findViewById(android.R.id.progress);
        if (mProgressView != null) {
            mProgressView.setOnTouchListener(new View.OnTouchListener() {
                //[123]++ use touch to replace click, capture all events
                @SuppressLint("ClickableViewAccessibility")
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    return true;//do nothing
                }
            });
        }
        mProgressText = findViewById(android.R.id.message);

        final Calendar now = CpblCalendarHelper.getNowTime();
        mYear = now.get(Calendar.YEAR);//[78]dolphin++ add initial value
        mYear = (mYear > 2013) ? mYear : 2014;//[89]dolphin++ set initial value
        mMonth = now.get(Calendar.MONTH) + 1;//[78]dolphin++ add initial value
        if (mRemoteConfig.getBoolean("override_start_enabled")) {
            mYear = Integer.parseInt(mRemoteConfig.getString("override_start_year"));
            mMonth = Integer.parseInt(mRemoteConfig.getString("override_start_month"));
            //kind = mRemoteConfig.getString("override_start_kind");
        }

        if (mSpinnerYear != null) {
            mSpinnerYear.setAdapter(Utils.buildYearAdapter(getBaseContext(), mYear));
            mSpinnerYear.setEnabled(!mCacheMode);
            mSpinnerYear.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    if (mSpinnerMonth != null && mSpinnerKind != null &&
                            mSpinnerKind.getSelectedItemPosition() == KIND_ALLSTAR_INDEX) {
                        mSpinnerMonth.setSelection(getAllstarMonthPositionByYearPosition());
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {

                }
            });
        }

        if (mSpinnerMonth != null) {
            mSpinnerMonth.setAdapter(Utils.buildMonthAdapter(getBaseContext()));
        }

        if (mSpinnerKind != null) {
            mSpinnerKind.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                    if (mSpinnerMonth != null && position == KIND_ALLSTAR_INDEX) {
                        mSpinnerMonth.setSelection(getAllstarMonthPositionByYearPosition());
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {

                }
            });
        }

        mButtonQuery = findViewById(android.R.id.button1);
        if (mButtonQuery != null) {
            mButtonQuery.setOnClickListener(onQueryClick);
        }
    }

    protected void performButtonQuery() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (getActivity() != null && mButtonQuery != null) {
                    mButtonQuery.performClick();
                }
            }
        });
    }

    protected int getAllstarMonthPositionByYearPosition() {
        if (mSpinnerYear != null) {
            int position = mSpinnerYear.getSelectedItemPosition();
            int length = mSpinnerYear.getCount();
            int index = length - position;
            if (mAllStarSpecialMonth.get(index) != 0) {
                return mAllStarSpecialMonth.get(index) - 1;
            }
        }
        return 6;//July
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
    public boolean onPrepareOptionsMenu(Menu menu) {
        mCacheMode = mPrefsHelper.getCacheModeEnabled();
        //Log.d(TAG, String.format("onPrepareOptionsMenu mCacheMode=%s", mCacheMode));
        MenuItem item = menu.findItem(R.id.action_cache_mode);
        if (item != null) {
            //item.setIcon(mCacheMode ? R.drawable.holo_green_btn_check_on_holo_dark
            //        : R.drawable.holo_green_btn_check_off_holo_dark);
            item.setTitle(mCacheMode ? R.string.action_leave_cache_mode
                    : R.string.action_cache_mode);
            //item.setCheckable(mCacheMode);
            item.setVisible(!mIsQuery && getResources().getBoolean(R.bool.feature_cache_mode));
        }

        //[154]dolphin++ add check if to show quick month chooser
        int month = mSpinnerMonth != null ? mSpinnerMonth.getSelectedItemPosition() + 1 : 3;
        int year = mSpinnerYear != null ? mSpinnerYear.getSelectedItemPosition() : 0;
        int yearSize = mSpinnerYear != null ? mSpinnerYear.getCount() : 0;
        item = menu.findItem(R.id.action_fast_rewind);
        if (item != null) {
            if (mKind == KIND_ALLSTAR_INDEX) {
                item.setEnabled(year < yearSize - 1);
                item.setTitle(R.string.title_fast_rewind_y);
            } else {
                item.setEnabled(month > 1 && month <= 12);
                item.setTitle(R.string.title_fast_rewind);
            }
        }
        item = menu.findItem(R.id.action_fast_forward);
        if (item != null) {
            if (mKind == KIND_ALLSTAR_INDEX) {
                item.setEnabled(year > 0);
                item.setTitle(R.string.title_fast_forward_y);
            } else {
                item.setEnabled(month < 12);
                item.setTitle(R.string.title_fast_forward);
            }
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivityForResult(new Intent(this, SettingsActivity3.class), 0);
                return true;
            case R.id.action_refresh://[13]dolphin++
                if (mPrefsHelper.getCacheModeEnabled()) {
                    runDownloadCache();
                } else {
//                    if (mYear >= 2014) {//remove cache
                    mHelper.removeDelayGames2014(this, mYear);//delete cache file
                    mDelayGames2014.remove(mYear);//remove from memory
//                    }
                    mAllGamesCache.clear();//clear game cache in memory
                    mButtonQuery.performClick();
                }
                return true;//break;
            case R.id.action_leader_board://[26]dolphin++
                //[87]dolphin-- showLeaderBoard(mHelper.getScoreBoardHtml());
                item.setVisible(false);
                //showLeaderBoard2014();//[87]dolphin++
                showLeaderBoard2016();
                if (mFirebaseAnalytics != null) {
                    Bundle bundle = new Bundle();
                    bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "show_leader_board");
                    bundle.putString(FirebaseAnalytics.Param.ITEM_NAME,
                            getString(R.string.action_leader_board));
                    bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "button");
                    mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
                }
                return true;//break;
            case R.id.action_go_to_cpbl:
                if (mFirebaseAnalytics != null) {
                    Bundle bundle = new Bundle();
                    bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "go_to_website");
                    bundle.putString(FirebaseAnalytics.Param.ITEM_NAME,
                            getString(R.string.action_go_to_website));
                    bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "button");
                    mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
                }
                Utils.startActivityToCpblSchedule(mActivity, mYear, mMonth, getGameKind(mKind),
                        mField/*, true*/);
                return true;
            case R.id.action_cache_mode:
                if (mCacheMode) {//cancel
                    mCacheMode = false;
                    mPrefsHelper.setCacheModeEnabled(false);
                    item.setIcon(R.drawable.holo_green_btn_check_off_holo_dark);
                    //item.setCheckable(mCacheMode);
                    mButtonQuery.performClick();//refresh a again
                } else {//show confirm dialog
                    showCacheModeEnableDialog(item);
                }
                return true;
            case R.id.action_fast_rewind://[154]dolphin++
                //select previous one, and do query
                if (mKind == KIND_ALLSTAR_INDEX) {
                    if (mSpinnerYear != null &&
                            mSpinnerYear.getSelectedItemPosition() < mSpinnerYear.getCount() - 1) {
                        mSpinnerYear.setSelection(mSpinnerYear.getSelectedItemPosition() + 1);
                        if (mSpinnerMonth != null) {
                            mSpinnerMonth.setSelection(getAllstarMonthPositionByYearPosition());
                        }
                        if (mButtonQuery != null) {
                            mButtonQuery.performClick();
                        }
                        sendFirebaseAnalyticsQuickAccess("fast_rewind", R.string.title_fast_rewind_y);
                    }
                    break;
                }
                if (mSpinnerMonth != null && mSpinnerMonth.getSelectedItemPosition() > 0) {
                    mSpinnerMonth.setSelection(mSpinnerMonth.getSelectedItemPosition() - 1);
                    if (mButtonQuery != null) {
                        mButtonQuery.performClick();
                    }
                    sendFirebaseAnalyticsQuickAccess("fast_rewind", R.string.title_fast_rewind);
                }
                break;
            case R.id.action_fast_forward://[154]dolphin++
                //select proceeding one, and do query
                if (mKind == KIND_ALLSTAR_INDEX) {
                    if (mSpinnerYear != null && mSpinnerYear.getSelectedItemPosition() > 0) {
                        mSpinnerYear.setSelection(mSpinnerYear.getSelectedItemPosition() - 1);
                        if (mSpinnerMonth != null) {
                            mSpinnerMonth.setSelection(getAllstarMonthPositionByYearPosition());
                        }

                        if (mButtonQuery != null) {
                            mButtonQuery.performClick();
                        }
                        sendFirebaseAnalyticsQuickAccess("fast_forward", R.string.title_fast_forward_y);
                    }
                    break;
                }
                if (mSpinnerMonth != null && mSpinnerMonth.getSelectedItemPosition() < 12) {
                    mSpinnerMonth.setSelection(mSpinnerMonth.getSelectedItemPosition() + 1);
                    if (mButtonQuery != null) {
                        mButtonQuery.performClick();
                    }
                    sendFirebaseAnalyticsQuickAccess("fast_forward", R.string.title_fast_forward);
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void sendFirebaseAnalyticsQuickAccess(String id, int valueResId) {
        if (mFirebaseAnalytics != null) {
            Bundle bundle = new Bundle();
            bundle.putString(FirebaseAnalytics.Param.ITEM_ID, id);
            bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, getString(valueResId));
            bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "button");
            mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
        }
    }

    private final Button.OnClickListener onQueryClick = new Button.OnClickListener() {
        @Override
        public void onClick(View view) {
            query_to_update(false);
        }
    };

    @SuppressWarnings("SameParameterValue")
    private void query_to_update(boolean quick_refresh) {
        onLoading(true);
        mIsQuery = true;//Log.d(TAG, "onQueryClick");
        //setProgressBarIndeterminateVisibility(true);

        final ActionBar actionBar = getSupportActionBar();

        String kind = mSpinnerKind != null ? mSpinnerKind.getSelectedItem().toString() : "01";

        Calendar now = CpblCalendarHelper.getNowTime();
        String gameYear = mSpinnerYear != null ? mSpinnerYear.getSelectedItem().toString()
                : String.format(Locale.US, "%d ", now.get(Calendar.YEAR));
        int year = Integer.parseInt(gameYear.split(" ")[0]);
        //Log.d(TAG, String.format("  mSpinnerYear: %d", year));
        int month = mSpinnerMonth != null ? mSpinnerMonth.getSelectedItemPosition() + 1
                : now.get(Calendar.MONTH) + 1;
        //Log.d(TAG, String.format(" mSpinnerMonth: %d", month));
        int fieldIndex = mSpinnerField != null ? mSpinnerField.getSelectedItemPosition() : 0;
        String fieldId = mGameField[fieldIndex];
        String leagueYear = mSpinnerYear != null ? mSpinnerYear.getSelectedItem().toString() : "";
        leagueYear = leagueYear.contains(" ") ? leagueYear.substring(leagueYear.indexOf(" ") + 1) : leagueYear;
        leagueYear = leagueYear.isEmpty() ? gameYear : leagueYear.replace("(", "").replace(")", "").trim();
        if (actionBar != null) {
            actionBar.setSubtitle(leagueYear.concat(" ").concat(kind));
        }

        final boolean bDemoCache = getResources().getBoolean(R.bool.demo_cache);
        final Activity activity = getActivity();
        if (mPrefsHelper.getCacheModeEnabled() || bDemoCache) {//do cache mode query
            doCacheModeQuery(activity, year, month, getOnQueryCallback());
        } else if (quick_refresh) {//do a quick refresh from local variable
            doQueryCallback(mGameList, false);//[126]dolphin++
        } else if (HttpHelper.checkNetworkConnected(activity)/* && !bDemoCache*/) {
            int kindPos = mSpinnerKind != null ? mSpinnerKind.getSelectedItemPosition() : 1;
            doWebQuery(activity, kindPos, year, month, fieldId, getOnQueryCallback());
        } else {//[35]dolphin++ check network
            Toast.makeText(activity, R.string.no_available_network,
                    Toast.LENGTH_LONG).show();//[47] change SHORT to LONG
            doQueryCallback(null, false);//[47]++ do callback to return
        }
    }

    /**
     * get OnQueryCallback interface implementation
     */
    protected abstract OnQueryCallback getOnQueryCallback();

    private String getGameKind(int index) {
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

    protected boolean IsQuery() {
        return mIsQuery;
    }

    protected TextView getProgressText() {
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
            mProgressText.setVisibility(is_load ? View.VISIBLE : View.GONE);
            mProgressText.setText(is_load ? getString(R.string.title_download) : "");
        }
        //setProgressBarIndeterminateVisibility(is_load);
    }

    protected void onLoading(boolean is_load) {
        internalLoading(is_load);
    }

    /**
     * do query action to the web
     */
    private void doWebQuery(Activity activity,
                            int kind, int year, int month, String field,
                            OnQueryCallback callback) {
        mActivity = activity;
        mQueryCallback = callback;
        if (mQueryCallback != null) {//call before start
            mQueryCallback.onQueryStart();
        }
        if (mActivity == null) {
            doQueryCallback(null, false);
            return;
        }

        final boolean clearCache = year != mYear;
        mKind = kind;//[22]dolphin++
        mYear = year;//[22]dolphin++
        mMonth = month;//[22]dolphin++
        mField = field;//[44]dolphin++

        final String gameKind = getGameKind(mKind);
        final Resources resources = mActivity.getResources();
        final boolean allowCache = mRemoteConfig.getBoolean("enable_delay_games_from_cache");
        final boolean allowDrive = mRemoteConfig.getBoolean("enable_delay_games_from_drive");
        //Log.d(TAG, "allowCache = " + allowCache);
        //Log.d(TAG, "allowDrive = " + allowDrive);
        new Thread(new Runnable() {
            @Override
            public void run() {
                long startTime = System.currentTimeMillis();
                if (mHelper == null) {//new object to start get data
                    //we now query for ASP.NET, so reuse the same object
                    mHelper = new CpblCalendarHelper(mActivity);
                }
                if (clearCache) {//remove all data
                    mAllGamesCache.clear();
                }
//                //[118]dolphin++ add check delay games
//                //[158]dolphin++ only 2005 and after has delay games record in web page
//                if (mYear >= 2005 && mDelayGames2014.get(mYear) == null) {
//                    doQueryStateUpdateCallback(R.string.title_download_delay_games);
//                    mDelayGames2014.put(mYear, mHelper.queryDelayGames2014(mActivity, mYear));
//                }

                if (isDestroyed()) {
                    Log.w(TAG, "activity destroyed");
                    return;
                }

                ArrayList<Game> gameList;
                Calendar now = CpblCalendarHelper.getNowTime();
                boolean thisYear = (mYear == now.get(Calendar.YEAR));

                if (mMonth > 12) {//read all
                    gameList = new ArrayList<>();
                    for (int i = 1; i <= 12; i++) {
                        int key = mYear * 12 + i;
                        doQueryStateUpdateCallback(getString(R.string.title_download_from_cpbl,
                                mYear, i));
                        ArrayList<Game> list = mAllGamesCache.get(key);
                        if ((mKind == 0 && list == null) || mKind > 0) {//query from Internet
////                            if (mYear < 2014) {
////                                list = mHelper.query(gameKind, mYear, i, mField);
////                            } else {//do real job
//                            list = mHelper.query2014(mYear, i, gameKind,
//                                    mDelayGames2014.get(mYear));
////                            }
                            list = mHelper.query2016(mYear, i, gameKind, mField, allowCache, allowDrive);
                            if (isDestroyed()) {
                                Log.w(TAG, "activity destroyed");
                                return;
                            }
                            boolean hasData = (list != null && list.size() > 0);
                            if (mField.equals("F00") && mKind == 0) {//put to local cache
                                mAllGamesCache.put(key, hasData ? list : new ArrayList<Game>());
                                if (!thisYear && hasData) {
                                    mHelper.putLocalCache(mYear, i, mKind, list);
                                }
                            }
                        }
                        if (list != null) {
                            gameList.addAll(list);
                        }
                    }
                    doQueryStateUpdateCallback(R.string.title_download_complete);
                    long endTime = System.currentTimeMillis() - startTime;
                    Log.v(TAG, "time cost:" + endTime);
                    doQueryCallback(gameList, true);
                    return;
                }

                try {
                    if (OFFLINE_DEBUG) {//offline debug
                        gameList = Utils.get_debug_list(getActivity(), mYear, mMonth);
                    } else if (resources.getBoolean(R.bool.demo_no_data)) {
                        gameList = new ArrayList<>();//null;//[74]++
                    } else {//read from internet
                        doQueryStateUpdateCallback(getString(R.string.title_download_from_cpbl,
                                mYear, mMonth));
                        gameList = mHelper.query2016(mYear, mMonth, gameKind, mField, allowCache, allowDrive);
                        doQueryStateUpdateCallback(R.string.title_download_complete);
                    }
                    doQueryStateUpdateCallback(R.string.title_download_complete);

                    if (isDestroyed()) {
                        Log.w(TAG, "activity destroyed");
                        return;
                    }
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
                        doQueryCallback(gameList, true);
                    } else {
                        throw new Exception("no data");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "doWebQuery: " + e.getMessage());
//                    mAnalytics.sendTrackerException("doWebQuery", e.getMessage(), 0);

                    //can use cache, so try to read from cache
                    if (mHelper.canUseCache()) {
                        doQueryStateUpdateCallback(getString(R.string.title_download_from_cache,
                                mYear, mMonth));
                        gameList = mHelper.getCache(mYear, mMonth);//try to read from cache
                        doQueryCallback(gameList, false);
                    } else {//try to get from web again
                        doQueryStateUpdateCallback(getString(R.string.title_download_from_zxc22,
                                mYear, mMonth));
                        gameList = mHelper.query2014zxc(mMonth);
                        if (isDestroyed()) {
                            Log.w(TAG, "activity destroyed");
                            return;
                        }

                        doQueryCallback(gameList, true);
                    }
                }
            }
        }).start();
    }

    private void doQueryStateUpdateCallback(int resId) {
        doQueryStateUpdateCallback(getString(resId));
    }

    private void doQueryStateUpdateCallback(final String message) {
        if (getActivity() != null && mQueryCallback != null) {
            CalendarActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mQueryCallback.onQueryStateChange(message);
                }
            });
        }
    }

    private ArrayList<Game> mGameList;

    /**
     * callback of Query action
     */
    private void doQueryCallback(ArrayList<Game> list, boolean bFromWeb) {
        mIsQuery = false;
        mGameList = list;

        if (bFromWeb) {//[126]++ only send message to Analytics when data is from web
            Log.v(TAG, "data from web");
        }

        Calendar now = CpblCalendarHelper.getNowTime();
        boolean thisYear = (mYear == now.get(Calendar.YEAR));
        boolean beforeThisMonth = !thisYear || (mMonth < (now.get(Calendar.MONTH) + 1));
        if (list != null && mHelper != null && mHelper.canUseCache()
                && !mPrefsHelper.getCacheModeEnabled() && beforeThisMonth && mField.equals("F00")) {
            //Log.d(TAG, String.format("write to cache %04d-%02d.json", mYear, mMonth));
            boolean r = mHelper.putLocalCache(mYear, mMonth, mKind, list);
            Log.v(TAG, String.format("%04d-%02d-%d.json result: %s", mYear, mMonth, mKind,
                    (r ? "success" : "failed")));
        }

        final ArrayList<Game> gameList = Utils.cleanUpGameList(mActivity, list, mYear,
                mSpinnerField != null ? mSpinnerField.getSelectedItemPosition() : 0);
        if (mActivity != null && mQueryCallback != null) {
            final ActionBar actionBar = getActivity().getSupportActionBar();
            mActivity.runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    if (gameList != null) {
                        Log.d(TAG, "list size: " + gameList.size());
                        //update subtitle
                        if (gameList.size() > 0 && gameList.get(0).Source == Game.SOURCE_ZXC22
                                && getActivity() != null && actionBar != null) {
                            showModeIndicator(true, String.format("%s: %s",
                                    getString(R.string.title_data_source),
                                    getString(R.string.summary_zxc22)));
                        } else {
                            showModeIndicator(false);
                        }

                        //show offline mode indicator
                        if (mPrefsHelper.getCacheModeEnabled()) {
                            //showSnackbar(getString(R.string.action_cache_mode));
                            showModeIndicator(true, getString(R.string.action_cache_mode));
                        } else {
                            showModeIndicator(false);
                        }

                        mQueryCallback.onQuerySuccess(mHelper, gameList, mYear, mMonth);
                    } else {
                        mQueryCallback.onQueryError(mYear, mMonth);
                    }

                    //internalLoading(false);//[87]dolphin++ but no need
                }
            });
        } else {
            Log.e(TAG, "what happened?");
        }
    }

    protected void quick_update() {
        doQueryCallback(mGameList, false);
    }

    protected void doHighlightCacheUpdate(ArrayList<Game> list) {
        mActivity = getActivity();
        mQueryCallback = getOnQueryCallback();
        doQueryCallback(list, false);
    }

    private void showLeaderBoard2016() {
        //https://goo.gl/GtBKgp
        CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder()
                .setToolbarColor(ContextCompat.getColor(this, R.color.holo_green_dark));
        CustomTabsIntent customTabsIntent = builder.build();
        customTabsIntent.launchUrl(this, Utils.LEADER_BOARD_URI);

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
                        mPrefsHelper.setCacheModeEnabled(true);
                        item.setIcon(R.drawable.holo_green_btn_check_on_holo_dark);
                        //item.setCheckable(mCacheMode);
                        //mButtonQuery.performClick();
                        if (mSpinnerYear != null) {
                            mSpinnerYear.setSelection(0);//[87]dolphin++
                        }
                        if (mSpinnerMonth != null) {
                            mSpinnerField.setSelection(0);//[87]dolphin++
                        }
                        if (mFirebaseAnalytics != null) {
                            Bundle bundle = new Bundle();
                            bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "enable_cache_mode");
                            bundle.putString(FirebaseAnalytics.Param.ITEM_NAME,
                                    getString(R.string.action_cache_mode));
                            bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "button");
                            mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
                        }
                        runDownloadCache();
                    }
                });
        dialog.show();
    }

    protected void runDownloadCache() {
        onLoading(true);
        mIsQuery = true;//[88]dolphin++ indicate now is loading
        invalidateOptionsMenu();

        mYear = CpblCalendarHelper.getNowTime().get(Calendar.YEAR);
        mHelper = new CpblCalendarHelper(getActivity());
        mQueryCallback = getOnQueryCallback();
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int m = 2; m <= 11; m++) {
                    doQueryStateUpdateCallback(getString(R.string.title_download_from_cpbl,
                            mYear, m));
                    ArrayList<Game> list = //mHelper.query2014(mYear, m, null, delayList);
                            mHelper.query2016(mYear, m, "01", "");
                    boolean r = mHelper.putCache(mYear, m, list);
                    Log.v(TAG, String.format("write %04d/%02d result: %s", mYear, m,
                            (r ? "success" : "failed")));
                }
                doQueryStateUpdateCallback(R.string.title_download_complete);
                performButtonQuery();
            }
        }).start();
    }

    private void doCacheModeQuery(Activity activity,
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
                doQueryCallback(gameList, true);
            }
        }).start();
    }

    protected void showSnackbar(String message) {
        if (mSnackbar != null && mSnackbar.isShown()) {
            mSnackbar.setText(message);
        } else {
            mSnackbar = Snackbar.make(findViewById(R.id.main_content_frame), message,
                    Snackbar.LENGTH_INDEFINITE);
            mSnackbar.show();
        }
    }

    @SuppressWarnings("SameParameterValue")
    private void showModeIndicator(boolean visible) {
        showModeIndicator(visible, null);
    }

    private void showModeIndicator(boolean visible, String text) {
        TextView modeIndicator = findViewById(R.id.mode_indicator);
        if (modeIndicator != null) {
            modeIndicator.setVisibility(visible ? View.VISIBLE : View.GONE);
            if (text != null) {
                modeIndicator.setText(text);
            }
        }
    }
}
