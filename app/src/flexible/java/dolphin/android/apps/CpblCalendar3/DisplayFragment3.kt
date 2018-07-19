package dolphin.android.apps.CpblCalendar3

import android.os.Bundle
import android.preference.PreferenceFragment

class DisplayFragment3 : PreferenceFragment()/*, Preference.OnPreferenceChangeListener*/ {
//    private lateinit var mCpblTeams: Array<String>
//    private lateinit var mFavoritePref: Preference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.prefs_display)
        addPreferencesFromResource(R.xml.prefs_display_extra3)
//        mFavoritePref = findPreference(PreferenceUtils.KEY_FAVORITE_TEAMS)
//        mFavoritePref.onPreferenceChangeListener = this
    }

//    override fun onResume() {
//        super.onResume()
//        refresh()
//    }
//
//    private fun refresh() {
//        mFavoritePref.summary = PreferenceUtils.getFavoriteTeamSummary(activity)
//    }

//    override fun onPreferenceChange(preference: Preference?, o: Any?): Boolean {
//        if (preference is FixedMultiSelectListPreference) {
//            val teamSet = preference.checkedValues
//            val summary = if (teamSet != null && teamSet.size > 0) {
//                if (mCpblTeams.size == teamSet.size) {
//                    getString(R.string.title_favorite_teams_all)
//                } else {//show the team names one by one
//                    val summaryBuilder = StringBuilder()
//                    for (aTeamSet in teamSet) {
//                        val id = Integer.parseInt(aTeamSet)
//                        summaryBuilder.append(Team.getTeamName(activity, id)).append(" ")
//                    }
//                    summaryBuilder.toString()
//                }
//            } else {
//                getString(R.string.no_favorite_teams)
//            }
//
//            preference.setSummary(summary)
//
//            (activity.application as CpblApplication).setPreferenceChanged(true)
//            return true
//        }
//        return false
//    }
}
