package dolphin.android.apps.CpblCalendar;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Html;
import android.text.SpannableString;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import dolphin.android.apps.CpblCalendar.preference.AlarmHelper;
import dolphin.android.apps.CpblCalendar.preference.PreferenceUtils;
import dolphin.android.apps.CpblCalendar.provider.CpblCalendarHelper;
import dolphin.android.apps.CpblCalendar.provider.Game;

/**
 * Created by dolphin on 2014/7/5.
 *
 * game adapter implementation
 */
public class GameAdapter extends BaseGameAdapter {

    public GameAdapter(Context context, List<Game> objects) {
        super(context, objects);
    }

    @Override
    protected int getLayoutResId(Game game) {
        boolean bIsLongName = game.Kind != null && game.Kind.equalsIgnoreCase("09");
        return bIsLongName ? R.layout.listview_item_matchup_09 : R.layout.listview_item_matchup;
    }

    @Override
    protected boolean supportLongName(Game game) {
        return game.Kind.equalsIgnoreCase("09");
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
                new SimpleDateFormat(bIsTablet ? "MMM dd (E)" : "MMM dd",
                        Locale.TAIWAN).format(c.getTime()),//[47]dolphin++
                //time
                DateFormat.getTimeFormat(context).format(c.getTime())
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
}
