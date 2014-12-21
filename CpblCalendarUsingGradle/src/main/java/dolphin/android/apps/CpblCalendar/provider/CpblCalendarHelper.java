/**
 *  Created by dolphin on 2013/6/1.
 *
 *  Date provider.
 *  Implements the download HTML source and transform to our data structure.
 */
package dolphin.android.apps.CpblCalendar.provider;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.util.SparseArray;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dolphin.android.apps.CpblCalendar.R;
import dolphin.android.net.HttpHelper;
import dolphin.android.util.FileUtils;

public class CpblCalendarHelper extends HttpHelper {

    private final static String TAG = "CpblCalendarHelper";

    public final static String URL_BASE = "http://www.cpbl.com.tw";

    public final static String URL_BASE_2013 = "http://cpblweb.ksi.com.tw";

    private final static String SCORE_QUERY_STRING =
            "?gamekind=@kind&myfield=@field&mon=@month&qyear=@year";

    private final static String ALL_SCORE_URL = URL_BASE_2013 + "/standings/AllScoreqry.aspx";

    private final static String RESULT_URL = URL_BASE_2013 + "/GResult/Result.aspx";

    private final static String RESULT_QUERY_STRING = "?gameno=@kind&pbyear=@year&game=@id";

    public final static String URL_SCHEDULE_2014 = URL_BASE + "/schedule.aspx";

    private Context mContext;

    //[26}++
    private ArrayList<Game> mGameList = null;

    private String mScoreBoardHtml = null;

    private boolean mUseCache = false;

    public CpblCalendarHelper(Context context) {
        mContext = context;
        mUseCache = mContext.getResources().getBoolean(R.bool.feature_cache);
    }

    /**
     * do the web query
     *
     * @param kind
     * @param year
     * @param month
     * @return
     */
    public ArrayList<Game> query(String kind, int year, int month) {
        return query(kind, year, month, "F00");
    }

    public ArrayList<Game> query(String kind, int year, int month, String field) {
        ArrayList<Game> gameList = new ArrayList<Game>();

        String url = ALL_SCORE_URL + SCORE_QUERY_STRING;
        url = url.replace("@year", String.valueOf(year));
        url = url.replace("@month", String.valueOf(month));
        url = url.replace("@kind", kind);
        url = url.replace("@field", field);
        //Log.d(TAG, url);

        long startTime = System.currentTimeMillis();
        try {
            String html = getUrlContent(url);
            //Log.d(TAG, String.format("html %d", html.length()));

            //[26]dolphin++ store the LeadBoard
            if (html.contains("FightScorebg2.gif")) {
                mScoreBoardHtml = html.substring(html.indexOf("FightScorebg2"));
                mScoreBoardHtml = mScoreBoardHtml.substring(mScoreBoardHtml.indexOf("<table"),
                        mScoreBoardHtml.indexOf("</table>") + 8);
                //Log.d(TAG, mScoreBoardHtml);
            }

            //http://www.cpbl.com.tw/Standings/AllScoreqry.aspx?gamekind=01&myfield=F00&mon=6&qyear=2013
            //<table id="Cal_Score" ... </table>
            if (html.indexOf("Cal_Score") > 0) {
                String rawData = html.substring(html.indexOf("Cal_Score"));
                rawData = rawData.substring("Cal_Score".length() + 1, rawData.indexOf("</table>"));
                //Log.d(TAG, String.format("rawData %d", rawData.length()));

                //href="javascript:__doPostBack('Cal_Score','4880')" 
                //style="color:#FFFFFF" title="5月12日">12</a>
                //<div align=right>68<br>犀牛 <font color=ffff00>2:3</font> 象<br>花蓮
                //<br>13:05<br>緯來體育台<br><font color=red>補 2013/5/11</font><br> 
                //<a href=http://www.cpbl.com.tw/GResult/Result.aspx?gameno=01&pbyear=2013&game=68 
                //target="new"><img src="../images/ico/final.gif" border=0></a><br>
                //<img src="../images/ico/ScoreqryLine.gif">
                //<br>71<br>獅 <font color=ffff00>11:3</font> 桃猿<br>桃園
                //<br>17:05<br>緯來體育台<br> 
                //<a href=http://www.cpbl.com.tw/GResult/Result.aspx?gameno=01&pbyear=2013&game=71 
                //target="new"><img src="../images/ico/final.gif" border=0></a><br>
                //</div</td></tr><tr><td align="left" valign="top" 
                //style="color:#FEFFAB;border-color:White;width:14%;">
                String[] days = rawData.split("__doPostBack");
                //Log.d(TAG, String.format("total days: %d", days.length));
                for (int d = 1; d < days.length; d++) {//String day : days) {
                    String day = days[d];
                    //Integer.parseInt(day.substring(day.indexOf(">") + 1));
                    //Log.d(TAG, String.format(" which day: %d >> %s", d, day));
                    if (day.contains("div align=right")) {
                        //at least one game
                        String data = day.substring(day.indexOf("</a>") + 4);
                        data = data.substring(0, data.indexOf("</div"));
                        //Log.d(TAG, data);
                        if (data.contains("ScoreqryLine")) {//multiple games
                            String[] games = data.split("ScoreqryLine");
                            //Log.d(TAG, String.format(" %d games today", games.length));
                            for (String g : games) {
                                Game g2 = parseOneGameHtml(kind, year, month, d, g);
                                if (g2 != null) {
                                    gameList.add(g2);
                                }
                            }
                        } else {//only one game today
                            Game g1 = parseOneGameHtml(kind, year, month, d, data);
                            if (g1 != null) {
                                gameList.add(g1);
                            }
                        }
                    } else {//no game today
                        //Log.d(TAG, String.format("no game: #%d", d));
                    }
                }
            } else {//no data to parse
                html = html.length() > 20 ? html.substring(0, 20) : html;
                Log.w(TAG, html);
            }
        } catch (Exception e) {
            Log.e(TAG, "query: " + e.getMessage());
            e.printStackTrace();
        }

        long endTime = System.currentTimeMillis();
        Log.v(TAG, String.format("query y=%d m=%d k=%s f=%s, wasted %d ms",
                year, month, kind, field, ((endTime - startTime))));
        mGameList = gameList;
        return gameList;
    }

    private final static String PATTERN_RESULT =
            //<div?|<br> #  <br>away   <font>  score   </font> home  <br> field <br> time       live
            "<[^>]*>([0-9]+)<br>([^<]+)<[^>]*>([0-9:]+)<[^>]*>([^<]+)<br>([^<]+)<br>([^<]+)<br>([^<]+)";

    private final static String PATTERN_RESULT_WITHOUT_LIVE =
            //<div?|<br> #  <br>away   <font>  score   </font> home  <br> field <br> time
            "<[^>]*>([0-9]+)<br>([^<]+)<[^>]*>([0-9:]+)<[^>]*>([^<]+)<br>([^<]+)<br>([^<]+)<br>";

    private final static String PATTERN_TOPLAY =
            //<div?|<br> #  <br> match      field <br> time  <br> live
            "<[^>]*>([0-9]+)<br>([^<]+)<br>([^<]+)<br>([^<]+)<br>([^<]+)";

    private final static String PATTERN_TOPLAY_WITHOUT_LIVE =
            //<div?|<br> #  <br> match      field <br> time
            "<[^>]*>([0-9]+)<br>([^<]+)<br>([^<]+)<br>([^<]+)<br>";

    /**
     * convert a game info to Game object
     *
     * @param kind
     * @param year
     * @param month
     * @param day
     * @param str
     * @return
     */
    private Game parseOneGameHtml(String kind, int year, int month, int day, String str) {
        Game game = new Game();
        game.Kind = kind;
        game.Source = Game.SOURCE_CPBL_2013;
        //Log.d(TAG, str);

        game.StartTime = getNowTime();
        game.StartTime.set(Calendar.YEAR, year);
        game.StartTime.set(Calendar.MONTH, month - 1);
        game.StartTime.set(Calendar.DAY_OF_MONTH, day);
        game.StartTime.set(Calendar.SECOND, 0);
        game.StartTime.set(Calendar.MILLISECOND, 0);//[51]dolphin++ fix alarm key floating
        //game.StartTime.setTimeZone(TimeZone.getTimeZone("GMT+0800"));//[55]dolphin++

        boolean bLive = kind.equalsIgnoreCase("01") && (year >= 2006 && year < 2014);

        // check if this game is final, that can help use different pattern
        // to get correct game info from website, also need URL for back link
        //<img src="../images/ico/final.gif" border=0>
        game.IsFinal = str.contains("images/ico/final.gif");
        String pattern = "";
        if (game.IsFinal) {
            pattern = bLive ? PATTERN_RESULT : PATTERN_RESULT_WITHOUT_LIVE;
        } else {
            pattern = bLive ? PATTERN_TOPLAY : PATTERN_TOPLAY_WITHOUT_LIVE;
        }

        String schedule = "";
        Matcher matcher = Pattern.compile(pattern).matcher(str);

        if (matcher != null && matcher.find()) {
            //we have data, parse it later
        } else if (bLive) {//[9]try no live when we assume it always has
            //Log.d(TAG, "we assume it has live... IsFinal=" + game.IsFinal);
            matcher = Pattern.compile(game.IsFinal ? PATTERN_RESULT_WITHOUT_LIVE
                    : PATTERN_TOPLAY_WITHOUT_LIVE).matcher(str);
            if (matcher != null && matcher.find()) {
                //we have data, parse it later
            } else {//still no match
                Log.e(TAG, "still no game? str: " + str);
                return null;
            }
            bLive = false;//it indeed has no live channel
        }
        game.Id = Integer.parseInt(matcher.group(1));
        //Log.d(TAG, String.format("game id = %d", game.Id));
        //Log.d(TAG, String.format(" id = %d, %s, %d",
        //        game.Id, game.IsFinal, matcher.groupCount()));

        String home_team, away_team;
        if (game.IsFinal) {
            String[] score = matcher.group(3).trim().split(":");
            away_team = matcher.group(2).trim();
            game.AwayScore = Integer.parseInt(score[0]);
            home_team = matcher.group(4).trim();
            game.HomeScore = Integer.parseInt(score[1]);
            game.Field = matcher.group(5).trim();
            schedule = matcher.group(6);
            game.Channel = (bLive) ? matcher.group(7).trim() : null;
        } else {
            String match = matcher.group(2);
            away_team = match.substring(0, match.indexOf("v") - 1).trim();
            home_team = match.substring(match.indexOf("s") + 2).trim();
            game.AwayScore = game.HomeScore = 0;
            game.Field = matcher.group(3).trim();
            schedule = matcher.group(4).trim();
            game.Channel = (bLive) ? matcher.group(5).trim() : null;
        }
        game.AwayTeam = new Team(mContext, away_team, year);
        game.HomeTeam = new Team(mContext, home_team, year);
        //Log.d(TAG, String.format(" match(%s): %d:%d",
        //        game.IsFinal, game.AwayScore, game.HomeScore));
        String[] time = schedule.split(":");
        game.StartTime.set(Calendar.HOUR_OF_DAY, Integer.parseInt(time[0]));
        game.StartTime.set(Calendar.MINUTE, Integer.parseInt(time[1]));
        //Log.d(TAG, game.StartTime.getTime().toString());

        //http://www.cpbl.com.tw/GResult/Result.aspx?gameno=04&pbyear=2013&game=52
        String url = RESULT_URL + RESULT_QUERY_STRING;
        url = url.replace("@year", String.valueOf(year));
        url = url.replace("@kind", kind);
        url = url.replace("@id", String.valueOf(game.Id));
        game.Url = url;

        // need to check if any other info behind
        //<br><font color=red>補 2013/5/11</font><br>
        //<br><font color=red>補 2013/4/20</font>(保留)<br>
        String msg = "";

        try {//try to get some extra info from the rest of string
            if (bLive && !game.Channel.isEmpty()) {
                msg = str.substring(str.indexOf(game.Channel) + game.Channel.length() + 4);
            } else {
                msg = str.substring(str.indexOf(schedule) + schedule.length() + 4);
            }
            msg = (msg.lastIndexOf("<img") >= 0) ? msg.substring(0, msg.lastIndexOf("<img")) : msg;
            msg = (msg.lastIndexOf("<a") >= 0) ? msg.substring(0, msg.lastIndexOf("<a")) : msg;
            msg = (msg.lastIndexOf("<br>") >= 0) ? msg.substring(0, msg.lastIndexOf("<br>")) : msg;
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "game extra " + e.getMessage());
        }
        //Log.d(TAG, "delay: " + msg);
        game.DelayMessage = msg.replace("<br>", " ").trim();
        game.IsDelay = (game.DelayMessage.length() <= 0);

        return game;
    }

    /**
     * get last query cached list
     *
     * @return
     */
    public ArrayList<Game> getLastQueryList() {
        return mGameList;
    }

    /**
     * get score board html
     *
     * @return
     */
    public String getScoreBoardHtml() {
        String html = (mScoreBoardHtml != null) ? mScoreBoardHtml.replace("href", "_href") : "";
        return html.replace("width=173", "width=100%% bgcolor=grey");
    }

    /**
     * get suggested game kind by date
     *
     * @param context
     * @return
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
                context.getResources().getStringArray(R.array.cpbl_game_kind_id);
        int k = 0;
        for (String ks : kinds) {
            if (ks.equalsIgnoreCase(kind)) {
                return k;
            }
            k++;
        }
        return 1;
    }

    public ArrayList<Game> query2014() {
        return query2014(0, 0, null, null);
    }

    public ArrayList<Game> query2014(int year, int month, String place, String game) {
        long startTime = System.currentTimeMillis();
        //Log.d(TAG, "query2014");
        ArrayList<Game> gameList = new ArrayList<Game>();
        try {
            String html = getUrlContent(URL_SCHEDULE_2014);
            //Log.d(TAG, "query2014 " + html.length());
            if (html.contains("<tr class=\"game\">")) {//have games
                String[] days = html.split("<table class=\"day\">");
                //Log.d(TAG, "days " + days.length);
                for (String day : days) {
                    //check those days with games
                    String data = day.substring(day.indexOf("<tr"));
                    //<tr class="beforegame">
                    //<tr class="gameing">
                    //<tr class="futuregame">
                    String date = data.substring(data.indexOf("<td>") + 4,
                            data.indexOf("</td>"));
                    //Log.d(TAG, "date: " + date);
                    int d = 0;
                    try {
                        d = Integer.parseInt(date);
                    } catch (NumberFormatException e) {
                        d = 0;
                    }
                    if (d > 0) {//day.contains("<td class=\"line\">")) {
                        String[] games = data.split("<td class=\"line\">");
                        //Log.d(TAG, String.format(" day=%d games=%d", d, games.length));
                        for (int g = 0; g < games.length; g++) {
                            //[92]dolphin++
                            if (games[g].contains("<tr class='suspend'>")) {
//      <table><tr class='suspend'><td>
//      <table><tr><th></th><th>51</th><th></th></tr></table>
//      </td></tr><tr><td><table><tr><td colspan='3' class='suspend'>延賽至2014/04/27</td></tr>
                                if (games[g].contains("<td colspan='3' class='suspend'>")) {
                                    //Log.v(TAG, String.format("suspend day=%d", d));
                                    continue;//don't add to list
                                }
                                //use <tr class="game"> to get games
                                String[] gamesHack = games[g].split("<tr class=\"game\">");
                                //Log.d(TAG, String.format("gamesHack %d", gamesHack.length));
                                for (int h = 1; h < gamesHack.length; h++) {
                                    String gameStr = gamesHack[0] + gamesHack[h];
                                    Game g1 = parseOneGameHtml2014(year, month, d, gameStr);
                                    if (g1 != null) {
                                        gameList.add(g1);
                                    }
                                }
                            } else if (games[g].contains("<tr class='normal'>")) {
                                //normal games
                                Game g2 = parseOneGameHtml2014(year, month, d, games[g]);
                                if (g2 != null) {
                                    gameList.add(g2);
                                }
                            }
                        }
//                        if (false && games.length > 2) {//more than one game
//                            for (int j = 1; j < (games.length - 1); j++) {
//                                Game g1 = gameList.get(gameList.size() - j);
//                                g1.Url = g1.IsFinal ? g1.Url : g1.Url.replace("1.html",
//                                        String.format("%d.html", (games.length - j)));
//                                //Log.d(TAG, String.format("%d: %s", g1.Id, g1.Url));
//                            }
//                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "query: " + e.getMessage());
            e.printStackTrace();
        }
        long endTime = System.currentTimeMillis();
        Log.v(TAG, String.format("query2014 %d/%d wasted %d ms",
                year, month, ((endTime - startTime))));
        mGameList = gameList;
        return gameList;
    }

    public static void startActivityToCpblSchedule(Context context) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(URL_SCHEDULE_2014));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    //                <div class="topinfo">
//                    <ul>
//                        <li><img src="assets/images/logo/L01_logo_07.png"></li>
//                        <li>4</li>
//                        <li>4</li>
//                        <li>1</li>
//                        <li>0.500</li>
//                        <li>1</li>
//                    </ul>
//                </div>
    private final static String PATTERN_BOARD_TEAM_2014 =
            "<div class=\"topinfo\">[^<]*<ul>[^<]*<li><a[^<]*<img src=\"([^\"]+)\"></a></li>[^<]*" +
                    "<li>([^<]+)</li>[^<]*<li>([^<]+)</li>[^<]*<li>([^<]+)</li>[^<]*" +
                    "<li>([^<]+)</li>[^<]*<li>([^<]*)</li>*";

    public ArrayList<Stand> query2014LeaderBoard() {
        long startTime = System.currentTimeMillis();
        ArrayList<Stand> list = new ArrayList<Stand>();
        try {
            String html = getUrlContent(URL_BASE);
            if (html != null && html.contains("<!--standing-->")
                    && html.contains("<!--top5-->")) {
                String boardHtml = html.substring(html.indexOf("<!--standing-->"),
                        html.indexOf("<!--top5-->"));
                //Log.d(TAG, mScoreBoardHtml);
                Matcher matchTeams = Pattern.compile(PATTERN_BOARD_TEAM_2014).matcher(boardHtml);
                while (matchTeams != null && matchTeams.find()) {
                    Log.d(TAG, matchTeams.group(1));
                    Team team = Team.getTeam2014(mContext, matchTeams.group(1));
                    int win = Integer.parseInt(matchTeams.group(2));
                    int lose = Integer.parseInt(matchTeams.group(3));
                    int tie = Integer.parseInt(matchTeams.group(4));
                    float rate = Float.parseFloat(matchTeams.group(5));
                    float behind = list.size() > 0 ? Float.parseFloat(matchTeams.group(6)) : 0;
                    Log.d(TAG, String.format("%d-%d-%d, %.03f, %.01f",
                            win, tie, lose, rate, behind));
                    list.add(new Stand(team, win, lose, tie, rate, behind));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "query2014LeaderBoard: " + e.getMessage());
            e.printStackTrace();
            mScoreBoardHtml = null;
            list = null;
        }
        long endTime = System.currentTimeMillis();
        Log.v(TAG, String.format("wasted %d ms", ((endTime - startTime))));
        return list;
    }

    private final static String PATTERN_TEAM_2014 =
            //<img src="assets/.../away.png"></td><td>field</td><td><img src="assets/.../home.png
            //"<img src=\"([^\"]+)[^>]*>[^>]*>[^>]*>";
            "<img src=\"([^\"]+)[^>]*>[^>]*>[^>]*>([^<]+)<[^<]*<[^<]*<img src=\"([^\"]+)";

    private final static String PATTERN_GAME_ID_2014 =
            //<table><tr class='normal'><td>
            //<table><tr><th></th><th>48</th><th></th></tr></table>
            "<table><tr><th>[^<]*</th><th>([\\d]+)</th><th>[^<]*</th></tr></table>";

    private final static String PATTERN_RESULT_2014 =
            //
            "<[^>]*>([0-9]+)<br>([^<]+)<br>([^<]+)<br>([^<]+)<br>([^<]+)";

    private Game parseOneGameHtml2014(int year, int month, int day, String str) {
        Game game = new Game();
        game.Source = Game.SOURCE_CPBL;
        game.Kind = "01";
        game.StartTime = getNowTime();
        if (year > 0) {
            game.StartTime.set(Calendar.YEAR, year);
        }
        if (month > 0) {
            game.StartTime.set(Calendar.MONTH, month - 1);
        }
        game.StartTime.set(Calendar.DAY_OF_MONTH, day);

//        <!--兩個對戰組合的分隔線-->
//        <tr class="team">
//        <td><div class="teamlogo">
//        <table><tr><td><img src="http://cpbl-elta.cdn.hinet.net/assets/images/logo/E02_logo_05.png" /></td>
//        <td class="stadium">桃園國際</td>
//        <td><img src="http://cpbl-elta.cdn.hinet.net/assets/images/logo/A02_logo_05.png" /></td>
//        </tr>
//        </table>
//        </div>
//        </td>
//        </tr>
//        <tr class="game">
//        <td class="score">
//        <table><tr class='suspend'><td>
//        <table><tr><th>補賽</th><th>50</th><th>雙賽1</th></tr></table>
//        </td></tr><tr><td>
//        <table><tr><td class='no'><a href='game/starters.aspx?gameno=01&year=2014&game=50'>
// <img src='http://cpbl-elta.cdn.hinet.net/assets/images/c_player.png'></a></td>
// <td>13:05</td><td class='info' title='博斯運動網.ELTA體育台.CPBLTV'>
// <img src='http://cpbl-elta.cdn.hinet.net/assets/images/c_tv.png' /></td></tr></table>
//        </td></tr></table>
//        </td></tr>
//
//        <tr class="game">
//        <td class="score">
//        <table><tr class='normal'><td>
//        <table><tr><th></th><th>52</th><th>雙賽2</th></tr></table>
//        </td></tr><tr><td>
//        <table><tr><td class='no'><a href='game/starters.aspx?gameno=01&year=2014&game=52'>
// <img src='http://cpbl-elta.cdn.hinet.net/assets/images/c_player.png'></a></td><td>17:05</td>
// <td class='info' title='博斯運動網 .華視 .CPBLTV'>
// <img src='http://cpbl-elta.cdn.hinet.net/assets/images/c_tv.png' /></td></tr></table>
//        </td></tr></table>
//        </td></tr>
//        <tr><td class="line"></td></tr>

//        <div class="team">
//        <table><tr><td><img src="assets/images/logo/B03_logo_05.png"></td>
//        <td>新莊</td>
//        <td><img src="assets/images/logo/E02_logo_05.png"></td>
//        </tr>
//        </table>
//        </div>
//        <div class='score'><table><tr><th colspan='3'>6</th></tr>
//        <tr><td class='no'></td><td>PM 06:35</td><td class='info'>
// <img src='assets/images/c_tv.png'></td></tr></table></div>

        game.IsFinal = str.contains("assets/images/c_final.png");

        Matcher matchTeams = Pattern.compile(PATTERN_TEAM_2014).matcher(str);
        if (matchTeams != null && matchTeams.find()) {
            //Log.d(TAG, "  AWAY: " + matchTeams.group(1));
            game.Field = matchTeams.group(2);
            //Log.d(TAG, "  HOME: " + matchTeams.group(3));
            game.AwayTeam = Team.getTeam2014(mContext, matchTeams.group(1));
            game.HomeTeam = Team.getTeam2014(mContext, matchTeams.group(3));
        } else {
            game.AwayTeam = new Team(mContext, Team.ID_UNKNOWN);
            game.HomeTeam = new Team(mContext, Team.ID_UNKNOWN);
        }

        Matcher matchID = Pattern.compile(PATTERN_GAME_ID_2014).matcher(str);
        if (matchID != null && matchID.find()) {
            game.Id = Integer.parseInt(matchID.group(1));
        }
        //Log.d(TAG, "  game.Id = " + game.Id);

        if (game.IsFinal) {
            String awayScore = str.substring(str.indexOf("<td class='no'>") + 15);
            awayScore = awayScore.substring(0, awayScore.indexOf("<"));
            //Log.d(TAG, "    awayScore = " + awayScore);
            game.AwayScore = Integer.parseInt(awayScore);
            String homeScore = str.substring(str.lastIndexOf("<td class='no'>") + 15);
            homeScore = homeScore.substring(0, homeScore.indexOf("<"));
            //Log.d(TAG, "    homeScore = " + homeScore);
            game.HomeScore = Integer.parseInt(homeScore);
            //<a href='game/box.aspx?gameno=07&year=2014&game=2'>
            String url = str.substring(str.lastIndexOf("<a href=") + 9);
            url = url.substring(0, url.indexOf("'"));
            game.Url = URL_BASE + "/" + url;
            //Log.d(TAG, "    url = " + game.Url);
        } else {
            //Log.d(TAG, "game.Id = " + game.Id);
            String time = "18:35";
            if (str.contains("assets/images/c_player.png")) {
                //assets/images/c_player.png'></a></td><td>17:05</td>
                time = str.substring(str.indexOf("assets/images/c_player.png"));
            } else {
                //<tr><td class='no'></td><td>PM 06:35</td><td class='info'>
                time = str.substring(str.indexOf("<td class='no'>") + 1);
            }
            time = time.substring(time.indexOf("<td") + 4);
            time = time.substring(0, time.indexOf("</td"));
            //Log.d(TAG, "    time = " + time);
            String[] ts = time.split("[ :]");
            //Log.d(TAG, "    time = " + ts.length);
            int hour, minute;
            if (ts.length > 2) {
                hour = Integer.parseInt(ts[1]);
                hour = ts[0].equalsIgnoreCase("PM") ? hour + 12 : hour;
                minute = Integer.parseInt(ts[2]);
            } else if (ts.length == 2) {
                hour = Integer.parseInt(ts[0]);
                minute = Integer.parseInt(ts[1]);
            } else {
                int dayOfWeek = game.StartTime.get(Calendar.DAY_OF_WEEK);
                boolean isWeekend = (dayOfWeek == Calendar.SATURDAY ||
                        dayOfWeek == Calendar.SUNDAY);
                hour = isWeekend ? 17 : 18;
                minute = isWeekend ? 05 : 35;
            }

            //Log.d(TAG, String.format("    time = %02d:%02d", hour, minute));
            game.StartTime.set(Calendar.HOUR_OF_DAY, hour);
            game.StartTime.set(Calendar.MINUTE, minute);

//            //[76]dolphin++
//            game.Url = String.format("%s/game/starters.aspx?gameno=%s&year=%d&game=%d",
//                    URL_BASE, game.Kind, game.StartTime.get(Calendar.YEAR), game.Id);
            game.Url = "http://www.cpbltv.com/channel/1.html";//[80]dolphin++
            game.Url = "http://www.cpbltv.com/";//[84]dolphin++
        }

        return game;
    }

    //<font color=blue>001 義大-統一(新莊)</font>
    private final static String PATTERN_GAME_2014_ZXC =
            "<font color=blue>([^/]+)";
    //"<font color=blue>([^ ]+) ([^\\-]+)-([^\\(]+)([^<]+)</font>";

    public ArrayList<Game> query2014zxc(int month) {
        long startTime = System.currentTimeMillis();
        //Log.v(TAG, "query2014 zxc22");
        ArrayList<Game> gameList = new ArrayList<Game>();
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
                        while (mGame != null && mGame.find()) {
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
                                String extra;
                                if (g < data.length - 1) {
                                    extra = data[g + 1];
                                } else {
                                    extra = data[g];
                                }
                                //Log.d(TAG, extra);
                                if (extra.startsWith("<font color=red>")) {
                                    g--;
                                    gameList.remove(gameList.size() - 1);
                                } else {//[93]dolphin++ fix delay message after game final
                                    if (extra.contains("<font color=blue>")) {
                                        extra.substring(
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
                        }
                        if (false && g > 1) {//more than one game
                            for (int j = 1; j < g; j++) {
                                Game g1 = gameList.get(gameList.size() - j);
                                g1.Url = g1.IsFinal ? g1.Url : g1.Url.replace("1.html",
                                        String.format("%d.html", (g - j + 1)));
                                //Log.d(TAG, String.format("%d: %s", g1.Id, g1.Url));
                            }
                        }
                    }
                }
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
        mGameList = gameList;
        return gameList;
    }

    private Game parseOneGameHtml2014zxc(int month, int day, String str) {
        Game game = new Game();
        game.Source = Game.SOURCE_ZXC22;
        game.Kind = "01";
        game.StartTime = getNowTime();
        game.StartTime.set(Calendar.YEAR, 2014);
        game.StartTime.set(Calendar.MONTH, month - 1);
        game.StartTime.set(Calendar.DAY_OF_MONTH, day);

        int dayOfWeek = game.StartTime.get(Calendar.DAY_OF_WEEK);
        boolean isWeekend = (dayOfWeek == Calendar.SATURDAY ||
                dayOfWeek == Calendar.SUNDAY);
        //int hour = isWeekend ? 17 : 18;
        //int minute = isWeekend ? 05 : 35;
        game.StartTime.set(Calendar.HOUR_OF_DAY, isWeekend ? 17 : 18);
        game.StartTime.set(Calendar.MINUTE, isWeekend ? 05 : 35);

        //<font color=blue>001 義大-統一(新莊)</font>
        String[] data = str.split("[ \\-\\(\\)]");
        //Log.d(TAG, "data " + data.length + ", " + data[0]);
        game.Id = Integer.parseInt(data[0]);
        game.AwayTeam = new Team(mContext, Team.getTeamId(mContext, data[1], 2014));
        game.HomeTeam = new Team(mContext, Team.getTeamId(mContext, data[2], 2014));
        game.Field = (data.length >= 4) ? data[3] : "";

        try {//<font color='#547425'><B>4:8(11288人)</B>
            if (str.contains("<font color='#547425'>")) {
                game.IsFinal = true;
                //http://www.cpbl.com.tw/game/box.aspx?gameno=07&year=2014&game=4
                game.Url = String.format("%s/game/box.aspx?gameno=%s&year=%d&game=%d",
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
        }

        return game;
    }

    public SparseArray<Game> getDelayGameList() {
        SparseArray<Game> list = new SparseArray<Game>();
        try {
            String html = getUrlContent("http://zxc22.idv.tw/delay.asp", ENCODE_BIG5);
            html = html.substring(html.indexOf("<table"));
            html = html.substring(html.indexOf("</tr>"));
            html = html.substring(html.indexOf("</tr>"));
            String[] games = html.split("<tr");
            //Log.d(TAG, "games " + games.length);
            for (int i = 2; i < games.length - 1; i++) {
                String[] info = games[i].split("</td>");
                //Log.d(TAG, ">>> " + game);
                //<td align=center><font face=arial size=2>1</font></td>
                String id = info[1].substring(info[1].lastIndexOf("<font"));
                id = id.substring(id.indexOf(">") + 1, id.indexOf("</"));
                String time = info[3].substring(info[3].lastIndexOf("<font"));
                time = time.substring(time.indexOf(">") + 1, time.indexOf("</"));
                //Log.d(TAG, "id=" + id + ", time=" + time);
                Game g = new Game();
                g.Id = Integer.parseInt(id);
                g.StartTime = CpblCalendarHelper.getNowTime();
                g.StartTime.set(Calendar.HOUR_OF_DAY, Integer.parseInt(time.split(":")[0]));
                g.StartTime.set(Calendar.MINUTE, Integer.parseInt(time.split(":")[1]));
                //Log.d(TAG, "id=" + g.Id);
                g.IsDelay = true;
                list.put(g.Id, g);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public static ArrayList<Game> getCache(Context context, String fileName) {
        //String fileName = String.format("%04d-%02d.json", year, month);
        File f = new File(context.getExternalCacheDir(), fileName);
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
        return null;
    }

    public static ArrayList<Game> getCache(Context context, int year, int month) {
        return getCache(context, String.format("%04d-%02d.json", year, month));
    }

    public ArrayList<Game> getCache(int year, int month) {
        if (canUseCache()) {
            return getCache(mContext, String.format("%04d-%02d.json", year, month));
        }
        return null;
    }

    public static boolean putCache(Context context, String fileName, ArrayList<Game> list) {
        //String fileName = String.format("%04d-%02d.json", year, month);
        File f = new File(context.getExternalCacheDir(), fileName);
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

    public static boolean putCache(Context context, int year, int month, ArrayList<Game> list) {
        return putCache(context, String.format("%04d-%02d.json", year, month), list);
    }

    public boolean putCache(int year, int month, ArrayList<Game> list) {
        if (canUseCache()) {
            return putCache(mContext, String.format("%04d-%02d.json", year, month), list);
        }
        return false;
    }

    public boolean canUseCache() {
        return mUseCache;
    }

    public static Calendar getNowTime() {
        //Log.d(TAG, Locale.TAIWAN.toString());
        //return Calendar.getInstance(Locale.TAIWAN);
        return Calendar.getInstance(TimeZone.getTimeZone("GMT+8"));
    }
}
