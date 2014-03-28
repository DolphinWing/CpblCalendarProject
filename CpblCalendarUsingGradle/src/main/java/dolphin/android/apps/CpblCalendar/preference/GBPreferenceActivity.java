package dolphin.android.apps.CpblCalendar.preference;

import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceGroup;

import com.google.analytics.tracking.android.EasyTracker;

import java.util.Locale;

import dolphin.android.apps.CpblCalendar.R;
import dolphin.android.util.PackageUtils;

/**
 * Created by dolphin on 2013/6/8.
 */
public class GBPreferenceActivity extends android.preference.PreferenceActivity {
    private boolean mIsEngineerMode = false;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.prefs_display);
        addPreferencesFromResource(R.xml.prefs_main);

        //getResources().getBoolean(R.bool.pref_engineer_mode);
        mIsEngineerMode = PreferenceUtils.isEngineerMode(this);

        PackageInfo pinfo = PackageUtils.getPackageInfo(this, PreferenceActivity.class);
        findPreference(PreferenceUtils.KEY_APP_VERSION).setSummary(String.format(Locale.US,
                mIsEngineerMode ? "%s  r%d (eng)" : "%s",
                pinfo.versionName, pinfo.versionCode));

        if (mIsEngineerMode)//[28]dolphin
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);

        //[29]dolphin++ no support GB/Froyo devices
        Preference pref = findPreference(PreferenceUtils.KEY_INCLUDE_LEADER);
        PreferenceGroup group =
                (PreferenceGroup) findPreference(PreferenceUtils.KEY_DISPLAY_GROUP);
        if (group != null && pref != null)
            group.removePreference(pref);
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