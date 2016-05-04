package dolphin.android.apps.CpblCalendar.preference;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.widget.Toast;

import java.io.File;

import dolphin.android.apps.CpblCalendar.CpblApplication;
import dolphin.android.apps.CpblCalendar.R;
import dolphin.android.apps.CpblCalendar.provider.AlarmProvider;
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
    private String[] mNotifySongValue;
    private String[] mNotifySongList;

    private final String KEY_DEBUG_LIST = "debug_alarm_list";
    private final String KEY_TEST_SONG = "test_notify_song";

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
        mNotifySongValue = resources.getStringArray(R.array.notify_music_value);
        mNotifySongList = resources.getStringArray(R.array.notify_music);
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
            } else if (group != null) {
                group.removePreference(p4);
                group.removePreference(findPreference(KEY_TEST_SONG));
            }
        }

        ListPreference p5 = (ListPreference) findPreference(PreferenceUtils.KEY_NOTIFY_SONG_LIST);
        if (p5 != null) {
            if (getResources().getBoolean(R.bool.feature_enable_song)) {
                p5.setOnPreferenceChangeListener(this);
                for (int i = 0; i < mNotifySongValue.length; i++) {
                    if (mNotifySongValue[i].equals(p5.getValue())) {
                        p5.setSummary(mNotifySongList[i]);
                        break;
                    }
                }
            } else if (group != null) {
                group.removePreference(p5);
            }
        }

        Preference p6 = findPreference(PreferenceUtils.KEY_NOTIFY_RINGTONE);
        if (p6 != null) {
            p6.setOnPreferenceChangeListener(this);
            //http://stackoverflow.com/a/5017279/2673859
            Uri ringtoneUri = PreferenceUtils.getNotificationUri(getActivity());
            if (ringtoneUri != null) {
                Ringtone ringtone = RingtoneManager.getRingtone(getActivity(), ringtoneUri);
                String name = ringtone.getTitle(getActivity());
                p6.setSummary(name);
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
                AlarmProvider.cancelAlarm((CpblApplication) getActivity().getApplication(), null);
            }
        } else if (key.equalsIgnoreCase(PreferenceUtils.KEY_NOTIFY_SONG)) {
            //check if we have the song on disk, if not, download it
            File f = PreferenceUtils.getNotifySong(getActivity());
            if (Boolean.parseBoolean(o.toString()) && !f.exists()) {
                //Log.d(PreferenceUtils.TAG, "download the theme...");
                DownloadFileDialog dialog = getDownloadCpblThemeDialog(getActivity());
                dialog.setOnDismissListener(this);
                dialog.show();
            }
        } else if (key.equalsIgnoreCase(PreferenceUtils.KEY_NOTIFY_SONG_LIST)) {
            //check if we have the song on disk, if not, download it
            File f = new File(getActivity().getCacheDir(), o.toString());
            if (!f.exists()) {
                //Log.d(PreferenceUtils.TAG, "download the theme: " + o.toString());
                DownloadFileDialog dialog = getDownloadCpblThemeDialog(getActivity(), o.toString());
                dialog.setOnDismissListener(this);
                dialog.show();
            }
            //preference.setSummary(mNotifySongList[Integer.parseInt(o.toString())]);
            for (int i = 0; i < mNotifySongValue.length; i++) {
                if (mNotifySongValue[i].equals(o)) {
                    preference.setSummary(mNotifySongList[i]);
                    break;
                }
            }
        } else if (key.equalsIgnoreCase(PreferenceUtils.KEY_NOTIFY_RINGTONE)) {
            Uri ringtoneUri = Uri.parse(o.toString());
            if (ringtoneUri != null) {
                Ringtone ringtone = RingtoneManager.getRingtone(getActivity(), ringtoneUri);
                String name = ringtone.getTitle(getActivity());
                preference.setSummary(name);
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
            final File song = PreferenceUtils.getNotifySong(getActivity());
            //Log.d(PreferenceUtils.TAG, song.getAbsolutePath());
            if (song.exists()) {
                copyAndPlayMusic(song);
            } else {
                DownloadFileDialog dialog = getDownloadCpblThemeDialog(getActivity());
                dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialogInterface) {
                        copyAndPlayMusic(song);
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
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        final String value = prefs.getString(PreferenceUtils.KEY_NOTIFY_SONG_LIST,
                activity.getString(R.string.def_notify_song));
        return getDownloadCpblThemeDialog(activity, value);
    }

    public static DownloadFileDialog getDownloadCpblThemeDialog(Activity activity, String value) {
        final String[] names = activity.getResources().getStringArray(R.array.notify_music);
        final String[] ids = activity.getResources().getStringArray(R.array.notify_music_google_drive_id);
        final String[] values = activity.getResources().getStringArray(R.array.notify_music_value);
        final int[] sizes = activity.getResources().getIntArray(R.array.notify_music_size);
        int i = 0;
        for (; i < values.length; i++) {
            if (values[i].equals(value)) {
                break;
            }
        }
        //Log.d(PreferenceUtils.TAG, String.format("%d %s", i, value));
        return new DownloadFileDialog.Builder(activity)
                .setMessage(activity.getString(R.string.title_download_song, names[i]))
                .setDownloadTask(ids[i], new File(activity.getCacheDir(), values[i]), sizes[i])
                .build();
    }

    private void copyAndPlayMusic(File song) {
        //File song = PreferenceUtils.getNotifySong(getActivity());
        //copy to somewhere else accessible to other apps
        File testFile = new File(Environment.getExternalStorageDirectory(), song.getName());
        //Log.v(PreferenceUtils.TAG, testFile.getAbsolutePath());
        boolean r = testFile.exists();
        //if (!r) {//[139]jimmy-- you need to copy this since the music may be changed
        if (r) {//delete it first
            testFile.delete();
        }
        r = FileUtils.copyFile(song, testFile);
        if (r) {//copy success
            startMusicPlayer(testFile);
        }
    }

    private void startMusicPlayer(File song) {
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
