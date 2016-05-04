package dolphin.android.apps.CpblCalendar;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ListFragment;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.melnykov.fab.FloatingActionButton;
import com.melnykov.fab.ScrollDirectionListener;

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
        this.setListAdapter(new GameAdapter(activity, gameArrayList,
                (CpblApplication) getActivity().getApplication()));
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

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        //Log.d(TAG, "onListItemClick: " + position);
        if (v != null) {
            //Game game = (Game) v.getTag();
            //Log.d(TAG, "onListItemClick: " + position);
            //Log.d(TAG, "  game.IsFinal: " + game.IsFinal);
            //Log.d(TAG, "  game.Url: " + game.Url);

            Utils.startGameActivity(getActivity(), (Game) v.getTag());
        }
    }

    //Long click on ListFragment
    //http://stackoverflow.com/a/6857819/2673859
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        final ListView listView = getListView();
        listView.setOnItemLongClickListener(this);

        final AppCompatActivity activity = (AppCompatActivity) getActivity();
        //https://github.com/makovkastar/FloatingActionButton
        FloatingActionButton floatingActionButton =
                (FloatingActionButton) activity.findViewById(R.id.button_floating_action);
        if (floatingActionButton != null) {
            floatingActionButton.attachToListView(listView, new ScrollDirectionListener() {
                @Override
                public void onScrollDown() {
                    //Log.d("ListViewFragment", "onScrollDown()");
//                    if (!activity.getSupportActionBar().isShowing()) {
//                        activity.getSupportActionBar().show();
//                    }
                }

                @Override
                public void onScrollUp() {
                    //Log.d("ListViewFragment", "onScrollUp()");
//                    if (activity.getSupportActionBar().isShowing()) {
//                        activity.getSupportActionBar().hide();
//                    }
                }
            }, new AbsListView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(AbsListView view, int scrollState) {
                    //Log.d("ListViewFragment", "onScrollStateChanged() " + scrollState);
                }

                @Override
                public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                    //Log.d("ListViewFragment", "onScroll()");
                }
            });
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long l) {
        //FIXME: don't do anything
        return true;
    }

}
