package dolphin.android.apps.CpblCalendar;

import android.app.Application;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;

import java.util.HashMap;

import dolphin.android.apps.CpblCalendar.provider.AlarmProvider;

/**
 * Created by dolphin on 2014/6/1.
 * http://wangshifuola.blogspot.tw/2011/12/androidapplicationglobal-variable.html
 */
public class CpblApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        //https://github.com/evernote/android-job
        AlarmProvider.registerJob(this);
    }

    /**
     * Enum used to identify the tracker that needs to be used for tracking.
     *
     * A single tracker is usually enough for most purposes. In case you do need multiple trackers,
     * storing them all in Application object helps ensure that they are created only once per
     * application instance.
     */
    public enum TrackerName {
        APP_TRACKER, // Tracker used only in this app.
        GLOBAL_TRACKER, // Tracker used by all the apps from a company. eg: roll-up tracking.
        ECOMMERCE_TRACKER, // Tracker used by all ecommerce transactions from a company.
    }

    private final HashMap<TrackerName, Tracker> mTrackers = new HashMap<>();

    public CpblApplication() {
        super();
    }

    public synchronized Tracker getTracker(TrackerName trackerId) {
        if (!mTrackers.containsKey(trackerId)) {

            GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
            if (analytics == null) {
                return null;
            }

            // When dry run is set, hits will not be dispatched, but will still be logged as
            // though they were dispatched.
            analytics.setDryRun(getResources().getBoolean(R.bool.pref_engineer_mode));

            Tracker t = (trackerId == TrackerName.APP_TRACKER)
                    ? analytics.newTracker(getString(R.string.ga_trackingId))
                    : (trackerId == TrackerName.GLOBAL_TRACKER)
                            ? analytics.newTracker(R.xml.global_tracker)
                            : analytics.newTracker(R.xml.ecommerce_tracker);
            if (trackerId == TrackerName.APP_TRACKER) {
                t.enableAutoActivityTracking(true);
                //t.enableExceptionReporting(true);
                t.enableAdvertisingIdCollection(true);
            }
            mTrackers.put(trackerId, t);

        }
        return mTrackers.get(trackerId);
    }
}
