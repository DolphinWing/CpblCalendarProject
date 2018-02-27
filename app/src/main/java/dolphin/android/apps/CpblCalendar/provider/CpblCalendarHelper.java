package dolphin.android.apps.CpblCalendar.provider;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.util.SparseArray;
import android.widget.ArrayAdapter;

import java.io.File;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dolphin.android.apps.CpblCalendar.Utils;
import dolphin.android.apps.CpblCalendar3.R;
import dolphin.android.net.GoogleDriveHelper;
import dolphin.android.net.HttpHelper;
import dolphin.android.util.FileUtils;

/**
 * Created by dolphin on 2013/6/1.
 * <p/>
 * Date provider.
 * Implements the download HTML source and transform to our data structure.
 */
public class CpblCalendarHelper extends HttpHelper {

    private final static String TAG = "CpblCalendarHelper";

    public final static String URL_BASE = "http://www.cpbl.com.tw";

    private final static String URL_SCHEDULE_2016 = URL_BASE +
            "/schedule/index/@year-@month-01.html?&date=@year-@month-01&gameno=01&sfieldsub=@field&sgameno=@kind";

    public final static String URL_FIELD_2017 = URL_BASE + "/footer/stadium/@field";

    private final Context mContext;

    protected Context getContext() {
        return mContext;
    }

    private boolean mUseCache = false;

    public CpblCalendarHelper(Context context) {
        mContext = context;
        mUseCache = mContext.getResources().getBoolean(R.bool.feature_cache);
    }

    /**
     * get suggested game kind by date
     *
     * @param context Context
     * @return suggested game kind index
     */
    public static int getSuggestedGameKind(Context context) {
        String kind = "01";
        Calendar now = getNowTime();
        int year = now.get(Calendar.YEAR);
        int month = now.get(Calendar.MONTH) + 1;
        int day = now.get(Calendar.DAY_OF_MONTH);
        if (year >= 2014) {
            return get_kind_seq(context, kind);//[70]++
        }
        //if (month > 3 && month < 10)
        //    kind = "01";
        //else
        if (month == 3 && day < 15 || month == 2) {
            kind = "07";//warm up games
        } else if (month == 10 && day >= 15) {
            kind = "03";//championship series
        } else if (month == 11) {
            kind = (day > 20) ? "16" : "09";
        } else if (month == 12 || month < 2) {
            kind = "16";//winter games
        }
        return get_kind_seq(context, kind);
    }

    private static int get_kind_seq(Context context, String kind) {
        final String[] kinds =
                context.getResources().getStringArray(R.array.cpbl_game_kind_id_2014);
        int k = 0;
        for (String ks : kinds) {
            if (ks.equalsIgnoreCase(kind)) {
                return k;
            }
            k++;
        }
        return 0;
    }

    /**
     * start CPBL website in browser
     *
     * @param context Context
     * @param year    year
     * @param month   month
     * @param kind    game kind
     * @param field   game field
     */
    public static void startActivityToCpblSchedule(Context context, int year, int month, String kind,
                                                   String field) {
//        startActivityToCpblSchedule(context, year, month, kind, field, false);
//    }
//
//    /**
//     * start CPBL website in browser
//     *
//     * @param context Context
//     * @param year    year
//     * @param month   month
//     * @param kind    game kind
//     * @param field   game field
//     * @param newTask true if in a new task
//     */
//    public static void startActivityToCpblSchedule(Context context, int year, int month, String kind,
//                                                   String field, boolean newTask) {
////        Intent intent = new Intent(Intent.ACTION_VIEW);
        String url = URL_SCHEDULE_2016.replace("@year", String.valueOf(year))
                .replace("@month", String.valueOf(month))
                .replace("@kind", kind).replace("@field", field.equals("F00") ? "" : field);
//        intent.setData(Uri.parse(url));//URL_SCHEDULE_2014
//        if (newTask) {//[170]++
//            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {//[167]++
//            //[164]dolphin++ add Chrome Custom Tabs
//            Bundle extras = new Bundle();
//            extras.putBinder(Utils.EXTRA_CUSTOM_TABS_SESSION, null);
//            extras.putInt(Utils.EXTRA_CUSTOM_TABS_TOOLBAR_COLOR,
//                    SupportV4Utils.getColor(context, R.color.holo_green_dark));
//            intent.putExtras(extras);
//        }
//        context.startActivity(intent);
        Utils.startBrowserActivity(context, url);
    }

    //<font color=blue>001 義大-統一(新莊)</font>
    private final static String PATTERN_GAME_2014_ZXC = "<font color=blue>([^/]+)";
    //"<font color=blue>([^ ]+) ([^\\-]+)-([^\\(]+)([^<]+)</font>";

    public ArrayList<Game> query2014zxc(int month) {
        long startTime = System.currentTimeMillis();
        //Log.v(TAG, "query2014 zxc22");
        ArrayList<Game> gameList = new ArrayList<>();
        try {
            String url = "http://zxc22.idv.tw/sche/main.asp?mmm=@month&place=&team=";
            url = url.replace("@month", String.valueOf(month));
            String html = getUrlContent(url, ENCODE_BIG5);
            //Log.d(TAG, "getUrlContent " + html.length());
            if (html.contains("<tr bgcolor=\"orange\">")) {
                //<font face=arial color=black>6<BR>
                String[] days = html.substring(html.indexOf("<tr bgcolor=\"orange\">"))
                        .split("<font face=arial");
                //Log.d(TAG, "days " + days.length);
                for (int i = 1; i < days.length; i++) {
                    //check those days with games
                    if (days[i].contains("<font color=blue>")) {
                        String[] data = days[i].split("<BR>");
                        //Log.d(TAG, String.valueOf(i) + " " + days[i]);
                        Matcher mGame = Pattern.compile(PATTERN_GAME_2014_ZXC).matcher(days[i]);
                        int g = 0;//to calculate how many games today
                        while (/*mGame != null && */mGame.find()) {
                            //Log.d(TAG, mGame.group(1));
                            try {
                                if (mGame.group(1).contains("-")) {
                                    gameList.add(parseOneGameHtml2014zxc(month, i,
                                            mGame.group(1)));
                                    g++;
                                } else {
                                    throw new Exception("Oops! suspended game");
                                }
                            } catch (Exception e) {
                                //Log.d(TAG, mGame.group(1));
                                //Log.e(TAG, "e: " + e.getMessage());
                                //e.printStackTrace();

                                //Log.d(TAG, "==> " + data.length + ", g=" + g);
                                String extra;//having extra message
                                if (g < data.length - 1) {
                                    extra = data[g + 1];
                                } else {
                                    extra = data[g];
                                }
                                //Log.d(TAG, extra);
                                if (extra.startsWith("<font color=red>")) {//delay games
                                    g--;
                                    gameList.remove(gameList.size() - 1);
                                } else {//[93]dolphin++ fix delay message after game final
                                    if (extra.contains("<font color=blue>")) {
                                        extra = extra.substring(
                                                0, extra.indexOf("<font color=blue>"));
                                    }
                                    if (extra.contains("<font color=green>")) {
                                        //              01234567890123456789
                                        extra = extra
                                                .substring(extra.indexOf("<font color=green>"));
                                        Game suspend = gameList.get(gameList.size() - 1);
                                        suspend.IsDelay = true;
                                        suspend.StartTime.set(Calendar.HOUR_OF_DAY, 13);
                                        suspend.StartTime.set(Calendar.MINUTE, 5);
                                        suspend.DelayMessage = extra.substring(18,
                                                extra.indexOf("</"));
                                        //Log.d(TAG, suspend.DelayMessage);
                                    }
                                }
                            }
                        }//find games
//                        if (false && g > 1) {//more than one game
//                            for (int j = 1; j < g; j++) {
//                                Game g1 = gameList.get(gameList.size() - j);
//                                g1.Url = g1.IsFinal ? g1.Url : g1.Url.replace("1.html",
//                                        String.format(Locale.US, "%d.html", (g - j + 1)));
//                                //Log.d(TAG, String.format("%d: %s", g1.Id, g1.Url));
//                            }
//                        }
                    }//if having games
                }//for possible days
            } else {
                Log.e(TAG, "no data: " + html);
            }
        } catch (Exception e) {
            Log.e(TAG, "query: " + e.getMessage());
            e.printStackTrace();
        }
        long endTime = System.currentTimeMillis();
        Log.v(TAG, String.format("query2014zxc m=%d wasted %d ms",
                month, ((endTime - startTime))));
        //mGameList = gameList;
        return gameList;
    }

    private Game parseOneGameHtml2014zxc(int month, int day, String str) {
        Context context = getContext();
        Game game = new Game();
        game.Source = Game.SOURCE_ZXC22;
        game.Kind = "01";
        game.StartTime = getGameTime(0, month - 1, day);

        int dayOfWeek = game.StartTime.get(Calendar.DAY_OF_WEEK);
        boolean isWeekend = (dayOfWeek == Calendar.SATURDAY ||
                dayOfWeek == Calendar.SUNDAY);
        //int hour = isWeekend ? 17 : 18;
        //int minute = isWeekend ? 05 : 35;
        game.StartTime.set(Calendar.HOUR_OF_DAY, isWeekend ? 17 : 18);
        game.StartTime.set(Calendar.MINUTE, isWeekend ? 5 : 35);

        //<font color=blue>001 義大-統一(新莊)</font>
        String[] data = str.split("[ \\-()]");
        //Log.d(TAG, "data " + data.length + ", " + data[0]);
        game.Id = Integer.parseInt(data[0]);
        game.AwayTeam = new Team(context, Team.getTeamId(context, data[1], 2014));
        game.HomeTeam = new Team(context, Team.getTeamId(context, data[2], 2014));
        game.Field = (data.length >= 4) ? data[3] : "";

        try {//<font color='#547425'><B>4:8(11288人)</B>
            if (str.contains("<font color='#547425'>")) {
                game.IsFinal = true;
                //http://www.cpbl.com.tw/game/box.aspx?gameno=07&year=2014&game=4
                game.Url = String.format(Locale.US, "%s/games/box.html?gameno=%s&pbyear=%d&game_id=%d&game_type=01",
                        URL_BASE, game.Kind, game.StartTime.get(Calendar.YEAR), game.Id);
                String result = str.substring(str.indexOf("<font color='#547425'>"));
                result = result.substring(result.indexOf("<B>") + 3);
                //Log.d(TAG, result);
                String[] res = result.split("[\\D]");
                game.AwayScore = Integer.parseInt(res[0]);
                game.HomeScore = Integer.parseInt(res[1]);
                //Log.d(TAG, String.format("==> %d people", Integer.parseInt(res[2])));
                game.People = Integer.parseInt(res[2]);
            } else {
//                game.Url = String.format("%s/game/starters.aspx?gameno=%s&year=%d&game=%d",
//                        URL_BASE, game.Kind, game.StartTime.get(Calendar.YEAR), game.Id);
                game.Url = "http://www.cpbltv.com/channel/1.html";//[80]dolphin++
                game.Url = "http://www.cpbltv.com/";//[84]dolphin++
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return game;
    }

    @SuppressWarnings("WeakerAccess")
    static ArrayList<Game> getCache(Context context, String fileName) {
        if (context != null) {
            //String fileName = String.format("%04d-%02d.json", year, month);
            File f = new File(getCacheDir(context), fileName);
            //Log.d(TAG, "getCache " + f.getAbsolutePath());
            if (f.exists()) {
                //convert JSON string to ArrayList<Game> object
                return Game.listFromJson(context, FileUtils.readFileToString(f));
            }
            f = new File(context.getCacheDir(), fileName);
            //Log.d(TAG, "getCache " + f.getAbsolutePath());
            if (f.exists()) {
                //convert JSON string to ArrayList<Game> object
                return Game.listFromJson(context, FileUtils.readFileToString(f));
            }
        }
        return null;
    }

    @SuppressWarnings("WeakerAccess")
    static ArrayList<Game> getCache(Context context, int year, int month) {
        return getCache(context, String.format(Locale.US, "%04d-%02d.json", year, month));
    }

    public ArrayList<Game> getCache(int year, int month) {
        if (canUseCache()) {
            return getCache(getContext(), String.format(Locale.US, "%04d-%02d.json", year, month));
        }
        return null;
    }

    @SuppressWarnings("WeakerAccess")
    static boolean putCache(Context context, String fileName, ArrayList<Game> list) {
        if (context == null) {
            return false;
        }
        //String fileName = String.format("%04d-%02d.json", year, month);
        File f = new File(getCacheDir(context), fileName);
        if (list == null) {
            return f.delete();
        }
        //convert ArrayList<Game> object to JSON string
        //Log.d(TAG, "putCache " + f.getAbsolutePath());
        boolean r = FileUtils.writeStringToFile(f, Game.listToJson(context, list));
        if (!r) {
            f = new File(context.getCacheDir(), fileName);
            //Log.d(TAG, "putCache " + f.getAbsolutePath());
            r = FileUtils.writeStringToFile(f, Game.listToJson(context, list));
        }
        return r;
    }

    @SuppressWarnings("unused")
    public static boolean putCache(Context context, int year, int month, ArrayList<Game> list) {
        return putCache(context, String.format(Locale.US, "%04d-%02d.json", year, month), list);
    }

    public boolean putCache(int year, int month, ArrayList<Game> list) {
        return canUseCache() && putCache(getContext(), String.format(Locale.US, "%04d-%02d.json", year, month), list);
    }

    public boolean canUseCache() {
        return mUseCache;
    }

    @SuppressWarnings("unused")
    public boolean hasCache(int year, int month) {
        return (getCache(year, month) != null);
    }

    @SuppressWarnings("unused")
    public static boolean hasCache(Context context, int year, int month) {
        return (getCache(context, year, month) != null);
    }

    public boolean putLocalCache(int year, int month, int kind, ArrayList<Game> list) {
        return putCache(getContext(), String.format(Locale.US, "%04d-%02d-%d.json", year, month, kind), list);
    }

    @SuppressWarnings("unused")
    public boolean removeLocalCache(int year, int month, int kind) {
        return putLocalCache(year, month, kind, null);
    }

    @SuppressWarnings("unused")
    public ArrayList<Game> getLocalCache(int year, int month, int kind) {
        if (canUseCache()) {
            return getCache(getContext(), String.format(Locale.US, "%04d-%02d-%d.json", year, month, kind));
        }
        return null;
    }

    /**
     * Always get Taipei time (GMT+8)
     *
     * @return Calendar
     */
    public static Calendar getNowTime() {
        //Log.d(TAG, Locale.TAIWAN.toString());
        //return Calendar.getInstance(Locale.TAIWAN);
        return Calendar.getInstance(TimeZone.getTimeZone("GMT+8"));
    }

    /**
     * get game time
     *
     * @param year  year
     * @param month month
     * @param day   day of month
     * @return Calendar
     */
    @SuppressWarnings("SameParameterValue")
    private static Calendar getGameTime(int year, int month, int day) {
        return getGameTime(year, month, day, 0, 0);
    }

    /**
     * get game time
     *
     * @param year   year
     * @param month  month
     * @param day    day of month
     * @param hour   hour of day (24HR)
     * @param minute minute
     * @return Calendar
     */
    @SuppressWarnings({"WeakerAccess", "SameParameterValue"})
    static Calendar getGameTime(int year, int month, int day, int hour, int minute) {
        Calendar now = getNowTime();
        if (year > 0) {
            now.set(Calendar.YEAR, year);
        }
        if (month > 0) {
            now.set(Calendar.MONTH, month - 1);
        }
        now.set(Calendar.DAY_OF_MONTH, day);
        now.set(Calendar.HOUR_OF_DAY, hour);
        now.set(Calendar.MINUTE, minute);
        now.set(Calendar.SECOND, 0);
        now.set(Calendar.MILLISECOND, 0);
        return now;
    }

//    public static boolean isToday(Game game) {
//        if (game != null) {
//            Calendar now = getNowTime();
//            return DateUtils.sameDay(game.StartTime, now);
//        }
//        return false;
//    }
//
//    public static boolean isSameDay(Game game1, Game game2) {
//        if (game1 == null || game2 == null) {
//            return false;
//        }
//        return DateUtils.sameDay(game1.StartTime, game2.StartTime);
//    }

    /**
     * build year array adapter
     *
     * @param context Context
     * @param nowYear current year
     * @return ArrayAdapter
     */
    public static ArrayAdapter<String> buildYearAdapter(Context context, int nowYear) {
        String[] years = new String[nowYear - 1990 + 1];
        for (int i = 1990; i <= nowYear; i++) {
            String y = context.getString(R.string.title_cpbl_year, (i - 1989));
            years[nowYear - i] = String.format(Locale.US, "%d (%s)", i, y);
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context,
                android.R.layout.simple_spinner_item, years);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return adapter;
    }

    /**
     * build month array adapter
     *
     * @param context Context
     * @return ArrayAdapter
     */
    public static ArrayAdapter<String> buildMonthAdapter(Context context) {
        ArrayList<String> months = new ArrayList<>(Arrays.asList(
                new DateFormatSymbols(Locale.TAIWAN).getMonths()));
        if (context.getResources().getBoolean(R.bool.feature_enable_all_months)) {
            months.add(context.getString(R.string.title_game_year_all_months));//[146]++
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context,
                android.R.layout.simple_spinner_item, months);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return adapter;
    }

    private void storeDelayGames2014(Context context, int year, SparseArray<Game> games) {
        //store all data to local cache
        String delay_str = "";
        for (int i = 0; i < games.size(); i++) {
            Game g = games.valueAt(i);
            delay_str += String.format(Locale.US, "%d/%s;", g.Id,
                    new SimpleDateFormat("MM/dd", Locale.TAIWAN).format(g.StartTime.getTime()));
        }
        storeDelayGames2014(context, year, delay_str);
    }

    //@Deprecated
    private void storeDelayGames2014(Context context, int year, String delay_str) {
        if (context == null) {
            return;//cannot store, no context
        }
        File f = new File(getCacheDir(context), String.format(Locale.US, "%d.delay", year));
        //Log.d(TAG, f.getAbsolutePath());
        FileUtils.writeStringToFile(f, delay_str);
    }

    //@Deprecated

    /**
     * remove stored delay games
     *
     * @param context Context
     * @param year    year
     * @return true if deleted
     */
    @SuppressWarnings("UnusedReturnValue")
    public boolean removeDelayGames2014(Context context, int year) {
        if (context != null) {
            File f = new File(getCacheDir(context), String.format(Locale.US, "%d.delay", year));
            return f.delete();
        }
        return false;
    }

    private SparseArray<Game> restoreDelayGames2014(Context context, int year) {
        SparseArray<Game> delayedGames = new SparseArray<>();
        if (context == null || getCacheDir(context) == null) {
            return delayedGames;//[160]++ avoid use NullPointer to File constructor
        }
        //restore data from cache
        File f = new File(getCacheDir(context), String.format(Locale.US, "%d.delay", year));
        //Log.d(TAG, f.getAbsolutePath());
        String delay_str = FileUtils.readFileToString(f);
        if (delay_str != null && !delay_str.isEmpty() && !delay_str.contains("HTTP")) {
            for (String delay : delay_str.split(";")) {
                //Log.d(TAG, delay);
                if (!delay.isEmpty()) {
                    String[] d = delay.split("/");
                    if (d.length <= 2) {
                        continue;//bypass the error record
                    }
                    Game g = new Game();
                    g.Id = Integer.parseInt(d[0]);
                    g.StartTime = getNowTime();
                    g.StartTime.set(Calendar.YEAR, year);
                    g.StartTime.set(Calendar.MONTH, Integer.parseInt(d[1]) - 1);
                    g.StartTime.set(Calendar.DAY_OF_MONTH, Integer.parseInt(d[2]));
                    g.StartTime.set(Calendar.HOUR_OF_DAY, 0);
                    delayedGames.put(g.Id, g);
                }
            }
        }
        return delayedGames;
    }

    /**
     * Get cache dir for app
     *
     * @param context Context
     * @return cache dir
     */
    public static File getCacheDir(Context context) {
        return (ContextCompat.checkSelfPermission(context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
                ? context.getExternalCacheDir() : context.getCacheDir();
    }

    private String getString(int id) {
        return getContext() != null && id > 0 ? getContext().getString(id) : "";
    }

    /**
     * New query game for new 2016 web pages
     *
     * @param year  year
     * @param month month
     * @param kind  game kind
     * @param field field
     * @return list of games
     */
    @SuppressWarnings("SameParameterValue")
    public ArrayList<Game> query2016(int year, int month, String kind, String field) {
        return query2016(year, month, kind, field, true, true);
    }

    public ArrayList<Game> query2016(int year, int month, String kind, String field,
                                     boolean allowDelayCache, boolean allowDelayDrive) {
        long start = System.currentTimeMillis();

        SparseArray<Game> delayedGames = kind.equals("01") //[193]++ only check regular games
                ? queryDelayGames2016(year, allowDelayCache, allowDelayDrive) : null;
//        year = 2016;
//        month = 3;
//        kind = "07";
        ArrayList<Game> gameList = new ArrayList<>();
        String url = URL_SCHEDULE_2016.replace("@year", String.valueOf(year))
                .replace("@month", String.valueOf(month))
                .replace("@kind", kind).replace("@field", field.equals("F00") ? "" : field);
        //Log.d(TAG, "url: " + url);
        String html = getUrlContent(url);
        if (html != null && html.contains("<div class=\"one_block\"")) {
            //Log.d(TAG, "check game list");
            html = html.substring(0, html.indexOf("<div class=\"footer\">"));

            //http://stackoverflow.com/a/7860836/2673859
            TreeMap<String, String> dayMap = new TreeMap<>();
            String[] tdDays = html.split("<td valign=\"top\">");
            //Log.d(TAG, "td days = " + tdDays.length);

            //<th class="past">29</th>
            //<th class="today">01</th>
            //<th class="future">02</th>
            Matcher matchGameDay = Pattern.compile("<th class=\"[^\"]*\">([0-9]+)</th>").matcher(html);
            int days = 0;
            while (matchGameDay.find()) {//find the first
                days++;
                //Log.d(TAG, String.format("%d, day=%s", day, matchGameDay.group(1)));
                if (tdDays[days].contains("one_block")) {
                    //Log.d(TAG, "  we have games today");
                    dayMap.put(matchGameDay.group(1), tdDays[days]);
                }
            }

            String[] one_block = html.split("<div class=\"one_block\"");
            //Log.d(TAG, "one_block = " + one_block.length);
            int games = one_block.length;
            Calendar now = getNowTime();
            for (int i = 1; i < games; i++) {
                int d = parseGame2016TestGameDay(dayMap, one_block[i]);
                Game game = parseOneGameHtml2016(one_block[i], year, month, d, kind);
                if (game != null) {
                    if (game.IsDelay && (!game.IsFinal && !game.IsLive) && game.StartTime.before(now)) {
                        Log.w(TAG, String.format("delay %d @ %s", game.Id, game.getDisplayDate()));
                        //Log.d(TAG, String.format("live? %s", game.IsLive));
                        continue;//don't add to game list
                    }
                    game.Source = Game.SOURCE_CPBL;
                    if (delayedGames != null && delayedGames.get(game.Id) != null) {
                        Game delayed = delayedGames.get(game.Id);
                        String d_date = new SimpleDateFormat("MMM d", Locale.TAIWAN)
                                .format(delayed.StartTime.getTime());
                        game.DelayMessage = game.DelayMessage == null
                                ? String.format("<b><font color='red'>%s&nbsp;%s</font></b>",
                                d_date, getString(R.string.title_delayed_game))
                                : String.format("<b><font color='red'>%s</font></b> %s",
                                d_date, game.DelayMessage);
                    }
                    if (kind.equals("07") && Utils.passed1Day(game.StartTime)) {
                        game.IsFinal = true;
                        game.IsDelay = true;
                        game.DelayMessage = getString(R.string.delayed_game_cancelled);
                    }
                    gameList.add(game);
                }
            }
        }
        long cost = System.currentTimeMillis() - start;
        Log.v(TAG, String.format("query %04d/%02d cost %d ms", year, month, cost));
        return gameList;
    }

    /**
     * Try to parse day of month of the block
     *
     * @param dayMap current existing days
     * @param block  block html
     * @return possible day of month
     */
    private int parseGame2016TestGameDay(TreeMap<String, String> dayMap, String block) {
        if (block.contains("<!-- one_block -->")) {
            block = block.substring(0, block.lastIndexOf("<!-- one_block -->"));
        }
        for (Map.Entry<String, String> pair : dayMap.entrySet()) {
            //System.out.println(pair.getKey() + " = " + pair.getValue());
            //it.remove(); // avoids a ConcurrentModificationException
            if (pair.getValue().contains(block)) {
                //Log.d(TAG, "day: " + pair.getKey());
                return Integer.parseInt(pair.getKey());
            }
        }
        return 0;
    }

    /**
     * get Team by PNG file name
     *
     * @param png  PNG file name
     * @param year year
     * @return Team object
     */
    private Team getTeamByPng(String png, int year) {
        return Team.getTeam2014(getContext(), png, year);
    }

    /**
     * Parse HTML to get game info
     *
     * @param html  source html
     * @param year  year
     * @param month month
     * @param day   day of month
     * @param kind  game kind
     * @return Game object
     */
    private Game parseOneGameHtml2016(String html, int year, int month, int day, String kind) {
        Game game = new Game();
        //onClick="location.href='/games/box.html?&game_type=01&game_id=158&game_date=2015-08-01&pbyear=2015';"
        game.StartTime = getNowTime();
        game.StartTime.set(Calendar.YEAR, year);
        game.StartTime.set(Calendar.MONTH, month - 1);//dolphin++@2016.02.21
        game.StartTime.set(Calendar.DAY_OF_MONTH, day);//dolphin++@2016.02.21
        game.StartTime.set(Calendar.SECOND, 0);
        game.StartTime.set(Calendar.MILLISECOND, 0);
        game.Kind = kind;//dolphin++@2016.02.21

        final String patternGameId = "game_id=([0-9]+)";
        Matcher matchId = Pattern.compile(patternGameId).matcher(html);
        if (matchId.find()) {
            game.Id = Integer.parseInt(matchId.group(1));
        }

        if (html.contains("schedule_info")) {
            String[] info = html.split("schedule_info");
            //schedule_info[1] contains game id and if this is delayed game or not
            if (info.length > 1) {
                String[] extra = info[1].split("<th");
                if (game.Id <= 0 && extra.length > 2) {
                    String id = extra[2];
                    id = id.substring(id.indexOf(">") + 1, id.indexOf("</th"));
                    game.Id = Integer.parseInt(id);
                    //Log.w(TAG, "not coming, no result " + game.Id);
                }
                if (info[1].contains("class=\"sp\"")) {
                    if (extra.length > 1) {//check delay game
                        String extraTitle = extra[1];
                        extraTitle = extraTitle.substring(extraTitle.indexOf(">") + 1,
                                extraTitle.indexOf("</th"));
                        extraTitle = extraTitle.replace("\r", "").replace("\n", "").trim();
                        //game.IsDelay = !extraTitle.isEmpty();
                        //Log.d(TAG, String.format("I am a delayed game %d", game.Id));
                        game.DelayMessage = String.format("<font color='red'>%s</font>", extraTitle);
                    }
                    game.IsDelay = true;
                }
                if (extra.length > 3) {//more info to find
                    String data = extra[3];
                    data = data.substring(data.indexOf(">") + 1, data.indexOf("</th"));
                    if (!data.isEmpty()) {
                        String msg = String.format("&nbsp;%s", data).trim();
                        if (game.DelayMessage != null) {
                            game.DelayMessage += msg;
                        } else {
                            game.DelayMessage = msg;
                        }
                    }
                }
            }

            //schedule_info[2] contains results
            if (info.length > 2 && info[2].contains("schedule_score")) {//we have results
                game.IsFinal = true;
                //<span class="schedule_score">7</span>
                final String patternScore = "schedule_score[^>]*>([\\d]+)";
                Matcher matchScore = Pattern.compile(patternScore).matcher(html);
                if (matchScore.find()) {//find the first
                    game.AwayScore = Integer.parseInt(matchScore.group(1));
                }
                if (matchScore.find()) {//find the second
                    game.HomeScore = Integer.parseInt(matchScore.group(1));
                }
                //Log.d(TAG, String.format("  score = %d:%d", game.AwayScore, game.HomeScore));
            }

            //schedule_info[3] contains delay messages
            //dolphin++@2016.02.21 or some start info
            if (info.length > 3 && info[3].contains("<td")) {
                String message = info[3].trim();
//                message = message.substring(message.indexOf("<tr") + 4, message.indexOf("</tr>"));
//                message = message.replace("\r", "").replace("\n", "").trim();
                //jimmy--@2016-03-26, maybe some game don't have live channel
                //if (message.contains("schedule_icon_tv.png")) {
                //Log.d(TAG, "try to check time: " + game.Id);
                String[] msg = message.split("</td>");
                //Log.d(TAG, "  msg.length: " + msg.length);
                if (msg.length > 1) {
                    String time = msg[1];
                    try {
                        time = time.substring(time.indexOf(">") + 1);
                        //Log.d(TAG, "time: " + time);
                        if (time.contains(":") && (time.indexOf(":") == 2 || time.indexOf(":") == 1)) {
                            String[] t = time.split(":");
                            game.StartTime.set(Calendar.HOUR_OF_DAY, Integer.parseInt(t[0]));
                            game.StartTime.set(Calendar.MINUTE, Integer.parseInt(t[1]));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                if (msg.length > 2) {
                    String channel = msg[2];
                    if (channel.contains("title=")) {//             01234567
                        channel = channel.substring(channel.indexOf("title=") + 7);
                        channel = channel.substring(0, channel.indexOf("\""));
                        //Log.d(TAG, "channel: " + channel);
                        game.Channel = channel;
                    }
                }
                //}

                if (game.IsDelay && message.contains("schedule_sp_txt")) {
                    message = message.substring(message.indexOf("schedule_sp_txt"));
                    message = message.substring(message.indexOf(">") + 1, message.indexOf("<"));
                } else {
                    message = null;
                }
                if (message != null && !message.isEmpty()) {
                    game.DelayMessage = message.replaceAll("<[^>]*>", "").replaceAll("[ ]+", " ");
                    //Log.d(TAG, "message: " + game.DelayMessage);
                }
            }
        }

        final String patternGameDate = "game_date=([\\d]+)-([\\d]+)-([\\d]+)";
        Matcher matchDate = Pattern.compile(patternGameDate).matcher(html);
        if (matchDate.find()) {
            year = Integer.parseInt(matchDate.group(1));
            int m = Integer.parseInt(matchDate.group(2));
            int d = Integer.parseInt(matchDate.group(3));
            //Log.d(TAG, String.format("%04d/%02d/%02d", year, m, d));
            game.StartTime.set(Calendar.YEAR, year);
            game.StartTime.set(Calendar.MONTH, m - 1);
            game.StartTime.set(Calendar.DAY_OF_MONTH, d);
            //} else {
            //    Log.w(TAG, "need to calculate new game time");
        }

        try {
            if (html.contains("class=\"schedule_team")) {
                String matchUpPlace = html.substring(html.indexOf("class=\"schedule_team"));
                matchUpPlace = matchUpPlace.substring(0, matchUpPlace.indexOf("</table"));
                String[] tds = matchUpPlace.split("<td");
                //Log.d(TAG, "tds = " + tds.length);
                if (tds.length > 3) {
                    String awayTeam = tds[1];//                     0123456789012345
                    //Log.d(TAG, "  away = " + awayTeam);
                    awayTeam = awayTeam.substring(awayTeam.indexOf("images/team/"));
                    awayTeam = awayTeam.substring(12, awayTeam.indexOf(".png"));
                    //Log.d(TAG, "  away = " + awayTeam);
                    game.AwayTeam = getTeamByPng(awayTeam, year);
                    String place = tds[2];
                    //Log.d(TAG, "  place = " + place);
                    place = place.substring(place.indexOf(">") + 1, place.indexOf("</td>"));
                    //Log.d(TAG, "  place = " + place);
                    game.Field = place.trim();
                    String homeTeam = tds[3];//                     0123456789012345
                    //Log.d(TAG, "  home = " + homeTeam);
                    homeTeam = homeTeam.substring(homeTeam.indexOf("images/team/"));
                    homeTeam = homeTeam.substring(12, homeTeam.indexOf(".png"));
                    //Log.d(TAG, "  home = " + homeTeam);
                    game.HomeTeam = getTeamByPng(homeTeam, year);
                } else {
                    Log.w(TAG, "no match up");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "parse game teams: " + e.getMessage());
            return null;
        }

        //<img src="http://cpbl-elta.cdn.hinet.net/phone/images/team/B03_logo_01.png"
        //<img src="http://cpbl-elta.cdn.hinet.net/phone/images/team/E02_logo_01.png"
        //<img src="http://cpbl-elta.cdn.hinet.net/phone/images/team/L01_logo_01.png"
        //<img src="http://cpbl-elta.cdn.hinet.net/phone/images/team/A02_logo_01.png"
        //Matcher matchTeams = Pattern.compile("").matcher(html);

        //http://www.cpbl.com.tw/games/box.html?&game_type=01&game_id=198&game_date=2015-10-01&pbyear=2015
        game.Url = String.format(Locale.US, "%s/games/box.html?game_date=%04d-%02d-%02d&game_id=%d&pbyear=%04d&game_type=%s",
                URL_BASE, year, month, day, game.Id, year, kind);

        if (html.contains("game_playing")) {
            int splitIndex = html.indexOf("game_playing");
            game.IsFinal = false;
            String msg = html.substring(splitIndex);
            msg = msg.substring(msg.indexOf(">") + 1);
            msg = msg.substring(0, msg.indexOf("<"));
            msg = String.format("<b><font color='red'>LIVE</font></b>&nbsp;&nbsp;%s", msg);
            game.LiveMessage = msg;
            game.IsLive = true;
            //play_by_play.html?&game_type=01&game_id=10&game_date=2016-03-26&pbyear=2016
            String url = null;
            try {
                url = html.substring(0, splitIndex - 9);
                url = url.substring(url.lastIndexOf("href=") + 6);
            } catch (Exception e) {
                e.printStackTrace();
            }
            //Log.d(TAG, "url = " + url);
            game.Url = url != null ? URL_BASE.concat(url) : game.Url;
            //Log.d(TAG, "live url = " + game.Url);
        } else if (html.contains("schedule_icon_starter.png")) {
            game.IsFinal = false;
            //                       9876543210
            //<a href="/games/starters.html?&game_type=01&game_id=9&game_date=2016-03-27&pbyear=2016">
            //<img src="http://cpbl-elta.cdn.hinet.net/web/images/schedule_icon_starter.png" width="20" height="18" /></a>
            String url = null;
            try {
                url = html.substring(0, html.indexOf("schedule_icon_starter.png"));
                url = url.substring(url.lastIndexOf("href=") + 6);
                url = url.substring(0, url.indexOf(">") - 1);
                //Log.d(TAG, "url = " + url);
            } catch (Exception e) {
                e.printStackTrace();
            }
            game.Url = url != null ? URL_BASE.concat(url) : game.Url;
            //Log.d(TAG, "next url = " + game.Url);
        } else if (html.contains("onClick")) {//no playing
            //Log.d(TAG, "complete " + game.Id);
            game.IsFinal = true;
//        } else {
//            //Log.d(TAG, "delayed " + game.Id);
//            //Log.d(TAG, "  " + game.StartTime.getTime().toString());
//            game.IsDelay = true;
        }

        return game;
    }

    /**
     * Get delayed game list from new 2016 web html
     *
     * @param year        year
     * @param allowCache  allow get data from cache folder
     * @param allowBackup allow get data from Google Drive backup
     * @return delayed game list
     */
    private SparseArray<Game> queryDelayGames2016(int year, boolean allowCache, boolean allowBackup) {
        long start = System.currentTimeMillis();
        //Log.d(TAG, "query delay games 2016: " + year);

        Context context = getContext();
        if (/*year < 2005 || */context == null) {
            Log.w(TAG, String.format("no %d delay game info in www.cpbl.com.tw", year));
            return null;
        }

        //read current month
        Calendar now = getNowTime();
        boolean isThisYear = now.get(Calendar.YEAR) == year;
        SparseArray<Game> delayedGames;

        if (allowCache) {
            delayedGames = restoreDelayGames2016(year);//new SparseArray<>();
            if (!isThisYear && delayedGames.size() > 0) {//use cache directly
                Log.v(TAG, String.format("use cached data, delay games = %d", delayedGames.size()));
                return delayedGames;
            }
        }

        if (allowBackup) {
            //read from Google Drive
            String[] driveIds = context.getResources().getStringArray(R.array.year_delay_game_2014);
            int index = year - 1990;
            if (index >= 0 && index < driveIds.length) {//already have cached data in Google Drive
                String driveId = driveIds[index];
                File f = new File(getCacheDir(context), String.format(Locale.US, "%d.delay", year));
                GoogleDriveHelper.download(context, driveId, f);
                delayedGames = restoreDelayGames2016(year);//read again
                if (delayedGames.size() > 0) {//use cache directly
                    Log.v(TAG, String.format("use Google Drive cached data (%d)", delayedGames.size()));
                    return delayedGames;
                }
            }
        }

        delayedGames = new SparseArray<>();
        int m2 = isThisYear ? now.get(Calendar.MONTH) + 1 : 11;
        for (int month = 2; month <= m2; month++) {
            String url = URL_SCHEDULE_2016.replace("@year", String.valueOf(year))
                    .replace("@month", String.valueOf(month))
                    .replace("@kind", "01").replace("@field", "");
            //Log.d(TAG, "url: " + url);
            String html = getUrlContent(url);
            //Log.d(TAG, "html: " + html.length());

            if (html != null && html.contains("<div class=\"one_block\"")) {
                //Log.d(TAG, "check game list");
                html = html.substring(0, html.indexOf("<div class=\"footer\">"));

                //http://stackoverflow.com/a/7860836/2673859
                TreeMap<String, String> dayMap = new TreeMap<>();
                String[] tdDays = html.split("<td valign=\"top\">");
                //Log.d(TAG, "td days = " + tdDays.length);

                //<th class="past">29</th>
                //<th class="today">01</th>
                //<th class="future">02</th>
                Matcher matchGameDay = Pattern.compile("<th class=\"[^\"]*\">([0-9]+)</th>").matcher(html);
                int days = 0;
                while (matchGameDay.find()) {//find the first
                    days++;
                    //Log.d(TAG, String.format("%d, day=%s", day, matchGameDay.group(1)));
                    if (tdDays[days].contains("one_block")) {
                        //Log.d(TAG, "  we have games today");
                        dayMap.put(matchGameDay.group(1), tdDays[days]);
                        //Log.d(TAG, String.format("day=%d, %s", days, matchGameDay.group(1)));
                    }
                }

                String[] one_block = html.split("<div class=\"one_block\"");
                //Log.d(TAG, "one_block = " + one_block.length);
                int games = one_block.length;
                //Calendar now = getNowTime();
                for (int i = 1; i < games; i++) {
                    int d = parseGame2016TestGameDay(dayMap, one_block[i]);
                    Game game = parseOneGameHtml2016(one_block[i], year, month, d, "01");
                    if (game != null && game.IsDelay && !game.IsFinal && game.StartTime.before(now)) {
                        //Log.w(TAG, String.format("bypass %d @ %s", game.Id, game.getDisplayDate()));
                        game.StartTime.set(Calendar.HOUR_OF_DAY, 0);
                        game.StartTime.set(Calendar.MINUTE, 0);
                        //continue;//don't add to game list
                        //Log.d(TAG, "  " + game.DelayMessage);

                        if (delayedGames.get(game.Id) == null) {
                            delayedGames.put(game.Id, game);
                        }
                        delayedGames.get(game.Id).DelayMessage = game.DelayMessage;
                        //continue;
                    }
                    //Log.d(TAG, "game ID: " + game.Id);
                    //if (delayedGames.get(game.Id) != null) {
                    //    //delayGames.get(game.Id).IsFinal = game.IsFinal;
                    //    Log.d(TAG, "completed! " + game.toString());
                    //    //delayGames.remove(game.Id);
                    //}
                }
            }
        }

        //for (int i = 0; i < delayedGames.size(); i++) {
        //    Log.d(TAG, delayedGames.valueAt(i).toString());
        //}
        if (allowCache) {
            storeDelayGames2016(year, delayedGames);
        }

        long cost = System.currentTimeMillis() - start;
        Log.v(TAG, String.format("query delay cost %d ms", cost));
        return delayedGames;
    }

    private void storeDelayGames2016(int year, SparseArray<Game> games) {
        //use previous data format
        storeDelayGames2014(getContext(), year, games);
    }

    private SparseArray<Game> restoreDelayGames2016(int year) {
        //use previous data format
        return restoreDelayGames2014(getContext(), year);
    }
}
