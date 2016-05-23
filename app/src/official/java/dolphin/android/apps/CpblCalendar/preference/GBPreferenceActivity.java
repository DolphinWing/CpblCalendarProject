package dolphin.android.apps.CpblCalendar.preference;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceGroup;

import java.util.Locale;

import dolphin.android.apps.CpblCalendar.CpblApplication;
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

        // Get tracker.
        Tracker t = ((CpblApplication) getApplication()).getTracker(
                CpblApplication.TrackerName.APP_TRACKER);
        if (t != null) {
            // Set screen name.
            // Where path is a String representing the screen name.
            t.setScreenName("dolphin.android.apps.CpblCalendar.preference.GBPreferenceActivity");
            // Send a screen view.
            t.send(new HitBuilders.AppViewBuilder().build());
        }

        //getResources().getBoolean(R.bool.pref_engineer_mode);
        mIsEngineerMode = PreferenceUtils.isEngineerMode(this);

        PackageInfo pinfo = PackageUtils.getPackageInfo(this, PreferenceActivity.class);
        findPreference(PreferenceUtils.KEY_APP_VERSION).setSummary(String.format(Locale.US,
                mIsEngineerMode ? "%s  r%d (eng)" : "%s",
                pinfo.versionName, pinfo.versionCode));

        if (mIsEngineerMode) {//[28]dolphin
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        }

        //[29]dolphin++ no support GB/Froyo devices
        Preference pref = findPreference(PreferenceUtils.KEY_INCLUDE_LEADER);
        PreferenceGroup group =
                (PreferenceGroup) findPreference(PreferenceUtils.KEY_DISPLAY_GROUP);
        if (group != null && pref != null) {
            group.removePreference(pref);
        }
    }

//    @Override
//    public void onStart() {
//        super.onStart();
//        //... // The rest of your onStart() code.
//        EasyTracker.getInstance(this).activityStart(this);
//    }
//
//    @Override
//    public void onStop() {
//        super.onStop();
//        //... // The rest of your onStop() code.
//        EasyTracker.getInstance(this).activityStop(this);
//    }
}