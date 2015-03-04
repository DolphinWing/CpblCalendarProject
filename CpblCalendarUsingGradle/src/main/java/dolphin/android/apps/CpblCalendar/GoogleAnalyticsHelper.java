package dolphin.android.apps.CpblCalendar;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import java.util.ArrayList;

import dolphin.android.apps.CpblCalendar.provider.Game;

/**
 * Created by dolphin on 2015/02/07.
 *
 * Google Analytics helper class
 */
public class GoogleAnalyticsHelper {
    public final static String SCREEN_CALENDAR_ACTIVITY_BASE =
            "dolphin.android.apps.CpblCalendar.CalendarActivity";
    public final static String SCREEN_CALENDAR_ACTIVITY_PHONE =
            "dolphin.android.apps.CpblCalendar.CalendarForPhoneActivity";
    public final static String SCREEN_CALENDAR_ACTIVITY_TABLET =
            "dolphin.android.apps.CpblCalendar.CalendarForTabletActivity";
    public final static String SCREEN_PREFERENCE_ACTIVITY =
            "dolphin.android.apps.CpblCalendar.preference.PreferenceActivity";
    public final static String SCREEN_PREFERENCE_ACTIVITY_GB =
            "dolphin.android.apps.CpblCalendar.preference.GBPreferenceActivity";

    private CpblApplication mApplication;
    private String mScreenName;

    public GoogleAnalyticsHelper(CpblApplication application) {
        mApplication = application;
        mScreenName = SCREEN_CALENDAR_ACTIVITY_BASE;
    }

    public GoogleAnalyticsHelper(CpblApplication application, String screenName) {
        mApplication = application;
        mScreenName = screenName;
    }

    public void sendTrackerException(String action, String label) {
        sendTrackerException(mScreenName, action, label);
    }

    public void sendTrackerException(String action, String label, long evtValue) {
        sendGmsGoogleAnalyticsReport(mScreenName, "Exception", action, label);
    }

    public void sendTrackerException(String path, String action, String label) {
        sendGmsGoogleAnalyticsReport(path, "Exception", action, label);
    }

    public void sendGmsGoogleAnalyticsReport(String category, String action, String label) {
        sendGmsGoogleAnalyticsReport(mScreenName, category, action, label);
    }

    public void sendGmsGoogleAnalyticsReport(String path, String category, String action,
                                             String label) {
        // Get tracker.
        Tracker t = mApplication.getTracker(CpblApplication.TrackerName.APP_TRACKER);

        // Set screen name.
        // Where path is a String representing the screen name.
        t.setScreenName(path);

        // Send a screen view.
        t.send(new HitBuilders.AppViewBuilder().build());

        // This event will also be sent with &cd=Home%20Screen.
        // Build and send an Event.
        t.send(new HitBuilders.EventBuilder()
                .setCategory(category)
                .setAction(action)
                .setLabel(label)
                .build());

        // Clear the screen name field when we're done.
        t.setScreenName(null);
    }

    public static String getExtraMessage(ArrayList<Game> gameList, boolean cacheMode) {
        int type = (gameList != null && gameList.size() > 0)
                ? gameList.get(0).Source : -1;
        String extra = cacheMode ? "cache" : "unknown";
        if (!cacheMode) {
            switch (type) {
                case Game.SOURCE_CPBL:
                    extra = "cpbl";
                    break;
                case Game.SOURCE_ZXC22:
                    extra = "zxc22";
                    break;
                case Game.SOURCE_CPBL_2013:
                    extra = "cpbl_old";
                    break;
            }
        }
        return extra;
    }
}
