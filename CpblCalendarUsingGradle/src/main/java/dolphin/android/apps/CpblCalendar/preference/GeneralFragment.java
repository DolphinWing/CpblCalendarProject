
package dolphin.android.apps.CpblCalendar.preference;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.TextView;

import java.util.Locale;

import dolphin.android.apps.CpblCalendar.R;
import dolphin.android.apps.CpblCalendar.SplashActivity;
import dolphin.android.util.AssetUtils;
import dolphin.android.util.PackageUtils;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class GeneralFragment extends PreferenceFragment {
    private final static String TAG = "GeneralFragment";

    public static final String KEY_APP_VERSION = "app_version";
    public static final String VERSION_FILE = "Version.txt";
    public static final String VERSION_FILE_ENCODE = "UTF-8";

    private boolean mIsEngineerMode = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.prefs_main);

        final Context context = getActivity().getBaseContext();
        mIsEngineerMode = PreferenceUtils.isEngineerMode(context);
        //getResources().getBoolean(R.bool.pref_engineer_mode);

        PackageInfo pinfo = PackageUtils.getPackageInfo(context, SplashActivity.class);
        if (pinfo != null) {
            findPreference(KEY_APP_VERSION).setSummary(String.format(Locale.US,
                    mIsEngineerMode ? "%s  r%d (eng)" : "%s (r%d)",
                    pinfo.versionName, pinfo.versionCode));
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
                                         @NonNull Preference preference) {
        String key = preference.getKey();
        if (key == null) {
            Log.wtf(TAG, "onPreferenceTreeClick key == null");//should not happen
        } else if (key.equals(KEY_APP_VERSION) && mIsEngineerMode) {
            AlertDialog dialog = new AlertDialog.Builder(getActivity()).create();
            dialog.setTitle(R.string.app_change_log);
            //[39]-- dialog.setIcon(android.R.drawable.ic_popup_reminder);
            // windows Unicode file http://goo.gl/gRyTU
            dialog.setMessage(AssetUtils.read_asset_text(getActivity(),
                    VERSION_FILE, VERSION_FILE_ENCODE));
            dialog.setButton(AlertDialog.BUTTON_POSITIVE, getActivity()
                    .getString(android.R.string.ok), new OnClickListener() {

                @Override
                public void onClick(DialogInterface arg0, int arg1) {
                    // do nothing, just dismiss
                }
            });
            dialog.show();

            // change AlertDialog message font size
            // http://stackoverflow.com/a/6563075
            TextView textView = (TextView) dialog.findViewById(android.R.id.message);
            textView.setTextSize(12);
            return true;
        } else if (key.equals(PreferenceUtils.KEY_CPBL_WEB)) {
            PreferenceUtils.startBrowserActivity(getActivity(), PreferenceUtils.URL_CPBL_OFFICAL_WEBSITE);
        } else if (key.equals(PreferenceUtils.KEY_TWBALL_WIKI)) {
            PreferenceUtils.startBrowserActivity(getActivity(), PreferenceUtils.URL_TW_BASEBALL_WIKI);
        } else if (key.equals(PreferenceUtils.KEY_ZXC22)) {
            PreferenceUtils.startBrowserActivity(getActivity(), PreferenceUtils.URL_ZXC22);
        } else if (key.equals(PreferenceUtils.KEY_LIB_EVERNOTE_JOB)) {//[188]dolphin++
            PreferenceUtils.startBrowserActivity(getActivity(), PreferenceUtils.URL_EVERNOTE_ANDROID_JOB);
        } else if (key.equals(PreferenceUtils.KEY_LIB_FAB)) {//[129]dolphin++
            PreferenceUtils.startBrowserActivity(getActivity(), PreferenceUtils.URL_FLOATING_ACTION_BUTTON);
        } else if (key.equals(PreferenceUtils.KEY_LIB_CIRCLE_IMAGE_VIEW)) {//[168]dolphin++
            PreferenceUtils.startBrowserActivity(getActivity(), PreferenceUtils.URL_CIRCLE_IMAGE_VIEW);
        } else if (key.equals(PreferenceUtils.KEY_LIB_ABS_WEB)) {
            PreferenceUtils.startBrowserActivity(getActivity(), PreferenceUtils.URL_ACTIONBAR_SHERLOCK);
        } else if (key.equals(PreferenceUtils.KEY_LIB_ABDT_GIT)) {
            PreferenceUtils.startBrowserActivity(getActivity(), PreferenceUtils.URL_ACTIONBAR_DRAWER_TOGGLE);
        } else if (key.equals(PreferenceUtils.KEY_RES_AAS)) {//[13]dolphin++
            PreferenceUtils.startBrowserActivity(getActivity(), PreferenceUtils.URL_ANDROID_ASSET_STUDIO);
        } else if (key.equals(PreferenceUtils.KEY_RES_ICONIC)) {//[13]dolphin++
            PreferenceUtils.startBrowserActivity(getActivity(), PreferenceUtils.URL_ICONIC_ICON_SET);
        } else if (key.equals(PreferenceUtils.KEY_LIB_SHOWCASE)) {//[18]dolphin++
            PreferenceUtils.startBrowserActivity(getActivity(), PreferenceUtils.URL_SHOWCASE_VIEW);
        } else if (key.equals(PreferenceUtils.KEY_LIB_NINEOLD)) {//[18]dolphin++
            PreferenceUtils.startBrowserActivity(getActivity(), PreferenceUtils.URL_NINEOLD_ANDROID);
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }


}
