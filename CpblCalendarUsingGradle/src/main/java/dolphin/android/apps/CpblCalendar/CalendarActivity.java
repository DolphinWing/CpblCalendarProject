package dolphin.android.apps.CpblCalendar;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.MapBuilder;

import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;

import dolphin.android.app.ABSFragmentActivity;
import dolphin.android.apps.CpblCalendar.preference.GBPreferenceActivity;
import dolphin.android.apps.CpblCalendar.preference.PreferenceActivity;
import dolphin.android.apps.CpblCalendar.preference.PreferenceUtils;
import dolphin.android.apps.CpblCalendar.provider.CpblCalendarHelper;
import dolphin.android.apps.CpblCalendar.provider.Game;
import dolphin.android.apps.CpblCalendar.provider.Stand;
import dolphin.android.apps.CpblCalendar.provider.Team;
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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_Holo_green);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        super.onCreate(savedInstanceState);

        if (PreferenceUtils.isEngineerMode(this))//[28]dolphin
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);

        //only after GB needs to set this
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            // http://goo.gl/cmG1V , solve android.os.NetworkOnMainThreadException
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork() // or .detectAll() for all detectable problems
                    .penaltyLog()
                    .permitDiskWrites()//[19]dolphin++
                    .permitDiskReads()//[19]dolphin++
                    .build());
        }

//        //[72]dolphin++ https://code.google.com/p/android-query/wiki/Service
//        MarketService ms = new MarketService(this);
//        ms.level(MarketService.REVISION).checkVersion();

        NotificationManager mNotifyMgr =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotifyMgr.cancelAll();//[51]dolphin++ clear all notifications

        mGameField = getResources().getStringArray(R.array.cpbl_game_field_id);
        mGameKind = getResources().getStringArray(R.array.cpbl_game_kind_id);
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
        if (PreferenceUtils.isEnableNotification(this))
            NotifyReceiver.setNextAlarm(this);
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
        mProgressText = (TextView) findViewById(android.R.id.message);

        final Calendar now = CpblCalendarHelper.getNowTime();
        mYear = now.get(Calendar.YEAR);//[78]dolphin++ add initial value
        mMonth = now.get(Calendar.MONTH) + 1;//[78]dolphin++ add initial value

        String[] years = new String[now.get(Calendar.YEAR) - 1990 + 1];
        for (int i = 1990; i <= now.get(Calendar.YEAR); i++) {
            years[now.get(Calendar.YEAR) - i] = String.format("%d (%s)",
                    i, getString(R.string.title_cpbl_year, (i - 1989)));
            //years[now.get(Calendar.YEAR) - i] = String.format("%d (%s)",
            //        i, getString(R.string.title_cpbl_year));
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getBaseContext(),
                android.R.layout.simple_spinner_item, years);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinnerYear.setAdapter(adapter);

        //[87]dolphin++ hide spinner when not applicable
        final View layout1 = findViewById(R.id.layout1);
        final View layout2 = findViewById(R.id.layout2);
        mSpinnerYear.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                //layout1.setVisibility(i == 0 ? View.INVISIBLE : View.VISIBLE);
                layout2.setVisibility(i == 0 ? View.INVISIBLE : View.VISIBLE);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        mSpinnerYear.setEnabled(!mCacheMode);

        adapter = new ArrayAdapter<String>(getBaseContext(),
                R.layout.sherlock_spinner_item,
                new DateFormatSymbols(Locale.TAIWAN).getMonths());
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinnerMonth.setAdapter(adapter);

        mButtonQuery = (Button) findViewById(android.R.id.button1);
        mButtonQuery.setOnClickListener(onQueryClick);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (outState != null) {//create a new object for save state
            if (mSpinnerKind != null)
                outState.putInt(KEY_GAME_KIND, mSpinnerKind.getSelectedItemPosition());
            if (mSpinnerField != null)
                outState.putInt(KEY_GAME_FIELD, mSpinnerField.getSelectedItemPosition());
            if (mSpinnerYear != null)
                outState.putInt(KEY_GAME_YEAR, mSpinnerYear.getSelectedItemPosition());
            if (mSpinnerMonth != null)
                outState.putInt(KEY_GAME_MONTH, mSpinnerMonth.getSelectedItemPosition());

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
            item.setVisible(getResources().getBoolean(R.bool.feature_cache_mode));
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
                if (PreferenceUtils.isCacheMode(this))
                    runDownloadCache();
                else
                    mButtonQuery.performClick();
                return true;//break;
            case R.id.action_leader_board://[26]dolphin++
                //[87]dolphin-- showLeaderBoard(mHelper.getScoreBoardHtml());
                item.setVisible(false);
                showLeaderBoard2014();//[87]dolphin++
                return true;//break;
            case R.id.action_go_to_cpbl: {
                EasyTracker easyTracker = EasyTracker.getInstance(CalendarActivity.this);
                if (easyTracker != null) {
                    easyTracker.send(MapBuilder.createEvent("UI",//Category
                                    "go_to_website",//Action (required)
                                    null,//label
                                    null)//Event value (long)
                                    .build()
                    );
                }
            }
            CpblCalendarHelper.startActivityToCpblSchedule(getBaseContext());
            return true;
            case R.id.action_cache_mode:
                if (mCacheMode) {//cancel
                    mCacheMode = false;
                    PreferenceUtils.setCacheMode(getBaseContext(), mCacheMode);
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
            setSupportProgressBarIndeterminateVisibility(mIsQuery);

            String kind = mSpinnerKind.getSelectedItem().toString();
            getSActionBar().setTitle(kind);

            String gameYear = mSpinnerYear.getSelectedItem().toString();
            int year = Integer.parseInt(gameYear.split(" ")[0]);
            //Log.d(TAG, String.format("  mSpinnerYear: %d", year));
            int month = mSpinnerMonth.getSelectedItemPosition() + 1;
            //Log.d(TAG, String.format(" mSpinnerMonth: %d", month));
            int fieldIndex = mSpinnerField.getSelectedItemPosition();
            String fieldId = mGameField[fieldIndex];
            if (fieldIndex > 0)
                getSActionBar().setTitle(String.format("%s@%s", kind,
                        mSpinnerField.getSelectedItem().toString()));

            getSActionBar().setSubtitle(String.format("%s %s",
                    gameYear, mSpinnerMonth.getSelectedItem().toString()));

            final boolean bDemoCache = getResources().getBoolean(R.bool.demo_cache);
            final SherlockFragmentActivity activity = getSFActivity();
            if (PreferenceUtils.isCacheMode(activity) || bDemoCache) //do cache mode query
                doCacheModeQuery(activity, year, month, getOnQueryCallback());
            else if (HttpHelper.checkNetworkAvailable(activity) && !bDemoCache)
                doWebQuery(activity, mSpinnerKind.getSelectedItemPosition(),
                        year, month, fieldId, getOnQueryCallback());
            else {//[35]dolphin++ check network
                Toast.makeText(activity, R.string.no_available_network,
                        Toast.LENGTH_LONG).show();//[47] change SHORT to LONG
                doQueryCallback(null);//[47]++ do callback to return
            }
        }
    };

    //public abstract void doQuery(int kind, int year, int month);

    /**
     * get Activity instance
     *
     * @return
     */
    public abstract SherlockFragmentActivity getSFActivity();

    /**
     * get OnQueryCallback interface implementation
     *
     * @return
     */
    public abstract OnQueryCallback getOnQueryCallback();

    public String getGameKind(int index) {
        return mGameKind[index];
    }

    public interface OnQueryCallback {
        public void onQueryStart();

        public void onQueryStateChange(String msg);

        public void onQuerySuccess(CpblCalendarHelper helper,
                                   ArrayList<Game> gameArrayList);

        public void onQueryError();
    }

    private Activity mActivity;
    private OnQueryCallback mQueryCallback;
    private boolean mIsQuery = false;
    private int mKind = 1;//[78]dolphin++ add initial value
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
     *
     * @param is_load
     */
    private void internalLoading(boolean is_load) {
        mButtonQuery.setEnabled(!is_load);//disable the button when loading
        mSpinnerField.setEnabled(!is_load);
        mSpinnerKind.setEnabled(!is_load);
        mSpinnerYear.setEnabled((!mCacheMode & !is_load));
        //Log.d(TAG, "mSpinnerYear isEnabled = " + mSpinnerYear.isEnabled());
        mSpinnerMonth.setEnabled(!is_load);

        if (mProgressView != null)
            mProgressView.setVisibility(is_load ? View.VISIBLE : View.GONE);
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
     *
     * @param activity
     * @param kind
     * @param year
     * @param month
     * @param callback
     */
    public void doWebQuery(SherlockFragmentActivity activity,
                           int kind, int year, int month, String field,
                           OnQueryCallback callback) {
        mActivity = activity;
        mQueryCallback = callback;
        if (mQueryCallback != null)//call before start
            mQueryCallback.onQueryStart();
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
                ArrayList<Game> gameList = null;
                try {
                    Calendar now = CpblCalendarHelper.getNowTime();
                    if (OFFLINE_DEBUG) {//offline debug
                        gameList = get_debug_list(mYear, mMonth);
                    } else if (resources.getBoolean(R.bool.demo_no_data)) {
                        gameList = new ArrayList<Game>();//null;//[74]++
                    } else {//try local cache
                        //query from Internet
//                        if (now.get(Calendar.YEAR) < 2014)
                        if (mYear < 2014)
                            gameList = mHelper.query(gameKind, mYear, mMonth, mField);
                        else if (resources.getBoolean(R.bool.demo_zxc22)) {
                            doQueryStateUpdateCallback(getString(R.string.title_download_from_zxc22,
                                    mYear, mMonth));
                            gameList = mHelper.query2014zxc(mMonth);
                        } else {//do real job
                            doQueryStateUpdateCallback(getString(R.string.title_download_from_cpbl,
                                    mYear, mMonth));
//                            gameList = mHelper.query2014();
                            ArrayList<Game> tmpList = mHelper.query(gameKind, mYear, mMonth, mField);
//                            if (gameList == null || gameList.size() <= 0) {//backup plan
                            doQueryStateUpdateCallback(getString(R.string.title_download_from_zxc22,
                                    mYear, mMonth));
                            gameList = mHelper.query2014zxc(mMonth);
//                            }
                            try {//update the data from CPBL website
                                doQueryStateUpdateCallback(R.string.title_download_complete);
                                if (tmpList != null)
                                    if (gameList != null) {
                                        for (Game g : gameList) {
                                            for (Game t : tmpList) {
                                                if (g.Id == t.Id) {
                                                    g.StartTime = t.StartTime;
                                                    g.Field = t.Field;
                                                    //Log.d(TAG, g.StartTime.toString());
                                                    break;
                                                }
                                            }
                                        }
                                    } else gameList = tmpList;
                            } catch (Exception e) {
                            }
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
                                if (g.StartTime.get(Calendar.DAY_OF_MONTH) > 15)
                                    break;
                                gameList.add(g);
                            }

                            doQueryStateUpdateCallback(R.string.title_download_complete);
                        }
                    }

                    if (gameList != null)
                        doQueryCallback(gameList);
                    else
                        throw new Exception("no data");
                } catch (Exception e) {
                    Log.e(TAG, "doWebQuery: " + e.getMessage());
                    EasyTracker easyTracker = EasyTracker.getInstance(CalendarActivity.this);
                    if (easyTracker != null) {
                        easyTracker.send(MapBuilder.createEvent("Exception",//Category
                                        "doWebQuery",//Action (required)
                                        e.getMessage(),//label
                                        null)//Event value
                                        .build()
                        );
                    }

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

    private void doQueryStateUpdateCallback(int resId) {
        doQueryStateUpdateCallback(getString(resId));
    }

    private void doQueryStateUpdateCallback(final String message) {
        if (mActivity != null && mQueryCallback != null)
            CalendarActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mQueryCallback.onQueryStateChange(message);
                }
            });
    }

    /**
     * callback of Query action
     *
     * @param gameList
     */
    private void doQueryCallback(final ArrayList<Game> gameList) {
        mIsQuery = false;

        EasyTracker easyTracker = EasyTracker.getInstance(CalendarActivity.this);
        if (easyTracker != null) {
            int type = (gameList != null && gameList.size() > 0)
                    ? gameList.get(0).Source : -1;
            String extra = mCacheMode ? "cache" : "unknown";
            if (!mCacheMode)
                switch (type) {
                    case Game.SOURCE_CPBL:
                        extra = "cpbl";
                        break;
                    case Game.SOURCE_ZXC22:
                        extra = "zxc22";
                        break;
                    case Game.SOURCE_CPBL_2013:
                        extra = "cpbl_old";
                        break;
                }
            easyTracker.send(MapBuilder.createEvent("UI", "doQueryCallback",
                    String.format("%04d/%02d:%s", mYear, mMonth, extra), null).build());
        }

        if (gameList != null && mHelper != null && mHelper.canUseCache()
                && !PreferenceUtils.isCacheMode(this)) {
            Log.d(TAG, String.format("write to cache %04d-%02d.json", mYear, mMonth));
            boolean r = mHelper.putCache(mYear, mMonth, gameList);
            Log.v(TAG, String.format("result: %s", (r ? "success" : "failed")));
        }

        if (mActivity != null && mQueryCallback != null)
            mActivity.runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    if (gameList != null) {
                        //[22]dolphin++ add check the favorite teams
                        HashMap<Integer, Team> teams =
                                PreferenceUtils.getFavoriteTeams(mActivity);
                        //2013 has 4 teams only, should I check this?
                        if (teams.size() < 4 /*&& mYear >= 2013*/) {
                            for (Iterator<Game> i = gameList.iterator(); i.hasNext(); ) {
                                Game game = i.next();
                                if (teams.containsKey(game.HomeTeam.getId())
                                        || teams.containsKey(game.AwayTeam.getId())) {
                                } else {//remove from the list
                                    i.remove();
                                }
                            }
                        }

                        //check field data
                        //Log.d(TAG, mSpinnerField.getSelectedItem().toString());
                        if (mYear >= 2014 && mSpinnerField.getSelectedItemPosition() > 0) {
                            String field = mSpinnerField.getSelectedItem().toString();
                            for (Iterator<Game> i = gameList.iterator(); i.hasNext(); ) {
                                Game game = i.next();
                                if (!game.Field.contains(field)) {
                                    i.remove();
                                }
                            }
                        }

                        if (gameList.size() > 0) {//update subtitle
                            switch (gameList.get(0).Source) {
                                case Game.SOURCE_ZXC22:
                                    getSFActivity().getSupportActionBar()
                                            .setSubtitle(String.format("%s: %s",
                                                    getString(R.string.title_data_source),
                                                    getString(R.string.summary_zxc22)));
                                    break;
                            }
                        }

                        //show offline mode indicator
                        if (PreferenceUtils.isCacheMode(getBaseContext()))
                            getSFActivity().getSupportActionBar()
                                    .setSubtitle(R.string.action_cache_mode);

                        mQueryCallback.onQuerySuccess(mHelper, gameList);
                    } else {
                        mQueryCallback.onQueryError();
                    }

                    //internalLoading(false);//[87]dolphin++ but no need
                }
            });
        else
            Log.e(TAG, "what happened?");
    }

    /**
     * generate a debug list for offline test
     *
     * @param year
     * @param month
     * @return
     */
    private ArrayList<Game> get_debug_list(int year, int month) {
        //Log.d(TAG, "get_debug_list");
        ArrayList<Game> gameList = new ArrayList<Game>();
        for (int i = 0; i < 30; i++) {
            Game game = new Game();
            game.IsFinal = ((i % 3) != 0);
            game.Id = month + i + (year % 100);
            game.HomeTeam = new Team(this, Team.ID_EDA_RHINOS);
            game.AwayTeam = new Team(this, Team.ID_LAMIGO_MONKEYS);
            game.Field = "@somewhere";
            game.StartTime = CpblCalendarHelper.getNowTime();
            game.StartTime.set(Calendar.HOUR_OF_DAY, 18);
            game.StartTime.add(Calendar.DAY_OF_YEAR, i - 3);
            game.AwayScore = (game.StartTime.get(Calendar.MILLISECOND) % 20);
            game.HomeScore = (game.StartTime.get(Calendar.SECOND) % 20);
            game.DelayMessage = ((i % 4) == 1) ? "wtf rainy day" : "";
            gameList.add(game);
        }
        return gameList;
    }

    /**
     * check if the tutorial is done
     *
     * @return
     */
    public boolean isTutorialDone() {
        if (CpblCalendarHelper.getNowTime().get(Calendar.YEAR) < 2014) {
            if (PreferenceUtils.isEngineerMode(getBaseContext()))
                return false;//[39]dolphin++ move to global method
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
            AlertDialog dialog = new AlertDialog.Builder(this).create();
            //dialog.setTitle(mSpinnerKind.getItemAtPosition(1).toString());
            dialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(android.R.string.ok),
                    new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface arg0, int arg1) {
                            // do nothing, just dismiss
                        }
                    }
            );
            // How to display the Html formatted text in the PopUp box
            // using alert dialog builder?
            // http://stackoverflow.com/a/8641399
            //dialog.setMessage(Html.fromHtml(mHelper.getScoreBoardHtml()));

            //change the style like the entire theme
            LayoutInflater inflater = LayoutInflater.from(this);
            View view = inflater.inflate(R.layout.leader_board, null);
            TextView textView = (TextView) view.findViewById(android.R.id.title);
            if (textView != null)
                textView.setText(mSpinnerKind.getItemAtPosition(1).toString());

            // android from html cannot recognize all HTML tag
            // http://stackoverflow.com/a/8632338
            WebView webView = (WebView) view.findViewById(R.id.webView);//new WebView(this);
            // http://pop1030123.iteye.com/blog/1399305
            //webView.getSettings().setDefaultTextEncodingName(CpblCalendarHelper.ENCODE_UTF8);

            //Log.d(TAG, html);
            // Encoding issue with WebView's loadData
            // http://stackoverflow.com/a/9402988
            if (webView != null)
                webView.loadData(html, "text/html; charset=" +
                        CpblCalendarHelper.ENCODE_UTF8, null);

            dialog.setView(view);//webView
            dialog.show();

            //set button style as holo green light
            View btOk = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
            if (btOk != null)
                btOk.setBackgroundResource(R.drawable.item_background_holo_light);

        } catch (Exception e) {
            Log.e(TAG, "showLeaderBoard: " + e.getMessage());
        }

        EasyTracker easyTracker = EasyTracker.getInstance(CalendarActivity.this);
        if (easyTracker != null)
            easyTracker.send(MapBuilder.createEvent("UI", "showLeaderBoard",
                    null, null).build());
    }

    public void showLeaderBoard2014() {
        internalLoading(true);
        new Thread(new Runnable() {
            @Override
            public void run() {
                final ArrayList<Stand> standing = mHelper.query2014LeaderBoard();
                CalendarActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        doShowLeaderBoard2014(standing);
                        internalLoading(false);
                        invalidateOptionsMenu();
                    }
                });
            }
        }).start();
    }

    private final static String TABLE_ROW_TEMPLATE = "<tr style='%style'>" +
            "<td style='width:40%;'>@team</td>" +
            "<td style='width:15%;text-align:center'>@win</td>" +
            "<td style='width:15%;text-align:center'>@lose</td>" +
            "<td style='width:15%;text-align:center'>@tie</td>" +
            "<td style='width:15%;text-align:center'>@behind</td></tr>";

    public void doShowLeaderBoard2014(ArrayList<Stand> standing) {
        String standingHtml = "<table style='width:100%;'>";
        standingHtml += TABLE_ROW_TEMPLATE
                .replace("@style", "background-color:#ccf;font-weight:bold;")
                .replace("@team", getString(R.string.title_team))
                .replace("@win", getString(R.string.title_win))
                .replace("@lose", getString(R.string.title_lose))
                .replace("@tie", getString(R.string.title_tie))
                .replace("@behind", getString(R.string.title_game_behind));
        //standingHtml += "<tr><td colspan='5'><hr /></td></tr>";
        for (Stand stand : standing)
            standingHtml += TABLE_ROW_TEMPLATE
                    .replace("@team", stand.getTeam().getName())
                    .replace("@win", String.valueOf(stand.getGamesWon()))
                    .replace("@lose", String.valueOf(stand.getGamesLost()))
                    .replace("@tie", String.valueOf(stand.getGamesTied()))
                    .replace("@behind", String.valueOf(stand.getGamesBehind()));
        standingHtml += "</table>";
        showLeaderBoard(standingHtml);
    }

    @Override
    public void onStart() {
        super.onStart();
        //... // The rest of your onStart() code.
        EasyTracker.getInstance(this).activityStart(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        //... // The rest of your onStop() code.
        EasyTracker.getInstance(this).activityStop(this);
    }

    private void showCacheModeEnableDialog(final MenuItem item) {
        new AlertDialog.Builder(CalendarActivity.this).setCancelable(true)
                .setMessage(R.string.title_cache_mode_enable_message)
                .setTitle(R.string.action_cache_mode)
                .setPositiveButton(R.string.title_cache_mode_start,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                mCacheMode = true;
                                PreferenceUtils.setCacheMode(getBaseContext(), mCacheMode);
                                item.setIcon(R.drawable.holo_green_btn_check_on_holo_dark);
                                //item.setCheckable(mCacheMode);
                                //mButtonQuery.performClick();
                                mSpinnerYear.setSelection(0);//[87]dolphin++
                                mSpinnerField.setSelection(0);//[87]dolphin++
                                runDownloadCache();
                            }
                        }
                )
                .setNegativeButton(android.R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //do nothing
                            }
                        }
                ).show();
//        Log.d(TAG, String.format("onOptionsItemSelected mCacheMode=%s", mCacheMode));
//        //item.setCheckable(mCacheMode);
//        item.setIcon(mCacheMode ? R.drawable.holo_green_btn_check_on_holo_dark
//                : R.drawable.holo_green_btn_check_off_holo_dark);
    }

    private void runDownloadCache() {
        onLoading(true);

        mHelper = new CpblCalendarHelper(mActivity);
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int m = 3; m <= 10; m++) {
                    doQueryStateUpdateCallback(getString(R.string.title_download_from_cpbl,
                            mYear, m));
                    ArrayList<Game> list = mHelper.query("01", mYear, m, "F00");
                    if (list == null) {
                        doQueryStateUpdateCallback(getString(R.string.title_download_from_zxc22,
                                mYear, m));
                        list = mHelper.query2014zxc(m);
                    }
                    boolean r = mHelper.putCache(mYear, m, list);
                    Log.v(TAG, String.format("%04d/%02d result: %s", mYear, m,
                            (r ? "success" : "failed")));
                }
                doQueryStateUpdateCallback(R.string.title_download_complete);

                CalendarActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mButtonQuery.performClick();
                    }
                });
            }
        }).start();
    }

    private void doCacheModeQuery(SherlockFragmentActivity activity,
                                  int year, int month, OnQueryCallback callback) {
        mActivity = activity;
        mQueryCallback = callback;
        if (mQueryCallback != null)//call before start
            mQueryCallback.onQueryStart();
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
                            if (g.StartTime.get(Calendar.DAY_OF_MONTH) > 15)
                                break;
                            gameList.add(g);
                        }

                        doQueryStateUpdateCallback(getString(R.string.title_download_complete));
                    }
                }
                doQueryCallback(gameList);
            }
        }).start();
    }
}
