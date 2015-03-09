package dolphin.android.apps.CpblCalendar.preference;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.widget.Toast;

import java.io.File;

import dolphin.android.apps.CpblCalendar.NotifyReceiver;
import dolphin.android.apps.CpblCalendar.R;
import dolphin.android.util.FileUtils;
import dolphin.android.util.PackageUtils;

/**
 * Created by dolphin on 2013/8/31.
 * <p/>
 * Notification Configurations
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class NotificationFragment extends PreferenceFragment
        implements Preference.OnPreferenceChangeListener, DialogInterface.OnDismissListener {
    private boolean mNotifyTeams;
    private String[] mPendingActions;
    private String[] mNotifyAlarms;
    private String[] mNotifyAlarmValues;//[54]dolphin++
    private AlarmHelper mHelper;
    private CheckBoxPreference mEnableNotifySong;

    private String KEY_DEBUG_LIST = "debug_alarm_list";
    private String KEY_TEST_SONG = "test_notify_song";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.prefs_notification);

        final Activity activity = getActivity();
        final Resources resources = activity.getResources();
        final PreferenceGroup group =
                (PreferenceGroup) findPreference(PreferenceUtils.KEY_NOTIFICATION_GROUP);

        mHelper = new AlarmHelper(activity);
        //[50]dolphin++ currently only support manual register alarm
        mNotifyTeams = resources.getBoolean(R.bool.feature_notify_teams);
        if (!mNotifyTeams) {

            if (group != null) {
                Preference pref = findPreference(PreferenceUtils.KEY_NOTIFY_TEAMS);
                if (pref != null)
                    group.removePreference(pref);
                pref = findPreference(PreferenceUtils.KEY_MANUAL_GAME_NOTIFY);
                if (pref != null)
                    group.removePreference(pref);
            }
        }

        mPendingActions = resources.getStringArray(R.array.notify_pending_action);
        mNotifyAlarms = resources.getStringArray(R.array.notify_time);
        mNotifyAlarmValues = resources.getStringArray(R.array.notify_time_value);//[54]dolphin++
        Preference p1 = findPreference(PreferenceUtils.KEY_NOTIFY_PENDING_ACTION);
        if (p1 != null) {
            p1.setSummary(mPendingActions[PreferenceUtils.getNotifyPendingAction(activity)]);
            p1.setOnPreferenceChangeListener(this);
        }

        Preference p2 = findPreference(PreferenceUtils.KEY_NOTIFY_ALARM);
        if (p2 != null) {
            int i = getAlarmTimeIndex(PreferenceUtils.getAlarmNotifyTime(activity));
            p2.setSummary(mNotifyAlarms[i]);
            p2.setOnPreferenceChangeListener(this);
        }

        Preference p3 = findPreference(PreferenceUtils.KEY_ENABLE_NOTIFICATION);
        if (p3 != null) {
            p3.setOnPreferenceChangeListener(this);
        }

        Preference p4 = findPreference(PreferenceUtils.KEY_NOTIFY_SONG);
        if (p4 != null) {
            if (getResources().getBoolean(R.bool.feature_enable_song)) {
                p4.setOnPreferenceChangeListener(this);
                mEnableNotifySong = (CheckBoxPreference) p4;
            } else {
                group.removePreference(p4);
                group.removePreference(findPreference(KEY_TEST_SONG));
            }
        }

        //for debug version, add show existing alarms
        Preference pList = findPreference(KEY_DEBUG_LIST);
        if (pList != null && !PreferenceUtils.isEngineerMode(getActivity())) {
            getPreferenceScreen().removePreference(pList);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object o) {
        String key = preference.getKey();
        if (key == null) {
            return false;
        } else if (key.equalsIgnoreCase(PreferenceUtils.KEY_NOTIFY_PENDING_ACTION)) {
            preference.setSummary(mPendingActions[Integer.parseInt(o.toString())]);
        } else if (key.equalsIgnoreCase(PreferenceUtils.KEY_NOTIFY_ALARM)) {
            int i = getAlarmTimeIndex(Integer.parseInt(o.toString()));
            preference.setSummary(mNotifyAlarms[i]);
        } else if (key.equalsIgnoreCase(PreferenceUtils.KEY_ENABLE_NOTIFICATION)) {
            //delete all pending alarms and cancel the notifications
            if (!Boolean.parseBoolean(o.toString())) {//[57]dolphin++
                Log.w(AlarmHelper.TAG, "clear all alarms");
                mHelper.clear();
                NotifyReceiver.cancelAlarm(getActivity(), null);
            }
        } else if (key.equalsIgnoreCase(PreferenceUtils.KEY_NOTIFY_SONG)) {
            //check if we have the song on disk, if not, download it
            File f = PreferenceUtils.getNotifySong(getActivity());
            if (Boolean.parseBoolean(o.toString()) && !f.exists()) {
                Log.d(PreferenceUtils.TAG, "download the theme");
                DownloadFileDialog dialog = getDownloadCpblThemeDialog(getActivity());
                dialog.setOnDismissListener(this);
                dialog.show();
            }
        }
        return true;
    }

    private int getAlarmTimeIndex(int value) {
        for (int i = 0; i < mNotifyAlarmValues.length; i++) {
            if (Integer.parseInt(mNotifyAlarmValues[i]) == value) {
                return i;
            }
        }
        return 0;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
                                         Preference preference) {
        if (preference == null) {
            return false;
        }
        if (preference.getKey().equalsIgnoreCase(KEY_DEBUG_LIST)) {
            return true;
        }
        if (preference.getKey().equalsIgnoreCase(KEY_TEST_SONG)) {
            File song = PreferenceUtils.getNotifySong(getActivity());
            if (song.exists()) {
                startMusicPlayer();
            } else {
                DownloadFileDialog dialog = getDownloadCpblThemeDialog(getActivity());
                dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialogInterface) {
                        startMusicPlayer();
                    }
                });
                dialog.show();
            }
            return true;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public void onDismiss(DialogInterface dialogInterface) {
        File f = PreferenceUtils.getNotifySong(getActivity());
        if (!f.exists() && mEnableNotifySong != null) {
            SharedPreferences preferences = mEnableNotifySong.getSharedPreferences();
            preferences.edit().putBoolean(PreferenceUtils.KEY_NOTIFY_SONG, false).apply();
        }
    }

    public static DownloadFileDialog getDownloadCpblThemeDialog(Activity activity) {
        return new DownloadFileDialog.Builder(activity)
                .setMessage(R.string.title_download_song)
                //0B-oMP4622t0hbFlrZTlpaXN4SUk 2775540
                .setDownloadTask("0B-oMP4622t0hVlhhNHlNamlvM1k",
                        PreferenceUtils.getNotifySong(activity), 3331472)
                .build();
    }

    private void startMusicPlayer() {
        File song = PreferenceUtils.getNotifySong(getActivity());
//        File testFile = new File(Environment.getExternalStorageDirectory(), "cpbl-theme.mp3");
//        Log.d(PreferenceUtils.TAG, testFile.getAbsolutePath());
//        boolean r = testFile.exists();
//        if (!r) {
//            r = FileUtils.copyFile(song, testFile);
//        }
//        if (r && testFile.exists()) {
        if (song.exists()) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.fromFile(song), "audio/*");
            //intent = Intent.createChooser(intent, null);
            if (PackageUtils.isCallable(getActivity(), intent)) {
                startActivity(intent);
            } else {
                Toast.makeText(getActivity(), R.string.title_test_song_no_support,
                        Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(getActivity(), R.string.title_test_song_fail, Toast.LENGTH_SHORT).show();
        }
    }
}
