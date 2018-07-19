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
import dolphin.android.apps.CpblCalendar.preference.PrefsHelper
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
        //http://stackoverflow.com/a/11621405
        //this.setListShown(false);
        //Log.d(TAG, String.format("updateAdapter gameArrayList.size = %d",
        //        ((gameArrayList != null) ? gameArrayList.size() : 0)));
        val adapter = GameAdapter(activity, gameArrayList, activity.application as CpblApplication)
        if (onOptionClickListener != null) {//set listener
            adapter.onOptionClickListener = onOptionClickListener
        }
        this.listAdapter = adapter
        //http://stackoverflow.com/a/5888331
        if (mPrefsHelper.autoScrollUpcoming && gameArrayList != null) {
            try {//[8]++ try to scroll to not playing game at beginning
                if (mPrefsHelper.cacheModeEnabled) {//[87]dolphin++ select after game
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

        if (mPrefsHelper.favoriteTeams.size() > 0) {
            this.listView.emptyView = getEmptyView(year, month)
        } else {
            this.setEmptyText(String.format("%s %s\n%s", year, month,
                    getString(R.string.no_favorite_teams)))
        }
        this.setListShown(true)
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
                button1.setOnClickListener {
                    Utils.startActivityToCpblSchedule(activity, y, m, "01", "F00")
                }
            }
            val text2 = mEmptyView!!.findViewById<TextView>(android.R.id.text2)
            if (text2 != null) {
                text2.text = String.format("%s %s", year, month)
            }
        }
        return mEmptyView
    }

    private lateinit var mPrefsHelper: PrefsHelper

    //Long click on ListFragment
    //http://stackoverflow.com/a/6857819/2673859
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val listView = listView
        listView.onItemLongClickListener = this
        mPrefsHelper = PrefsHelper(activity)
    }

    override fun onItemLongClick(adapterView: AdapterView<*>, view: View, position: Int, l: Long): Boolean {
        onOptionClickListener?.onOptionClicked(view, view.tag as Game)
        return true
    }

    companion object {
        private const val TAG = "GameListFragment"
    }
}
