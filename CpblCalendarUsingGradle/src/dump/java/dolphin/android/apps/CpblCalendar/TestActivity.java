package dolphin.android.apps.CpblCalendar;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

import dolphin.android.apps.CpblCalendar.provider.CpblCalendarHelper;
import dolphin.android.apps.CpblCalendar.provider.Game;

/**
 * Created by dolphin on 2016/03/26.
 * Dump data test
 */
public class TestActivity extends Activity implements View.OnClickListener {
    private int mYear = 2008;
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

        EditText text = (EditText) findViewById(android.R.id.edit);
        if (text != null) {
            text.setText(String.valueOf(mYear));
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
        EditText text = (EditText) findViewById(android.R.id.edit);
        if (text != null) {
            mYear = Integer.parseInt(text.getText().toString());
        }

        final int year = mYear;
        new Thread(new Runnable() {
            @Override
            public void run() {
                long start = System.currentTimeMillis();
                CpblCalendarHelper helper = new CpblCalendarHelper(getBaseContext());
                SparseArray<Game> list = helper.queryDelayGames2016(year, false, false);
                if (list != null) {
                    helper.storeDelayGames2016(year, list);
                }
                final long cost = System.currentTimeMillis() - start;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getBaseContext(), String.format(Locale.US,
                                "cost %d ms", cost), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).start();
    }

    private void readDelayGamesFromCache() {
        EditText text = (EditText) findViewById(android.R.id.edit);
        if (text != null) {
            mYear = Integer.parseInt(text.getText().toString());
        }

        CpblCalendarHelper helper = new CpblCalendarHelper(this);
        SparseArray<Game> delayedGames = helper.restoreDelayGames2016(mYear);

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
