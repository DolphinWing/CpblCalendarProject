package dolphin.android.apps.CpblCalendar.preference;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import dolphin.android.apps.CpblCalendar.R;
import dolphin.android.apps.CpblCalendar.provider.Team;
import se.pixelcoding.wikitab.preferences.FixedMultiSelectListPreference;

/**
 * Created by dolphin on 2013/6/8.
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class DisplayFragment extends PreferenceFragment
        implements Preference.OnPreferenceChangeListener {

    private Preference mFavoritePref = null;
    private String[] mCpblTeams;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.prefs_display);

        //dolphin++ 2013-06-17

        // Multi select ListPreference now comes
        // natively with Android from API level 11 (Honeycomb)
        // http://stackoverflow.com/a/5091659

        //MultiSelectListPreference treats entries as keys
        //https://code.google.com/p/android/issues/detail?id=15966
        addPreferencesFromResource(R.xml.prefs_display_extra);

        mFavoritePref = findPreference(PreferenceUtils.KEY_FAVORITE_TEAMS);
        if (mFavoritePref != null) {
            mFavoritePref.setOnPreferenceChangeListener(this);
        }

        //[88]dolphin++ force to remove
        PreferenceGroup group =
                (PreferenceGroup) findPreference(PreferenceUtils.KEY_DISPLAY_GROUP);
        if (group != null) {
            Preference pref = findPreference(PreferenceUtils.KEY_INCLUDE_LEADER);
            if (pref != null) group.removePreference(pref);
        }

        mCpblTeams = getActivity().getResources().getStringArray(R.array.cpbl_team_id);
    }

    private void refresh() {
        String summary = "";
        HashMap<Integer, Team> teamMap = PreferenceUtils.getFavoriteTeams(getActivity());
        if (teamMap != null && teamMap.size() > 0) {
            if (mCpblTeams.length == teamMap.size()) {
                summary = getString(R.string.title_favorite_teams_all);
            } else {//show the team names one by one
                Iterator<Map.Entry<Integer, Team>> iterator = teamMap.entrySet().iterator();
                while (iterator.hasNext()) {
                    Team team = iterator.next().getValue();
                    summary += team.getName() + " ";
                }
            }
        } else {//[34]dolphin++ no team games to show
            summary = getString(R.string.no_favorite_teams);
        }

        //ListPreference pref = //[34]dolphin++
        //        (ListPreference) findPreference(PreferenceUtils.KEY_FAVORITE_TEAMS);
        if (mFavoritePref != null) {
            mFavoritePref.setSummary(summary);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        refresh();//[34]dolphin++
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object o) {
        //refresh();
        if (preference instanceof FixedMultiSelectListPreference) {
            String summary = "";
            Set<String> teamSet =
                    ((FixedMultiSelectListPreference) preference).getCheckedValues();
            if (teamSet != null && teamSet.size() > 0)
                if (mCpblTeams.length == teamSet.size()) {
                    summary = getString(R.string.title_favorite_teams_all);
                } else {//show the team names one by one
                    Iterator<String> iterator = teamSet.iterator();
                    while (iterator.hasNext()) {
                        int id = Integer.parseInt(iterator.next());
                        summary += Team.getTeamName(getActivity(), id) + " ";
                    }
                }
            else
                summary = getString(R.string.no_favorite_teams);

            preference.setSummary(summary);
        }
        return true;
    }
}
