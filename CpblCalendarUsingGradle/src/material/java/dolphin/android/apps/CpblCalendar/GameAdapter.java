package dolphin.android.apps.CpblCalendar;

import android.content.Context;
import android.text.Html;
import android.text.format.DateFormat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import dolphin.android.apps.CpblCalendar.provider.Game;

/**
 * Created by dolphin on 2014/7/5.
 * <p/>
 * game adapter implementation
 */
public class GameAdapter extends BaseGameAdapter {

    public GameAdapter(Context context, List<Game> objects) {
        super(context, objects);
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
            tv8.setText(Html.fromHtml(game.DelayMessage));
            tv8.setVisibility(View.VISIBLE);
        } else {
            tv8.setVisibility(View.GONE);
        }
    }

    @Override
    protected int getLayoutResId(Game game) {
        return R.layout.listview_item_matchup;
    }

    @Override
    protected boolean supportLongName(Game game) {
        return true;//always long name
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
