package dolphin.android.apps.CpblCalendar;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v7.graphics.Palette;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

import java.lang.ref.WeakReference;
import java.util.Calendar;

import dolphin.android.apps.CpblCalendar.provider.CpblCalendarHelper;
import dolphin.android.apps.CpblCalendar.provider.Team;
import dolphin.android.util.PackageUtils;

public class SplashActivity extends Activity {
    private final static String TAG = "SplashActivity";
    private FirebaseRemoteConfig mRemoteConfig;
    private MyHandler myHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        myHandler = new MyHandler(this);
//        //http://aleung.github.io/blog/2012/10/06/change-locale-in-android-application/
//        Locale.setDefault(Locale.TAIWAN);
//        Configuration config = getBaseContext().getResources().getConfiguration();
//        config.locale = Locale.TAIWAN;
//        getBaseContext().getResources().updateConfiguration(config,
//                getBaseContext().getResources().getDisplayMetrics());

        FirebaseAnalytics.getInstance(this);//initialize this

        setContentView(R.layout.activity_splash);
        //http://stackoverflow.com/a/31016761/2673859
        //check google play service and authentication
        GoogleApiAvailability googleAPI = GoogleApiAvailability.getInstance();
        int result = googleAPI.isGooglePlayServicesAvailable(this);
        if (result != ConnectionResult.SUCCESS) {
            Log.e(TAG, googleAPI.getErrorString(result));
            TextView textView = (TextView) findViewById(android.R.id.message);
            if (textView != null) {
                textView.setText(googleAPI.getErrorString(result));
            }
            return;//don't show progress bar
        }

        myHandler.sendEmptyMessageDelayed(0, 8000);//set a backup startActivity
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                showLoadingInProgress();
            }
        }, 500);
        new Thread(new Runnable() {
            @Override
            public void run() {
                preparePalette();
                prepareRemoteConfig();
                //myHandler.sendEmptyMessage(0);
            }
        }).start();
    }

    private void showLoadingInProgress() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                View progress = findViewById(android.R.id.progress);
                if (progress != null) {
                    progress.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void prepareRemoteConfig() {
        mRemoteConfig = FirebaseRemoteConfig.getInstance();
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setDeveloperModeEnabled(getResources().getBoolean(R.bool.developer_mode))
                .build();
        mRemoteConfig.setConfigSettings(configSettings);
        mRemoteConfig.setDefaults(R.xml.remote_config_defaults);
        fetchRemoteConfig();
    }

    /**
     * Fetch RemoteConfig from server.
     */
    private void fetchRemoteConfig() {
        long cacheExpiration = 43200; // 12 hours in seconds.
        // If in developer mode cacheExpiration is set to 0 so each fetch will retrieve values from
        // the server.
        if (mRemoteConfig.getInfo().getConfigSettings().isDeveloperModeEnabled()) {
            cacheExpiration = 60;
        }

        // [START fetch_config_with_callback]
        final long start = System.currentTimeMillis();
        // cacheExpirationSeconds is set to cacheExpiration here, indicating that any previously
        // fetched and cached config would be considered expired because it would have been fetched
        // more than cacheExpiration seconds ago. Thus the next fetch would go to the server unless
        // throttling is in progress. The default expiration duration is 43200 (12 hours).
        mRemoteConfig.fetch(cacheExpiration)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        myHandler.removeMessages(0);//remove backup startActivity
                        long cost = System.currentTimeMillis() - start;
                        if (task.isSuccessful()) {
                            Log.v(TAG, String.format("Fetch Succeeded: %s ms", cost));
                            // Once the config is successfully fetched it must be activated before
                            // newly fetched values are returned.
                            mRemoteConfig.activateFetched();
                            checkLatestVersion();
                        } else {
                            Log.e(TAG, "Fetch failed");
                            startNextActivity();
                        }
                    }
                });
        // [END fetch_config_with_callback]
    }

    private static class MyHandler extends Handler {
        private WeakReference<SplashActivity> mActivity;

        MyHandler(SplashActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            //super.handleMessage(msg);
            mActivity.get().startNextActivity();
        }
    }

    private void startNextActivity() {
        overridePendingTransition(0, 0);
        Intent intent = new Intent(this, CalendarForPhoneActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(0, 0);
        this.finish();
        overridePendingTransition(0, 0);
    }

    private final int[] LATEST_TEAMS = {
            Team.ID_LAMIGO_MONKEYS, Team.ID_FUBON_GUARDIANS, Team.ID_UNI_711_LIONS,
            Team.ID_CT_ELEPHANTS//, Team.ID_ALL_STAR_POWER_RED, Team.ID_ALL_STAR_SPEED_WHITE
    };

    private void preparePalette() {
        //FIXME: maybe we can get this from Firebase Remote Config
        CpblApplication application = (CpblApplication) getApplication();
        int year = CpblCalendarHelper.getNowTime().get(Calendar.YEAR);
        for (int id : LATEST_TEAMS) {
            //Log.d(TAG, String.format("preparePalette %d-%d", year, id));
            int logoId = Team.getTeamLogo(id, year);
            //Log.d(TAG, String.format(">>> id=%x", logoId));
            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), logoId);
//            Drawable drawable = getResources().getDrawable(logoId);
//            Bitmap bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888);
//            drawable.setBounds(0, 0, 64, 64);
//            Canvas canvas = new Canvas(bitmap);
//            drawable.draw(canvas);
            Palette p = Palette.from(bitmap).generate();
            //new Palette.Builder(bitmap).maximumColorCount(6).generate();
//            int color = p.getVibrantColor(Color.BLACK);
//            Log.d(TAG, String.format("color = %x %x", color, p.getMutedColor(Color.BLACK)));
//            Log.d(TAG, String.format("  %x, %x, %x, %x, %x", p.getDominantColor(Color.BLACK),
//                    p.getDarkMutedColor(Color.BLACK), p.getLightMutedColor(Color.BLACK),
//                    p.getDarkVibrantColor(Color.BLACK), p.getLightVibrantColor(Color.BLACK)));
//            for (Palette.Swatch swatch : p.getSwatches()) {
//                Log.d(TAG, String.format("rgb: %x, %x, %x", swatch.getRgb(),
//                        swatch.getTitleTextColor(), swatch.getBodyTextColor()));
//            }
            application.setTeamLogoPalette(logoId, p);
        }
    }

    private void checkLatestVersion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            if (isDestroyed() || isFinishing()) {
                return;
            }
        } else {
            if (isFinishing()) {
                return;
            }
        }

        PackageInfo info = PackageUtils.getPackageInfo(this, SplashActivity.class);
        int versionCode = info != null ? info.versionCode : 0;
        if (versionCode > mRemoteConfig.getLong("latest_version_code")) {
            startNextActivity();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.new_version_available_title)
                .setMessage(R.string.new_version_available_message)
                .setPositiveButton(R.string.new_version_available_go,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                startGooglePlayApp();
                            }
                        })
                .setNegativeButton(R.string.new_version_available_later,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                startNextActivity();
                            }
                        })
                .show();
    }

    private void startGooglePlayApp() {
        //http://stackoverflow.com/a/11753070/2673859
        final String appPackageName = getPackageName();
        try {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=" + appPackageName)));
        } catch (android.content.ActivityNotFoundException anfe) {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
        }
        finish();
    }
}
