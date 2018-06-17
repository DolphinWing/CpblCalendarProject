
package dolphin.android.apps.CpblCalendar.preference;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

import androidx.annotation.NonNull;
import dolphin.android.apps.CpblCalendar.Utils;
import dolphin.android.apps.CpblCalendar3.R;
import dolphin.android.util.AssetUtils;
import dolphin.android.util.PackageUtils;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class GeneralFragment extends PreferenceFragment {
    private final static String TAG = "GeneralFragment";

    public static final String KEY_APP_VERSION = "app_version";
    public static final String VERSION_FILE = "version.txt";
    public static final String VERSION_FILE_ENCODE = "UTF-8";

    private final static int TAPS_TO_BE_A_DEVELOPER = 5;
    private int mDevHitCountdown = TAPS_TO_BE_A_DEVELOPER;
    private Toast mDevHitToast = null;

    private boolean mIsEngineerMode = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.prefs_main);

        final Context context = getActivity().getBaseContext();
        mIsEngineerMode = PreferenceUtils.isEngineerMode(context);
        //getResources().getBoolean(R.bool.pref_engineer_mode);

        PackageInfo pinfo = PackageUtils.getPackageInfo(context, getActivity().getClass());
        if (pinfo != null) {
            findPreference(KEY_APP_VERSION).setSummary(String.format(Locale.US,
                    mIsEngineerMode ? "%s  r%d (eng)" : "%s (r%d)",
                    pinfo.versionName, pinfo.versionCode));
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        mDevHitCountdown = TAPS_TO_BE_A_DEVELOPER;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
                                         @NonNull Preference preference) {
        String key = preference.getKey();
        if (key == null) {
            Log.wtf(TAG, "onPreferenceTreeClick key == null");//should not happen
        } else if (key.equals(KEY_APP_VERSION)) {
            if (--mDevHitCountdown <= 0 || mIsEngineerMode) {
                showVersionTxtDialog();
            } else {
                if (mDevHitToast != null) {
                    mDevHitToast.cancel();
                }
                mDevHitToast = Toast.makeText(getActivity(),
                        getString(R.string.app_change_log_click_toast, mDevHitCountdown),
                        Toast.LENGTH_LONG);
                mDevHitToast.show();
            }
            return true;
        } else if (key.equals(PreferenceUtils.KEY_CPBL_WEB)) {
            Utils.startBrowserActivity(getActivity(), PreferenceUtils.URL_CPBL_OFFICAL_WEBSITE);
        } else if (key.equals(PreferenceUtils.KEY_TWBALL_WIKI)) {
            Utils.startBrowserActivity(getActivity(), PreferenceUtils.URL_TW_BASEBALL_WIKI);
        } else if (key.equals(PreferenceUtils.KEY_ZXC22)) {
            Utils.startBrowserActivity(getActivity(), PreferenceUtils.URL_ZXC22);
        } else if (key.equals(PreferenceUtils.KEY_LIB_EVERNOTE_JOB)) {//[188]dolphin++
            Utils.startBrowserActivity(getActivity(), PreferenceUtils.URL_EVERNOTE_ANDROID_JOB);
//        } else if (key.equals(PreferenceUtils.KEY_LIB_FAB)) {//[129]dolphin++
//            Utils.startBrowserActivity(getActivity(), PreferenceUtils.URL_FLOATING_ACTION_BUTTON);
//        } else if (key.equals(PreferenceUtils.KEY_LIB_CIRCLE_IMAGE_VIEW)) {//[168]dolphin++
//            Utils.startBrowserActivity(getActivity(), PreferenceUtils.URL_CIRCLE_IMAGE_VIEW);
//        } else if (key.equals(PreferenceUtils.KEY_LIB_ABS_WEB)) {
//            Utils.startBrowserActivity(getActivity(), PreferenceUtils.URL_ACTIONBAR_SHERLOCK);
//        } else if (key.equals(PreferenceUtils.KEY_LIB_ABDT_GIT)) {
//            Utils.startBrowserActivity(getActivity(), PreferenceUtils.URL_ACTIONBAR_DRAWER_TOGGLE);
        } else if (key.equals(PreferenceUtils.KEY_RES_AAS)) {//[13]dolphin++
            Utils.startBrowserActivity(getActivity(), PreferenceUtils.URL_ANDROID_ASSET_STUDIO);
        } else if (key.equals(PreferenceUtils.KEY_RES_ICONIC)) {//[13]dolphin++
            Utils.startBrowserActivity(getActivity(), PreferenceUtils.URL_ICONIC_ICON_SET);
//        } else if (key.equals(PreferenceUtils.KEY_LIB_SHOWCASE)) {//[18]dolphin++
//            Utils.startBrowserActivity(getActivity(), PreferenceUtils.URL_SHOWCASE_VIEW);
//        } else if (key.equals(PreferenceUtils.KEY_LIB_NINEOLD)) {//[18]dolphin++
//            Utils.startBrowserActivity(getActivity(), PreferenceUtils.URL_NINEOLD_ANDROID);
        } else if (key.equals(PreferenceUtils.KEY_LIB_FLEXIBLE_ADAPTER)) {//[13]dolphin++
            Utils.startBrowserActivity(getActivity(), "https://github.com/davideas/FlexibleAdapter");
        } else if (key.equals(PreferenceUtils.KEY_LIB_NUMBER_VIEW_PICKER)) {//[13]dolphin++
            Utils.startBrowserActivity(getActivity(), "https://github.com/Carbs0126/NumberPickerView");
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private void showVersionTxtDialog() {
        AlertDialog dialog = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.app_change_log)
                .setMessage(AssetUtils.read_asset_text(getActivity(),
                        VERSION_FILE, VERSION_FILE_ENCODE))// windows Unicode file http://goo.gl/gRyTU
                .setPositiveButton(android.R.string.ok, null)
                .setCancelable(true)
                .create();
        //[39]-- dialog.setIcon(android.R.drawable.ic_popup_reminder);
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();

        if (!getResources().getBoolean(R.bool.config_tablet)) {
            // change AlertDialog message font size
            // http://stackoverflow.com/a/6563075
            TextView textView = dialog.findViewById(android.R.id.message);
            if (textView != null) {
                textView.setTextSize(12);
            }
        }
    }
}
