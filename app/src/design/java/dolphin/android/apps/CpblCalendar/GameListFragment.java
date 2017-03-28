package dolphin.android.apps.CpblCalendar;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ListFragment;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;

import dolphin.android.apps.CpblCalendar.preference.PreferenceUtils;
import dolphin.android.apps.CpblCalendar.provider.CpblCalendarHelper;
import dolphin.android.apps.CpblCalendar.provider.Game;

/**
 * Created by dolphin on 2013/6/3.
 * <p/>
 * GameList implementation
 */
@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class GameListFragment extends ListFragment implements ListView.OnItemLongClickListener {

    private final static String TAG = "GameListFragment";

//    private FirebaseAnalytics mFirebaseAnalytics;

    private GameAdapter.OnOptionClickListener mOnOptionClickListener;

    public void setOnOptionClickListener(GameAdapter.OnOptionClickListener listener) {
        mOnOptionClickListener = listener;
    }

    /**
     * update the adapter to ListView
     *
     * @param gameArrayList game list
     */
    public void updateAdapter(ArrayList<Game> gameArrayList, String year, String month) {
        final Activity activity = getActivity();
        //http://stackoverflow.com/a/11621405
        //this.setListShown(false);
        //Log.d(TAG, String.format("updateAdapter gameArrayList.size = %d",
        //        ((gameArrayList != null) ? gameArrayList.size() : 0)));
        GameAdapter adapter = new GameAdapter(activity, gameArrayList,
                (CpblApplication) getActivity().getApplication());
        if (mOnOptionClickListener != null) {//set listener
            adapter.setOnOptionclickListener(mOnOptionClickListener);
        }
        this.setListAdapter(adapter);
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

        if (PreferenceUtils.getFavoriteTeams(activity).size() > 0) {
            this.getListView().setEmptyView(getEmptyView(year, month));
        } else {
            this.setEmptyText(String.format("%s %s\n%s", year, month,
                    getString(R.string.no_favorite_teams)));
        }
        this.setListShown(true);
    }

    private View mEmptyView = null;

    private View getEmptyView(String year, String month) {
        //http://stackoverflow.com/a/15990955/2673859
        if (mEmptyView == null) {
            mEmptyView = getActivity().getLayoutInflater().inflate(R.layout.listview_empty_view, null);
            //Add the view to the list view. This might be what you are missing
            ((ViewGroup) getListView().getParent()).addView(mEmptyView);
        }
        if (mEmptyView != null) {
            View button1 = mEmptyView.findViewById(android.R.id.button1);
            if (button1 != null) {
                int y1 = CpblCalendarHelper.getNowTime().get(Calendar.YEAR);
                try {
                    y1 = Integer.parseInt(year.split(" ")[0]);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
                int m1 = 3;
                try {
                    m1 = month.equals(getString(R.string.title_game_year_all_months))
                            ? 3 : Integer.parseInt(month.substring(0, month.length() - 1).trim());
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
                final int y = y1;
                final int m = m1;
                button1.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        CpblCalendarHelper.startActivityToCpblSchedule(getActivity(), y, m, "01", "F00");
                    }
                });
            }
            TextView text2 = (TextView) mEmptyView.findViewById(android.R.id.text2);
            if (text2 != null) {
                text2.setText(String.format("%s %s", year, month));
            }
        }
        return mEmptyView;
    }

//    @Override
//    public void onListItemClick(ListView l, View v, int position, long id) {
//        super.onListItemClick(l, v, position, id);
//        //Log.d(TAG, "onListItemClick: " + position);
//        if (v != null) {
//            Game game = (Game) v.getTag();
//            //Log.d(TAG, "onListItemClick: " + position);
//            //Log.d(TAG, "  game.IsFinal: " + game.IsFinal);
//            //Log.d(TAG, "  game.Url: " + game.Url);
//            if (mFirebaseAnalytics != null) {
//                Bundle bundle = new Bundle();
//                bundle.putString(FirebaseAnalytics.Param.ITEM_ID, String.format(Locale.US,
//                        "%d-%d", game.StartTime.get(Calendar.YEAR), game.Id));
//                bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, String.format(Locale.TAIWAN,
//                        "%s vs %s", game.AwayTeam.getShortName(), game.HomeTeam.getShortName()));
//                bundle.putString(FirebaseAnalytics.Param.ITEM_CATEGORY, game.Kind);
//                bundle.putString(FirebaseAnalytics.Param.ITEM_LOCATION_ID, game.Field);
//                mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM, bundle);
//            }
//
//            Utils.startGameActivity(getActivity(), game);
//        }
//    }

    //Long click on ListFragment
    //http://stackoverflow.com/a/6857819/2673859
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        final ListView listView = getListView();
        listView.setOnItemLongClickListener(this);

//        final AppCompatActivity activity = (AppCompatActivity) getActivity();

//        mFirebaseAnalytics = FirebaseAnalytics.getInstance(activity);
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long l) {
        //FIXME: don't do anything
        return true;
    }
}
