package dolphin.android.apps.CpblCalendar.preference

import android.app.AlertDialog
import android.os.Bundle
import android.preference.Preference
import android.preference.PreferenceFragment
import android.preference.PreferenceScreen
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import dolphin.android.apps.CpblCalendar.Utils
import dolphin.android.apps.CpblCalendar3.R
import dolphin.android.util.AssetUtils
import dolphin.android.util.PackageUtils
import java.util.*

class GeneralFragment : PreferenceFragment() {
    private var mDevHitCountdown = TAPS_TO_BE_A_DEVELOPER
    private var mDevHitToast: Toast? = null

    private var mIsEngineerMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.prefs_main)

        mIsEngineerMode = PrefsHelper(activity).engineerMode
        //getResources().getBoolean(R.bool.pref_engineer_mode);

        PackageUtils.getPackageInfo(activity, activity.javaClass)?.let {
            findPreference(KEY_APP_VERSION).summary = String.format(Locale.US,
                    if (mIsEngineerMode) "%s  r%d (eng)" else "%s (r%d)",
                    it.versionName, it.versionCode)
        }
    }

    override fun onResume() {
        super.onResume()

        mDevHitCountdown = TAPS_TO_BE_A_DEVELOPER
    }

    override fun onPreferenceTreeClick(preferenceScreen: PreferenceScreen,
                                       preference: Preference): Boolean {
        val key = preference.key
        when (key) {
            null -> Log.wtf(TAG, "onPreferenceTreeClick key == null")//should not happen
            KEY_APP_VERSION -> {
                if (--mDevHitCountdown <= 0 || mIsEngineerMode) {
                    showVersionTxtDialog()
                } else {
                    if (mDevHitToast != null) {
                        mDevHitToast!!.cancel()
                    }
                    mDevHitToast = Toast.makeText(activity,
                            getString(R.string.app_change_log_click_toast, mDevHitCountdown),
                            Toast.LENGTH_LONG)
                    mDevHitToast!!.show()
                }
                return true
            }
            "data_from_cpbl" -> Utils.startBrowserActivity(activity, "http://www.cpbl.com.tw/")
            "data_from_twbsball" ->
                Utils.startBrowserActivity(activity, "http://twbsball.dils.tku.edu.tw/")
            "data_from_zxc22" -> Utils.startBrowserActivity(activity, "http://zxc22.idv.tw/")
            "lib_evernote_android_job" -> //[188]dolphin++
                Utils.startBrowserActivity(activity,
                        "https://blog.evernote.com/tech/2015/10/26/unified-job-library-android/")
            "res_asset_studio" -> //[13]dolphin++
                Utils.startBrowserActivity(activity, "https://romannurik.github.io/AndroidAssetStudio/")
            "res_iconic_set" -> //[13]dolphin++
                Utils.startBrowserActivity(activity, "http://somerandomdude.com/work/iconic/")
            "lib_flexible_adapter" -> //[13]dolphin++
                Utils.startBrowserActivity(activity, "https://github.com/davideas/FlexibleAdapter")
            "lib_number_picker_view" -> //[13]dolphin++
                Utils.startBrowserActivity(activity, "https://github.com/Carbs0126/NumberPickerView")
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference)
    }

    private fun showVersionTxtDialog() {
        val dialog = AlertDialog.Builder(activity)
                .setTitle(R.string.app_change_log)
                .setMessage(AssetUtils.read_asset_text(activity,
                        VERSION_FILE, VERSION_FILE_ENCODE))// windows Unicode file http://goo.gl/gRyTU
                .setPositiveButton(android.R.string.ok, null)
                .setCancelable(true)
                .create()
        //[39]-- dialog.setIcon(android.R.drawable.ic_popup_reminder);
        dialog.setCanceledOnTouchOutside(true)
        dialog.show()

        if (!resources.getBoolean(R.bool.config_tablet)) {
            // change AlertDialog message font size
            // http://stackoverflow.com/a/6563075
            val textView = dialog.findViewById<TextView>(android.R.id.message)
            if (textView != null) {
                textView.textSize = 12f
            }
        }
    }

    companion object {
        private const val TAG = "GeneralFragment"

        private const val KEY_APP_VERSION = "app_version"
        const val VERSION_FILE = "version.txt"
        const val VERSION_FILE_ENCODE = "UTF-8"

        private const val TAPS_TO_BE_A_DEVELOPER = 5
    }
}
