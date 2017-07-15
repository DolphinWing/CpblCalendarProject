package dolphin.android.apps.CpblCalendar.preference;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.widget.Toast;

import java.io.File;

import dolphin.android.apps.CpblCalendar3.CpblApplication;
import dolphin.android.apps.CpblCalendar3.R;
import dolphin.android.apps.CpblCalendar3.SplashActivity;
import dolphin.android.apps.CpblCalendar.provider.CpblCalendarHelper;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class AdvancedFragment extends PreferenceFragment {
    private final static String TAG = "AdvancedFragment";

    private static final String KEY_CLEAR_CACHE = "action_clear_cache";

    private final static String KEY_RESTART_APP = "action_restart_app";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.prefs_advanced);

        Preference p1 = findPreference(KEY_CLEAR_CACHE);
        if (p1 != null) {
            if (PreferenceUtils.isCacheMode(getActivity())) {
                p1.setEnabled(false);
                p1.setSummary(R.string.summary_clear_cache_off);
            }
            if ((getActivity() == null || CpblCalendarHelper.getCacheDir(getActivity()) == null)) {
                p1.setEnabled(false);
                p1.setSummary(R.string.summary_clear_cache_null);
            }
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
                                         Preference preference) {
        String key = preference != null ? preference.getKey() : null;
        if (key == null) {
            Log.wtf(TAG, "onPreferenceTreeClick key == null");//should not happen
        } else if (key.equals(KEY_CLEAR_CACHE)) {
            boolean r = true;
            File[] caches = null;
            if (getActivity() != null) {
                File cacheDir = CpblCalendarHelper.getCacheDir(getActivity());
                if (cacheDir != null) {
                    caches = cacheDir.listFiles();
                }
                if (caches != null && caches.length > 0) {
                    for (File f : caches) {//delete every file
                        r &= f.delete();
                    }
                }
                Toast.makeText(getActivity(), R.string.title_clear_cache_complete,
                        Toast.LENGTH_SHORT).show();

                ((CpblApplication) getActivity().getApplication()).setPreferenceChanged(true);
            }
            return r;
        } else if (key.equals(KEY_RESTART_APP)) {
            Intent intent = new Intent(getActivity(), SplashActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            getActivity().finish();
            return true;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

}
