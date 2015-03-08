package dolphin.android.apps.CpblCalendar;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import java.io.File;
import java.io.IOException;

import dolphin.android.apps.CpblCalendar.preference.DownloadFileDialog;
import dolphin.android.apps.CpblCalendar.preference.NotificationFragment;
import dolphin.android.apps.CpblCalendar.preference.PreferenceUtils;
import dolphin.android.apps.CpblCalendar.provider.Game;

/**
 * Created by dolphin on 2013/9/1.
 * <p/>
 * Notification Dialog-style Activity
 */
public class NotifyDialog extends Activity implements DialogInterface.OnDismissListener {
    private final static String TAG = "NotifyDialog";

    public final static String EXTRA_GAME1 = "game1";

    public final static String EXTRA_GAME2 = "game2";

    private MediaPlayer mMediaPlayer;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (PreferenceUtils.isEngineerMode(this)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        }

        setContentView(R.layout.activity_notify_dialog);

        View button1 = findViewById(android.R.id.button1);
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NotificationManager mNotifyMgr =
                        (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                mNotifyMgr.cancelAll();//[51]dolphin++ clear all notifications
                NotifyDialog.this.finish();
            }
        });

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            final Game game1 = bundle.containsKey(EXTRA_GAME1)
                    ? Game.fromPrefString(this, bundle.getString(EXTRA_GAME1)) : null;
            final Game game2 = bundle.containsKey(EXTRA_GAME2)
                    ? Game.fromPrefString(this, bundle.getString(EXTRA_GAME2)) : null;

            new Handler().post(new Runnable() {
                @Override
                public void run() {
                    load_layout(game1, game2);
                }
            });
        }

        if (PreferenceUtils.isEnableNotifySong(this)) {//[122]++
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    startMusic();
                }
            }, 1000);
        }
    }

    private void startMusic() {
        File song = PreferenceUtils.getNotifySong(this);
        if (!song.exists()) {
            DownloadFileDialog dialog = NotificationFragment.getDownloadCpblThemeDialog(this);
            dialog.setOnDismissListener(this);
            dialog.show();
            return;
        }

        mMediaPlayer = new MediaPlayer();
        try {
            mMediaPlayer.setDataSource(song.getAbsolutePath());
            mMediaPlayer.prepare();
            mMediaPlayer.start();
        } catch (IOException e) {
            Log.e(TAG, "MediaPlayer: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onDismiss(DialogInterface dialogInterface) {
        startMusic();
    }

    @Override
    protected void onDestroy() {
        if (mMediaPlayer != null) {//[122]++
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        super.onDestroy();
    }

    private void load_layout(Game game1, Game game2) {
        ViewGroup layout1 = (ViewGroup) findViewById(R.id.layout1);
        ViewGroup layout2 = (ViewGroup) findViewById(R.id.layout2);
        boolean bIsTablet = getResources().getBoolean(R.bool.config_tablet);
        boolean bShowLogo = PreferenceUtils.isTeamLogoShown(this);

        if (game1 != null) {
            GameAdapter.updateNotifyDialogMatchUp(this, layout1, game1, bIsTablet, bShowLogo);
        } else {
            layout1.setVisibility(View.GONE);
        }

        if (game2 != null) {
            GameAdapter.updateNotifyDialogMatchUp(this, layout2, game2, bIsTablet, bShowLogo);
        } else {
            findViewById(R.id.separator).setVisibility(View.GONE);
            layout2.setVisibility(View.GONE);
        }

        int alarmTime = PreferenceUtils.getAlarmNotifyTime(getBaseContext());
        boolean isVibrate = PreferenceUtils.isEnableNotifyVibrate(getBaseContext());
        sendGmsGoogleAnalyticsReport("UI", "NotifyDialog",
                String.format("before=%d, vibrate=%s", alarmTime, isVibrate));
    }

    protected void sendGmsGoogleAnalyticsReport(String category, String action, String label) {
        // Get tracker.
        Tracker t = ((CpblApplication) getApplication()).getTracker(
                CpblApplication.TrackerName.APP_TRACKER);

        // Set screen name.
        // Where path is a String representing the screen name.
        t.setScreenName("dolphin.android.apps.CpblCalendar.NotifyDialog");

        // Send a screen view.
        t.send(new HitBuilders.AppViewBuilder().build());

        // This event will also be sent with &cd=Home%20Screen.
        // Build and send an Event.
        t.send(new HitBuilders.EventBuilder()
                .setCategory(category)
                .setAction(action)
                .setLabel(label)
                .build());

        // Clear the screen name field when we're done.
        t.setScreenName(null);
    }

}
