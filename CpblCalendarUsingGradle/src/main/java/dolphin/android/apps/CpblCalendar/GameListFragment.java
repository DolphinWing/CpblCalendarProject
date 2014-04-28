package dolphin.android.apps.CpblCalendar;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.app.SherlockListFragment;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.SpannableString;
import android.text.format.DateUtils;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import dolphin.android.apps.CpblCalendar.preference.AlarmHelper;
import dolphin.android.apps.CpblCalendar.preference.PreferenceUtils;
import dolphin.android.apps.CpblCalendar.provider.CpblCalendarHelper;
import dolphin.android.apps.CpblCalendar.provider.Game;

/**
 * Created by dolphin on 2013/6/3.
 */
public class GameListFragment extends SherlockListFragment
        implements ListView.OnItemLongClickListener {

    private final static String TAG = "GameListFragment";

    private final static long ONE_DAY = 1000 * 60 * 60 * 24;

    private final static long ONE_WEEK = ONE_DAY * 7;

    /**
     * update the adapter to ListView
     *
     * @param gameArrayList
     */
    public void updateAdapter(ArrayList<Game> gameArrayList) {
        final SherlockFragmentActivity activity = getSherlockActivity();
        //http://stackoverflow.com/a/11621405
        //this.setListShown(false);
        //Log.d(TAG, String.format("updateAdapter gameArrayList.size = %d",
        //        ((gameArrayList != null) ? gameArrayList.size() : 0)));
        this.setListAdapter(new MyAdapter(activity, gameArrayList));
        //http://stackoverflow.com/a/5888331
        if (PreferenceUtils.isUpComingOn(activity) && gameArrayList != null) {
            try {//[8]++ try to scroll to not playing game at beginning
                if (PreferenceUtils.isCacheMode(activity)) {//[87]dolphin++ select after game
                    Calendar now = CpblCalendarHelper.getNowTime();
                    for (int i = 0; i < gameArrayList.size(); i++) {
                        if (gameArrayList.get(i).StartTime.after(now)) {
                            this.getListView().setSelection(i);
                            break;//break when the upcoming game if found
                        }
                    }
                } else {
                    for (int i = 0; i < gameArrayList.size(); i++) {
                        if (!gameArrayList.get(i).IsFinal) {
                            this.getListView().setSelection(i);
                            break;//break when the upcoming game if found
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "isUpComingOn: " + e.getMessage());
            }
        } else {
            Log.e(TAG, "updateAdapter null");
        }
//        this.setEmptyText(PreferenceUtils.getFavoriteTeams(activity).size() > 0
//                ? getString(R.string.no_games_this_month)
//                : getString(R.string.no_favorite_teams));//[31]dolphin++
        if (PreferenceUtils.getFavoriteTeams(activity).size() > 0) {
            this.getListView().setEmptyView(getEmptyView());
        } else {
            this.setEmptyText(getString(R.string.no_favorite_teams));
        }
        this.setListShown(true);
    }

    private View mEmptyView = null;

    private View getEmptyView() {
        //http://stackoverflow.com/a/15990955/2673859
        if (mEmptyView == null) {
            mEmptyView = getLayoutInflater(null).inflate(R.layout.listview_empty_view, null);
            //Add the view to the list view. This might be what you are missing
            ((ViewGroup) getListView().getParent()).addView(mEmptyView);
        }
        if (mEmptyView != null) {
//            TextView tv = (TextView) mEmptyView.findViewById(android.R.id.text1);
//            tv.setText(R.string.no_games_this_month);
            View button1 = mEmptyView.findViewById(android.R.id.button1);
            if (button1 != null) {
                button1.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        CpblCalendarHelper.startActivityToCpblSchedule(getActivity());
                    }
                });
            }
        }
        return mEmptyView;
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        //super.onListItemClick(l, v, position, id);
        if (v != null) {
            Game game = (Game) v.getTag();
            //Log.d(TAG, "onListItemClick: " + position);
            //Log.d(TAG, "  game.IsFinal: " + game.IsFinal);
            //Log.d(TAG, "  game.Url: " + game.Url);

            Calendar now = CpblCalendarHelper.getNowTime();
            String url = null;
            if (game.StartTime.after(now)) {
                url = within3Days(game.StartTime) ?
                        String.format("%s/game/starters.aspx?gameno=%s&year=%d&game=%d",
                                CpblCalendarHelper.URL_BASE,
                                game.Kind, game.StartTime.get(Calendar.YEAR), game.Id) : null;
            } else if (game.IsFinal || PreferenceUtils.isCacheMode(getActivity())) {
                url = String.format("%s/game/box.aspx?gameno=%s&year=%d&game=%d",
                        CpblCalendarHelper.URL_BASE,
                        game.Kind, game.StartTime.get(Calendar.YEAR), game.Id);
            } else {
                url = game.Url;//default is live url
                //Log.d(TAG, game.StartTime.getTime().toString());
            }

            if (game != null && url != null) {//[78]-- game.IsFinal) {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                if (PreferenceUtils.isEngineerMode(getActivity())) {
                    Log.d(TAG, "Url=" + url.substring(url.lastIndexOf("/")));
                } else {
                    startActivity(i);
                }
            }
        }
    }

    private boolean within3Days(Calendar c) {
        return ((c.getTimeInMillis() - System.currentTimeMillis()) <= ONE_DAY * 2);
    }

    //Long click on ListFragment
    //http://stackoverflow.com/a/6857819/2673859
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getListView().setOnItemLongClickListener(this);
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long l) {
        return true;
    }

    /**
     * private implementation of our game show
     */
    private class MyAdapter extends ArrayAdapter<Game> {

        private Context mContext;

        private LayoutInflater mInflater;

        private boolean bShowWinner;

        private boolean bShowLogo;

        private boolean bShowToday;

        private boolean bIsTablet;//[47]dolphin++

        private AlarmHelper mAlarmHelper;//[50]dolphin++

        private Calendar mNow;

        public MyAdapter(Context context, List<Game> objects) {
            super(context, android.R.id.text1, objects);
            mContext = context;
            mInflater = LayoutInflater.from(mContext);

            bShowWinner = PreferenceUtils.isHighlightWin(mContext);
            bShowLogo = PreferenceUtils.isTeamLogoShown(mContext);
            bShowToday = PreferenceUtils.isHighlightToday(mContext);
            bIsTablet = mContext.getResources().getBoolean(R.bool.config_tablet);

            mAlarmHelper = new AlarmHelper(mContext);
            mNow = CpblCalendarHelper.getNowTime();
            //Log.d(TAG, mNow.toString());
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            //Log.d(TAG, "getView: " + position);
            Game game = getItem(position);
            //Log.d(TAG, String.format("game id = %d", game.Id));
            //[67]++ for long team name
            boolean bIsLongName = game.Kind.equalsIgnoreCase("09");
            if (convertView == null) {
                int layout = bIsLongName ? R.layout.listview_item_matchup_09
                        : R.layout.listview_item_matchup;
                convertView = mInflater.inflate(layout, parent, false);
            }
            convertView.setTag(game);

            //game time
            Calendar c = game.StartTime;
            boolean bNoScoreNoLive = (game.Source == Game.SOURCE_CPBL_2013
                    && c.get(Calendar.YEAR) >= 2014);

            TextView tv1 = (TextView) convertView.findViewById(R.id.textView1);
            String date_str = String.format("%s, %02d:%02d",
                    //date //[47] use Taiwan only, add tablet DAY_OF_WEEK
                    //[53]dolphin++ use DAY_OF_WEEK to all devices
                    //new SimpleDateFormat(bIsTablet ? "MMM dd (E)" : "MMM dd",
                    new SimpleDateFormat("MMM dd (E)", Locale.TAIWAN).format(c.getTime()),
                    //time
                    //DateFormat.getTimeFormat(getSherlockActivity()).format(c.getTime()));
                    c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE));
            date_str = bIsTablet && withinOneWeek(c) //[53]dolphin++ only tablets
                    ? String.format("%s (%s)", date_str,
                    //relative time span to have better date idea
                    DateUtils.getRelativeTimeSpanString(c.getTimeInMillis(),
                            System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS)
            )
                    : date_str;
            date_str = game.IsFinal ? new SimpleDateFormat("MMM dd (E)",
                    Locale.TAIWAN).format(c.getTime()) : date_str;//[70]++
            tv1.setText(date_str);

            //matchup
            TextView tv2 = (TextView) convertView.findViewById(R.id.textView2);
            TextView tv3 = (TextView) convertView.findViewById(R.id.textView3);
            //[13]++ add highlight winning team
            if (bShowWinner && game.AwayScore > game.HomeScore) {
                //[67]++ for long team name
                SpannableString span1 = new SpannableString(bIsLongName
                        ? game.AwayTeam.getName() : game.AwayTeam.getShortName());
                span1.setSpan(new StyleSpan(Typeface.BOLD), 0, span1.length(), 0);
                tv2.setText(span1);
                //tv2.getPaint().setFakeBoldText(true);//http://goo.gl/sK7cN
                //[45]-- tv2.setBackgroundResource(R.drawable.ab_transparent_holo_green);
                SpannableString span2 = new SpannableString(String.valueOf(game.AwayScore));
                span2.setSpan(new StyleSpan(Typeface.BOLD), 0, span2.length(), 0);
                tv3.setText(span2);
                tv3.setBackgroundResource(R.drawable.ab_transparent_holo_green);//[45]++
            } else {
                //[67]++ for long team name
                tv2.setText(bIsLongName ? game.AwayTeam.getName()
                        : game.AwayTeam.getShortName());
                tv2.setBackgroundResource(android.R.color.transparent);//reset back
                tv3.setText(String.valueOf(game.AwayScore));
                tv3.setBackgroundResource(android.R.color.transparent);//[45]++ reset back
            }
            tv3.setTextColor(getResources().getColor(game.IsFinal
                    ? android.R.color.primary_text_light
                    : android.R.color.secondary_text_light_nodisable));
            //[72]dolphin++ no score
            //[87]dolphin++ CPBL_2013 source no score
            if (bNoScoreNoLive) {
                tv3.setText("-");//no score
            }

            TextView tv4 = (TextView) convertView.findViewById(R.id.textView4);
            TextView tv5 = (TextView) convertView.findViewById(R.id.textView5);
            //[13]++ add highlight winning team
            if (bShowWinner && game.HomeScore > game.AwayScore) {
                SpannableString span2 = new SpannableString(String.valueOf(game.HomeScore));
                span2.setSpan(new StyleSpan(Typeface.BOLD), 0, span2.length(), 0);
                tv4.setText(span2);
                tv4.setBackgroundResource(R.drawable.ab_transparent_holo_green);//[45]++
                //[67]++ for long team name
                SpannableString span1 = new SpannableString(bIsLongName
                        ? game.HomeTeam.getName() : game.HomeTeam.getShortName());
                span1.setSpan(new StyleSpan(Typeface.BOLD), 0, span1.length(), 0);
                tv5.setText(span1);
                //tv5.getPaint().setFakeBoldText(true);//http://goo.gl/sK7cN
                //[45]-- tv5.setBackgroundResource(R.drawable.ab_transparent_holo_green);
            } else {
                tv4.setText(String.valueOf(game.HomeScore));
                tv4.setBackgroundResource(android.R.color.transparent);//[45]++ reset back
                //[67]++ for long team name
                tv5.setText(bIsLongName ? game.HomeTeam.getName()
                        : game.HomeTeam.getShortName());
                tv5.setBackgroundResource(android.R.color.transparent);//reset back
            }
            tv4.setTextColor(getResources().getColor(game.IsFinal
                    ? android.R.color.primary_text_light
                    : android.R.color.secondary_text_light_nodisable));
            //[72]dolphin++ no score
            //[87]dolphin++ CPBL_2013 source no score
            if (bNoScoreNoLive) {
                tv4.setText("-");//no score
            }

            //[84]dolphin++//live channel
            boolean bLiveNow = (!game.IsFinal && game.StartTime.before(mNow));
            bLiveNow &= !bNoScoreNoLive;//[87]dolphin++
            //Log.d(TAG, c.toString() + " live=" + bLiveNow);
            TextView tv6 = (TextView) convertView.findViewById(R.id.textView6);
            if (mNow.get(Calendar.YEAR) >= 2014) {//CPBL TV live!
                tv6.setVisibility(bLiveNow ? View.VISIBLE : View.GONE);
                tv6.setText(bLiveNow ? getString(R.string.title_live_on_cpbltv) : "");
                tv6.setTextColor(Color.RED);
            } else if (game.Channel != null) {
                tv6.setVisibility(View.VISIBLE);
                tv6.setText(getString(R.string.title_live_now, game.Channel));
            } else {
                tv6.setVisibility(View.GONE);
            }

            //game field
            TextView tv7 = (TextView) convertView.findViewById(R.id.textView7);
            tv7.setText(game.Source == Game.SOURCE_CPBL ||
                    !game.Field.contains(getString(R.string.title_at))
                    ? String.format("%s%s", getString(R.string.title_at), game.Field) : game.Field);

            //delay message
            TextView tv8 = (TextView) convertView.findViewById(R.id.textView8);
            if (game.DelayMessage != null && game.DelayMessage.length() > 0) {
                tv8.setText(Html.fromHtml(game.DelayMessage));
                tv8.setVisibility(View.VISIBLE);
            } else {
                tv8.setVisibility(View.GONE);
            }

            TextView tv9 = (TextView) convertView.findViewById(R.id.textView9);
            tv9.setText(String.valueOf(game.Id));//game number as id

            //url indicator
            ImageView iv1 = (ImageView) convertView.findViewById(android.R.id.icon);
            iv1.setVisibility(game.IsFinal ? View.VISIBLE : View.INVISIBLE);

            //team logo
            ImageView ic1 = (ImageView) convertView.findViewById(android.R.id.icon1);
            ic1.setImageResource(game.AwayTeam.getLogo(game.StartTime.get(Calendar.YEAR)));
            ic1.setVisibility(bShowLogo ? View.VISIBLE : View.GONE);
            ImageView ic2 = (ImageView) convertView.findViewById(android.R.id.icon2);
            ic2.setImageResource(game.HomeTeam.getLogo(game.StartTime.get(Calendar.YEAR)));
            ic2.setVisibility(bShowLogo ? View.VISIBLE : View.GONE);

            //[23]dolphin++ add a background layer id
            View bg = convertView.findViewById(R.id.match_title);
            if (bg != null && bShowToday) {//[22]dolphin++
                convertView.setBackgroundResource(DateUtils.isToday(c.getTimeInMillis())
                        ? R.drawable.item_highlight_background_holo_light
                        : android.R.color.transparent);
            }
            //}//[24]++ fix darker highlight when enable highlightToday

            //[50]++ notification
            View alarm = convertView.findViewById(R.id.icon_alarm);
            if (alarm != null) {
                alarm.setTag(game);
                if (PreferenceUtils.isEnableNotification(getSherlockActivity())) {
                    alarm.setVisibility(game.IsFinal || bLiveNow
                            || game.StartTime.before(mNow) ? View.GONE : View.VISIBLE);
                    alarm.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Game g = (Game) view.getTag();
                            //Log.d(TAG, "id = " + g.Id);

                            ImageView img = (ImageView) view;
                            if (mAlarmHelper.hasAlarm(g)) {
                                mAlarmHelper.removeGame(g);
                                img.setImageResource(R.drawable.ic_device_access_alarm);
                            } else {
                                mAlarmHelper.addGame(g);
                                img.setImageResource(R.drawable.ic_device_access_alarmed);
                            }

                            NotifyReceiver.setNextAlarm(getSherlockActivity());
                        }
                    });

                    ((ImageView) alarm).setImageResource(mAlarmHelper.hasAlarm(game)
                            ? R.drawable.ic_device_access_alarmed
                            : R.drawable.ic_device_access_alarm);
                } else {
                    alarm.setVisibility(View.GONE);
                }
            }

            //Log.d(TAG, String.format("%d: %s", game.Id, game.Url));
            return convertView;//super.getView(position, convertView, parent);
        }

        /**
         * check if the day is within a week
         *
         * @param c
         * @return
         */
        private boolean withinOneWeek(Calendar c) {
            return Math.abs(c.getTimeInMillis() - System.currentTimeMillis()) <= (ONE_WEEK);
        }

//        /**
//         * check if we are in the same date
//         *
//         * @param c
//         * @return
//         */
//        private boolean isToday(Calendar c) {
//            //return DateUtils.isToday(c.getTimeInMillis());
//            Calendar now = Calendar.getInstance();
//            //http://stackoverflow.com/a/2517824
//            return now.get(Calendar.YEAR) == c.get(Calendar.YEAR) &&
//                    now.get(Calendar.DAY_OF_YEAR) == c.get(Calendar.DAY_OF_YEAR);
//        }
    }
}
