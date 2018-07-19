package dolphin.android.apps.CpblCalendar3

import android.app.Application
import android.content.Context
import androidx.palette.graphics.Palette
import android.util.SparseArray

import com.google.android.gms.analytics.GoogleAnalytics
import com.google.android.gms.analytics.Tracker

import java.util.HashMap

import dolphin.android.apps.CpblCalendar.Utils

/**
 * Created by dolphin on 2014/6/1.
 * http://wangshifuola.blogspot.tw/2011/12/androidapplicationglobal-variable.html
 */
class CpblApplication : Application() {

    private val mTrackers = HashMap<TrackerName, Tracker>()

    val defaultTracker: Tracker?
        @Synchronized get() = getTracker(TrackerName.APP_TRACKER)

    /**
     * check if we need to download data from server again
     *
     * @return true means we need to donwload again
     */
    var isUpdateRequired = false

    private val mTeamLogoPalette = SparseArray<Palette>()

    internal val cacheList = SparseArray<GameListLiveData>()

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(Utils.onAttach(base))
    }

//    override fun onCreate() {
//        super.onCreate()
//
//        //        //only register it when we have enabled it
//        //        if (getResources().getBoolean(R.bool.feature_notify)) {
//        //            AlarmProvider.registerJob(this);//https://github.com/evernote/android-job
//        //        }
//    }

    /**
     * Enum used to identify the tracker that needs to be used for tracking.
     *
     *
     * A single tracker is usually enough for most purposes. In case you do need multiple trackers,
     * storing them all in Application object helps ensure that they are created only once per
     * application instance.
     */
    internal enum class TrackerName {
        APP_TRACKER, // Tracker used only in this app.
        GLOBAL_TRACKER, // Tracker used by all the apps from a company. eg: roll-up tracking.
        ECOMMERCE_TRACKER
        // Tracker used by all ecommerce transactions from a company.
    }

    private fun getTracker(trackerId: TrackerName): Tracker? {
        if (!mTrackers.containsKey(trackerId)) {
            val analytics = GoogleAnalytics.getInstance(this) ?: return null

            // When dry run is set, hits will not be dispatched, but will still be logged as
            // though they were dispatched.
            analytics.setDryRun(resources.getBoolean(R.bool.pref_engineer_mode))

            val t = analytics.newTracker(getString(R.string.ga_trackingId))
            t.enableAutoActivityTracking(true)
            //t.enableExceptionReporting(true);
            t.enableAdvertisingIdCollection(true)
            mTrackers[TrackerName.APP_TRACKER] = t
        }
        return mTrackers[trackerId]
    }

    fun setTeamLogoPalette(id: Int, palette: Palette) {
        //Log.d("CpblApp", String.format("set %d-%d", year, teamId));
        mTeamLogoPalette.put(id, palette)
    }

    fun getTeamLogoPalette(id: Int): Palette? {
        //Log.d("CpblApp", String.format("get %d-%d", year, teamId));
        //Palette logo = mTeamLogoPalette.get(year * 10000 + teamId);
        //if (logo == null) {
        //    logo = getResources().getDrawable(R.drawable.no_logo);
        //}
        return mTeamLogoPalette.get(id)
    }
}
