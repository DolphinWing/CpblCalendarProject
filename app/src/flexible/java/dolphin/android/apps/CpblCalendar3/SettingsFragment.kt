package dolphin.android.apps.CpblCalendar3

import android.os.Build
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.preference.PreferenceFragmentCompat
import dolphin.android.apps.CpblCalendar.Utils
import dolphin.android.apps.CpblCalendar.preference.PrefsHelper
import dolphin.android.apps.CpblCalendar.provider.CacheFileHelper
import dolphin.android.util.AssetUtils
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
            val info = PackageUtils.getPackageInfo(activity!!, ListActivity::class.java)
            summary = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                "${info?.versionName} (${info?.longVersionCode})"
            } else {
                "${info?.versionName} (${info?.versionCode})"
            }
        }
    }

    override fun onPreferenceTreeClick(preference: androidx.preference.Preference?): Boolean {
        when (preference?.key) {
            "action_clear_cache" -> {
                var r = true
                activity?.let { ctx ->
                    CacheFileHelper.getCacheDir(ctx)?.listFiles()?.forEach { f ->
                        r = r and f.delete() //delete every file
                    }
                    Toast.makeText(ctx, R.string.title_clear_cache_complete, Toast.LENGTH_SHORT)
                            .show()
                    //(ctx.application as? CpblApplication)?.setPreferenceChanged(true)
                }
                return r
            }
            "use_old_drawer_menu" -> {
                useOldDrawer()
                return true
            }
            "action_restart_app" ->
                activity?.let { a ->
                    startActivity(a.packageManager?.getLaunchIntentForPackage(a.packageName))
                    a.finish()
                    return true
                }
            "data_from_cpbl" -> {
                launchUrl("http://www.cpbl.com.tw/")
                return true
            }
            "data_from_twbsball" -> {
                launchUrl("http://twbsball.dils.tku.edu.tw/")
                return true
            }
            "data_from_zxc22" -> {
                launchUrl("http://zxc22.idv.tw/")
                return true
            }
            "res_asset_studio" -> {
                launchUrl("https://romannurik.github.io/AndroidAssetStudio/")
                return true
            }
            "lib_flexible_adapter" -> {
                launchUrl("https://github.com/davideas/FlexibleAdapter")
                return true
            }
            "lib_number_picker_view" -> {
                launchUrl("https://github.com/Carbs0126/NumberPickerView")
                return true
            }
            "privacy_policy" -> {
                showPrivacyPolicy()
                return true
            }
        }
        return super.onPreferenceTreeClick(preference)
    }

    private fun launchUrl(url: String) {
        Utils.startBrowserActivity(activity, url)
    }

    private fun useOldDrawer() {
        activity?.let { a ->
            //prefs.useDrawerMenu = true
            //startActivity(it.packageManager?.getLaunchIntentForPackage(it.packageName))
            //it.finish()
            //Toast.makeText(it, "clicked!", Toast.LENGTH_SHORT).show()
            AlertDialog.Builder(a)
                    .setTitle(R.string.title_use_drawer_menu)
                    .setMessage(R.string.message_use_drawer_menu)
                    .setPositiveButton(R.string.action_enable) { _, _ ->
                        prefs.useDrawerMenu = true
                        startActivity(a.packageManager?.getLaunchIntentForPackage(a.packageName))
                        a.finish()
                    }
                    .setNegativeButton(android.R.string.cancel) { _, _ -> }
                    .show()
        }
    }

    private fun showPrivacyPolicy() {
        activity?.let { a ->
            AlertDialog.Builder(a)
                    .setTitle(R.string.title_privacy_policy)
                    .setMessage(AssetUtils.read_asset_text(a, "privacy_policy.txt", "utf-8"))
                    .setPositiveButton(android.R.string.ok, null)
                    .show().apply {
                        findViewById<TextView>(android.R.id.message)?.textSize = 12f
                    }
        }
    }
}