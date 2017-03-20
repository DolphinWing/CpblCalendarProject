package dolphin.android.apps.CpblCalendar;

import android.content.Context;
import android.graphics.PorterDuff;
import android.os.Build;
import android.text.Html;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import dolphin.android.apps.CpblCalendar.preference.AlarmHelper;
import dolphin.android.apps.CpblCalendar.preference.PreferenceUtils;
import dolphin.android.apps.CpblCalendar.provider.AlarmProvider;
import dolphin.android.apps.CpblCalendar.provider.CpblCalendarHelper;
import dolphin.android.apps.CpblCalendar.provider.Game;
import dolphin.android.apps.CpblCalendar.provider.TeamHelper;

/**
 * Created by dolphin on 2014/7/5.
 * <p/>
 * game adapter implementation
 */
public class GameAdapter extends BaseGameAdapter {
    private final CpblApplication mApplication;
    private final TeamHelper mTeamHelper;

    public GameAdapter(Context context, List<Game> objects, CpblApplication application) {
        super(context, objects);
        mApplication = application;
        mTeamHelper = new TeamHelper(mApplication);
    }

    @Override
    protected void decorate(View convertView, Game game) {
        super.decorate(convertView, game);

        //url indicator
        ImageView iv1 = (ImageView) convertView.findViewById(android.R.id.icon);
        iv1.setVisibility(View.INVISIBLE);

        //delay message
        TextView tv8 = (TextView) convertView.findViewById(R.id.textView8);
        if (game.DelayMessage != null && game.DelayMessage.length() > 0) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                tv8.setText(Html.fromHtml(game.DelayMessage,
                        Html.TO_HTML_PARAGRAPH_LINES_INDIVIDUAL));
            } else {
                tv8.setText(Html.fromHtml(game.DelayMessage));
            }
            tv8.setVisibility(View.VISIBLE);
        } else {
            tv8.setVisibility(View.GONE);
        }

        //team logo
        int year = game.StartTime.get(Calendar.YEAR);
        //Log.d("GameAdapter", String.format("year = %d", year));
        ImageView awayLogo = (ImageView) convertView.findViewById(android.R.id.icon1);
        if (awayLogo != null) {
            awayLogo.setImageResource(R.drawable.ic_baseball);
            awayLogo.setColorFilter(mTeamHelper.getLogoColorFilter(game.AwayTeam, year),
                    PorterDuff.Mode.SRC_IN);
        }
        ImageView homeLogo = (ImageView) convertView.findViewById(android.R.id.icon2);
        if (homeLogo != null) {
            homeLogo.setImageResource(R.drawable.ic_baseball);
            homeLogo.setColorFilter(mTeamHelper.getLogoColorFilter(game.HomeTeam, year),
                    PorterDuff.Mode.SRC_IN);
        }
    }

//    private int getPalettePrimaryColor(Palette palette) {
//        int color = Color.BLACK;
//        if (palette == null) {
//            return color;
//        }
//        if (palette.getVibrantSwatch() != null) {
//            color = palette.getVibrantColor(color);
//        }
//        if (color == Color.BLACK && palette.getMutedSwatch() != null) {
//            color = palette.getMutedColor(color);
//        }
//        return color;
//    }

    @Override
    protected int getLayoutResId(Game game) {
        return R.layout.listview_item_matchup;
    }

    @Override
    protected boolean supportLongName(Game game) {
        return true;//always long name
    }

    @Override
    protected void setAlarm(Game game) {
        //AlarmProvider.setNextAlarm(mApplication);
        Context context = getContext();
        ArrayList<Game> list = getAlarmHelper().getAlarmList();
        if (!list.isEmpty()) {//something in the list, check the start time
            for (Game g : list) {
                if (g.Id == game.Id) {
                    continue;//bypass myself
                }
                if (g.StartTime.compareTo(game.StartTime) == 0) {//same day
                    Log.v(AlarmHelper.TAG, String.format("same day: %d %d", g.Id, game.Id));
                    return;
                }
            }
        }

        Calendar alarm = game.StartTime;
        alarm.add(Calendar.MINUTE, -PreferenceUtils.getAlarmNotifyTime(context));
        if (context.getResources().getBoolean(R.bool.demo_notification)) {//debug alarm
            alarm = CpblCalendarHelper.getNowTime();
            //already stored in the map, so use the count to do the demo
            alarm.add(Calendar.SECOND, 15 * list.size());
            //Log.d(AlarmHelper.TAG, "demo alarm: " + alarm.getTime().toString());
            //alarm.add(Calendar.MINUTE, 10);
        }
        String key = AlarmHelper.getAlarmIdKey(game);
        AlarmProvider.setAlarm(mApplication, alarm, key);
    }

    @Override
    protected void cancelAlarm(Game game) {
        String key = AlarmHelper.getAlarmIdKey(game);
        Game anotherGame = null;
        //check if we have the same day job
        ArrayList<Game> list = getAlarmHelper().getAlarmList();
        if (!list.isEmpty()) {//cancel the same day
            for (Game g : list) {
                if (g.Id == game.Id) {
                    continue;//bypass myself
                }
                if (g.StartTime.compareTo(game.StartTime) == 0) {//same day
                    Log.v(AlarmHelper.TAG, String.format("same day: %d %d", g.Id, game.Id));
                    int previousJobId = getAlarmHelper().getJobId(key);
                    if (previousJobId > 0) {//yes, previous registered
                        //Log.d(AlarmHelper.TAG, "yes, previous registered: " + previousJobId);
                        anotherGame = g;
                    }
                }
            }
        }

        AlarmProvider.cancelAlarm(mApplication, key);
        if (anotherGame != null) {//set alarm for another game
            Log.v(AlarmHelper.TAG, "set alarm for another game " + anotherGame.Id);
            setAlarm(anotherGame);
        }
    }

    public static void updateNotifyDialogMatchUp(Context context, ViewGroup convertView, Game game,
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
                new SimpleDateFormat(bIsTablet ? "MMM d (E)" : "MMM d",
                        Locale.TAIWAN).format(c.getTime()),//[47]dolphin++
                //time
                DateFormat.getTimeFormat(context).format(c.getTime())
        ));

        TextView tv2 = (TextView) convertView.findViewById(R.id.textView2);
        tv2.setText(game.AwayTeam.getName());
        TextView tv5 = (TextView) convertView.findViewById(R.id.textView5);
        tv5.setText(game.HomeTeam.getName());

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
}
