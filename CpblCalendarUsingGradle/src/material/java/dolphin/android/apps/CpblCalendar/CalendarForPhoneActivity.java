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
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import java.util.ArrayList;
import java.util.Calendar;

import dolphin.android.apps.CpblCalendar.provider.CpblCalendarHelper;
import dolphin.android.apps.CpblCalendar.provider.Game;

/**
 * Created by dolphin on 2013/6/3.
 * <p/>
 * CalendarActivity for phone version, with a ActionBarDrawer pane.
 */
public class CalendarForPhoneActivity extends CalendarActivity implements OnQueryCallback,
        ActivityCompat.OnRequestPermissionsResultCallback {

    private DrawerLayout mDrawerLayout;

    private View mDrawerList;

    //private ArrayList<Game> mGameList = null;

    private AdView mAdView;

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
//        if (getResources().getBoolean(R.bool.config_tablet)) {
//            mToolbar.setLogo(R.drawable.ic_launcher);
//        }

        initQueryPane();

        mAdView = (AdView) findViewById(R.id.adView);

        findViewById(R.id.button_floating_action).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mDrawerLayout.openDrawer(mDrawerList);
                    }
                }
        );

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
                mSpinnerField.setSelection(field);
                mSpinnerKind.setSelection(kind);
                mSpinnerYear.setSelection(debugMode ? 1 : year);
                mSpinnerMonth.setSelection(debugMode ? 5 : month);
                mButtonQuery.performClick();//load at beginning
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
        boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerList);
        boolean visible = !drawerOpen & !IsQuery();
        menu.findItem(R.id.action_settings).setVisible(visible);
        menu.findItem(R.id.action_refresh).setVisible(visible);
        menu.findItem(R.id.action_leader_board).setVisible(true);
        menu.findItem(R.id.action_leader_board).setEnabled(visible);//keep the menu order
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_search://[133]dolphin++
                mDrawerLayout.openDrawer(mDrawerList);
                return true;
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
    public void onQuerySuccess(CpblCalendarHelper helper, ArrayList<Game> gameArrayList, int year, int month) {
        mDrawerLayout.closeDrawer(mDrawerList);//[138]++ close it//[87]dolphin++ put to success?
        //Log.d(TAG, "onSuccess");
        updateGameListFragment(gameArrayList, year, month);
        invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
    }

    private void updateGameListFragment(ArrayList<Game> gameArrayList, int year, int month) {
        //mGameList = gameArrayList;//as a temp storage

        FragmentManager fmgr = getFragmentManager();
        //if (fmgr != null) {//update the fragment
        FragmentTransaction trans = fmgr.beginTransaction();
        GameListFragment frag1 = (GameListFragment) fmgr.findFragmentById(R.id.content_frame);
        if (frag1 != null) {
            frag1.updateAdapter(gameArrayList, mSpinnerYear.getSelectedItem().toString(),
                    mSpinnerMonth.getSelectedItem().toString());
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
//        View fab = findViewById(R.id.button_floating_action);
//        if (fab != null) {
//            fab.setEnabled(!is_load);
//        }
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
        //query_to_update(true);//[126]dolphin++ quick refresh
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
        if (mDrawerLayout.isDrawerOpen(mDrawerList)) {
            mDrawerLayout.closeDrawer(mDrawerList);
            return;//[146]++
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
}
