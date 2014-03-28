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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
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
                mButtonQuery.performClick();
                return true;//break;
            case R.id.action_leader_board://[26]dolphin++
                showLeaderBoard();
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
        }
        return super.onOptionsItemSelected(item);
    }

    private Button.OnClickListener onQueryClick = new Button.OnClickListener() {
        @Override
        public void onClick(View view) {
            mButtonQuery.setEnabled(false);//disable the button
            mSpinnerField.setEnabled(false);
            mSpinnerKind.setEnabled(false);
            mSpinnerYear.setEnabled(false);
            mSpinnerMonth.setEnabled(false);
            mIsQuery = true;//Log.d(TAG, "onQueryClick");
            setSupportProgressBarIndeterminateVisibility(mIsQuery);

            getSActionBar().setTitle(mSpinnerKind.getSelectedItem().toString());

            String gameYear = mSpinnerYear.getSelectedItem().toString();
            int year = Integer.parseInt(gameYear.split(" ")[0]);
            //Log.d(TAG, String.format("  mSpinnerYear: %d", year));
            int month = mSpinnerMonth.getSelectedItemPosition() + 1;
            //Log.d(TAG, String.format(" mSpinnerMonth: %d", month));
            String field = mGameField[mSpinnerField.getSelectedItemPosition()];

            getSActionBar().setSubtitle(String.format("%s %s",
                    gameYear, mSpinnerMonth.getSelectedItem().toString()));

            final boolean bDemoCache = getResources().getBoolean(R.bool.demo_cache);
            final SherlockFragmentActivity activity = getSFActivity();
            if (HttpHelper.checkNetworkAvailable(activity) && !bDemoCache)
                doWebQuery(activity, mSpinnerKind.getSelectedItemPosition(),
                        year, month, field, getOnQueryCallback());
            else {//[35]dolphin++ check network
                if (!bDemoCache)
                    Toast.makeText(activity, R.string.no_available_network,
                            Toast.LENGTH_LONG).show();//[47] change SHORT to LONG
                //[79]dolphin-- doQueryCallback(null);//[47]++ do callback to return
                mActivity = activity;
                mQueryCallback = getOnQueryCallback();
                doQueryCallback(CpblCalendarHelper.getCache(activity, mYear, mMonth));//[79]dolphin++
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
    private String mField = "00";
    private CpblCalendarHelper mHelper = null;

    public boolean IsQuery() {
        return mIsQuery;
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
                    } else {//TODO try local cache
                        //query from Internet
                        if (now.get(Calendar.YEAR) < 2014)
                            gameList = mHelper.query(gameKind, mYear, mMonth, mField);
                        else if (resources.getBoolean(R.bool.demo_zxc22)) {
                            gameList = mHelper.query2014zxc(mMonth);
                        } else {//do real job
                            gameList = mHelper.query2014();
                            if (gameList == null || gameList.size() <= 0)//backup plan
                                gameList = mHelper.query2014zxc(mMonth);
                        }
                    }

                    if (resources.getBoolean(R.bool.feature_auto_load_next)) {
                        //auto load next month games
                        if (mMonth >= 3 && mMonth < 10 && now.get(Calendar.DAY_OF_MONTH) > 15) {
                            ArrayList<Game> list = mHelper.query2014zxc(mMonth + 1);
                            for (Game g : list) {
                                if (g.StartTime.get(Calendar.DAY_OF_MONTH) > 15)
                                    break;
                                gameList.add(g);
                            }
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

                    if (mHelper.canUseCache())
                        gameList = mHelper.getCache(mYear, mMonth);//try to read from cache
                    else
                        gameList = mHelper.query2014zxc(mMonth);
                    doQueryCallback(gameList);
                }
            }
        }).start();
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
            int type = (gameList != null && gameList.size() > 0) ? gameList.get(0).Source : -1;
            easyTracker.send(MapBuilder.createEvent("UI",//Category
                            "doQueryCallback",//Action (required)
                            String.format("%04d/%02d:%d", mYear, mMonth, type),//label
                            null)//Event value (long)
                            .build()
            );
        }

        if (gameList != null && mHelper != null && mHelper.canUseCache()) {
            Log.d(TAG, String.format("write to cache %04d-%02d.json", mYear, mMonth));
            boolean r = mHelper.putCache(mYear, mMonth, gameList);
            Log.v(TAG, String.format(" result: %s", (r ? "success" : "failed")));
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
                        mQueryCallback.onQuerySuccess(mHelper, gameList);
                    } else {
                        mQueryCallback.onQueryError();
                    }

                    mSpinnerField.setEnabled(true);
                    mSpinnerKind.setEnabled(true);
                    mSpinnerYear.setEnabled(true);
                    mSpinnerMonth.setEnabled(true);

                    mButtonQuery.setEnabled(true);//enable the button
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
        Log.d(TAG, "get_debug_list");
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
    public void showLeaderBoard() {
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
                webView.loadData(mHelper.getScoreBoardHtml(),
                        "text/html; charset=" + CpblCalendarHelper.ENCODE_UTF8, null);

            dialog.setView(view);//webView
            dialog.show();

            //set button style as holo green light
            View btOk = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
            if (btOk != null)
                btOk.setBackgroundResource(R.drawable.item_background_holo_light);

        } catch (Exception e) {
            Log.e(TAG, "showLeaderBoard: " + e.getMessage());
        }
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
}
