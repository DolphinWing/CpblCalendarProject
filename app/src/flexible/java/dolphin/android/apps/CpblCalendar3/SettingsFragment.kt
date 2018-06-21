package dolphin.android.apps.CpblCalendar3

import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.preference.PreferenceFragmentCompat
import dolphin.android.apps.CpblCalendar.Utils
import dolphin.android.apps.CpblCalendar.preference.PrefsHelper
import dolphin.android.apps.CpblCalendar.provider.CpblCalendarHelper
import dolphin.android.util.PackageUtils

class SettingsFragment : PreferenceFragmentCompat() {
    private lateinit var prefs: PrefsHelper

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        prefs = PrefsHelper(activity!!)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.prefs_display3)
        addPreferencesFromResource(R.xml.prefs_main)
        addPreferencesFromResource(R.xml.prefs_advanced)

        findPreference("app_version")?.apply {
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

                    //(activity?.application as? CpblApplication)?.setPreferenceChanged(true)
                }
                return r
            }
            "use_old_drawer_menu" -> activity?.let {
                //prefs.useDrawerMenu = true
                //startActivity(it.packageManager?.getLaunchIntentForPackage(it.packageName))
                //it.finish()
                //Toast.makeText(it, "clicked!", Toast.LENGTH_SHORT).show()
                AlertDialog.Builder(it)
                        .setTitle("hello")
                        .setMessage("bye")
                        .setPositiveButton(R.string.action_open_app) { _, _ ->
                            prefs.useDrawerMenu = true
                            startActivity(it.packageManager?.getLaunchIntentForPackage(it.packageName))
                            it.finish()
                        }
                        .setNegativeButton(android.R.string.cancel) { _, _ -> }
                        .show()
                return true
            }
            "action_restart_app" -> activity?.let {
                startActivity(it.packageManager?.getLaunchIntentForPackage(it.packageName))
                it.finish()
                return true
            }
            "data_from_cpbl" -> {
                Utils.startBrowserActivity(activity, "http://www.cpbl.com.tw/")
                return true
            }
            "data_from_twbsball" -> {
                Utils.startBrowserActivity(activity, "http://twbsball.dils.tku.edu.tw/")
                return true
            }
            "data_from_zxc22" -> {
                Utils.startBrowserActivity(activity, "http://zxc22.idv.tw/")
                return true
            }
            "res_asset_studio" -> {
                Utils.startBrowserActivity(activity, "https://romannurik.github.io/AndroidAssetStudio/")
                return true
            }
            "lib_flexible_adapter" -> {
                Utils.startBrowserActivity(activity, "https://github.com/davideas/FlexibleAdapter")
                return true
            }
            "lib_number_picker_view" -> {
                Utils.startBrowserActivity(activity, "https://github.com/Carbs0126/NumberPickerView")
                return true
            }
        }
        return super.onPreferenceTreeClick(preference)
    }
}