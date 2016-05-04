package dolphin.android.apps.CpblCalendar.provider;

import android.content.Context;
import android.content.Intent;

/**
 * Created by dolphin on 2016/04/28.
 * <p />
 * To fix Alarm Intent in new structure
 */
public class AlarmProvider {
    public final static String KEY_GAME = "_game";

    public final static String ACTION_ALARM = "dolphin.android.apps.CpblCalendar.ALARM";
    public final static String ACTION_DELETE_NOTIFICATION =
            "dolphin.android.apps.CpblCalendar.DELETE_NOTIFICATION";

    public static Intent getIntent(Context context) {
        return null;
    }

    public static void setNextAlarm(Context context) {
        //do nothing... we don't do this in dump build
    }

    public static void cancelAlarm(Context context, String key) {
        //do nothing... we don't do this in dump build
    }
}
