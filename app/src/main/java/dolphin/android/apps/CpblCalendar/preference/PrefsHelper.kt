package dolphin.android.apps.CpblCalendar.preference

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.util.SparseArray
import dolphin.android.apps.CpblCalendar.provider.Team
import dolphin.android.apps.CpblCalendar3.R

class PrefsHelper(private val context: Context) {
    private val sharedPrefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(
            context)

    private fun getResBoolean(resId: Int) = context.resources.getBoolean(resId)

    val engineerMode: Boolean
        get() = getResBoolean(R.bool.pref_engineer_mode)

    val isTeamLogoShown: Boolean
        get() = sharedPrefs.getBoolean("team_logo", getResBoolean(R.bool.def_team_logo))

    val isHighlightWin: Boolean
        get() = sharedPrefs.getBoolean("highlight_win", getResBoolean(R.bool.def_highlight_win))

    val isHighlightToday: Boolean
        get() = sharedPrefs.getBoolean("highlight_today", getResBoolean(R.bool.def_highlight_today))

    val autoScrollUpcoming: Boolean
        get() = sharedPrefs.getBoolean("upcoming_game", getResBoolean(R.bool.def_upcoming_game))

    var favoriteTeams: SparseArray<Team>
        get() {
            val teams = SparseArray<Team>()
            sharedPrefs.getStringSet("favorite_teams", HashSet<String>().apply {
                addAll(context.resources.getStringArray(R.array.cpbl_team_id))
            })?.forEach {
                val id = it.toInt()
                when (id) {
                    Team.ID_ELEPHANTS, Team.ID_CT_ELEPHANTS ->
                        teams.put(Team.ID_CT_ELEPHANTS, Team(context, Team.ID_CT_ELEPHANTS))
                    Team.ID_UNI_LIONS, Team.ID_UNI_711_LIONS ->
                        teams.put(Team.ID_UNI_711_LIONS, Team(context, Team.ID_UNI_711_LIONS))
                    Team.ID_EDA_RHINOS, Team.ID_FUBON_GUARDIANS ->
                        teams.put(Team.ID_FUBON_GUARDIANS, Team(context, Team.ID_FUBON_GUARDIANS))
                    else -> teams.put(id, Team(context, id))
                }
            }
            return teams
        }
        set(value) {
            val teams = HashSet<String>()
            for (i in 0 until value.size()) {
                teams.add(value.valueAt(i).id.toString())
            }
            sharedPrefs.edit().putStringSet("favorite_teams", teams).apply()
        }

    val pullToRefreshEnabled: Boolean
        get() = sharedPrefs.getBoolean("pull_to_refresh",
                getResBoolean(R.bool.def_enable_pull_to_refresh))

    var useDrawerMenu: Boolean
        get() = sharedPrefs.getBoolean("use_drawer_menu", false)
        set(value) = sharedPrefs.edit().putBoolean("use_drawer_menu", value).apply()

    var cacheModeEnabled: Boolean
        get() = sharedPrefs.getBoolean("cache_mode", getResBoolean(R.bool.def_cache_mode))
        set(value) = sharedPrefs.edit().putBoolean("cache_mode", value).apply()

    val showHighlightOnLoadEnabled: Boolean
        get() = sharedPrefs.getBoolean("show_highlight_onload",
                getResBoolean(R.bool.def_show_highlight))
}