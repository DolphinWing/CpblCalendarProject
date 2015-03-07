package dolphin.android.apps.CpblCalendar;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.io.File;
import java.io.FileDescriptor;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dolphin.android.apps.CpblCalendar.provider.AspNetHelper;
import dolphin.android.apps.CpblCalendar.provider.CpblCalendarHelper;
import dolphin.android.apps.CpblCalendar.provider.Game;
import dolphin.android.net.GoogleDriveHelper;
import dolphin.android.net.HttpProgressListener;


public class DelayGameActivity extends Activity {
    private final static String TAG = "DelayGame";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delay_game);

        Utils.enableStrictMode();

        View button1 = findViewById(android.R.id.button1);
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SparseArray<Game> games = queryDelayGame();
                Log.d(TAG, "total delay games: " + games.size());
            }
        });

        View button2 = findViewById(android.R.id.button2);
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                downloadFile();
            }
        });
    }

    private SparseArray<Game> queryDelayGame() {
        long startTime = System.currentTimeMillis();

        SparseArray<Game> delayedGames = new SparseArray<>();
        int year = 2014;
        for (int month = 3; month <= 10; month++) {
            String html;// = getUrlContent(URL_SCHEDULE_2014);
            AspNetHelper helper = new AspNetHelper("http://www.cpbl.com.tw/schedule.aspx");
            html = helper.makeRequest("ctl00$cphBox$ddl_year", String.valueOf(year));
            html = helper.makeRequest("ctl00$cphBox$ddl_month", String.format("/%d/1", month));

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
                                if (game1.contains("<td colspan='3' class='suspend'>")) {
                                    Log.v(TAG, String.format("suspend month=%d day=%d", month, d));
                                    //continue;//don't add to list
                                    //TODO: we need these games

                                    Matcher m = Pattern.compile("<th>([0-9]+)</th>").matcher(game1);
                                    if (m.find()) {
                                        Log.d(TAG, "  id = " + m.group(1));
                                        Game game = new Game();
                                        game.Id = Integer.parseInt(m.group(1));
                                        game.StartTime = CpblCalendarHelper.getNowTime();
                                        game.StartTime.set(Calendar.YEAR, year);
                                        game.StartTime.set(Calendar.MONTH, month - 1);
                                        game.StartTime.set(Calendar.DAY_OF_MONTH, d);

                                        if (delayedGames.get(game.Id) != null) {
                                            Log.d(TAG, "bypass " + game.toString());
                                            continue;
                                        }
                                        delayedGames.put(game.Id, game);
                                    }
                                }
                                //normal games
                            }
                        }
                    }
                }
            }
        }
        long endTime = System.currentTimeMillis();
        Log.v(TAG, String.format("delay game query wasted %d ms", ((endTime - startTime))));
        return delayedGames;
    }

    private void downloadFile() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                doDownloadFile();
            }
        }).start();
    }

    private void doDownloadFile() {
        Log.d(TAG, "start download...");
        File f = new File(getCacheDir(), "test.ogg");
        //https://drive.google.com/file/d/0B-oMP4622t0hbFlrZTlpaXN4SUk/view?usp=sharing
        boolean r = GoogleDriveHelper.download(this, "0B-oMP4622t0hbFlrZTlpaXN4SUk", f, mListener);
        if (r) {
            Log.d(TAG, f.getAbsolutePath());
        } else {
            Log.e(TAG, "gg!");
        }
    }

    private HttpProgressListener mListener = new HttpProgressListener() {
        private long startTime;

        @Override
        public void onStart(long contentSize) {
            startTime = System.currentTimeMillis();
            Log.d(TAG, String.format("onStart: size = %d", contentSize));
        }

        @Override
        public void onUpdate(int bytes, long totalBytes) {
            Log.d(TAG, String.format("onUpdate: byte = %d, total = %d", bytes, totalBytes));
        }

        @Override
        public void onComplete(long totalBytes) {
            Log.d(TAG, String.format("onComplete: size = %d, cost %d ms", totalBytes,
                    System.currentTimeMillis() - startTime));
        }
    };
}
