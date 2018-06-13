package dolphin.android.apps.CpblCalendar3;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.graphics.PorterDuff;
import android.os.Build;
import android.text.Html;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import dolphin.android.apps.CpblCalendar.preference.PreferenceUtils;
import dolphin.android.apps.CpblCalendar.provider.CpblCalendarHelper;
import dolphin.android.apps.CpblCalendar.provider.Game;
import dolphin.android.apps.CpblCalendar.provider.TeamHelper;
import dolphin.android.util.PackageUtils;

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
    private final static int TYPE_ANNOUNCE = 11;
    private final static int TYPE_UPDATE = 12;

    private final static int ID_MORE = -TYPE_MORE;
    private final static int ID_ANNOUNCE = -TYPE_ANNOUNCE;
    private final static int ID_UPDATE = -TYPE_UPDATE;

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

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
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
            default:
                layoutId = R.layout.recyclerview_item_more;
                break;
            case TYPE_ANNOUNCE:
                layoutId = R.layout.recyclerview_item_announce;
                break;
            case TYPE_UPDATE:
                layoutId = R.layout.recyclerview_item_update_info;
                break;
        }
        //if (/*parent != null &&*/ layoutId != -1) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false));
        //}
        //return null;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.Container.setOnClickListener(this);
        Game game = mGames.get(position);
        holder.Container.setTag(game);
        if (game.Id < 0) {
            if (holder.LiveText != null) {//for TYPE_UPDATE and TYPE_ANNOUNCE
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    holder.LiveText.setText(Html.fromHtml(game.LiveMessage,
                            Html.TO_HTML_PARAGRAPH_LINES_INDIVIDUAL));
                } else {
                    holder.LiveText.setText(Html.fromHtml(game.LiveMessage));
                }
            }
            if (holder.Option2 != null) {//for TYPE_UPDATE
                holder.Option2.setTag(game);
                holder.Option2.setOnClickListener(this);
            }
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
            //holder.AwayLogo.setVisibility(View.GONE);//[1021]++
        }
        if (holder.HomeLogo != null) {
            holder.HomeLogo.setImageResource(R.drawable.ic_baseball);
            holder.HomeLogo.setColorFilter(mTeamHelper.getLogoColorFilter(game.HomeTeam, year),
                    PorterDuff.Mode.SRC_IN);
            holder.HomeLogo.setVisibility(isShowLogo() ? View.VISIBLE : View.INVISIBLE);
            //holder.HomeLogo.setVisibility(View.GONE);//[1021]++
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
                holder.AwayTeamName.setText(game.AwayTeam.getShortName());
                holder.HomeTeamName.setText(game.HomeTeam.getShortName());
                holder.Field.setText(game.getFieldFullName(mContext));
                break;
            case TYPE_RESULT:
                holder.GameTime.setText(date_str);
                //holder.GameTime.setVisibility(View.GONE);
                holder.AwayTeamScore.setText(String.valueOf(game.AwayScore));
                holder.HomeTeamScore.setText(String.valueOf(game.HomeScore));
                //always use long name
                holder.AwayTeamName.setText(game.AwayTeam.getName());
                holder.HomeTeamName.setText(game.HomeTeam.getName());
                if (holder.Option1 != null) {
                    holder.Option1.setTag(game);
                    holder.Option1.setOnClickListener(this);
                }
                if (holder.Option3 != null) {
                    Game g = new Game(game.Id);
                    g.People = position;//use this field to save position to remove
                    holder.Option3.setTag(g);
                    holder.Option3.setOnClickListener(this);
                }
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
//                if (holder.Option1 != null) {
//                    holder.Option1.setTag(game);
//                    holder.Option1.setOnClickListener(this);
//                }
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
        } else if (game.Id < 0) {
            return -game.Id;//because we use minus type value as id
        }
        return TYPE_UPCOMING;//super.getItemViewType(position);
    }

    @Override
    public void onClick(View view) {
        Game game = (Game) view.getTag();
        if (mListener != null && game.Id != ID_ANNOUNCE) {
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
        View Option1, Option2, Option3;

        ViewHolder(View parent) {
            super(parent);
            Container = parent;
            GameTime = parent.findViewById(R.id.textView1);
            GameId = parent.findViewById(R.id.textView9);
            AwayTeamName = parent.findViewById(R.id.textView2);
            AwayTeamScore = parent.findViewById(R.id.textView3);
            HomeTeamName = parent.findViewById(R.id.textView5);
            HomeTeamScore = parent.findViewById(R.id.textView4);
            Channel = parent.findViewById(R.id.textView6);
            Field = parent.findViewById(R.id.textView7);
            LiveText = parent.findViewById(R.id.textView10);
            AwayLogo = parent.findViewById(android.R.id.icon1);
            HomeLogo = parent.findViewById(android.R.id.icon2);
            Option1 = parent.findViewById(R.id.card_option1);
            Option2 = parent.findViewById(R.id.card_option2);
            //Option3 = parent.findViewById(R.id.card_option3);
        }
    }

    public interface OnClickListener {
        void onClick(View view, Game game);
    }

    static Game createMoreCard() {
        return new Game(ID_MORE);
    }

    static boolean isMoreCard(Game game) {
        return game != null && game.Id == ID_MORE;
    }

    private static Game createAnnouncementCard(String message) {
        Game card = new Game(ID_ANNOUNCE);
        card.LiveMessage = message;
        return card;
    }

    static List<Game> getAnnouncementCards(Context context, FirebaseRemoteConfig config) {
        List<Game> list = new ArrayList<>();
        Calendar now = CpblCalendarHelper.getNowTime();
        String keys = config.getString("add_highlight_card");
        if (keys != null && !keys.isEmpty()) {
//            if (DEBUG_LOG) {
//                Log.w(TAG, "check new announce");
//            }
            String[] ids = keys.split(";");
            for (String id : ids) {
//                if (DEBUG_LOG) {
//                    Log.d(TAG, "  " + id);
//                }
                String msg = config.getString("add_highlight_card_".concat(id));
                if (msg != null && id.length() >= 6) {
                    Calendar cal = CpblCalendarHelper.getNowTime();
                    cal.set(Calendar.YEAR, Integer.parseInt(id.substring(0, 4)));
                    cal.set(Calendar.MONTH, Integer.parseInt(id.substring(4, 5)));
                    cal.set(Calendar.DAY_OF_MONTH, Integer.parseInt(id.substring(5, 6)));
                    cal.set(Calendar.HOUR_OF_DAY, 0);
                    cal.set(Calendar.MINUTE, 0);
                    cal.set(Calendar.SECOND, 0);
                    if (cal.before(now)) {
                        continue;//check if the announcement is expired
                    }

                    list.add(0, GameCardAdapter.createAnnouncementCard(msg));
                }
            }
        }
        return list;
    }

    private static Game createUpdateCard(String message) {
        Game card = new Game(ID_UPDATE);
        card.LiveMessage = message;
        return card;
    }

    static Game getNewVersionCard(Context context, FirebaseRemoteConfig config) {
        Class<?> cls;
        if (context instanceof Activity) {
            cls = context.getClass();
        } else {
            cls = SplashActivity.class;
        }
        PackageInfo info = PackageUtils.getPackageInfo(context, cls);
        int versionCode = info != null ? info.versionCode : Integer.MAX_VALUE;
        long latestCode = config.getLong("latest_version_code");
        Log.v("CpblCalendar3", String.format("versionCode: %d, play: %d", versionCode, latestCode));
        if (versionCode < latestCode) {
            String summary = config.getString("latest_version_summary");
            summary = summary != null && !summary.isEmpty() ? summary
                    : context.getString(R.string.new_version_available_message);
            return createUpdateCard(summary);
        }
        return null;
    }

    static boolean isUpdateCard(Game game) {
        return game != null && game.Id == ID_UPDATE;
    }

    void removeAt(int position) {
        mGames.remove(position);
        notifyDataSetChanged();
    }
}
