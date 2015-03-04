package dolphin.android.apps.CpblCalendar;

import android.content.Context;
import android.text.Html;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

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

//        View homeView = convertView.findViewById(R.id.home_team);
//        View awayView = convertView.findViewById(R.id.away_team);
//        if (isShowWinner()) {
//            if (game.HomeScore > game.AwayScore) {
//                homeView.setBackgroundResource(R.drawable.ab_transparent_holo_green);
//                awayView.setBackgroundResource(android.R.color.transparent);
//            } else {
//                homeView.setBackgroundResource(android.R.color.transparent);
//                awayView.setBackgroundResource(R.drawable.ab_transparent_holo_green);
//            }
//        } else {
//            homeView.setBackgroundResource(android.R.color.transparent);
//            awayView.setBackgroundResource(android.R.color.transparent);
//        }
    }

    @Override
    protected int getLayoutResId(Game game) {
        return R.layout.listview_item_matchup;
    }

    @Override
    protected boolean supportLongName(Game game) {
        return true;//always long name
    }
}
