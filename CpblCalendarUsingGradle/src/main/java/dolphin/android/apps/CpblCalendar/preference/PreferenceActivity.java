package dolphin.android.apps.CpblCalendar.preference;

import android.annotation.TargetApi;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;

import com.google.analytics.tracking.android.EasyTracker;

import java.util.List;

import dolphin.android.apps.CpblCalendar.R;

/**
 * Created by dolphin on 2013/6/8.
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class PreferenceActivity extends android.preference.PreferenceActivity {
    //boolean bEnableNotify = true;

    //This API was added due to a newly discovered vulnerability.
    // Please see http://ibm.co/1bAA8kF or http://ibm.co/IDm2Es
    @TargetApi(Build.VERSION_CODES.KITKAT)
    //http://stackoverflow.com/a/20494759/2673859
    @Override
    protected boolean isValidFragment(String fragmentName) {
        return GeneralFragment.class.getName().equals(fragmentName)
                || DisplayFragment.class.getName().equals(fragmentName)
                || NotificationFragment.class.getName().equals(fragmentName);
    }

    public void onCreate(Bundle savedInstanceState) {
        //setTheme(R.style.Theme_Holo_green);
        super.onCreate(savedInstanceState);

        if (PreferenceUtils.isEngineerMode(this))//[28]dolphin
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
    }

    @Override
    public void onBuildHeaders(List<Header> target) {
        //super.onBuildHeaders(target);
        loadHeadersFromResource(R.xml.setting_headers, target);

        boolean bEnableNotify = getResources().getBoolean(R.bool.feature_notify);
        //Log.d(PreferenceUtils.TAG, "id = " + R.string.title_notification_settings);
        for (Header header : target) {//[63]dolphin++
            int id = header.titleRes;
            //Log.d(PreferenceUtils.TAG, "header.titleRes = " + id);
            if ((id == R.string.title_notification_settings) && !bEnableNotify) {
                target.remove(header);
                //Log.w(PreferenceUtils.TAG, "remove " + getString(id));
            }
        }
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