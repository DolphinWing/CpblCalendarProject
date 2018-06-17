package dolphin.android.apps.CpblCalendar

import android.annotation.SuppressLint
import android.app.ListFragment
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.TextView
import androidx.annotation.Keep
import dolphin.android.apps.CpblCalendar.preference.PreferenceUtils
import dolphin.android.apps.CpblCalendar.provider.CpblCalendarHelper
import dolphin.android.apps.CpblCalendar.provider.Game
import dolphin.android.apps.CpblCalendar3.CpblApplication
import dolphin.android.apps.CpblCalendar3.R
import java.util.*

/**
 * Created by dolphin on 2013/6/3.
 *
 *
 * GameList implementation
 */
@Keep
internal class GameListFragment : ListFragment(), AdapterView.OnItemLongClickListener {

    //    private FirebaseAnalytics mFirebaseAnalytics;

    var onOptionClickListener: GameAdapter.OnOptionClickListener? = null

    private var mEmptyView: View? = null

//    fun setOnOptionClickListener(listener: GameAdapter.OnOptionClickListener) {
//        mOnOptionClickListener = listener
//    }

    /**
     * update the adapter to ListView
     *
     * @param gameArrayList game list
     */
    fun updateAdapter(gameArrayList: ArrayList<Game>?, year: String, month: String) {
        val activity = activity
        //http://stackoverflow.com/a/11621405
        //this.setListShown(false);
        //Log.d(TAG, String.format("updateAdapter gameArrayList.size = %d",
        //        ((gameArrayList != null) ? gameArrayList.size() : 0)));
        val adapter = GameAdapter(activity, gameArrayList,
                getActivity().application as CpblApplication)
        if (onOptionClickListener != null) {//set listener
            adapter.setOnOptionClickListener(onOptionClickListener)
        }
        this.listAdapter = adapter
        //http://stackoverflow.com/a/5888331
        if (PreferenceUtils.isUpComingOn(activity) && gameArrayList != null) {
            try {//[8]++ try to scroll to not playing game at beginning
                if (PreferenceUtils.isCacheMode(activity)) {//[87]dolphin++ select after game
                    val now = CpblCalendarHelper.getNowTime()
                    for (i in gameArrayList.indices) {
                        if (gameArrayList[i].StartTime.after(now)) {
                            this.listView.setSelection(i)
                            break//break when the upcoming game if found
                        }
                    }
                } else {
                    for (i in gameArrayList.indices) {
                        if (!gameArrayList[i].IsFinal) {
                            this.listView.setSelection(i)
                            break//break when the upcoming game if found
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "isUpComingOn: " + e.message)
            }

        } else {
            Log.e(TAG, "updateAdapter null")
        }

        if (PreferenceUtils.getFavoriteTeams(activity).size() > 0) {
            this.listView.emptyView = getEmptyView(year, month)
        } else {
            this.setEmptyText(String.format("%s %s\n%s", year, month,
                    getString(R.string.no_favorite_teams)))
        }
        this.setListShown(true)
        //        if (PreferenceUtils.isCacheMode(getActivity())) {
        //            this.getListView().setPadding(0, 0, 0, 96);
        //        }
    }

    @SuppressLint("InflateParams")
    private fun getEmptyView(year: String, month: String): View? {
        //http://stackoverflow.com/a/15990955/2673859
        if (mEmptyView == null) {
            mEmptyView = activity.layoutInflater.inflate(R.layout.listview_empty_view, null)
            //Add the view to the list view. This might be what you are missing
            (listView.parent as ViewGroup).addView(mEmptyView)
        }
        if (mEmptyView != null) {
            val button1 = mEmptyView!!.findViewById<View>(android.R.id.button1)
            if (button1 != null) {
                var y1 = CpblCalendarHelper.getNowTime().get(Calendar.YEAR)
                try {
                    y1 = Integer.parseInt(year.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0])
                } catch (e: NumberFormatException) {
                    e.printStackTrace()
                }

                var m1 = 3
                try {
                    m1 = if (month == getString(R.string.title_game_year_all_months))
                        3
                    else
                        Integer.parseInt(month.substring(0, month.length - 1).trim { it <= ' ' })
                } catch (e: NumberFormatException) {
                    e.printStackTrace()
                }

                val y = y1
                val m = m1
                button1.setOnClickListener { CpblCalendarHelper.startActivityToCpblSchedule(activity, y, m, "01", "F00") }
            }
            val text2 = mEmptyView!!.findViewById<TextView>(android.R.id.text2)
            if (text2 != null) {
                text2.text = String.format("%s %s", year, month)
            }
        }
        return mEmptyView
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
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val listView = listView
        listView.onItemLongClickListener = this

        //        final AppCompatActivity activity = (AppCompatActivity) getActivity();
        //        mFirebaseAnalytics = FirebaseAnalytics.getInstance(activity);
    }

    override fun onItemLongClick(adapterView: AdapterView<*>, view: View, position: Int, l: Long): Boolean {
        onOptionClickListener?.onOptionClicked(view, view.tag as Game)
        return true
    }

    companion object {
        private val TAG = "GameListFragment"
    }
}
