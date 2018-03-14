package dolphin.android.apps.CpblCalendar;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.text.Html;
import android.text.SpannableString;
import android.text.Spanned;
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
import dolphin.android.apps.CpblCalendar3.R;

/**
 * Created by dolphin on 2015/02/07.
 * <p/>
 * common implementation for game adapter
 */
abstract class BaseGameAdapter extends ArrayAdapter<Game> {
    private final Context mContext;

    final static long ONE_DAY = 1000 * 60 * 60 * 24;

    private final static long ONE_WEEK = ONE_DAY * 7;

    private final static long LONGEST_GAME = 60 * 60 * 7 * 1000;

    private final LayoutInflater mInflater;

    private final boolean bShowWinner;

    private final boolean bShowLogo;

    private final boolean bShowToday;

    private final boolean bIsTablet;//[47]dolphin++

    private final AlarmHelper mAlarmHelper;//[50]dolphin++

    private final Calendar mNow;

    BaseGameAdapter(Context context, List<Game> objects) {
        super(context, android.R.layout.activity_list_item, objects);

        mContext = context;
        mInflater = LayoutInflater.from(mContext);

        bShowWinner = PreferenceUtils.isHighlightWin(mContext);
        bShowLogo = PreferenceUtils.isTeamLogoShown(mContext);
        bShowToday = PreferenceUtils.isHighlightToday(mContext);
        bIsTablet = mContext.getResources().getBoolean(R.bool.config_tablet);

        mAlarmHelper = new AlarmHelper(mContext);
        mNow = CpblCalendarHelper.getNowTime();
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        //Log.d(TAG, "getView: " + position);
        Game game = getItem(position);
        //Log.d(TAG, String.format("game id = %d", game.Id));

        if (convertView == null) {
            convertView = mInflater.inflate(getLayoutResId(game), parent, false);
        }
        convertView.setTag(game);

        //Log.d(TAG, String.format("%d: %s", game.Id, game.Url));
        decorate(convertView, game);
        return convertView;//super.getView(position, convertView, parent);
    }

    private boolean isLiveNow(Game game) {
        Calendar c = game.StartTime;
        boolean bNoScoreNoLive = (game.Source == Game.SOURCE_CPBL_2013
                && c.get(Calendar.YEAR) >= 2014);
        //[84]dolphin++//live channel
        boolean bLiveNow = (!game.IsFinal && game.StartTime.before(mNow));
        bLiveNow &= !bNoScoreNoLive;//[87]dolphin++
        //Log.d("GameAdapter", c.toString() + " live=" + bLiveNow);
        bLiveNow &= ((mNow.getTimeInMillis() - game.StartTime.getTimeInMillis()) < LONGEST_GAME);
        return bLiveNow;
    }

    String getGameDateStr(Game game) {
        Calendar c = game.StartTime;
        boolean bLiveNow = isLiveNow(game);
        String date_str = String.format(Locale.TAIWAN, "%s, %02d:%02d",
                //date //[47] use Taiwan only, add tablet DAY_OF_WEEK
                //[53]dolphin++ use DAY_OF_WEEK to all devices
                //new SimpleDateFormat(bIsTablet ? "MMM dd (E)" : "MMM dd",
                new SimpleDateFormat("MMM d (E)", Locale.TAIWAN).format(c.getTime()),
                //time
                //DateFormat.getTimeFormat(getSherlockActivity()).format(c.getTime()));
                c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE));
        date_str = isTablet() && withinOneWeek(c) //[53]dolphin++ only tablets
                ? String.format("%s (%s)", date_str,
                //relative time span to have better date idea
                DateUtils.getRelativeTimeSpanString(c.getTimeInMillis(),
                        System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS))
                : date_str;
        date_str = game.IsFinal || bLiveNow ? new SimpleDateFormat("MMM d (E)",
                Locale.TAIWAN).format(c.getTime()) : date_str;//[70]++
        return date_str;
    }

    protected void decorate(View convertView, Game game) {
        //[67]++ for long team name
        boolean bIsLongName = supportLongName(game);

        //game time
        Calendar c = game.StartTime;
        boolean bNoScoreNoLive = (game.Source == Game.SOURCE_CPBL_2013 && c.get(Calendar.YEAR) >= 2014);
        //[84]dolphin++//live channel
        boolean bLiveNow = isLiveNow(game);
        //Log.d("GameAdapter", game.StartTime.getTime().toString() + " " + bLiveNow);
        if (game.Kind.equals("07") && game.DelayMessage != null && //dolphin++@2018-03-14, don't show score
                game.DelayMessage.equals(mContext.getString(R.string.delayed_game_cancelled))) {
            bNoScoreNoLive = true;//warm up game cancelled
        }

        TextView tv1 = convertView.findViewById(R.id.textView1);
        String date_str = getGameDateStr(game);
        if (game.IsLive) {//[181]++
            date_str = String.format("%s&nbsp;&nbsp;%s", date_str, game.LiveMessage);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                tv1.setText(Html.fromHtml(date_str, Html.TO_HTML_PARAGRAPH_LINES_INDIVIDUAL));
            } else {
                tv1.setText(Html.fromHtml(date_str));
            }
        } else {
            tv1.setText(date_str);
        }

        //match up
        TextView tv2 = convertView.findViewById(R.id.textView2);
        TextView tv3 = convertView.findViewById(R.id.textView3);
        //[13]++ add highlight winning team
        if (isShowWinner() && game.AwayScore > game.HomeScore) {
            if (game.AwayTeam != null && !game.AwayTeam.getName().isEmpty()) {//[67]++ for long team name
                SpannableString span1 = new SpannableString(bIsLongName
                        ? game.AwayTeam.getName() : game.AwayTeam.getShortName());
                span1.setSpan(new StyleSpan(Typeface.BOLD), 0, span1.length(), 0);
                tv2.setText(span1);
            }
            //tv2.getPaint().setFakeBoldText(true);//http://goo.gl/sK7cN
            //[45]-- tv2.setBackgroundResource(R.drawable.ab_transparent_holo_green);
            SpannableString span2 = new SpannableString(String.valueOf(game.AwayScore));
            span2.setSpan(new StyleSpan(Typeface.BOLD), 0, span2.length(), 0);
            tv3.setText(span2);
            //[45]-- tv3.setBackgroundResource(R.drawable.ab_transparent_holo_green);//[45]++
        } else {
            //[67]++ for long team name
            if (game.AwayTeam != null) {
                tv2.setText(bIsLongName ? game.AwayTeam.getName()
                        : game.AwayTeam.getShortName());
            }
            tv2.setBackgroundResource(android.R.color.transparent);//reset back
            //tv2.setBackgroundResource(android.R.color.holo_red_light);
            tv3.setText(String.valueOf(game.AwayScore));
            tv3.setBackgroundResource(android.R.color.transparent);//[45]++ reset back
        }
        tv3.setTextColor(ContextCompat.getColor(mContext,
                game.IsFinal || bLiveNow ? android.R.color.primary_text_light
                        : android.R.color.secondary_text_light_nodisable));
        //[72]dolphin++ no score
        //[87]dolphin++ CPBL_2013 source no score
        if (bNoScoreNoLive || (!game.IsFinal && !game.IsLive)) {
            tv3.setText("-");//no score
        }

        TextView tv4 = convertView.findViewById(R.id.textView4);
        TextView tv5 = convertView.findViewById(R.id.textView5);
        //[13]++ add highlight winning team
        if (isShowWinner() && game.HomeScore > game.AwayScore) {
            SpannableString span2 = new SpannableString(String.valueOf(game.HomeScore));
            span2.setSpan(new StyleSpan(Typeface.BOLD), 0, span2.length(), 0);
            tv4.setText(span2);
            //[45]-- tv4.setBackgroundResource(R.drawable.ab_transparent_holo_green);//[45]++
            if (game.HomeTeam != null && !game.HomeTeam.getName().isEmpty()) {//[67]++ for long team name
                SpannableString span1 = new SpannableString(bIsLongName
                        ? game.HomeTeam.getName() : game.HomeTeam.getShortName());
                span1.setSpan(new StyleSpan(Typeface.BOLD), 0, span1.length(), 0);
                tv5.setText(span1);
            }
            //tv5.getPaint().setFakeBoldText(true);//http://goo.gl/sK7cN
            //[45]-- tv5.setBackgroundResource(R.drawable.ab_transparent_holo_green);
        } else {
            tv4.setText(String.valueOf(game.HomeScore));
            tv4.setBackgroundResource(android.R.color.transparent);//[45]++ reset back
            if (game.HomeTeam != null) {//[67]++ for long team name
                tv5.setText(bIsLongName ? game.HomeTeam.getName()
                        : game.HomeTeam.getShortName());
            }
            tv5.setBackgroundResource(android.R.color.transparent);//reset back
            //tv5.setBackgroundResource(android.R.color.holo_red_light);
        }
        tv4.setTextColor(ContextCompat.getColor(mContext,
                game.IsFinal || bLiveNow ? android.R.color.primary_text_light
                        : android.R.color.secondary_text_light_nodisable));
        //[72]dolphin++ no score
        //[87]dolphin++ CPBL_2013 source no score
        if (bNoScoreNoLive || (!game.IsFinal && !game.IsLive)) {
            tv4.setText("-");//no score
        }

        TextView tv6 = convertView.findViewById(R.id.textView6);
//        if (mNow.get(Calendar.YEAR) >= 2014 && game.Channel == null) {//CPBL TV live!
//            tv6.setVisibility(bLiveNow ? View.VISIBLE : View.GONE);
//            tv6.setText(bLiveNow ? mContext.getString(R.string.title_live_on_cpbltv) : "");
//            if (bLiveNow) {
//                tv6.setTextColor(Color.RED);
//            }
//        } else
        if (game.Channel != null) {
            tv6.setVisibility(View.VISIBLE);
            Spanned spanned;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                spanned = Html.fromHtml(String.format("<b><font color='red'>%s</font></b> %s",
                        mContext.getString(R.string.title_live_now), game.Channel),
                        Html.TO_HTML_PARAGRAPH_LINES_INDIVIDUAL);
            } else {
                spanned = Html.fromHtml(String.format("<b><font color='red'>%s</font></b> %s",
                        mContext.getString(R.string.title_live_now), game.Channel));
            }
            tv6.setText(bLiveNow ? spanned : game.Channel);
        } else {
            tv6.setVisibility(View.INVISIBLE);
        }

        //game field
        TextView tv7 = convertView.findViewById(R.id.textView7);
        if (tv7 != null) {
            tv7.setText(game.Source == Game.SOURCE_CPBL ||
                    !game.Field.contains(mContext.getString(R.string.title_at))
                    ? String.format("%s%s", mContext.getString(R.string.title_at),
                    game.getFieldFullName(getContext())) : game.Field);
        }

        //delay message
        TextView tv8 = convertView.findViewById(R.id.textView8);
        if (game.DelayMessage != null && game.DelayMessage.length() > 0) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                tv8.setText(Html.fromHtml(game.DelayMessage, Html.TO_HTML_PARAGRAPH_LINES_INDIVIDUAL));
            } else {
                tv8.setText(Html.fromHtml(game.DelayMessage));
            }
            tv8.setVisibility(View.VISIBLE);
        } else {
            tv8.setVisibility(View.GONE);
        }

        TextView tv9 = convertView.findViewById(R.id.textView9);
        tv9.setText(String.valueOf(game.Id));//game number as id

        //url indicator
        ImageView iv1 = convertView.findViewById(android.R.id.icon);
        if (iv1 != null) {
            iv1.setVisibility(game.IsFinal ? View.VISIBLE : View.INVISIBLE);
        }

        //team logo
        int year = game.StartTime.get(Calendar.YEAR);
        ImageView ic1 = convertView.findViewById(android.R.id.icon1);
        if (ic1 != null && game.AwayTeam != null) {
            ic1.setImageResource(game.AwayTeam.getLogo(year));
            ic1.setVisibility(isShowLogo() ? View.VISIBLE : View.GONE);
            //ic1.setBackgroundResource(android.R.color.holo_red_light);
        }
        ImageView ic2 = convertView.findViewById(android.R.id.icon2);
        if (ic2 != null && game.HomeTeam != null) {
            ic2.setImageResource(game.HomeTeam.getLogo(year));
            ic2.setVisibility(isShowLogo() ? View.VISIBLE : View.GONE);
            //ic2.setBackgroundResource(android.R.color.holo_red_light);
        }

        //[23]dolphin++ add a background layer id
        View bg = convertView.findViewById(R.id.match_title);
        if (bg != null) {//[22]dolphin++
            convertView.setBackgroundResource((DateUtils.isToday(c.getTimeInMillis()) && isShowToday())
                    ? R.drawable.item_highlight_background_holo_light
                    : R.drawable.selectable_background_holo_green);
        }
        //}//[24]++ fix darker highlight when enable highlightToday

        //[50]++ notification
        View alarm = convertView.findViewById(R.id.icon_alarm);
        if (alarm != null) {
            alarm.setTag(game);
            if (PreferenceUtils.isEnableNotification(mContext)) {
                alarm.setVisibility(game.IsFinal || bLiveNow
                        || game.StartTime.before(mNow) ? View.GONE : View.VISIBLE);
                alarm.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Game g = (Game) view.getTag();
                        //Log.d(TAG, "id = " + g.Id);
                        ImageView img = (ImageView) view;
                        updateGameNotification(img, g);
                    }
                });

                ((ImageView) alarm).setImageResource(mAlarmHelper.hasAlarm(game)
                        ? R.drawable.ic_device_access_alarmed
                        : R.drawable.ic_device_access_alarm);
            } else {
                alarm.setVisibility(View.GONE);
            }
        }
    }

    private void updateGameNotification(ImageView icon, Game game) {
        if (mAlarmHelper.hasAlarm(game)) {
            mAlarmHelper.removeGame(game);
            icon.setImageResource(R.drawable.ic_device_access_alarm);
            cancelAlarm(game);
        } else {
            mAlarmHelper.addGame(game);
            icon.setImageResource(R.drawable.ic_device_access_alarmed);
            setAlarm(game);
        }

        //AlarmProvider.setNextAlarm(mContext);
    }

    protected abstract int getLayoutResId(Game game);

    protected abstract boolean supportLongName(Game game);

    protected abstract void setAlarm(Game game);

    protected abstract void cancelAlarm(Game game);

//    public static void updateNotifyDialogMatchUp(Context context, ViewGroup convertView, Game game,
//                                                 boolean bIsTablet, boolean bShowLogo) {
//        //TODO: update NotifyDialog match up layout
//    }

    /**
     * check if the day is within a week
     *
     * @param c Calendar object
     * @return true if the day is within this week
     */
    private boolean withinOneWeek(Calendar c) {
        return Math.abs(c.getTimeInMillis() - System.currentTimeMillis()) <= (ONE_WEEK);
    }

    private boolean isShowWinner() {
        return bShowWinner;
    }

    boolean isShowLogo() {
        return bShowLogo;
    }

    private boolean isShowToday() {
        return bShowToday;
    }

    private boolean isTablet() {
        return bIsTablet;
    }

    AlarmHelper getAlarmHelper() {
        return mAlarmHelper;
    }
}
