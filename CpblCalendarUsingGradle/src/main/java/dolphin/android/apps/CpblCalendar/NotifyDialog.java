package dolphin.android.apps.CpblCalendar;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import dolphin.android.apps.CpblCalendar.preference.PreferenceUtils;
import dolphin.android.apps.CpblCalendar.provider.Game;

/**
 * Created by dolphin on 2013/9/1.
 */
public class NotifyDialog extends Activity {

    public final static String EXTRA_GAME1 = "game1";

    public final static String EXTRA_GAME2 = "game2";

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (PreferenceUtils.isEngineerMode(this)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        }

        setContentView(R.layout.activity_notify_dialog);

        View button1 = findViewById(android.R.id.button1);
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NotificationManager mNotifyMgr =
                        (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                mNotifyMgr.cancelAll();//[51]dolphin++ clear all notifications
                NotifyDialog.this.finish();
            }
        });

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            final Game game1 = bundle.containsKey(EXTRA_GAME1)
                    ? Game.fromPrefString(this, bundle.getString(EXTRA_GAME1)) : null;
            final Game game2 = bundle.containsKey(EXTRA_GAME2)
                    ? Game.fromPrefString(this, bundle.getString(EXTRA_GAME2)) : null;

            new android.os.Handler().post(new Runnable() {
                @Override
                public void run() {
                    load_layout(game1, game2);
                }
            });
        }
    }

    private void load_layout(Game game1, Game game2) {
        ViewGroup layout1 = (ViewGroup) findViewById(R.id.layout1);
        ViewGroup layout2 = (ViewGroup) findViewById(R.id.layout2);
        boolean bIsTablet = getResources().getBoolean(R.bool.config_tablet);
        boolean bShowLogo = PreferenceUtils.isTeamLogoShown(this);

        if (game1 != null) {
            updateMatchUp(layout1, game1, bIsTablet, bShowLogo);
        } else {
            layout1.setVisibility(View.GONE);
        }

        if (game2 != null) {
            updateMatchUp(layout2, game2, bIsTablet, bShowLogo);
        } else {
            findViewById(R.id.separator).setVisibility(View.GONE);
            layout2.setVisibility(View.GONE);
        }

        int alarmTime = PreferenceUtils.getAlarmNotifyTime(getBaseContext());
        boolean isVibrate = PreferenceUtils.isEnableNotifyVibrate(getBaseContext());
//        EasyTracker easyTracker = EasyTracker.getInstance(NotifyDialog.this);
//        if (easyTracker != null) {
//            easyTracker.send(MapBuilder.createEvent("UI",//Category
//                            "NotifyDialog",//Action (required)
//                            String.format("before=%d, vibrate=%s",
//                                    alarmTime, isVibrate),//label
//                            null
//                    ).build()
//            );
//        }
        sendGmsGoogleAnalyticsReport("UI", "NotifyDialog",
                String.format("before=%d, vibrate=%s", alarmTime, isVibrate));
    }

    private void updateMatchUp(ViewGroup convertView, Game game,
            boolean bIsTablet, boolean bShowLogo) {
        convertView.findViewById(R.id.textView3).setVisibility(View.GONE);
        convertView.findViewById(R.id.textView4).setVisibility(View.GONE);
        convertView.findViewById(R.id.textView8).setVisibility(View.GONE);
        convertView.findViewById(android.R.id.icon).setVisibility(View.GONE);
        convertView.findViewById(R.id.icon_alarm).setVisibility(View.GONE);

        TextView tv1 = (TextView) convertView.findViewById(R.id.textView1);
        Calendar c = game.StartTime;
        tv1.setText(String.format("%s, %s",
                //date //[47] use Taiwan only, add tablet DAY_OF_WEEK
                new SimpleDateFormat(bIsTablet ? "MMM dd (E)" : "MMM dd",
                        Locale.TAIWAN).format(c.getTime()),//[47]dolphin++
                //time
                DateFormat.getTimeFormat(this).format(c.getTime())
        ));

        TextView tv2 = (TextView) convertView.findViewById(R.id.textView2);
        tv2.setText(game.AwayTeam.getShortName());
        TextView tv5 = (TextView) convertView.findViewById(R.id.textView5);
        tv5.setText(game.HomeTeam.getShortName());

        TextView tv6 = (TextView) convertView.findViewById(R.id.textView6);
        if (game.Channel != null) {
            tv6.setVisibility(View.VISIBLE);
            tv6.setText(game.Channel);
        } else {
            tv6.setVisibility(View.GONE);
        }

        TextView tv7 = (TextView) convertView.findViewById(R.id.textView7);
        tv7.setText(game.Field);

        TextView tv9 = (TextView) convertView.findViewById(R.id.textView9);
        tv9.setText(String.valueOf(game.Id));//game number as id

        //team logo
        ImageView ic1 = (ImageView) convertView.findViewById(android.R.id.icon1);
        ic1.setImageResource(game.AwayTeam.getLogo(game.StartTime.get(Calendar.YEAR)));
        ic1.setVisibility(bShowLogo ? View.VISIBLE : View.GONE);
        ImageView ic2 = (ImageView) convertView.findViewById(android.R.id.icon2);
        ic2.setImageResource(game.HomeTeam.getLogo(game.StartTime.get(Calendar.YEAR)));
        ic2.setVisibility(bShowLogo ? View.VISIBLE : View.GONE);
    }

    //
//    @Override
//    public void onStart() {
//        super.onStart();
//        //... // The rest of your onStart() code.
//        EasyTracker.getInstance(this).activityStart(this);
//    }
//
//    @Override
//    public void onStop() {
//        super.onStop();
//        //... // The rest of your onStop() code.
//        EasyTracker.getInstance(this).activityStop(this);
//    }
    protected void sendGmsGoogleAnalyticsReport(String category, String action, String label) {
        // Get tracker.
        Tracker t = ((CpblApplication) getApplication()).getTracker(
                CpblApplication.TrackerName.APP_TRACKER);

        // Set screen name.
        // Where path is a String representing the screen name.
        t.setScreenName("dolphin.android.apps.CpblCalendar.NotifyDialog");

        // Send a screen view.
        t.send(new HitBuilders.AppViewBuilder().build());

        // This event will also be sent with &cd=Home%20Screen.
        // Build and send an Event.
        t.send(new HitBuilders.EventBuilder()
                .setCategory(category)
                .setAction(action)
                .setLabel(label)
                .build());

        // Clear the screen name field when we're done.
        t.setScreenName(null);
    }
}
