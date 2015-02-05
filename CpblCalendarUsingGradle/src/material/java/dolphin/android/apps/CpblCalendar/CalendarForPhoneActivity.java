package dolphin.android.apps.CpblCalendar;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;

import dolphin.android.apps.CpblCalendar.provider.CpblCalendarHelper;
import dolphin.android.apps.CpblCalendar.provider.Game;

/**
 * Created by dolphin on 2013/6/3.
 * <p/>
 * CalendarActivity for phone version, with a ActionBarDrawer pane.
 */
public class CalendarForPhoneActivity extends CalendarActivity
        implements CalendarActivity.OnQueryCallback {

    private DrawerLayout mDrawerLayout;

    private View mDrawerList;

    private ArrayList<Game> mGameList = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar_phone);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = findViewById(R.id.left_drawer);
        mDrawerList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //used to avoid clicking objects behind drawer
            }
        });

        // set a custom shadow that overlays the main content when the drawer opens
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        initQueryPane();

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
                mSpinnerField.setSelection(field);
                mSpinnerKind.setSelection(kind);
                mSpinnerYear.setSelection(debugMode ? 1 : year);
                mSpinnerMonth.setSelection(debugMode ? 5 : month);
                mButtonQuery.performClick();//load at beginning
            }
        });


        findViewById(R.id.button_floating_action).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mDrawerLayout.openDrawer(mDrawerList);
                    }
                }
        );
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.splash, menu);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {//[29]dolphin++
            if (menu.findItem(R.id.action_leader_board) != null) {
                menu.removeItem(R.id.action_leader_board);
            }
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // If the nav drawer is open, hide action items related to the content view
        boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerList);
        boolean visible = !drawerOpen & !IsQuery();
        menu.findItem(R.id.action_settings).setVisible(visible);
        menu.findItem(R.id.action_refresh).setVisible(visible);
        MenuItem item = menu.findItem(R.id.action_leader_board);//[26]dolphin++
        if (item != null) {
            item.setVisible(visible);//[87]dolphin++
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    protected ActionBarActivity getActivity() {
        return this;
    }

    @Override
    public OnQueryCallback getOnQueryCallback() {
        return this;
    }

    @Override
    public void onQueryStart() {
        mDrawerLayout.closeDrawer(mDrawerList);
        invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
        onLoading(true);//[30]dolphin++
    }

    @Override
    public void onQueryStateChange(String msg) {
        //Log.d(TAG, "onQueryUpdate: " + msg);
        if (getProgressText() != null) {
            getProgressText().setText(msg);
        }
    }

    @Override
    public void onQuerySuccess(CpblCalendarHelper helper, ArrayList<Game> gameArrayList) {
        //mDrawerLayout.closeDrawer(mDrawerList);//[87]dolphin++ put to success?
        //Log.d(TAG, "onSuccess");
        updateGameListFragment(gameArrayList);
        invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
    }

    private void updateGameListFragment(ArrayList<Game> gameArrayList) {
        mGameList = gameArrayList;//as a temp storage

        FragmentManager fmgr = getFragmentManager();
        //if (fmgr != null) {//update the fragment
        FragmentTransaction trans = fmgr.beginTransaction();
        GameListFragment frag1 =
                (GameListFragment) fmgr.findFragmentById(R.id.content_frame);
        if (frag1 != null) {
            frag1.updateAdapter(gameArrayList);
            try {
                trans.commitAllowingStateLoss();//[30]dolphin++
            } catch (IllegalStateException e) {
                Log.e(TAG, "updateGameListFragment: " + e.getMessage());
                sendTrackerException();
            }
        }
        //}

        super.onLoading(false);
    }

    @Override
    public void onQueryError() {
        //Log.e(TAG, "onError");
        onLoading(false);//[30]dolphin++
        updateGameListFragment(new ArrayList<Game>());//[59]++ no data
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
                (GameListFragment) fmgr.findFragmentById(R.id.content_frame);
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

        super.onLoading(is_load);
    }

    private void sendTrackerException() {
        sendTrackerException("commitAllowingStateLoss", "phone", 0);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //[22]dolphin++ need get the data from server again!
        // because the favorite team preference may remove some teams
        //onQuerySuccess(mGameList);//reload the fragment when possible preferences change
        mButtonQuery.performClick();//[22]dolphin++
    }

    @Override
    public void OnFragmentAttached() {
        //[33]dolphin++
    }
}
