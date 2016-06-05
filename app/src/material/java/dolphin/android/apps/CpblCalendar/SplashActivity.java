package dolphin.android.apps.CpblCalendar;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
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
import java.util.Locale;

public class SplashActivity extends Activity {
    private final static String TAG = "SplashActivity";
    private FirebaseRemoteConfig mRemoteConfig;
    private MyHandler myHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //http://aleung.github.io/blog/2012/10/06/change-locale-in-android-application/
        Locale.setDefault(Locale.TAIWAN);
        Configuration config = getBaseContext().getResources().getConfiguration();
        config.locale = Locale.TAIWAN;
        getBaseContext().getResources().updateConfiguration(config,
                getBaseContext().getResources().getDisplayMetrics());

        FirebaseAnalytics.getInstance(this);//initialize this
        prepareRemoteConfig();

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

        myHandler = new MyHandler(this);
        myHandler.sendEmptyMessageDelayed(0, 1000);//set a backup startActivity
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                showLoadingInProgress();
            }
        }, 500);
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
        //fetchRemoteConfig();
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
                        } else {
                            Log.e(TAG, "Fetch failed");
                        }
                        startNextActivity();
                    }
                });
        // [END fetch_config_with_callback]
    }

    static class MyHandler extends Handler {
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
}
