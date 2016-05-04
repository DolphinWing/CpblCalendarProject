package dolphin.android.apps.CpblCalendar;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.TextView;

import dolphin.android.apps.CpblCalendar.provider.CpblCalendarHelper;
import dolphin.android.apps.CpblCalendar.provider.Game;

/**
 * Created by dolphin on 2016/03/26.
 * Dump data test
 */
public class TestActivity extends Activity implements View.OnClickListener {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        Utils.enableStrictMode();

        View button1 = findViewById(android.R.id.button1);
        if (button1 != null) {
            button1.setOnClickListener(this);
        }

        View button2 = findViewById(android.R.id.button2);
        if (button2 != null) {
            button2.setOnClickListener(this);
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case android.R.id.button1:
                queryDelayGames();
                break;
            case android.R.id.button2:
                readDelayGamesFromCache();
                break;
        }
    }

    private void queryDelayGames() {
        final int year = 2015;
        new Thread(new Runnable() {
            @Override
            public void run() {
                CpblCalendarHelper helper = new CpblCalendarHelper(getBaseContext());
                SparseArray<Game> list = helper.queryDelayGames2016(year, false, false);
                helper.storeDelayGames2016(year, list);
            }
        }).start();
    }

    private void readDelayGamesFromCache() {
        CpblCalendarHelper helper = new CpblCalendarHelper(this);
        SparseArray<Game> delayedGames = helper.restoreDelayGames2016(2015);

        String dataList = "";
        for (int i = 0; i < delayedGames.size(); i++) {
            dataList += delayedGames.valueAt(i).toString() + "\n";
        }

        TextView text1 = (TextView) findViewById(android.R.id.text1);
        if (text1 != null) {
            text1.setText(dataList);
        }
    }
}
