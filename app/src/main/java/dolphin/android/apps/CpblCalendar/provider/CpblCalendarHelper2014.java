/**
 * Created by dolphin on 2013/6/1.
 * <p/>
 * Date provider.
 * Implements the download HTML source and transform to our data structure.
 */
package dolphin.android.apps.CpblCalendar.provider;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.io.File;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dolphin.android.apps.CpblCalendar.R;
import dolphin.android.apps.CpblCalendar.Utils;
import dolphin.android.net.GoogleDriveHelper;
import dolphin.android.util.FileUtils;

@Deprecated
public class CpblCalendarHelper2014 extends CpblCalendarHelper {

    private final static String TAG = "CpblCalendarHelper";

    private final static String URL_BASE_2013 = "http://cpblweb.ksi.com.tw";

    private final static String SCORE_QUERY_STRING =
            "?gamekind=@kind&myfield=@field&mon=@month&qyear=@year";

    private final static String ALL_SCORE_URL = URL_BASE_2013 + "/standings/AllScoreqry.aspx";

    private final static String RESULT_URL = URL_BASE_2013 + "/GResult/Result.aspx";

    private final static String RESULT_QUERY_STRING = "?gameno=@kind&pbyear=@year&game=@id";

    private final static String URL_SCHEDULE_2014 = URL_BASE + "/schedule.aspx";

    private AspNetHelper mAspNetHelper;
    private int mYear;
    private int mMonth;
    private String mKind;

    public CpblCalendarHelper2014(Context context) {
        super(context);
    }

    @Deprecated
    public ArrayList<Game> query2014() {
        return query2014(0, 0, null);
    }

    @Deprecated
    public ArrayList<Game> query2014(int year, int month) {
        return query2014(year, month, null);
    }

    @Deprecated
    public ArrayList<Game> query2014(int year, int month, String kind) {
        return query2014(year, month, kind, null);
    }

    @Deprecated
    public ArrayList<Game> query2014(int year, int month, String kind, SparseArray<Game> delayGames) {
        long startTime = System.currentTimeMillis();

        //Log.d(TAG, "query2014");
        ArrayList<Game> gameList = new ArrayList<Game>();
        try {//
            if (mAspNetHelper == null) {
                mAspNetHelper = new AspNetHelper(URL_SCHEDULE_2014);
            }
            String html = mAspNetHelper.getLastResponse();// = getUrlContent(URL_SCHEDULE_2014);
            try {
                //AspNetHelper helper = new AspNetHelper(URL_SCHEDULE_2014);
                Log.d(TAG, String.format("mYear=%d, year=%d", mYear, year));
                if (mYear != year) {
                    html = mAspNetHelper.makeUrlRequest("syear", String.valueOf(year));
                    if (html == null) {
                        throw new Exception("can't switch year");
                    }
                    mYear = year;
                }
                Log.d(TAG, String.format("mMonth=%d, month=%d", mMonth, month));
                if (mMonth != month) {
                    html = mAspNetHelper.makeUrlRequest("smonth",
                            getContext().getString(R.string.aspnet_month_template, month));
                    if (html == null) {
                        throw new Exception("can't switch month");
                    }
                    mMonth = month;
                }
                Log.d(TAG, String.format("mKind=%s, kind=%s", mKind, kind));
                if (kind != null && !kind.isEmpty() && !kind.equals(mKind)) {//choose game kind
                    html = mAspNetHelper.makeUrlRequest("sgameno", kind);
                    if (html == null) {
                        throw new Exception("can't switch kind");
                    }
                    mKind = kind;
                }
            } catch (Exception e) {
                Log.e(TAG, "unable to get ASP.NET data: " + e.getMessage());
                html = null;
            }

            //Log.d(TAG, "query2014 " + html.length());
            if (html != null && html.contains("<tr class=\"game\">")) {//have games
                String[] days = html.split("<table class=\"day\">");
                Log.d(TAG, "days " + days.length);
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
                            if (games[g].contains("<tr class='suspend'>")) {//delay games
//      <table><tr class='suspend'><td>
//      <table><tr><th></th><th>51</th><th></th></tr></table>
//      </td></tr><tr><td><table><tr><td colspan='3' class='suspend'>延賽至2014/04/27</td></tr>

                                //use <tr class="game"> to get games
                                String[] gamesHack = games[g].split("<tr class=\"game\">");
                                //Log.d(TAG, String.format("gamesHack %d", gamesHack.length));
                                for (int h = 1; h < gamesHack.length; h++) {
                                    String gameStr = gamesHack[0] + gamesHack[h];
                                    if (gameStr.contains("<td colspan='3' class='suspend'>")) {
                                        //Log.v(TAG, String.format("suspend day=%d", d));
                                        continue;//don't add to list
                                    }
                                    //Log.d(TAG, gameStr);
                                    Game g1 = parseOneGameHtml2014(year, month, d, kind, gameStr,
                                            delayGames);
                                    if (g1 != null) {
                                        gameList.add(g1);
                                    }
                                }
                            } else if (games[g].contains("<tr class='normal'>")) {
                                //normal games
                                Game g2 = parseOneGameHtml2014(year, month, d, kind, games[g],
                                        delayGames);
                                if (g2 != null) {
                                    gameList.add(g2);
                                }
                            }
                        }//check every games
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "query: " + e.getMessage());
            e.printStackTrace();
        }
        long endTime = System.currentTimeMillis();
        Log.v(TAG, String.format("query2014 %d/%02d wasted %d ms",
                year, month, ((endTime - startTime))));
        //mGameList = gameList;
        return gameList;
    }

//    <div class="topinfo">
//          <ul>
//              <li>
//                  <!-- team_rank: 1 -->
//                  <a href="/team_brief_L01.aspx"><img src="http://cpbl-elta.cdn.hinet.net/assets/images/logo/L01_logo_07.png"></a></li>
//              <li>4</li>
//              <li>1</li>
//              <li>0</li>
//              <li>0.800</li>
//              <li>-</li>
//          </ul>

    private final static String PATTERN_BOARD_TEAM_2014 =
            "<div class=\"topinfo\">[^<]*" +
                    "<ul>[^<]*<li>[^<]*<[^<]*" +
                    "<a[^<]*<img src=\"([^\"]+)\"></a></li>[^<]*" +
                    "<li>([^<]+)</li>[^<]*" +
                    "<li>([^<]+)</li>[^<]*" +
                    "<li>([^<]+)</li>[^<]*" +
                    "<li>([^<]+)</li>[^<]*" +
                    "<li>([^<]*)</li>*";

    @Deprecated
    public ArrayList<Stand> query2014LeaderBoard() {
        long startTime = System.currentTimeMillis();
        ArrayList<Stand> list = new ArrayList<>();
        try {
            String html = getUrlContent(URL_BASE);
            if (html != null && html.contains("<!--standing-->") && html.contains("<!--top5-->")) {
                String boardHtml = html.substring(html.indexOf("<!--standing-->"),
                        html.indexOf("<!--top5-->"));
                //Log.d(TAG, mScoreBoardHtml);
                Matcher matchTeams = Pattern.compile(PATTERN_BOARD_TEAM_2014).matcher(boardHtml);
                while (matchTeams.find()) {
                    Log.d(TAG, matchTeams.group(1));
                    Team team = Team.getTeam2014(getContext(), matchTeams.group(1),
                            getNowTime().get(Calendar.YEAR));
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
            //mScoreBoardHtml = null;
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
            "<table><tr><th>([^<]*)</th><th>([\\d]+)</th><th>([^<]*)</th></tr></table>";

    private final static String PATTERN_RESULT_2014 =
            //
            "<[^>]*>([0-9]+)<br>([^<]+)<br>([^<]+)<br>([^<]+)<br>([^<]+)";

    @Deprecated
    private Game parseOneGameHtml2014(int year, int month, int day, String kind, String str,
                                      SparseArray<Game> delayGames) {
        Context context = getContext();
        Game game = new Game();
        game.Source = Game.SOURCE_CPBL;
        game.Kind = kind;
        game.StartTime = getGameTime(year, month, day);

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
        if (/*matchTeams != null && */matchTeams.find()) {
            //Log.d(TAG, "  AWAY: " + matchTeams.group(1));
            game.Field = matchTeams.group(2);
            //Log.d(TAG, "  HOME: " + matchTeams.group(3));
            game.AwayTeam = Team.getTeam2014(context, matchTeams.group(1), year);
            game.HomeTeam = Team.getTeam2014(context, matchTeams.group(3), year);
        } else {
            game.AwayTeam = new Team(context, Team.ID_UNKNOWN);
            game.HomeTeam = new Team(context, Team.ID_UNKNOWN);
        }

        Matcher matchID = Pattern.compile(PATTERN_GAME_ID_2014).matcher(str);
        if (/*matchID != null && */matchID.find()) {
            game.Id = Integer.parseInt(matchID.group(2));
            if (!matchID.group(3).isEmpty()) {
                game.DelayMessage = matchID.group(3);
            }
            if (!matchID.group(1).isEmpty()) {
                //game.DelayMessage += String.format("(%s)", matchID.group(1));
                game.DelayMessage = game.DelayMessage == null //[138]+color
                        ? String.format("<font color='red'>(%s)</font>", matchID.group(1))
                        : String.format("%s <font color='red'>(%s)</font>",//[138]+color
                        game.DelayMessage, matchID.group(1));//[115]++
            }
            //[118]jimmy++ add delay games list, needs about 10 seconds to generate the list
            if (delayGames != null && delayGames.get(game.Id) != null) {
                String d_date = new SimpleDateFormat("yyyy/MM/dd", Locale.TAIWAN).format(
                        delayGames.get(game.Id).StartTime.getTime());
                game.DelayMessage = game.DelayMessage == null
                        ? String.format("<font color='red'>(%s)</font>", d_date)//[138]+color
                        : String.format("%s <font color='red'>(%s)</font>",//[138]+color
                        game.DelayMessage, d_date);
            }
        }
        //Log.d(TAG, "  game.Id = " + game.Id);

        if (game.IsFinal) {
            String awayScore = str.substring(str.indexOf("<td class='no'>") + 15);
            awayScore = awayScore.substring(0, awayScore.indexOf("<"));
            //Log.d(TAG, "    awayScore = " + awayScore);
            game.AwayScore = Integer.parseInt(awayScore);
            //[110]dolphin-- String homeScore = str.substring(str.lastIndexOf("<td class='no'>") + 15);
            String homeScore = str.substring(str.lastIndexOf("<td class='info'>") + 17);
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
                minute = isWeekend ? 5 : 35;
            }

            //Log.d(TAG, String.format("    time = %02d:%02d", hour, minute));
            game.StartTime.set(Calendar.HOUR_OF_DAY, hour);
            game.StartTime.set(Calendar.MINUTE, minute);

//            //[76]dolphin++
//            game.Url = String.format("%s/game/starters.aspx?gameno=%s&year=%d&game=%d",
//                    URL_BASE, game.Kind, game.StartTime.get(Calendar.YEAR), game.Id);
//            game.Url = "http://www.cpbltv.com/channel/1.html";//[80]dolphin++
//            game.Url = "http://www.cpbltv.com/";//[84]dolphin++

            try {//[149]dolphin++
                if (str.contains("assets/images/c_tv.png")) {//show game live channel
                    String channel = str.substring(str.indexOf("<td class='info' title"));
                    channel = channel.substring(channel.indexOf("title="));
                    channel = channel.substring(channel.indexOf("'") + 1, channel.indexOf("'>"));
                    //Log.d(TAG, channel);
                    game.Channel = channel;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
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
                g.StartTime = getNowTime();
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

    @Deprecated
    /**
     * query delay games from CPBL website (using ASP.NET), costs more time
     *
     * @param context Context
     * @param year    year
     * @return delay game list
     */
    public SparseArray<Game> queryDelayGames2014(Context context, int year) {
        long startTime = System.currentTimeMillis();
        if (year < 2005 || context == null) {
            Log.w(TAG, String.format("no %d delay game info in www.cpbl.com.tw", year));
            return null;
        }

        //read current month
        Calendar now = getNowTime();
        boolean isThisYear = now.get(Calendar.YEAR) == year;

        SparseArray<Game> delayedGames = restoreDelayGames2014(context, year);//new SparseArray<>();
        if (!isThisYear && delayedGames.size() > 0) {//use cache directly
            Log.v(TAG, String.format("use cached data, delay games = %d", delayedGames.size()));
            return delayedGames;
        }

        //read from Google Drive
        String[] driveIds = context.getResources().getStringArray(R.array.year_delay_game_2014);
        int index = year - 2005;
        if (index < driveIds.length) {//already have cached data in Google Drive
            String driveId = driveIds[year - 2005];
            File f = new File(getCacheDir(context), String.format(Locale.US, "%d.delay", year));
            GoogleDriveHelper.download(context, driveId, f);
            delayedGames = restoreDelayGames2014(context, year);//read again
            if (delayedGames.size() > 0) {//use cache directly
                Log.v(TAG, String.format("use Google Drive cached data (%d)", delayedGames.size()));
                return delayedGames;
            }
        }

        //if year == this year, do to current month
        //if year == last year, do all
        int m1 = isThisYear ? now.get(Calendar.MONTH) + 1 : 3;
        int m2 = isThisYear ? now.get(Calendar.MONTH) + 1 : 10;
        for (int month = 3; month <= m2; month++) {
            String html = null;// = getUrlContent(URL_SCHEDULE_2014);
            if (month < m1) {//are we in the same month?
                //use old cache since the data won't changed anymore
                html = readDelayGamesCache2014(context, year, month);
            }

            if (html == null || html.isEmpty()) {//no previous data, try it from web
                try {
                    //AspNetHelper helper = new AspNetHelper(URL_SCHEDULE_2014);
                    mKind = "01";
                    html = mAspNetHelper.makeUrlRequest("ctl00$cphBox$ddl_gameno", mKind);
                    if (html == null) {
                        throw new Exception("can't switch kind");
                    }
                    //Log.d(TAG, String.format("mYear=%d, year=%d", mYear, year));
                    if (mYear != year) {
                        html = mAspNetHelper.makeUrlRequest("ctl00$cphBox$ddl_year", String.valueOf(year));
                        if (html == null) {
                            throw new Exception("can't switch year");
                        }
                        mYear = year;
                        //switch back to game kind 01
                    }
                    //Log.d(TAG, String.format("mMonth=%d, month=%d", mMonth, month));
                    if (mMonth != month) {
                        html = mAspNetHelper.makeUrlRequest("ctl00$cphBox$ddl_month",
                                String.format(Locale.US, "/%d/1", month));
                        if (html == null) {
                            throw new Exception("can't switch month");
                        }
                        mMonth = month;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "unable to get ASP.NET data: " + e.getMessage());
                    html = null;//bypass parsing if exception happens
                }
            }

            //Log.d(TAG, "query2014 " + html.length());
            if (html != null && html.contains("<tr class=\"game\">")) {//have games
                writeDelayGamesCache2014(context, year, month, html);//write to cache
                String[] days = html.split("<table class=\"day\">");
                //Log.d(TAG, "days " + days.length);
                for (String day : days) {
                    //check those days with games
                    String data = day.substring(day.indexOf("<tr"));
                    //<tr class="beforegame">
                    //<tr class="gameing">
                    //<tr class="futuregame">
                    String date = data.substring(data.indexOf("<td>") + 4, data.indexOf("</td>"));
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
                        for (String game1 : games) {
                            //[92]dolphin++
                            if (game1.contains("<tr class='suspend'>")) {
//      <table><tr class='suspend'><td>
//      <table><tr><th></th><th>51</th><th></th></tr></table>
//      </td></tr><tr><td><table><tr><td colspan='3' class='suspend'>延賽至2014/04/27</td></tr>

// @2015/05/24 another example that one has final but another can't play
//		<table><tr class='suspend'><td>
//		<table><tr><th>補賽</th><th>87</th><th></th></tr></table>
//		</td></tr><tr><td>
//		<table><tr><td class='no'>1</td>
//        <td><a href='game/box.aspx?gameno=01&year=2015&game=87'>
//        <img src='http://cpbl-elta.cdn.hinet.net/assets/images/c_final.png'></a></td>
//        <td class='info'>2</td></tr></table>
//		</td></tr></table>
//		</td></tr>
//		<tr class="game">
//                 				<td class="score">
//			<table><tr class='suspend'><td>
//			<table><tr><th></th><th>89</th><th></th></tr></table>
//			</td></tr><tr><td><table><tr><td colspan='3' class='suspend'>延賽至2015/06/20</td>

                                String[] gamesHack = game1.split("<tr class=\"game\">");
                                for (int h = 1; h < gamesHack.length; h++) {
                                    String gameStr = gamesHack[0] + gamesHack[h];
                                    if (gameStr.contains("<td colspan='3' class='suspend'>")) {
                                        //we need these games
                                        Matcher m = Pattern.compile("<th>([0-9]+)</th>").matcher(gameStr);
                                        if (m.find()) {
                                            //Log.d(TAG, "  id = " + m.group(1));
                                            Game game = new Game();
                                            game.Id = Integer.parseInt(m.group(1));
                                            game.StartTime = getGameTime(year, month, d);
                                            if (delayedGames.get(game.Id) != null) {
                                                //Log.d(TAG, "bypass " + game.toString());
                                                continue;
                                            }
                                            delayedGames.put(game.Id, game);
                                        }
                                    }//else: normal games
                                }
                            }
                        }//check every games
                    }
                }//for possible days
            }
            Log.v(TAG, String.format("%d query wasted %d ms", month,
                    ((System.currentTimeMillis() - startTime))));
        }
        long endTime = System.currentTimeMillis();
        Log.v(TAG, String.format("delay game query wasted %d ms", ((endTime - startTime))));

        if (delayedGames.size() > 0) {//store all data to local cache
            storeDelayGames2014(context, year, delayedGames);
        }

        return delayedGames;
    }

    @Deprecated
    private void writeDelayGamesCache2014(Context context, int year, int month, String html) {
        if (context == null) {
            return;//cannot store, no context
        }
        File f = new File(getCacheDir(context), String.format(Locale.US, "%d-%02d.delay_cache", year, month));
        FileUtils.writeStringToFile(f, html);
    }

    @Deprecated
    private String readDelayGamesCache2014(Context context, int year, int month) {
        if (context != null) {
            //restore data from cache
            File f = new File(getCacheDir(context), String.format(Locale.US, "%d-%02d.delay_cache", year, month));
            if (f.exists()) {
                return FileUtils.readFileToString(f);
            }
        }
        return null;
    }

    private final static String TABLE_ROW_TEMPLATE =
            "<tr style='%style' bgcolor='@bgcolor'>" +
                    "<td style='width:40%;'>@team</td>" +
                    "<td style='width:15%;text-align:center'>@win</td>" +
                    "<td style='width:15%;text-align:center'>@lose</td>" +
                    "<td style='width:15%;text-align:center'>@tie</td>" +
                    "<td style='width:15%;text-align:center'>@behind</td></tr>";

    /**
     * prepare leader board html
     *
     * @param context  Context
     * @param standing output data
     * @return html
     */
    public static String prepareLeaderBoard2014(Context context, ArrayList<Stand> standing) {
        if (standing == null)
            return "";
        String standingHtml = "<table style='width:100%;'>";
        standingHtml += TABLE_ROW_TEMPLATE
                .replace("@style", "color:white;")
                .replace("@bgcolor", "#669900")
                .replace("td", "th")
                .replace("@team", context.getString(R.string.title_team))
                .replace("@win", context.getString(R.string.title_win))
                .replace("@lose", context.getString(R.string.title_lose))
                .replace("@tie", context.getString(R.string.title_tie))
                .replace("@behind", context.getString(R.string.title_game_behind));
        //standingHtml += "<tr><td colspan='5'><hr /></td></tr>";
        final String[] color = {"#F1EFE6", "#E6F1EF"};
        int c = 0;
        for (Stand stand : standing) {
            standingHtml += TABLE_ROW_TEMPLATE
                    .replace("@style", "")
                    .replace("@bgcolor", color[(c++ % 2)])
                    .replace("@team", stand.getTeam().getName())
                    .replace("@win", String.valueOf(stand.getGamesWon()))
                    .replace("@lose", String.valueOf(stand.getGamesLost()))
                    .replace("@tie", String.valueOf(stand.getGamesTied()))
                    .replace("@behind", String.valueOf(stand.getGamesBehind()));
        }
        standingHtml += "</table>";
        return standingHtml;
    }

    /**
     * build a leader board dialog for 2014 data
     *
     * @param context Context
     * @param html    html content
     * @param title   dialog title
     * @return leader board dialog
     */
    public static AlertDialog buildLeaderBoard2014Dialog(Context context, String html, String title) {
        final AlertDialog dialog = new AlertDialog.Builder(context).create();
        //change the style like the entire theme
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.leader_board, null);
        TextView textView = (TextView) view.findViewById(android.R.id.title);
        if (textView != null && title != null) {
            textView.setText(title);
        }

        // android from html cannot recognize all HTML tag
        // http://stackoverflow.com/a/8632338
        WebView webView = (WebView) view.findViewById(R.id.webView);//new WebView(this);
        // http://pop1030123.iteye.com/blog/1399305
        //webView.getSettings().setDefaultTextEncodingName(CpblCalendarHelper.ENCODE_UTF8);
        webView.getSettings().setJavaScriptEnabled(false);
        webView.getSettings().setSupportZoom(false);
        // Encoding issue with WebView's loadData
        // http://stackoverflow.com/a/9402988
        webView.loadData(html, "text/html; charset=" + CpblCalendarHelper.ENCODE_UTF8, null);

        View btOk = view.findViewById(android.R.id.button1);
        if (btOk != null) {
            btOk.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dialog.dismiss();
                }
            });
        }

        dialog.setView(view);//webView
        dialog.show();
        return dialog;
    }
}
