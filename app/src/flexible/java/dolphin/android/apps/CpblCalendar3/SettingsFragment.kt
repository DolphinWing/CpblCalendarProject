package dolphin.android.apps.CpblCalendar3

import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.preference.PreferenceFragmentCompat
import dolphin.android.apps.CpblCalendar.Utils
import dolphin.android.apps.CpblCalendar.preference.PreferenceUtils
import dolphin.android.apps.CpblCalendar.provider.CpblCalendarHelper
import dolphin.android.util.PackageUtils

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.prefs_main)
        addPreferencesFromResource(R.xml.prefs_display)
        addPreferencesFromResource(R.xml.prefs_display_extra3)
        addPreferencesFromResource(R.xml.prefs_advanced)

        findPreference(PreferenceUtils.KEY_APP_VERSION)?.apply {
            val info = PackageUtils.getPackageInfo(activity, activity?.javaClass)
            summary = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                "${info.versionName} (${info.longVersionCode})"
            } else {
                "${info.versionName} (${info.versionCode})"
            }
        }
    }

    override fun onPreferenceTreeClick(preference: androidx.preference.Preference?): Boolean {
        when (preference?.key) {
            "action_clear_cache" -> {
                var r = true
                if (activity != null) {
                    CpblCalendarHelper.getCacheDir(activity)?.listFiles()?.forEach {
                        r = r and it.delete() //delete every file
                    }
                    Toast.makeText(activity, R.string.title_clear_cache_complete,
                            Toast.LENGTH_SHORT).show()

                    (activity?.application as? CpblApplication)?.setPreferenceChanged(true)
                }
                return r
            }
            "action_restart_app" -> activity?.let {
                startActivity(it.packageManager?.getLaunchIntentForPackage(it.packageName))
                it.finish()
                return true
            }
            PreferenceUtils.KEY_CPBL_WEB -> {
                Utils.startBrowserActivity(activity, "http://www.cpbl.com.tw/")
                return true
            }
            PreferenceUtils.KEY_TWBALL_WIKI -> {
                Utils.startBrowserActivity(activity, "http://twbsball.dils.tku.edu.tw/")
                return true
            }
            PreferenceUtils.KEY_ZXC22 -> {
                Utils.startBrowserActivity(activity, "http://zxc22.idv.tw/")
                return true
            }
            PreferenceUtils.URL_ANDROID_ASSET_STUDIO -> {
                Utils.startBrowserActivity(activity, "https://romannurik.github.io/AndroidAssetStudio/")
                return true
            }
            PreferenceUtils.KEY_LIB_FLEXIBLE_ADAPTER -> {
                Utils.startBrowserActivity(activity, "https://github.com/davideas/FlexibleAdapter")
                return true
            }
            PreferenceUtils.KEY_LIB_NUMBER_VIEW_PICKER -> {
                Utils.startBrowserActivity(activity, "https://github.com/Carbs0126/NumberPickerView")
                return true
            }
        }
        return super.onPreferenceTreeClick(preference)
    }
}