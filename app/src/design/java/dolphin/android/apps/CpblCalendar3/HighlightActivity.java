package dolphin.android.apps.CpblCalendar3;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.customtabs.CustomTabsIntent;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;

import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

import java.util.ArrayList;
import java.util.Calendar;

import dolphin.android.apps.CpblCalendar.CalendarForPhoneActivity;
import dolphin.android.apps.CpblCalendar.Utils;
import dolphin.android.apps.CpblCalendar.provider.CpblCalendarHelper;
import dolphin.android.apps.CpblCalendar.provider.Game;
import dolphin.android.apps.CpblCalendar.provider.Team;
import dolphin.android.apps.CpblCalendar.provider.TeamHelper;
import dolphin.android.util.DateUtils;
import dolphin.android.util.PackageUtils;

/**
 * Created by jimmyhu on 2017/5/16.
 * <p>
 * New Activity to show highlights like Google-now style.
 */

public class HighlightActivity extends AppCompatActivity
        implements ActivityCompat.OnRequestPermissionsResultCallback {
    private final static String TAG = "HighlightActivity";
    private final static boolean DEBUG_LAYOUT = false;
    private final static boolean DEBUG_LOG = false;

    public final static String KEY_CACHE = "cache";

    private CpblCalendarHelper mHelper = null;
    private FirebaseRemoteConfig mRemoteConfig;
    private ArrayList<Game> mCacheGames;

    private SwipeRefreshLayout mSwipeRefreshLayout;
    private Snackbar mSnackbar;
    private RecyclerView mList;
    private View mProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_highlight);

        mRemoteConfig = FirebaseRemoteConfig.getInstance();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayUseLogoEnabled(true);
            actionBar.setLogo(R.mipmap.ic_launcher);
            //actionBar.setDisplayHomeAsUpEnabled(false);
        }

        mList = (RecyclerView) findViewById(android.R.id.list);
        if (mList != null) {
            if (DEBUG_LAYOUT) {
                createDebugData();
            } else {
                if (ContextCompat.checkSelfPermission(this,
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED) {
                    downloadCalendar();
                } else {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                            android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                        showRequestStorageRationale();
                    } else {
                        requestStoragePermission();
                    }
                }
            }
        }

        mProgress = findViewById(android.R.id.progress);
        if (mProgress != null) {//add dummy listener
            mProgress.setOnTouchListener(new View.OnTouchListener() {
                @SuppressLint("ClickableViewAccessibility")
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    return true;
                }
            });
            mProgress.setVisibility(View.GONE);
        }

        //http://sapandiwakar.in/pull-to-refresh-for-android-recyclerview-or-any-other-vertically-scrolling-view/
        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipeRefreshLayout);
        if (mSwipeRefreshLayout != null) {
            mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    //mSwipeRefreshLayout.setRefreshing(false);//we have our refreshing animation
                    downloadCalendar();
                }
            });
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings: {
                startActivityForResult(new Intent(HighlightActivity.this, SettingsActivity.class), 0);
                return true;
            }
            case R.id.action_go_to_cpbl: {
                Calendar now = CpblCalendarHelper.getNowTime();
                int year = now.get(Calendar.YEAR);
                int month = now.get(Calendar.MONTH) + 1;
                CpblCalendarHelper.startActivityToCpblSchedule(this, year, month, "01", "F00");
                return true;
            }
            case R.id.action_leader_board: {
//                if (Utils.isGoogleChromeInstalled(this)) {//[190]++ use Chrome Custom Tabs
//                    //http://stackoverflow.com/a/15629199/2673859
//                    Utils.startBrowserActivity(this, Utils.LEADER_BOARD_URL);
//                } else {
//                    try {
//                        Utils.buildLeaderBoardZxc22(this);
//                    } catch (Exception e) {
//                        Log.e(TAG, "showLeaderBoard: " + e.getMessage());
//                    }
//                }

                //https://goo.gl/GtBKgp
                CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder()
                        .setToolbarColor(ContextCompat.getColor(this, R.color.holo_green_dark));
                CustomTabsIntent customTabsIntent = builder.build();
                customTabsIntent.launchUrl(this, Utils.LEADER_BOARD_URI);
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_highlight, menu);
        return super.onCreateOptionsMenu(menu);
    }

    private void createDebugData() {
        showSnackbar("create debug data");

        ArrayList<Game> list = new ArrayList<>();

        //1 final game
        Game game1 = new Game();
        game1.IsFinal = true;
        game1.StartTime = CpblCalendarHelper.getNowTime();
        game1.StartTime.add(Calendar.DAY_OF_YEAR, -1);
        game1.AwayTeam = new Team(this, Team.ID_LAMIGO_MONKEYS);
        game1.HomeTeam = new Team(this, Team.ID_FUBON_GUARDIANS);
        list.add(game1);
        //1 live game
        Game game2 = new Game();
        game2.IsLive = true;
        game2.StartTime = CpblCalendarHelper.getNowTime();
        game2.AwayTeam = new Team(this, Team.ID_UNI_711_LIONS);
        game2.HomeTeam = new Team(this, Team.ID_CT_ELEPHANTS);
        game2.LiveMessage = "LIVE&nbsp;&nbsp;2 Top";
        list.add(game2);
        //2 upcoming games
        Game game3 = new Game();
        game3.StartTime = CpblCalendarHelper.getNowTime();
        game3.StartTime.add(Calendar.HOUR_OF_DAY, +1);
        game3.AwayTeam = new Team(this, Team.ID_FUBON_GUARDIANS);
        game3.HomeTeam = new Team(this, Team.ID_UNI_711_LIONS);
        game3.Channel = "CPBL.tv, FOX sports";
        list.add(game3);
        Game game4 = new Game();
        game4.StartTime = CpblCalendarHelper.getNowTime();
        game4.StartTime.add(Calendar.DAY_OF_YEAR, +1);
        game4.AwayTeam = new Team(this, Team.ID_LAMIGO_MONKEYS);
        game4.HomeTeam = new Team(this, Team.ID_CT_ELEPHANTS);
        game4.Channel = "CPBL.tv, Eleven Sports, VL Sports";
        list.add(game4);

        updateViews(list);
    }

    private void downloadCalendar() {
        //final Activity activity = this;
        final boolean allowCache = mRemoteConfig.getBoolean("enable_delay_games_from_cache");
        final boolean allowDrive = mRemoteConfig.getBoolean("enable_delay_games_from_drive");

        //showSnackbar("downloading");
        new Thread(new Runnable() {
            @Override
            public void run() {
                doDownloadCalendar(allowCache, allowDrive);
            }
        }, "CpblCalendar").start();
    }

    private void doDownloadCalendar(boolean allowCache, boolean allowDrive) {
        if (mHelper == null) {//new object to start get data
            //we now query for ASP.NET, so reuse the same object
            mHelper = new CpblCalendarHelper(this);
        }

        Calendar now = CpblCalendarHelper.getNowTime();
        int year = now.get(Calendar.YEAR);
        int month = now.get(Calendar.MONTH) + 1;
        //int day = now.get(Calendar.DAY_OF_MONTH);
        String kind = "01";
        String field = "F00";
        ArrayList<Game> gameList = new ArrayList<>();
        tryShowSnackbar(getString(R.string.title_download_from_cpbl, year, month));
        mCacheGames = mHelper.query2016(year, month, kind, field, allowCache, allowDrive);
        //we cache it for CalendarActivity quick load, no more download again
        if (mCacheGames != null && mCacheGames.size() > 0) {
            //check today and if we have games before and after
            Game g1 = mCacheGames.get(0);
            if (g1.StartTime.after(now)) {//upcoming, so we have to get previous month data
                if (month > Calendar.JANUARY) {//get last month
                    tryShowSnackbar(getString(R.string.title_download_from_cpbl, year, month - 1));
                    ArrayList<Game> list1 = mHelper.query2016(year, month - 1, kind, field,
                            allowCache, allowDrive);
                    if (list1 != null && list1.size() > 0) {
                        gameList.addAll(list1);
                    }
                }
            }
            gameList.addAll(mCacheGames);
            Game g2 = mCacheGames.get(mCacheGames.size() - 1);
            if (g2.StartTime.before(now)) {//check if it is final
                boolean needMore = g2.IsFinal;
                if (mCacheGames.size() > 1) {//check if we have more games today
                    Game g3 = mCacheGames.get(mCacheGames.size() - 2);//check another game
                    if (g3.StartTime.equals(g2.StartTime)) {//same start time
                        needMore |= g3.IsFinal;//we should
                    }
                }
                if (needMore && month < Calendar.DECEMBER) {//get next month
                    tryShowSnackbar(getString(R.string.title_download_from_cpbl, year, month + 1));
                    ArrayList<Game> list3 = mHelper.query2016(year, month + 1, kind, field,
                            allowCache, allowDrive);
                    if (list3 != null && list3.size() > 0) {
                        gameList.addAll(list3);
                    }
                }
            }
        }

        tryShowSnackbar(getString(R.string.title_download_complete));
        final ArrayList<Game> list = cleanUpGameList(gameList);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateViews(list);
            }
        });
    }

    //private final static long MAX_TIME_DIFF = (long) (1000 * 60 * 60 * 24 * 1.5);

    private ArrayList<Game> cleanUpGameList(ArrayList<Game> list) {
        ArrayList<Game> gameList = new ArrayList<>();
        //see if we have new games and upcoming games
        Calendar now = CpblCalendarHelper.getNowTime();
        long beforeDiff = Long.MIN_VALUE, afterDiff = Long.MAX_VALUE;
        int beforeIndex = -1, afterIndex = -1;
        int i;
        for (i = 0; i < list.size(); i++) {
            Game game = list.get(i);
            long diff = game.StartTime.getTimeInMillis() - now.getTimeInMillis();
            if (game.StartTime.before(now)) {//final or live
                if (game.isToday() && beforeDiff != 0) {//show all games today, final or live
                    beforeIndex = i;
                    beforeDiff = 0;//get all today games in the list
                } else if (diff > beforeDiff) {//no games today, try to get the closest games
                    beforeDiff = diff;
                    beforeIndex = i;
                } else if (diff == beforeDiff) {//add to list later
                    if (DEBUG_LOG) {
                        Log.d(TAG, String.format("before. same day game %d, %d", beforeIndex, i));
                    }
                }//don't care those
            } else if (game.StartTime.after(now)) {//upcoming games
                //Log.d(TAG, String.format("after: %d, %s", game.Id, game.getDisplayDate()));
                if (game.isToday()) {//show all games today
                    afterIndex = i;
                    afterDiff = diff;
                } else if (afterIndex != -1 && DateUtils.sameDay(list.get(afterIndex).StartTime, game.StartTime)) {
                    afterDiff = diff;
                    //Log.d(TAG, String.format("one more game %d", game.Id));
                } else if (diff < afterDiff) {//no games today, try to find the closest games
                    afterDiff = diff;
                    afterIndex = i;
                    if (DEBUG_LOG) {
                        Log.d(TAG, String.format("afterIndex=%d, afterDiff=%d", afterIndex, afterDiff));
                    }
                } else if (diff == afterDiff) {//same start time
                    if (DEBUG_LOG) {
                        Log.d(TAG, String.format("after. same day game %d, %d", afterIndex, i));
                    }
                } else {//stop here, don't have to check again
                    //Log.d(TAG, "stop here, don't have to check again");
                    break;
                }
            }
        }
        if (DEBUG_LOG) {
            Log.d(TAG, String.format("before=%d, after=%d", beforeIndex, afterIndex));
        }

        boolean lived = false;
        for (i = beforeIndex; i < list.size(); i++) {
            Game game = list.get(i);
            if (game.StartTime.before(now)) {//final or live
                if (DEBUG_LOG) {
                    Log.d(TAG, String.format("%d: %s, %s", game.Id, game.getDisplayDate(), game.IsLive));
                }
                gameList.add(game);//add all of them
                lived |= game.IsLive;//only no live games that we will show upcoming games
            } else if (!lived) {//don't show upcoming when games are live
                long diff = game.StartTime.getTimeInMillis() - now.getTimeInMillis();
                //Log.d(TAG, String.format("after: diff=%d", diff));
                if (diff > afterDiff) {//ignore all except the closest upcoming games
                    //Log.d(TAG, "ignore all except the closest upcoming games");
                    break;
                }
                gameList.add(game);
                if (DEBUG_LOG) {
                    Log.d(TAG, String.format("%d: %s", game.Id, game.getDisplayDate()));
                }
            }
        }

        return gameList;
    }

    private void updateViews(ArrayList<Game> list) {
        Game more = new Game();
        more.Id = -1;
        list.add(more);//add a more button to last row
        //final ArrayList<Game> gameList = list;
        TeamHelper helper = new TeamHelper((CpblApplication) getApplication());
        final GameCardAdapter adapter = new GameCardAdapter(this, list, helper);
        final Activity activity = this;
        adapter.setOnClickListener(new GameCardAdapter.OnClickListener() {
            @Override
            public void onClick(View view, Game game) {
                if (game.Id == -1) {
                    Intent intent = new Intent(activity, CalendarForPhoneActivity.class);
                    intent.putParcelableArrayListExtra(KEY_CACHE, mCacheGames);
                    //Log.d(TAG, String.format("list %d", mCacheGames.size()));
                    startActivity(intent);
//                } else if (view.getId() == R.id.card_option1) {
//                    game.getFieldId(getBaseContext());//update game field id
//                    String url = CpblCalendarHelper.URL_FIELD_2017.replace("@field", game.FieldId);
//                    //Log.d(TAG, "url = " + url);
//                    Utils.startBrowserActivity(activity, url);
                } else if (view.getId() == R.id.card_option2) {
                    Intent calIntent = Utils.createAddToCalendarIntent(activity, game);
                    if (PackageUtils.isCallable(activity, calIntent)) {
                        startActivity(calIntent);
                    }
                } else {
                    Utils.startGameActivity(activity, game);
                }
            }
        });

        final int columnPerRow = getResources().getBoolean(R.bool.config_tablet) ? 2 : 1;
        final GridLayoutManager layoutManager = new GridLayoutManager(this, columnPerRow);
        //layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                switch (adapter.getItemViewType(position)) {
                    case GameCardAdapter.TYPE_LIVE:
                    case GameCardAdapter.TYPE_MORE:
                        return columnPerRow;
                }
                return 1;
            }
        });
        mList.setLayoutManager(layoutManager);
        mList.setAdapter(adapter);
        hideSnackbar();
    }

    private void tryShowSnackbar(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                showSnackbar(message);
            }
        });
    }

    private void showSnackbar(String message) {
        if (mSwipeRefreshLayout != null) {
            mSwipeRefreshLayout.setRefreshing(true);
        }
        if (mProgress != null) {
            mProgress.setVisibility(View.VISIBLE);
        }
        if (mSnackbar != null && mSnackbar.isShown()) {
            mSnackbar.setText(message);
        } else {
            mSnackbar = Snackbar.make(findViewById(R.id.main_content_frame), message,
                    Snackbar.LENGTH_INDEFINITE);
            mSnackbar.show();
        }
    }

    private void hideSnackbar() {
        if (mSnackbar != null) {
            mSnackbar.dismiss();
            mSnackbar = null;
        }
        if (mProgress != null) {
            mProgress.setVisibility(View.GONE);
        }
        if (mSwipeRefreshLayout != null) {
            mSwipeRefreshLayout.setRefreshing(false);//we have our refreshing animation
        }
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
                        downloadCalendar();
                    }
                });
        builder.show();
    }

    private void requestStoragePermission() {
        ActivityCompat.requestPermissions(this, new String[]{
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
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
        //if not granted, just store in inner memory
        downloadCalendar();//no matter the result is, we can continue
    }
}
