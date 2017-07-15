package dolphin.android.apps.CpblCalendar;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.StrictMode;
import android.provider.CalendarContract;
import android.support.customtabs.CustomTabsIntent;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import dolphin.android.apps.CpblCalendar.preference.PreferenceUtils;
import dolphin.android.apps.CpblCalendar.provider.CpblCalendarHelper;
import dolphin.android.apps.CpblCalendar.provider.Game;
import dolphin.android.apps.CpblCalendar.provider.Team;
import dolphin.android.apps.CpblCalendar3.R;

/**
 * Created by dolphin on 2015/02/07.
 * <p/>
 * Collection of utils in CalendarActivity
 */
public class Utils {
    static void enableStrictMode() {
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
    static ArrayList<Game> get_debug_list(Context context, int year, int month) {
        //Log.d(TAG, "get_debug_list");
        ArrayList<Game> gameList = new ArrayList<>();
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

    //http://zxc22.idv.tw/rank_up.asp
    private final static String LEADER_BOARD_URL = "http://www.cpbl.com.tw/standing/season/";
    public final static Uri LEADER_BOARD_URI = Uri.parse(LEADER_BOARD_URL);

    @SuppressLint("InflateParams")
    public static AlertDialog buildLeaderBoardZxc22(Context context) {
        if (context == null) {
            return null;
        }

        final AlertDialog dialog = new AlertDialog.Builder(context).create();
        //change the style like the entire theme
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.leader_board, null);
        TextView textView = (TextView) view.findViewById(android.R.id.title);
        if (textView != null) {
            //textView.setText(String.format("%s %s", context.getString(R.string.title_zxc22),
            //        context.getString(R.string.summary_zxc22)));
            textView.setText(R.string.action_leader_board);
        }

        WebView webView = (WebView) view.findViewById(R.id.webView);
        webView.getSettings().setJavaScriptEnabled(false);
        webView.getSettings().setSupportZoom(false);
        webView.loadUrl(LEADER_BOARD_URL);

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

        //http://stackoverflow.com/a/15847580/2673859
        DisplayMetrics display = context.getResources().getDisplayMetrics();
        int width = (int) (display.widthPixels * (display.widthPixels > 1200 ? .8 : .95));
        width = width > 1600 ? 1600 : width;
        int height = (int) (display.heightPixels * .9);
        //height = height < 480 ? 480 : height;
        //Log.d("CpblCalendarHelper", String.format("w=%d, h=%d", width, height));
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(width, height);
        }
        return dialog;
    }

    /**
     * build cache mode dialog
     *
     * @param context  Context
     * @param listener positive button click listener
     * @return cache mode dialog
     */
    static AlertDialog buildEnableCacheModeDialog(Context context,
                                                  DialogInterface.OnClickListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context).setCancelable(true)
                .setMessage(R.string.title_cache_mode_enable_message)
                .setTitle(R.string.action_cache_mode)
                .setPositiveButton(R.string.title_cache_mode_start, listener)
                .setNegativeButton(R.string.title_cache_mode_cancel,
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

    static ArrayList<Game> cleanUpGameList(Context context, ArrayList<Game> gameList,
                                           int year, int fieldIndex) {
        if (context == null || context.getResources() == null || gameList == null) {
            return gameList;//don't do anything
        }
        final Resources resources = context.getResources();
        String fieldName = resources.getStringArray(R.array.cpbl_game_field_name)[fieldIndex];
        String fieldId = resources.getStringArray(R.array.cpbl_game_field_id)[fieldIndex];
        //[22]dolphin++ add check the favorite teams
        SparseArray<Team> teams = PreferenceUtils.getFavoriteTeams(context);
        //2013 has 4 teams only, should I check this?
        //[89]dolphin++ only filter out this year, check array size
        if (teams.size() < resources.getStringArray(R.array.cpbl_team_id).length
                && year >= CpblCalendarHelper.getNowTime().get(Calendar.YEAR)) {
            for (Iterator<Game> i = gameList.iterator(); i.hasNext(); ) {
                Game game = i.next();
                if (teams.get(game.HomeTeam.getId()) == null
                        && teams.get(game.AwayTeam.getId()) == null) {
                    //if (!teams.containsKey(game.HomeTeam.getId())
                    //        && !teams.containsKey(game.AwayTeam.getId())) {
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

//    private static boolean within3Days(Calendar c) {
//        return ((c.getTimeInMillis() - System.currentTimeMillis()) <= BaseGameAdapter.ONE_DAY * 2);
//    }

    public static boolean passed1Day(Calendar c) {
        return (System.currentTimeMillis() - c.getTimeInMillis() >= BaseGameAdapter.ONE_DAY);
    }

//    //https://developer.chrome.com/multidevice/android/customtabs
//    //https://github.com/GoogleChrome/custom-tabs-client
//    private static final String EXTRA_CUSTOM_TABS_SESSION = "android.support.customtabs.extra.SESSION";
//    private static final String EXTRA_CUSTOM_TABS_TOOLBAR_COLOR = "android.support.customtabs.extra.TOOLBAR_COLOR";

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

        if (/*game != null &&*/ url != null) {//[78]-- game.IsFinal) {
//            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
//
//            //[164]++
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
//                //[164]dolphin++ add Chrome Custom Tabs
//                Bundle extras = new Bundle();
//                extras.putBinder(EXTRA_CUSTOM_TABS_SESSION, null);
//                extras.putInt(EXTRA_CUSTOM_TABS_TOOLBAR_COLOR,
//                        context.getResources().getColor(R.color.holo_green_dark));
//                i.putExtras(extras);
//            } else {
//                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//            }

            if (PreferenceUtils.isEngineerMode(context)) {
                Log.d("CpblCalendarHelper", "Url=" + url.substring(url.lastIndexOf("/")));
            } else {
                startBrowserActivity(context, url);
            }
        }
    }

    /**
     * start a browser activity
     *
     * @param context Context
     * @param url     url
     */
    public static void startBrowserActivity(Context context, String url) {
        if (context == null) {
            Log.e("CpblCalendarHelper", "no Context, no Activity");
            return;
        }

//        Intent intent = new Intent(Intent.ACTION_VIEW);
//        intent.setData(Uri.parse(url));
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
//            //[169]dolphin++ add Chrome Custom Tabs
//            Bundle extras = new Bundle();
//            extras.putBinder(Utils.EXTRA_CUSTOM_TABS_SESSION, null);
//            extras.putInt(Utils.EXTRA_CUSTOM_TABS_TOOLBAR_COLOR,
//                    ContextCompat.getColor(context, R.color.holo_green_dark));
//            intent.putExtras(extras);
////            if (!isGoogleChromeInstalled(context)) {//for non-chrome app
////                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
////            }
//        } else {
//            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        }
//
//        try {//[97]dolphin++
//            context.startActivity(intent);
//        } catch (ActivityNotFoundException e) {
//            Toast.makeText(context, R.string.query_error, Toast.LENGTH_SHORT).show();
//        }

        //https://goo.gl/GtBKgp
        CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder()
                .setToolbarColor(ContextCompat.getColor(context, R.color.holo_green_dark));
        CustomTabsIntent customTabsIntent = builder.build();
        customTabsIntent.launchUrl(context, Uri.parse(url));
    }

    /**
     * Check if Google Chrome is installed.
     *
     * @param context Context
     * @return true if installed
     */
    @SuppressWarnings("unused")
    public static boolean isGoogleChromeInstalled(Context context) {
        if (context == null) {
            return false;
        }

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(CpblCalendarHelper.URL_BASE));
        List<ResolveInfo> list = context.getPackageManager().queryIntentActivities(intent, 0);
        if (list != null && list.size() > 0) {
            for (ResolveInfo resolveInfo : list) {
                //Log.d("CpblCalendarHelper", resolveInfo.activityInfo.packageName);
                if (resolveInfo.activityInfo.packageName.startsWith("com.android.chrome")) {
                    return true;
                }
            }
        }
        return false;
    }

    //http://gunhansancar.com/change-language-programmatically-in-android/
    @SuppressWarnings("WeakerAccess")
    public static Context onAttach(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return updateResources(context, "zh_TW");
        }
        return updateResourcesLegacy(context, "zh_TW");
    }

    //http://gunhansancar.com/change-language-programmatically-in-android/
    @SuppressWarnings("SameParameterValue")
    @TargetApi(Build.VERSION_CODES.N)
    private static Context updateResources(Context context, String language) {
        Locale locale = new Locale(language);
        if (language.contains("_")) {//http://blog.xuite.net/saso0704/wretch/379638793
            String[] s = language.split("_");
            locale = new Locale(s[0], s[1]);
        }
        Locale.setDefault(locale);

        Configuration configuration = context.getResources().getConfiguration();
        configuration.setLocale(locale);
        configuration.setLayoutDirection(locale);

        return context.createConfigurationContext(configuration);
    }

    //http://gunhansancar.com/change-language-programmatically-in-android/
    @SuppressLint("ObsoleteSdkInt")
    @SuppressWarnings({"deprecation", "SameParameterValue"})
    private static Context updateResourcesLegacy(Context context, String language) {
        Locale locale = new Locale(language);
        if (language.contains("_")) {//http://blog.xuite.net/saso0704/wretch/379638793
            String[] s = language.split("_");
            locale = new Locale(s[0], s[1]);
        }
        Locale.setDefault(locale);

        Resources resources = context.getResources();

        Configuration configuration = resources.getConfiguration();
        configuration.locale = locale;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            configuration.setLayoutDirection(locale);
        }

        resources.updateConfiguration(configuration, resources.getDisplayMetrics());

        return context;
    }

    public static Intent createAddToCalendarIntent(Context context, Game game) {
        Intent calIntent = new Intent(Intent.ACTION_INSERT);
        calIntent.setData(CalendarContract.Events.CONTENT_URI);
        calIntent.setType("vnd.android.cursor.item/event");

        String contentText = context.getString(R.string.msg_content_text,
                game.AwayTeam.getShortName(), game.HomeTeam.getShortName());
        calIntent.putExtra(CalendarContract.Events.TITLE, contentText);

        //FIXME: change to full game field name
        calIntent.putExtra(CalendarContract.Events.EVENT_LOCATION, game.Field);

        String description = "";
        if (game.DelayMessage != null && !game.DelayMessage.isEmpty()) {
            description = game.DelayMessage.replaceAll("&nbsp;", " ")
                    .replaceAll("<br>", "\n").replaceAll("<br />", "\n")
                    .replaceAll("<[^>]*>", "");
        }
        if (game.Channel != null && !game.Channel.isEmpty()) {
            description = description.isEmpty() ? game.Channel
                    : description.concat("\n").concat(game.Channel);
        }
        if (!description.isEmpty()) {
            calIntent.putExtra(CalendarContract.Events.DESCRIPTION, description);
        }

        long startTime = game.StartTime.getTimeInMillis();
        long scheduledEndTime = (long) (startTime + 3.5 * 60 * 60 * 1000);
        calIntent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startTime);
        calIntent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, scheduledEndTime);
        calIntent.putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, false);
        return calIntent;
    }
}
