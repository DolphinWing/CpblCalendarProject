package dolphin.android.apps.CpblCalendar3;

import android.content.Context;
import android.graphics.PorterDuff;
import android.os.Build;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import dolphin.android.apps.CpblCalendar.preference.PreferenceUtils;
import dolphin.android.apps.CpblCalendar.provider.Game;
import dolphin.android.apps.CpblCalendar.provider.TeamHelper;

/**
 * Created by jimmyhu on 2017/5/17.
 * <p>
 * A card for HighlightActivity
 * http://www.jcodecraeer.com/a/anzhuokaifa/androidkaifa/2015/0722/3214.html
 */

class GameCardAdapter extends RecyclerView.Adapter<GameCardAdapter.ViewHolder>
        implements View.OnClickListener {
    private final static int TYPE_RESULT = 1;
    private final static int TYPE_UPCOMING = 2;
    private final static int TYPE_LIVE = 3;
    private final static int TYPE_MORE = 10;

    private final Context mContext;
    private final ArrayList<Game> mGames;
    private final boolean mIsTablet;
    private final boolean mShowLogo;
    private final TeamHelper mTeamHelper;

    GameCardAdapter(Context context, ArrayList<Game> list, TeamHelper helper) {
        mContext = context;
        mGames = list;
        mIsTablet = mContext.getResources().getBoolean(R.bool.config_tablet);
        mShowLogo = PreferenceUtils.isTeamLogoShown(mContext);
        mTeamHelper = helper;
    }

    private boolean isTablet() {
        return mIsTablet;
    }

    private boolean isShowLogo() {
        return mShowLogo;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        int layoutId = -1;
        switch (viewType) {
            case TYPE_RESULT:
                layoutId = R.layout.recyclerview_item_matchup_final;
                break;
            case TYPE_LIVE:
                layoutId = R.layout.recyclerview_item_matchup;
                break;
            case TYPE_UPCOMING:
                layoutId = R.layout.recyclerview_item_matchup_upcoming;
                break;
            case TYPE_MORE:
                layoutId = R.layout.recyclerview_item_more;
                break;
        }
        if (parent != null && layoutId != -1) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(layoutId, parent,
                    false));
        }
        return null;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.Container.setOnClickListener(this);
        Game game = mGames.get(position);
        holder.Container.setTag(game);
        if (game.Id == -1) {
            return;//don't update UI
        }
        String date_str = new SimpleDateFormat("MMM d (E)", Locale.TAIWAN)
                .format(game.StartTime.getTime());
        String time_str = new SimpleDateFormat("HH:mm", Locale.TAIWAN)
                .format(game.StartTime.getTime());
        String field = (game.Source == Game.SOURCE_CPBL ||
                !game.Field.contains(mContext.getString(R.string.title_at)))
                ? String.format("%s%s", mContext.getString(R.string.title_at), game.Field) : game.Field;
        String diff_str = DateUtils.getRelativeTimeSpanString(game.StartTime.getTimeInMillis(),
                System.currentTimeMillis(), DateUtils.DAY_IN_MILLIS).toString();
        int year = game.StartTime.get(Calendar.YEAR);
        holder.GameId.setText(String.valueOf(game.Id));
        holder.AwayTeamName.setText(isTablet() ? game.AwayTeam.getName() : game.AwayTeam.getShortName());
        holder.HomeTeamName.setText(isTablet() ? game.HomeTeam.getName() : game.HomeTeam.getShortName());
        if (holder.AwayLogo != null) {
            holder.AwayLogo.setImageResource(R.drawable.ic_baseball);
            holder.AwayLogo.setColorFilter(mTeamHelper.getLogoColorFilter(game.AwayTeam, year),
                    PorterDuff.Mode.SRC_IN);
            holder.AwayLogo.setVisibility(isShowLogo() ? View.VISIBLE : View.INVISIBLE);
            holder.AwayLogo.setVisibility(View.GONE);//[1021]++
        }
        if (holder.HomeLogo != null) {
            holder.HomeLogo.setImageResource(R.drawable.ic_baseball);
            holder.HomeLogo.setColorFilter(mTeamHelper.getLogoColorFilter(game.HomeTeam, year),
                    PorterDuff.Mode.SRC_IN);
            holder.HomeLogo.setVisibility(isShowLogo() ? View.VISIBLE : View.INVISIBLE);
            holder.HomeLogo.setVisibility(View.GONE);//[1021]++
        }
        holder.Field.setText(field);
        switch (getItemViewType(position)) {
            case TYPE_LIVE:
                //holder.GameTime.setText("");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    holder.GameTime.setText(Html.fromHtml(game.LiveMessage,
                            Html.TO_HTML_PARAGRAPH_LINES_INDIVIDUAL));
                } else {
                    holder.GameTime.setText(Html.fromHtml(game.LiveMessage));
                }
                //holder.GameTime.setVisibility(View.VISIBLE);
                holder.AwayTeamScore.setText(String.valueOf(game.AwayScore));
                holder.HomeTeamScore.setText(String.valueOf(game.HomeScore));
//                //use live text field in design flavor
//                if (holder.LiveText != null && game.LiveMessage != null) {
//                    String msg = game.LiveMessage.replace("&nbsp;&nbsp;", "<br>");
//                    msg = msg.substring(msg.lastIndexOf("<br>") + 4);
//                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//                        holder.LiveText.setText(Html.fromHtml(msg, Html.TO_HTML_PARAGRAPH_LINES_INDIVIDUAL));
//                    } else {
//                        holder.LiveText.setText(Html.fromHtml(msg));
//                    }
//                    holder.LiveText.setVisibility(View.VISIBLE);
//                }
                break;
            case TYPE_RESULT:
                holder.GameTime.setText(date_str);
                //holder.GameTime.setVisibility(View.GONE);
                holder.AwayTeamScore.setText(String.valueOf(game.AwayScore));
                holder.HomeTeamScore.setText(String.valueOf(game.HomeScore));
                //always use long name
                holder.AwayTeamName.setText(game.AwayTeam.getName());
                holder.HomeTeamName.setText(game.HomeTeam.getName());
                break;
            case TYPE_UPCOMING:
                if (game.isToday()) {
                    holder.GameTime.setText(diff_str.concat(" ").concat(time_str));
                } else {
                    holder.GameTime.setText(diff_str);
                }
                //holder.GameTime.setVisibility(View.VISIBLE);
                //holder.AwayTeamScore.setText("");//no score before game start
                //holder.HomeTeamScore.setText("");//no score before game start
                if (game.Channel == null || game.Channel.isEmpty()) {
                    holder.Channel.setVisibility(View.GONE);
                } else {
                    holder.Channel.setText(game.Channel);
                    holder.Channel.setVisibility(View.VISIBLE);
                }
                //dolphin++@2017.07.10, add more buttons
                if (holder.Option1 != null) {
                    holder.Option1.setTag(game);
                    holder.Option1.setOnClickListener(this);
                }
                if (holder.Option2 != null) {
                    holder.Option2.setTag(game);
                    holder.Option2.setOnClickListener(this);
                }
                break;
        }
    }

    @Override
    public int getItemCount() {
        return mGames != null ? mGames.size() : 0;
    }

    @Override
    public int getItemViewType(int position) {
        Game game = mGames.get(position);
        if (game.IsFinal) {
            return TYPE_RESULT;
        } else if (game.IsLive) {
            return TYPE_LIVE;
        } else if (game.Id == -1) {
            return TYPE_MORE;
        }
        return TYPE_UPCOMING;//super.getItemViewType(position);
    }

    @Override
    public void onClick(View view) {
        Game game = (Game) view.getTag();
        if (mListener != null) {
            mListener.onClick(view, game);
        }
    }

    private GameCardAdapter.OnClickListener mListener;

    void setOnClickListener(GameCardAdapter.OnClickListener listener) {
        mListener = listener;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        View Container;
        TextView AwayTeamName, AwayTeamScore;
        TextView HomeTeamName, HomeTeamScore;
        TextView GameTime, GameId;
        TextView Channel, Field;
        TextView LiveText;
        ImageView HomeLogo, AwayLogo;
        View Option1, Option2;

        ViewHolder(View parent) {
            super(parent);
            Container = parent;
            GameTime = (TextView) parent.findViewById(R.id.textView1);
            GameId = (TextView) parent.findViewById(R.id.textView9);
            AwayTeamName = (TextView) parent.findViewById(R.id.textView2);
            AwayTeamScore = (TextView) parent.findViewById(R.id.textView3);
            HomeTeamName = (TextView) parent.findViewById(R.id.textView5);
            HomeTeamScore = (TextView) parent.findViewById(R.id.textView4);
            Channel = (TextView) parent.findViewById(R.id.textView6);
            Field = (TextView) parent.findViewById(R.id.textView7);
            LiveText = (TextView) parent.findViewById(R.id.textView10);
            AwayLogo = (ImageView) parent.findViewById(android.R.id.icon1);
            HomeLogo = (ImageView) parent.findViewById(android.R.id.icon2);
            //Option1 = parent.findViewById(R.id.card_option1);
            Option2 = parent.findViewById(R.id.card_option2);
        }
    }

    public interface OnClickListener {
        void onClick(View view, Game game);
    }
}
