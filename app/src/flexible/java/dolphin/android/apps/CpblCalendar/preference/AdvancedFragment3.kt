package dolphin.android.apps.CpblCalendar.preference

import android.os.Bundle
import android.preference.Preference
import android.preference.PreferenceScreen
import android.widget.Toast
import androidx.preference.PreferenceFragmentCompat
import dolphin.android.apps.CpblCalendar.provider.CpblCalendarHelper
import dolphin.android.apps.CpblCalendar3.CpblApplication
import dolphin.android.apps.CpblCalendar3.R

class AdvancedFragment3 : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.prefs_advanced)
        findPreference(KEY_CLEAR_CACHE)?.apply {
            if (activity != null && PrefsHelper(activity!!).cacheModeEnabled) {
                isEnabled = false
                setSummary(R.string.summary_clear_cache_off)
            }
            if (activity == null || CpblCalendarHelper.getCacheDir(activity) == null) {
                isEnabled = false
                setSummary(R.string.summary_clear_cache_null)
            }
        }
    }

    override fun onPreferenceTreeClick(preference: androidx.preference.Preference?): Boolean {
        when (preference?.key) {
            KEY_CLEAR_CACHE -> {
                var r = true
                if (activity != null) {
                    CpblCalendarHelper.getCacheDir(activity)?.listFiles()?.forEach {
                        r = r and it.delete() //delete every file
                    }
                    Toast.makeText(activity, R.string.title_clear_cache_complete,
                            Toast.LENGTH_SHORT).show()

                    (activity?.application as? CpblApplication)?.isUpdateRequired = true
                }
                return r
            }
            KEY_RESTART_APP -> activity?.let {
                startActivity(it.packageManager?.getLaunchIntentForPackage(it.packageName))
                it.finish()
                return true
            }
        }
        return super.onPreferenceTreeClick(preference)
    }

    companion object {
        //private const val TAG = "AdvancedFragment"
        private const val KEY_CLEAR_CACHE = "action_clear_cache"
        private const val KEY_RESTART_APP = "action_restart_app"
    }

}
