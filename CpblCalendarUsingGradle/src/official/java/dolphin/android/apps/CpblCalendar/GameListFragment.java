package dolphin.android.apps.CpblCalendar;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.app.SherlockListFragment;

import java.util.ArrayList;
import java.util.Calendar;

import dolphin.android.apps.CpblCalendar.preference.PreferenceUtils;
import dolphin.android.apps.CpblCalendar.provider.CpblCalendarHelper;
import dolphin.android.apps.CpblCalendar.provider.Game;

/**
 * Created by dolphin on 2013/6/3.
 */
public class GameListFragment extends SherlockListFragment
        implements ListView.OnItemLongClickListener {

    private final static String TAG = "GameListFragment";

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
        this.setListAdapter(new GameAdapter(activity, gameArrayList));
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
        getListView().setOnItemLongClickListener(this);
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long l) {
        return true;
    }

}
