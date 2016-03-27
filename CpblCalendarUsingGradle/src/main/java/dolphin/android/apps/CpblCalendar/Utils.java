package dolphin.android.apps.CpblCalendar;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import dolphin.android.apps.CpblCalendar.preference.PreferenceUtils;
import dolphin.android.apps.CpblCalendar.provider.CpblCalendarHelper;
import dolphin.android.apps.CpblCalendar.provider.Game;
import dolphin.android.apps.CpblCalendar.provider.Stand;
import dolphin.android.apps.CpblCalendar.provider.Team;

/**
 * Created by dolphin on 2015/02/07.
 * <p/>
 * Collection of utils in CalendarActivity
 */
public class Utils {
    public static void enableStrictMode() {
        // http://goo.gl/cmG1V , solve android.os.NetworkOnMainThreadException
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork() // or .detectAll() for all detectable problems
                .penaltyLog()
                .permitDiskWrites()//[19]dolphin++
                .permitDiskReads()//[19]dolphin++
                .permitNetwork()//[101]dolphin++
                .build());
    }

    /**
     * generate a debug list for offline test
     */
    public static ArrayList<Game> get_debug_list(Context context, int year, int month) {
        //Log.d(TAG, "get_debug_list");
        ArrayList<Game> gameList = new ArrayList<Game>();
        for (int i = 0; i < 30; i++) {
            Game game = new Game();
            game.IsFinal = ((i % 3) != 0);
            game.Id = month + i + (year % 100);
            game.HomeTeam = new Team(context, Team.ID_EDA_RHINOS);
            game.AwayTeam = new Team(context, Team.ID_LAMIGO_MONKEYS);
            game.Field = context.getString(R.string.title_at) + "somewhere";
            game.StartTime = CpblCalendarHelper.getNowTime();
            game.StartTime.set(Calendar.HOUR_OF_DAY, 18);
            game.StartTime.add(Calendar.DAY_OF_YEAR, i - 3);
            game.AwayScore = (game.StartTime.get(Calendar.MILLISECOND) % 20);
            game.HomeScore = (game.StartTime.get(Calendar.SECOND) % 20);
            game.DelayMessage = ((i % 4) == 1) ? "wtf rainy day" : "";
            gameList.add(game);
        }
        return gameList;
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

    public static AlertDialog buildLeaderBoardZxc22(Context context) {
        final AlertDialog dialog = new AlertDialog.Builder(context).create();
        //change the style like the entire theme
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.leader_board, null);
        TextView textView = (TextView) view.findViewById(android.R.id.title);
        if (textView != null) {
            textView.setText(String.format("%s %s", context.getString(R.string.title_zxc22),
                    context.getString(R.string.summary_zxc22)));
        }

        WebView webView = (WebView) view.findViewById(R.id.webView);
        webView.getSettings().setJavaScriptEnabled(false);
        webView.getSettings().setSupportZoom(false);
        webView.loadUrl("http://zxc22.idv.tw/rank_up.asp");

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

        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        int width = (int) (display.getWidth() * .9);
        width = width > 1600 ? 1600 : width;
        int height = (int) (display.getHeight() * .9);
        Log.d("CpblCalendarHelper", String.format("w=%d, h=%d", width, height));
        dialog.getWindow().setLayout(width, height);
        return dialog;
    }

    /**
     * build cache mode dialog
     *
     * @param context  Context
     * @param listener positive button click listener
     * @return cache mode dialog
     */
    public static AlertDialog buildEnableCacheModeDialog(Context context,
                                                         DialogInterface.OnClickListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context).setCancelable(true)
                .setMessage(R.string.title_cache_mode_enable_message)
                .setTitle(R.string.action_cache_mode)
                .setPositiveButton(R.string.title_cache_mode_start, listener)
                .setNegativeButton(android.R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //do nothing
                            }
                        }
                );
        return builder.create();
    }

    private static boolean matchField(Context context, Game game, String field, String fieldId) {
        //String field = mSpinnerField.getSelectedItem().toString();
        boolean matched = game.Field.contains(field);
        //int fieldIndex = mSpinnerField.getSelectedItemPosition();
        //String fieldId = mGameField[fieldIndex];
        if (fieldId.equals("F19")) {
            matched |= game.Field.contains(context.getString(R.string.cpbl_game_field_name_F19));
        }
        if (fieldId.equals("F23")) {
            matched |= game.Field.contains(context.getString(R.string.cpbl_game_field_name_F23));
        }
        return matched;
    }

    public static ArrayList<Game> cleanUpGameList(Context context, ArrayList<Game> gameList,
                                                  int year, int fieldIndex) {
        if (context == null || context.getResources() == null || gameList == null) {
            return gameList;//don't do anything
        }
        final Resources resources = context.getResources();
        String fieldName = resources.getStringArray(R.array.cpbl_game_field_name)[fieldIndex];
        String fieldId = resources.getStringArray(R.array.cpbl_game_field_id)[fieldIndex];
        //[22]dolphin++ add check the favorite teams
        HashMap<Integer, Team> teams = PreferenceUtils.getFavoriteTeams(context);
        //2013 has 4 teams only, should I check this?
        //[89]dolphin++ only filter out this year, check array size
        if (teams.size() < resources.getStringArray(R.array.cpbl_team_id).length
                && year >= CpblCalendarHelper.getNowTime().get(Calendar.YEAR)) {
            for (Iterator<Game> i = gameList.iterator(); i.hasNext(); ) {
                Game game = i.next();
                if (!teams.containsKey(game.HomeTeam.getId())
                        && !teams.containsKey(game.AwayTeam.getId())) {
                    i.remove();//remove from the list
                }
            }
        }
        //check field data
        //Log.d(TAG, mSpinnerField.getSelectedItem().toString());
        if (year >= 2014 && fieldIndex > 0) {
            //String field = mSpinnerField.getSelectedItem().toString();
            for (Iterator<Game> i = gameList.iterator(); i.hasNext(); ) {
                //Game game = i.next();
                //if (!game.Field.contains(field)) {
                if (!matchField(context, i.next(), fieldName, fieldId)) {
                    i.remove();
                }
            }
        }
        return gameList;
    }

    private static boolean within3Days(Calendar c) {
        return ((c.getTimeInMillis() - System.currentTimeMillis()) <= BaseGameAdapter.ONE_DAY * 2);
    }

    //https://developer.chrome.com/multidevice/android/customtabs
    //https://github.com/GoogleChrome/custom-tabs-client
    public static final String EXTRA_CUSTOM_TABS_SESSION = "android.support.customtabs.extra.SESSION";
    public static final String EXTRA_CUSTOM_TABS_TOOLBAR_COLOR = "android.support.customtabs.extra.TOOLBAR_COLOR";

    /**
     * start browser activity
     *
     * @param context Context
     * @param game    target game
     */
    public static void startGameActivity(Context context, Game game) {
        Calendar now = CpblCalendarHelper.getNowTime();
        String url = game.Url;//null;
//        int year = game.StartTime.get(Calendar.YEAR);
//        if (game.StartTime.after(now)) {
//            url = within3Days(game.StartTime) ?
//                    String.format("%s/game/starters.aspx?gameno=%s&year=%d&game=%d",
//                            CpblCalendarHelper.URL_BASE, game.Kind, year, game.Id) : null;
//        } else if (game.IsFinal || PreferenceUtils.isCacheMode(context)) {
//            url = String.format("%s/game/box.aspx?gameno=%s&year=%d&game=%d",
//                    CpblCalendarHelper.URL_BASE, game.Kind, year, game.Id);
//        } else {
//            //url = game.Url;//default is live url
//            //Log.d(TAG, game.StartTime.getTime().toString());
//            //http://www.cpbl.com.tw/game/playbyplay.aspx?gameno=01&year=2015&game=133
//            url = String.format("%s/game/playbyplay.aspx?gameno=%s&year=%d&game=%d",
//                    CpblCalendarHelper.URL_BASE, game.Kind, year, game.Id);
//        }
        if (game.StartTime.after(now) && url.contains("box.html")) {
            url = null;
        }

        if (/*game != null && */url != null) {//[78]-- game.IsFinal) {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));

            //[164]++
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                //[164]dolphin++ add Chrome Custom Tabs
                Bundle extras = new Bundle();
                extras.putBinder(EXTRA_CUSTOM_TABS_SESSION, null);
                extras.putInt(EXTRA_CUSTOM_TABS_TOOLBAR_COLOR,
                        context.getResources().getColor(R.color.holo_green_dark));
                i.putExtras(extras);
            } else {
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }

            if (PreferenceUtils.isEngineerMode(context)) {
                Log.d("CpblCalendarHelper", "Url=" + url.substring(url.lastIndexOf("/")));
            } else {
                try {//[97]dolphin++
                    context.startActivity(i);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(context, R.string.query_error, Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    /**
     * Check if Google Chrome is installed.
     *
     * @param context Context
     * @return true if installed
     */
    public static boolean isGoogleChromeInstalled(Context context) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(CpblCalendarHelper.URL_BASE));
        List<ResolveInfo> list = context.getPackageManager().queryIntentActivities(intent, 0);
        if (list != null && list.size() > 0) {
            for (ResolveInfo resolveInfo : list) {
                Log.d("CpblCalendarHelper", resolveInfo.activityInfo.packageName);
                if (resolveInfo.activityInfo.packageName.startsWith("com.android.chrome")) {
                    return true;
                }
            }
        }
        return false;
    }
}
