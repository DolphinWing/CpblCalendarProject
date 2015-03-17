package dolphin.android.apps.CpblCalendar.provider;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Calendar;

import dolphin.android.apps.CpblCalendar.R;

/**
 * Created by dolphin on 2013/6/8.
 */
public class Team {
    public final static int ID_UNKNOWN = 0;
    //ID_ELEPHANTS -> ID_CT_ELEPHANTS
    public final static int ID_ELEPHANTS = 1;
    //
    public final static int ID_UNI_LIONS = 2;
    //
    public final static int ID_W_DRAGONS = 3;
    //
    public final static int ID_SS_TIGERS = 4;
    //
    public final static int ID_JUNGO_BEARS = 5;
    //ID_JUNGO_BEARS -> ID_SINON_BULLS
    public final static int ID_SINON_BULLS = 6;
    //ID_SINON_BULLS -> ID_EDA_RHINOS
    public final static int ID_EDA_RHINOS = 7;
    //
    public final static int ID_TIME_EAGLES = 8;
    //
    public final static int ID_CT_WHALES = 9;
    //ID_MKT_SUNS -> ID_MKT_COBRAS
    public final static int ID_MKT_COBRAS = 10;
    //ID_MKT_COBRAS -> ID_MEDIA_T_REX
    public final static int ID_MEDIA_T_REX = 11;
    //
    public final static int ID_FIRST_KINGO = 12;
    //ID_FIRST_KINGO -> ID_LANEW_BEARS
    public final static int ID_LANEW_BEARS = 13;
    //ID_LANEW_BEARS -> ID_LAMIGO_MONKEYS
    public final static int ID_LAMIGO_MONKEYS = 14;
    //ID_KG_WHALES -> ID_CT_WHALES
    public final static int ID_KG_WHALES = 15;
    //ID_MKT_SUNS -> ID_MKT_COBRAS -> ID_MEDIA_T_REX
    public final static int ID_MKT_SUNS = 16;
    //ID_ELEPHANTS -> ID_CT_ELEPHANTS
    public final static int ID_CT_ELEPHANTS = 17;

    /**
     * get the team id from raw data
     *
     * @param c
     * @param name
     * @return
     */
    public static int getTeamId(Context c, String name) {
        return getTeamId(c, name, CpblCalendarHelper.getNowTime().get(Calendar.YEAR));
    }

    /**
     * get the team id from raw data
     *
     * @param c
     * @param name
     * @param year
     * @return
     */
    public static int getTeamId(Context c, String name, int year) {
        if (name.equalsIgnoreCase(c.getString(R.string.team_elephants_short)))
            return (year > 2013) ? ID_CT_ELEPHANTS : ID_ELEPHANTS;//[69]dolphin++
        if (name.equalsIgnoreCase(c.getString(R.string.team_ct_elephants_short))
                || name.equalsIgnoreCase(c.getString(R.string.team_ct_elephants_short2)))
            return ID_CT_ELEPHANTS;
        if (name.equalsIgnoreCase(c.getString(R.string.team_lions_short))
                || name.equalsIgnoreCase(c.getString(R.string.team_711_lions_short))
                || name.equalsIgnoreCase(c.getString(R.string.team_711_lions_short2)))
            return ID_UNI_LIONS;
        if (name.equalsIgnoreCase(c.getString(R.string.team_eda_rhinos_short))
                || name.equalsIgnoreCase(c.getString(R.string.team_eda_rhinos_short2)))
            return ID_EDA_RHINOS;
        if (name.equalsIgnoreCase(c.getString(R.string.team_lamigo_monkeys_short))
                || name.equalsIgnoreCase(c.getString(R.string.team_lamigo_monkeys_short2))
                || name.equalsIgnoreCase(c.getString(R.string.team_lamigo_monkeys_short3)))
            return ID_LAMIGO_MONKEYS;
        if (name.equalsIgnoreCase(c.getString(R.string.team_sinon_bulls_short)))
            return ID_SINON_BULLS;
        if (name.equalsIgnoreCase(c.getString(R.string.team_lanew_bears_short)))
            return (year >= 2004) ? ID_LANEW_BEARS : ID_JUNGO_BEARS;
        if (name.equalsIgnoreCase(c.getString(R.string.team_ct_whales_short)))
            return (year >= 2002) ? ID_CT_WHALES : ID_KG_WHALES;
        if (name.equalsIgnoreCase(c.getString(R.string.team_eagles_short)))
            return ID_TIME_EAGLES;
        if (name.equalsIgnoreCase(c.getString(R.string.team_tigers_short)))
            return ID_SS_TIGERS;
        if (name.equalsIgnoreCase(c.getString(R.string.team_dragons_short)))
            return ID_W_DRAGONS;
        if (name.equalsIgnoreCase(c.getString(R.string.team_first_kinkon_short2)))
            return ID_FIRST_KINGO;
        if (name.equalsIgnoreCase(c.getString(R.string.team_makoto_cobras_short2)))
            return (year > 2003) ? ID_MKT_COBRAS : ID_MKT_SUNS;
        if (name.equalsIgnoreCase(c.getString(R.string.team_media_t_rex_short2)))
            return ID_MEDIA_T_REX;

        return ID_UNKNOWN;
    }

    /**
     * get team logo resource id from team id
     *
     * @param id
     * @param year
     * @return
     */
    public static int getTeamLogo(int id, int year) {
        switch (id) {
            case ID_ELEPHANTS:
                return R.drawable.elephant_1990_2013;
            case ID_CT_ELEPHANTS://[69]dolphin++ //[134]dolphin++ new logo for 2015
                return year > 2014 ? R.drawable.elephant_2015 : R.drawable.elephant_2014;
            case ID_UNI_LIONS:
                if (year >= 2009)
                    return R.drawable.lion_2009_2013;
                else if (year >= 2007)
                    return R.drawable.lion_2007_2008;
                else if (year >= 2005)
                    return R.drawable.lion_2005_2006;
                else if (year >= 1993)
                    return R.drawable.lion_1993_2004;
                else if (year >= 1991)
                    return R.drawable.lion_1991_1992;
                return R.drawable.lion_1989_1990;
            case ID_EDA_RHINOS:
                return R.drawable.rhino_2013;
            case ID_W_DRAGONS:
                return R.drawable.dragons_1990_1999;
            case ID_SS_TIGERS:
                return R.drawable.tiger_1990_1999;
            case ID_JUNGO_BEARS:
                return R.drawable.jungo_1993_1995;
            case ID_SINON_BULLS:
                return R.drawable.sinon_1996_2012;
            case ID_TIME_EAGLES:
                return R.drawable.eagle_1993_1997;
            case ID_CT_WHALES:
                return (year >= 1991) ? R.drawable.whale_2002_2008 : R.drawable.whale_1997_2001;
            case ID_KG_WHALES:
                return R.drawable.whale_1997_2001;
            case ID_MKT_SUNS:
                return R.drawable.sun_2003;
            case ID_MKT_COBRAS:
                return R.drawable.cobras_2004_2007;
            case ID_MEDIA_T_REX:
                return R.drawable.dmedia_2008;
            case ID_FIRST_KINGO:
                return R.drawable.lanew_2003;
            case ID_LANEW_BEARS:
                return R.drawable.lanew_2004_2010;
            case ID_LAMIGO_MONKEYS:
                return R.drawable.lamigo_2011_2013;
        }
        return R.drawable.ic_launcher;
    }

    /**
     * get full team name
     *
     * @param context
     * @param id
     * @return
     */
    public static String getTeamName(Context context, int id) {
        int string_id = R.string.empty_data;
        switch (id) {
            case ID_ELEPHANTS:
                string_id = R.string.team_elephants;
                break;
            case ID_UNI_LIONS:
                string_id = R.string.team_711_lions;
                break;
            case ID_EDA_RHINOS:
                string_id = R.string.team_eda_rhinos;
                break;
            case ID_W_DRAGONS:
                string_id = R.string.team_dragons;
                break;
            case ID_SS_TIGERS:
                string_id = R.string.team_tigers;
                break;
            case ID_JUNGO_BEARS:
                string_id = R.string.team_jungo_bears;
                break;
            case ID_SINON_BULLS:
                string_id = R.string.team_sinon_bulls;
                break;
            case ID_TIME_EAGLES:
                string_id = R.string.team_eagles;
                break;
            case ID_CT_WHALES:
                string_id = R.string.team_ct_whales;
                break;
            case ID_KG_WHALES:
                string_id = R.string.team_kg_whales;
                break;
            case ID_MKT_SUNS:
                string_id = R.string.team_makoto_sun;
                break;
            case ID_MKT_COBRAS:
                string_id = R.string.team_makoto_cobras;
                break;
            case ID_MEDIA_T_REX:
                string_id = R.string.team_media_t_rex;
                break;
            case ID_FIRST_KINGO:
                string_id = R.string.team_first_kinkon;
                break;
            case ID_LANEW_BEARS:
                string_id = R.string.team_lanew_bears;
                break;
            case ID_LAMIGO_MONKEYS:
                string_id = R.string.team_lamigo_monkeys;
                break;
            case ID_CT_ELEPHANTS://[69]dolphin++
                string_id = R.string.team_ct_elephants;
                break;
        }
        return context.getString(string_id);
    }

    /**
     * get short team name
     *
     * @param context
     * @param id
     * @return
     */
    public static String getTeamNameShort(Context context, int id) {
        int string_id = R.string.empty_data;
        switch (id) {
            case ID_ELEPHANTS:
                string_id = R.string.team_elephants_short;
                break;
            case ID_UNI_LIONS:
                string_id = R.string.team_711_lions_short;
                break;
            case ID_EDA_RHINOS:
                string_id = R.string.team_eda_rhinos_short;
                break;
            case ID_W_DRAGONS:
                string_id = R.string.team_dragons_short;
                break;
            case ID_SS_TIGERS:
                string_id = R.string.team_tigers_short;
                break;
            case ID_JUNGO_BEARS:
                string_id = R.string.team_jungo_bears_short;
                break;
            case ID_SINON_BULLS:
                string_id = R.string.team_sinon_bulls_short;
                break;
            case ID_TIME_EAGLES:
                string_id = R.string.team_eagles_short;
                break;
            case ID_CT_WHALES:
                string_id = R.string.team_ct_whales_short;
                break;
            case ID_KG_WHALES:
                string_id = R.string.team_kg_whales_short;
                break;
            case ID_MKT_SUNS:
                string_id = R.string.team_makoto_sun_short;
                break;
            case ID_MKT_COBRAS:
                string_id = R.string.team_makoto_cobras_short;
                break;
            case ID_MEDIA_T_REX:
                string_id = R.string.team_media_t_rex_short;
                break;
            case ID_FIRST_KINGO:
                string_id = R.string.team_first_kinkon_short;
                break;
            case ID_LANEW_BEARS:
                string_id = R.string.team_lanew_bears_short;
                break;
            case ID_LAMIGO_MONKEYS:
                string_id = R.string.team_lamigo_monkeys_short;
                break;
            case ID_CT_ELEPHANTS://[69]dolphin++
                string_id = R.string.team_ct_elephants_short;
                break;
        }
        return context.getString(string_id);
    }

    private int mId;
    private String mName;//[66]++
    private Context mContext;

    public Team(Context context, int id) {
        mContext = context;
        mId = id;
    }

    public Team(Context context, String name, int year) {
        this(context, getTeamId(context, name, year));
        mName = name;//[66]++
    }

    /**
     * get team id
     *
     * @return
     */
    public int getId() {
        return mId;
    }

    /**
     * get team name
     *
     * @return
     */
    public String getName() {
        if (getId() != ID_UNKNOWN)
            return getTeamName(mContext, mId);
        return mName;
    }

    /**
     * get short team name
     *
     * @return
     */
    public String getShortName() {
        if (getId() != ID_UNKNOWN)
            return getTeamNameShort(mContext, mId);
        return mName;
    }

    /**
     * get team logo resource id
     *
     * @param year
     * @return
     */
    public int getLogo(int year) {
        if (getId() != ID_UNKNOWN)
            return getTeamLogo(mId, year);
        return R.drawable.no_logo;
    }

    public static Team getTeam2014(Context context, String png) {
        int id = ID_UNKNOWN;
        if (png.contains("E02")) {
            id = ID_CT_ELEPHANTS;
        } else if (png.contains("L01")) {
            id = ID_UNI_LIONS;
        } else if (png.contains("A02")) {
            id = ID_LAMIGO_MONKEYS;
        } else if (png.contains("B03")) {
            id = ID_EDA_RHINOS;
        }
        return new Team(context, id);
    }

    //http://goo.gl/JeMgZh
    public static class TeamTypeAdapter extends TypeAdapter<Team> {
        private Context mContext;

        public TeamTypeAdapter(Context context) {
            super();
            mContext = context;
        }

        @Override
        public void write(JsonWriter jsonWriter, Team team) throws IOException {
            if (team == null) {
                jsonWriter.nullValue();
                return;
            }
            jsonWriter.beginObject();
            jsonWriter.name("id").value(team.getId());
            if (team.getShortName() != null)
                jsonWriter.name("name").value(team.getName());
            jsonWriter.endObject();
        }

        @Override
        public Team read(JsonReader jsonReader) throws IOException {
            if (jsonReader.peek() == JsonToken.NULL) {
                jsonReader.nextNull();
                return null;
            }

            int teamId = ID_UNKNOWN;
            String teamName = null;

            jsonReader.beginObject();
            while (jsonReader.hasNext()) {
                String name = jsonReader.nextName();
                if (name.equals("id")) {
                    teamId = jsonReader.nextInt();
                } else if (name.equals("name")) {
                    teamName = jsonReader.nextString();
                }
            }
            jsonReader.endObject();
            return (teamId != ID_UNKNOWN) ? new Team(mContext, teamId)
                    : new Team(mContext, teamName, 2014);
        }
    }

    public static String toJson(Context context, Team team) {
        Type type = new TypeToken<Team>() {
        }.getType();
        Gson gson = new GsonBuilder().registerTypeAdapter(type,
                new TeamTypeAdapter(context)).create();
        //Log.d(TAG, gson.toJson(team));
        return gson.toJson(team);
    }

    public static Team fromJson(Context context, String json) {
        Type type = new TypeToken<Team>() {
        }.getType();
        Gson gson = new GsonBuilder().registerTypeAdapter(type,
                new TeamTypeAdapter(context)).create();
        return gson.fromJson(json, type);
    }
}
