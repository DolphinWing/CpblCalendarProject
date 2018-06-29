package dolphin.android.apps.CpblCalendar.provider

import android.content.Context
import androidx.annotation.Keep
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import dolphin.android.apps.CpblCalendar3.R
import java.io.IOException
import java.util.*

/**
 * Team object
 * Created by dolphin on 2013/6/8.
 */
@Suppress("MemberVisibilityCanBePrivate")
@Keep
class Team {

    /**
     * get team id
     *
     * @return team id
     */
    val id: Int
    private var mName: String? = null//[66]++
    private val mContext: Context?
    private var mShortName: String? = null
    private var mLogoId: Int = 0
    var isHomeTeam = false

    /**
     * get team name
     *
     * @return team name
     */
    //fix the empty name
    val name: String
        get() {
            if (mContext != null && id != ID_UNKNOWN) {
                mName = getTeamName(mContext, id)
            }
            if (mName == null || mName!!.isEmpty()) {
                mName = getTeamName(mContext, if (isHomeTeam) ID_UNKNOWN_HOME else ID_UNKNOWN_AWAY)
            }
            return mName ?: ""
        }

    /**
     * get short team name
     *
     * @return team name
     */
    val shortName: String?
        get() {
            if (mShortName != null) {
                return mShortName
            }
            if (mContext != null && id != ID_UNKNOWN) {
                mShortName = getShortName(mContext, id)
            }
            return if (mShortName != null) {
                mShortName
            } else name
        }

    @JvmOverloads
    constructor(context: Context?, id: Int, home: Boolean = false) {
        mContext = context
        this.id = id
        isHomeTeam = home
    }

    constructor(context: Context, name: String?, year: Int) : this(context, getTeamId(context, name, year)) {
        mName = name//[66]++
    }

    constructor(id: Int, name: String, shortName: String, logo: Int) {
        this.id = id
        mContext = null
        mName = name
        mShortName = shortName
        mLogoId = logo
    }

    /**
     * get team logo resource id
     *
     * @param year year
     * @return logo drawable
     */
    fun getLogo(year: Int): Int {
        if (mLogoId > 0) {
            return mLogoId
        }
        return if (id != ID_UNKNOWN) getTeamLogo(id, year) else R.drawable.no_logo
    }

    //http://goo.gl/JeMgZh
    private class TeamTypeAdapter internal constructor(private val mContext: Context) : TypeAdapter<Team>() {

        @Throws(IOException::class)
        override fun write(jsonWriter: JsonWriter, team: Team?) {
            if (team == null) {
                jsonWriter.nullValue()
                return
            }
            jsonWriter.beginObject()
            jsonWriter.name("id").value(team.id.toLong())
            if (team.shortName != null)
                jsonWriter.name("name").value(team.name)
            jsonWriter.endObject()
        }

        @Throws(IOException::class)
        override fun read(jsonReader: JsonReader): Team? {
            if (jsonReader.peek() == JsonToken.NULL) {
                jsonReader.nextNull()
                return null
            }

            var teamId = ID_UNKNOWN
            var teamName: String? = null

            jsonReader.beginObject()
            while (jsonReader.hasNext()) {
                val name = jsonReader.nextName()
                if (name == "id") {
                    teamId = jsonReader.nextInt()
                } else if (name == "name") {
                    teamName = jsonReader.nextString()
                }
            }
            jsonReader.endObject()
            return if (teamId != ID_UNKNOWN) Team(mContext, teamId) else Team(mContext, teamName, 2014)
        }
    }

    companion object {
        const val ID_UNKNOWN = 0
        const val ID_UNKNOWN_AWAY = -1
        const val ID_UNKNOWN_HOME = -2
        //ID_ELEPHANTS -> ID_CT_ELEPHANTS
        const val ID_ELEPHANTS = 1
        //
        const val ID_UNI_LIONS = 2
        const val ID_UNI_711_LIONS = 7112//[184]++
        //
        const val ID_W_DRAGONS = 3
        //
        const val ID_SS_TIGERS = 4
        //
        const val ID_JUNGO_BEARS = 5
        //ID_JUNGO_BEARS -> ID_SINON_BULLS
        const val ID_SINON_BULLS = 6
        //ID_SINON_BULLS -> ID_EDA_RHINOS
        const val ID_EDA_RHINOS = 7
        //
        const val ID_TIME_EAGLES = 8
        //
        const val ID_CT_WHALES = 9
        //ID_MKT_SUNS -> ID_MKT_COBRAS
        const val ID_MKT_COBRAS = 10
        //ID_MKT_COBRAS -> ID_MEDIA_T_REX
        const val ID_MEDIA_T_REX = 11
        //
        const val ID_FIRST_KINGO = 12
        //ID_FIRST_KINGO -> ID_LANEW_BEARS
        const val ID_LANEW_BEARS = 13
        //ID_LANEW_BEARS -> ID_LAMIGO_MONKEYS
        const val ID_LAMIGO_MONKEYS = 14
        //ID_KG_WHALES -> ID_CT_WHALES
        const val ID_KG_WHALES = 15
        //ID_MKT_SUNS -> ID_MKT_COBRAS -> ID_MEDIA_T_REX
        const val ID_MKT_SUNS = 16
        //ID_ELEPHANTS -> ID_CT_ELEPHANTS
        const val ID_CT_ELEPHANTS = 17
        //
        const val ID_FUBON_GUARDIANS = 18

        const val ID_ALL_STAR_RED = 101
        const val ID_ALL_STAR_WHITE = 102
        const val ID_ALL_STAR_SPEED_WHITE = 103
        const val ID_ALL_STAR_POWER_RED = 104
        const val ID_ALL_STAR_WHITE_2016 = 105
        const val ID_ALL_STAR_RED_2016 = 106

        const val ID_SOUTH_KOREA = 203
        const val ID_CHINESE_TAIPEI = 204

        /**
         * get the team id from raw data
         *
         * @param c    context
         * @param name team name
         * @param year year
         * @return team id
         */
        fun getTeamId(c: Context?, name: String?,
                      year: Int = CpblCalendarHelper.getNowTime().get(Calendar.YEAR)): Int {
            if (name == null || c == null) return ID_UNKNOWN
            if (name.equals(c.getString(R.string.team_elephants_short), ignoreCase = true))
                return if (year > 2013) ID_CT_ELEPHANTS else ID_ELEPHANTS//[69]dolphin++
            if (name.equals(c.getString(R.string.team_ct_elephants_short), ignoreCase = true) ||
                    name.equals(c.getString(R.string.team_ct_elephants_short2), ignoreCase = true))
                return ID_CT_ELEPHANTS
            if (name.equals(c.getString(R.string.team_lions_short), ignoreCase = true)
                    || name.equals(c.getString(R.string.team_711_lions_short), ignoreCase = true)
                    || name.equals(c.getString(R.string.team_711_lions_short2), ignoreCase = true))
                return if (year < 2007) ID_UNI_LIONS else ID_UNI_711_LIONS
            if (name.equals(c.getString(R.string.team_eda_rhinos_short), ignoreCase = true) ||
                    name.equals(c.getString(R.string.team_eda_rhinos_short2), ignoreCase = true))
                return ID_EDA_RHINOS
            if (name.equals(c.getString(R.string.team_lamigo_monkeys_short), ignoreCase = true)
                    || name.equals(c.getString(R.string.team_lamigo_monkeys_short2), ignoreCase = true)
                    || name.equals(c.getString(R.string.team_lamigo_monkeys_short3), ignoreCase = true))
                return ID_LAMIGO_MONKEYS
            if (name.equals(c.getString(R.string.team_sinon_bulls_short), ignoreCase = true))
                return ID_SINON_BULLS
            if (name.equals(c.getString(R.string.team_lanew_bears_short), ignoreCase = true))
                return if (year >= 2004) ID_LANEW_BEARS else ID_JUNGO_BEARS
            if (name.equals(c.getString(R.string.team_ct_whales_short), ignoreCase = true))
                return if (year >= 2002) ID_CT_WHALES else ID_KG_WHALES
            if (name.equals(c.getString(R.string.team_eagles_short), ignoreCase = true))
                return ID_TIME_EAGLES
            if (name.equals(c.getString(R.string.team_tigers_short), ignoreCase = true))
                return ID_SS_TIGERS
            if (name.equals(c.getString(R.string.team_dragons_short), ignoreCase = true))
                return ID_W_DRAGONS
            if (name.equals(c.getString(R.string.team_first_kinkon_short2), ignoreCase = true))
                return ID_FIRST_KINGO
            if (name.equals(c.getString(R.string.team_makoto_cobras_short2), ignoreCase = true))
                return if (year > 2003) ID_MKT_COBRAS else ID_MKT_SUNS
            return if (name.equals(c.getString(R.string.team_media_t_rex_short2), ignoreCase = true)) ID_MEDIA_T_REX else ID_UNKNOWN
        }

        /**
         * get team logo resource id from team id
         *
         * @param id   team id
         * @param year year
         * @return logo drawable of that year
         */
        @JvmStatic
        fun getTeamLogo(id: Int, year: Int): Int {
            //if (year >= 2017) {
            //    return R.drawable.no_logo;
            //}
            when (id) {
                ID_ELEPHANTS -> return R.drawable.elephant_1990_2013
                ID_CT_ELEPHANTS ->//[69]dolphin++ //[134]dolphin++ new logo for 2015
                    return if (year > 2014) R.drawable.elephant_2015 else R.drawable.elephant_2014
            //[184]++
                ID_UNI_LIONS, ID_UNI_711_LIONS -> return when {
                    year >= 2017 -> R.drawable.lion_2017
                    year >= 2009 -> R.drawable.lion_2009_2013
                    year >= 2007 -> R.drawable.lion_2007_2008
                    year >= 2005 -> R.drawable.lion_2005_2006
                    year >= 1993 -> R.drawable.lion_1993_2004
                    year >= 1991 -> R.drawable.lion_1991_1992
                    else -> R.drawable.lion_1989_1990
                }

                ID_FUBON_GUARDIANS -> return R.drawable.fubon_2017
                ID_EDA_RHINOS -> return R.drawable.rhino_2013
                ID_W_DRAGONS -> return R.drawable.dragons_1990_1999
                ID_SS_TIGERS -> return R.drawable.tiger_1990_1999
                ID_JUNGO_BEARS -> return R.drawable.jungo_1993_1995
                ID_SINON_BULLS -> return R.drawable.sinon_1996_2012
                ID_TIME_EAGLES -> return R.drawable.eagle_1993_1997
                ID_CT_WHALES -> return if (year > 2004) R.drawable.whale_2002_2008 else R.drawable.whale_2001_2004
                ID_KG_WHALES -> return if (year >= 2001) R.drawable.whale_2001_2004 else R.drawable.whale_1997_2001
                ID_MKT_SUNS -> return R.drawable.sun_2003
                ID_MKT_COBRAS -> return R.drawable.cobras_2004_2007
                ID_MEDIA_T_REX -> return R.drawable.dmedia_2008
                ID_FIRST_KINGO -> return R.drawable.lanew_2003
                ID_LANEW_BEARS -> return R.drawable.lanew_2004_2010
                ID_LAMIGO_MONKEYS -> return if (year >= 2017) R.drawable.lamigo_2017 else R.drawable.lamigo_2011_2013
            //[161]++
                ID_ALL_STAR_RED -> return R.drawable.allstar_red
                ID_ALL_STAR_WHITE -> return R.drawable.allstar_white
                ID_ALL_STAR_POWER_RED, ID_ALL_STAR_RED_2016 -> return R.drawable.allstar_power_red
                ID_ALL_STAR_SPEED_WHITE, ID_ALL_STAR_WHITE_2016 -> return R.drawable.allstar_speed_white
            //[181]++
                ID_CHINESE_TAIPEI -> return R.drawable.z04_logo_01
            }
            return R.drawable.no_logo
        }

        /**
         * get full team name
         *
         * @param context context
         * @param id      team id
         * @return team name
         */
        @JvmStatic
        fun getTeamName(context: Context?, id: Int): String? {
            var stringId = -1//R.string.empty_data;
            when (id) {
                ID_ELEPHANTS -> stringId = R.string.team_elephants
                ID_UNI_LIONS -> stringId = R.string.team_lions
                ID_UNI_711_LIONS -> stringId = R.string.team_711_lions //[184]++
                ID_EDA_RHINOS -> stringId = R.string.team_eda_rhinos
                ID_W_DRAGONS -> stringId = R.string.team_dragons
                ID_SS_TIGERS -> stringId = R.string.team_tigers
                ID_JUNGO_BEARS -> stringId = R.string.team_jungo_bears
                ID_SINON_BULLS -> stringId = R.string.team_sinon_bulls
                ID_TIME_EAGLES -> stringId = R.string.team_eagles
                ID_CT_WHALES -> stringId = R.string.team_ct_whales
                ID_KG_WHALES -> stringId = R.string.team_kg_whales
                ID_MKT_SUNS -> stringId = R.string.team_makoto_sun
                ID_MKT_COBRAS -> stringId = R.string.team_makoto_cobras
                ID_MEDIA_T_REX -> stringId = R.string.team_media_t_rex
                ID_FIRST_KINGO -> stringId = R.string.team_first_kinkon
                ID_LANEW_BEARS -> stringId = R.string.team_lanew_bears
                ID_LAMIGO_MONKEYS -> stringId = R.string.team_lamigo_monkeys
                ID_CT_ELEPHANTS -> stringId = R.string.team_ct_elephants //[69]dolphin++

            //[161]++
                ID_ALL_STAR_RED -> stringId = R.string.team_all_star_red
                ID_ALL_STAR_WHITE -> stringId = R.string.team_all_star_white
                ID_ALL_STAR_POWER_RED -> stringId = R.string.team_all_star_power_red
                ID_ALL_STAR_SPEED_WHITE -> stringId = R.string.team_all_star_speed_white
                ID_ALL_STAR_RED_2016 -> stringId = R.string.team_all_star_red_2016
                ID_ALL_STAR_WHITE_2016 -> stringId = R.string.team_all_star_white_2016

            //[181]++
                ID_SOUTH_KOREA -> stringId = R.string.team_south_korea
                ID_CHINESE_TAIPEI -> stringId = R.string.team_chinese_taipei

                ID_FUBON_GUARDIANS -> stringId = R.string.team_fubon_guardians
                ID_UNKNOWN_AWAY -> stringId = R.string.team_unknown_away
                ID_UNKNOWN_HOME -> stringId = R.string.team_unknown_home
            }
            return if (stringId > 0 && context != null) context.getString(stringId) else null
        }

        /**
         * get short team name
         *
         * @param context context
         * @param id      team id
         * @return team name
         */
        fun getTeamNameShort(context: Context?, id: Int): String? {
            var stringId = R.string.empty_data
            when (id) {
                ID_ELEPHANTS -> stringId = R.string.team_elephants_short
                ID_UNI_LIONS, ID_UNI_711_LIONS -> stringId = R.string.team_711_lions_short //[184]++
                ID_EDA_RHINOS -> stringId = R.string.team_eda_rhinos_short
                ID_W_DRAGONS -> stringId = R.string.team_dragons_short
                ID_SS_TIGERS -> stringId = R.string.team_tigers_short
                ID_JUNGO_BEARS -> stringId = R.string.team_jungo_bears_short
                ID_SINON_BULLS -> stringId = R.string.team_sinon_bulls_short
                ID_TIME_EAGLES -> stringId = R.string.team_eagles_short
                ID_CT_WHALES -> stringId = R.string.team_ct_whales_short
                ID_KG_WHALES -> stringId = R.string.team_kg_whales_short
                ID_MKT_SUNS -> stringId = R.string.team_makoto_sun_short
                ID_MKT_COBRAS -> stringId = R.string.team_makoto_cobras_short
                ID_MEDIA_T_REX -> stringId = R.string.team_media_t_rex_short
                ID_FIRST_KINGO -> stringId = R.string.team_first_kinkon_short
                ID_LANEW_BEARS -> stringId = R.string.team_lanew_bears_short
                ID_LAMIGO_MONKEYS -> stringId = R.string.team_lamigo_monkeys_short
                ID_CT_ELEPHANTS -> stringId = R.string.team_ct_elephants_short //[69]dolphin++
                ID_ALL_STAR_RED,//[161]++
                ID_ALL_STAR_POWER_RED,//[161]++
                ID_ALL_STAR_RED_2016,
                    // string_id = R.string.team_all_star_red_short;
                ID_ALL_STAR_WHITE,//[161]++
                ID_ALL_STAR_SPEED_WHITE,//[161]++
                ID_ALL_STAR_WHITE_2016,
                    //string_id = R.string.team_all_star_white_short;
                ID_UNKNOWN_AWAY, ID_UNKNOWN_HOME -> return getTeamName(context, id)
            //[181]++
                ID_SOUTH_KOREA -> stringId = R.string.team_south_korea
                ID_CHINESE_TAIPEI -> stringId = R.string.team_chinese_taipei
                ID_FUBON_GUARDIANS -> stringId = R.string.team_fubon_guardians_short
            }
            return context?.getString(stringId)
        }

        fun getShortName(context: Context?, id: Int): String? {
            val stringId: Int// = R.string.empty_data;
            when (id) {
                ID_ELEPHANTS -> stringId = R.string.team_elephants_short2
                ID_UNI_LIONS -> stringId = R.string.team_711_lions_short2
                ID_UNI_711_LIONS -> stringId = R.string.team_711_lions_short2
                ID_EDA_RHINOS -> stringId = R.string.team_eda_rhinos_short2
                ID_W_DRAGONS -> stringId = R.string.team_dragons_short2
                ID_SS_TIGERS -> stringId = R.string.team_tigers_short2
                ID_JUNGO_BEARS -> stringId = R.string.team_jungo_bears_short2
                ID_SINON_BULLS -> stringId = R.string.team_sinon_bulls_short2
                ID_TIME_EAGLES -> stringId = R.string.team_eagles_short2
                ID_CT_WHALES -> stringId = R.string.team_ct_whales_short2
                ID_KG_WHALES -> stringId = R.string.team_kg_whales_short2
                ID_MKT_SUNS, ID_MKT_COBRAS -> stringId = R.string.team_makoto_sun_short2
                ID_MEDIA_T_REX -> stringId = R.string.team_media_t_rex_short3
                ID_FIRST_KINGO -> stringId = R.string.team_first_kinkon_short3
                ID_LANEW_BEARS -> stringId = R.string.team_lanew_bears_short2
                ID_LAMIGO_MONKEYS -> stringId = R.string.team_lamigo_monkeys_short3
                ID_CT_ELEPHANTS -> stringId = R.string.team_ct_elephants_short2
                ID_FUBON_GUARDIANS -> stringId = R.string.team_fubon_guardians_short
                else -> return getTeamNameShort(context, id)
            }
            return context?.getString(stringId)
        }

        @JvmStatic
        fun getTeam2014(context: Context, png: String, year: Int, isHomeTeam: Boolean): Team {
            var id = ID_UNKNOWN
            when {
                png.contains("E02") -> id = ID_CT_ELEPHANTS
                png.contains("L01") -> id = if (year < 2007) ID_UNI_LIONS else ID_UNI_711_LIONS
                png.contains("A02") -> id = when {
                    year <= 2003 -> ID_FIRST_KINGO
                    year <= 2010 -> ID_LANEW_BEARS
                    else -> ID_LAMIGO_MONKEYS
                }
                png.contains("B04") -> id = ID_FUBON_GUARDIANS
                png.contains("B03") -> id = ID_EDA_RHINOS
                png.contains("E01") -> id = ID_ELEPHANTS
                png.contains("B02") -> id = ID_SINON_BULLS
                png.contains("W01") -> id = if (year <= 2001) ID_KG_WHALES else ID_CT_WHALES
                png.contains("G02") -> id = ID_MEDIA_T_REX
                png.contains("G01") -> id = if (year <= 2003) ID_MKT_SUNS else ID_MKT_COBRAS
                png.contains("A01") -> id = ID_FIRST_KINGO
                png.contains("T01") -> id = ID_SS_TIGERS
                png.contains("D01") -> id = ID_W_DRAGONS
                png.contains("C01") -> id = ID_TIME_EAGLES
                png.contains("B01") -> id = ID_JUNGO_BEARS
                png.contains("S01") -> id = ID_ALL_STAR_RED
                png.contains("S02") -> id = ID_ALL_STAR_WHITE
                png.contains("S04") -> id = ID_ALL_STAR_SPEED_WHITE
                png.contains("S05") -> id = ID_ALL_STAR_POWER_RED
                png.contains("S03") -> id = ID_ALL_STAR_RED_2016
                png.contains("S06") -> id = ID_ALL_STAR_WHITE_2016

                png.contains("z03") -> id = ID_SOUTH_KOREA //[181]++
                png.contains("z04") -> id = ID_CHINESE_TAIPEI //[181]++
            }
            return Team(context, id)
        }

        fun toJson(context: Context, team: Team): String {
            val type = object : TypeToken<Team>() {}.type
            val gson = GsonBuilder().registerTypeAdapter(type, TeamTypeAdapter(context)).create()
            //Log.d(TAG, gson.toJson(team));
            return gson.toJson(team)
        }

        fun fromJson(context: Context, json: String): Team? {
            val type = object : TypeToken<Team>() {}.type
            val gson = GsonBuilder().registerTypeAdapter(type, TeamTypeAdapter(context)).create()
            return gson.fromJson<Team>(json, type)
        }
    }
}
