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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dolphin.android.apps.CpblCalendar.R;

@Deprecated
public class CpblCalendarHelper2013 extends CpblCalendarHelper {

    private final static String TAG = "CpblCalendarHelper";

    private final static String URL_BASE_2013 = "http://cpblweb.ksi.com.tw";

    private final static String SCORE_QUERY_STRING =
            "?gamekind=@kind&myfield=@field&mon=@month&qyear=@year";

    private final static String ALL_SCORE_URL = URL_BASE_2013 + "/standings/AllScoreqry.aspx";

    private final static String RESULT_URL = URL_BASE_2013 + "/GResult/Result.aspx";

    private final static String RESULT_QUERY_STRING = "?gameno=@kind&pbyear=@year&game=@id";

    //[26}++
    private ArrayList<Game> mGameList = null;

    private String mScoreBoardHtml = null;

    public CpblCalendarHelper2013(Context context) {
        super(context);
    }

    @Deprecated
    /**
     * do the web query
     *
     * @param kind  game kind
     * @param year  year
     * @param month month
     * @return game list
     */
    public ArrayList<Game> query(String kind, int year, int month) {
        return query(kind, year, month, "F00");
    }

    @Deprecated
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
//                    } else {//no game today
//                        //Log.d(TAG, String.format("no game: #%d", d));
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

    @Deprecated
    /**
     * convert a game info to Game object
     *
     * @param kind  game kind
     * @param year  year
     * @param month month
     * @param day   day
     * @param str   html source
     * @return game object
     */
    private Game parseOneGameHtml(String kind, int year, int month, int day, String str) {
        Context context = getContext();
        Game game = new Game();
        game.Kind = kind;
        game.Source = Game.SOURCE_CPBL_2013;
        //Log.d(TAG, str);

        game.StartTime = getGameTime(year, month, day);
        //[51]dolphin++ fix alarm key floating
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

        String schedule;
        Matcher matcher = Pattern.compile(pattern).matcher(str);

//        if (matcher != null && matcher.find()) {
//            //we have data, parse it later
//        } else if (bLive) {//[9]try no live when we assume it always has
        if (!matcher.find() && bLive) {
            //Log.d(TAG, "we assume it has live... IsFinal=" + game.IsFinal);
            matcher = Pattern.compile(game.IsFinal ? PATTERN_RESULT_WITHOUT_LIVE
                    : PATTERN_TOPLAY_WITHOUT_LIVE).matcher(str);
//            if (matcher != null && matcher.find()) {
//                //we have data, parse it later
//            } else {//still no match
            if (!matcher.find()) {
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
        game.AwayTeam = new Team(context, away_team, year);
        game.HomeTeam = new Team(context, home_team, year);
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
     * @return game list
     */
    public ArrayList<Game> getLastQueryList() {
        return mGameList;
    }

    /**
     * get score board html
     *
     * @return score board html
     */
    public String getScoreBoardHtml() {
        String html = (mScoreBoardHtml != null) ? mScoreBoardHtml.replace("href", "_href") : "";
        return html.replace("width=173", "width=100%% bgcolor=grey");
    }


    /**
     * build a leader board dialog
     *
     * @param context Context
     * @param html    html content
     * @param title   dialog title
     * @return a leader board dialog
     */
    public static AlertDialog buildLeaderBoardDialog(Context context, String html, String title) {
        //[42]dolphin++ WindowManager$BadTokenException reported @ 2013-07-23
        AlertDialog dialog = new AlertDialog.Builder(context).create();
        //dialog.setTitle(mSpinnerKind.getItemAtPosition(1).toString());
        dialog.setButton(AlertDialog.BUTTON_POSITIVE, context.getString(android.R.string.ok),
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        // do nothing, just dismiss
                    }
                }
        );
        // How to display the Html formatted text in the PopUp box
        // using alert dialog builder?
        // http://stackoverflow.com/a/8641399
        //dialog.setMessage(Html.fromHtml(mHelper.getScoreBoardHtml()));

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
        dialog.setView(view);//webView
        dialog.show();

        //set button style as holo green light
        View btOk = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        if (btOk != null) {
            btOk.setBackgroundResource(R.drawable.item_background_holo_light);
        }
        return dialog;
    }
}
