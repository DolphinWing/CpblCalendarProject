package dolphin.android.apps.CpblCalendar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import java.util.Calendar;

import dolphin.android.apps.CpblCalendar.preference.AlarmHelper;
import dolphin.android.apps.CpblCalendar.preference.PreferenceUtils;
import dolphin.android.apps.CpblCalendar.provider.AlarmProvider;
import dolphin.android.apps.CpblCalendar.provider.CpblCalendarHelper;
import dolphin.android.apps.CpblCalendar.provider.Game;

/**
 * Created by dolphin on 2016/03/26.
 * <p/>
 * Notification receiver and some helper methods
 */
public abstract class BaseNotifyReceiver extends BroadcastReceiver {
    private final static String TAG = "NotifyReceiver";
    protected final static String KEY_GAME = "_game";

    public final static String ACTION_ALARM = "dolphin.android.apps.CpblCalendar.ALARM";
    public final static String ACTION_DELETE_NOTIFICATION =
            "dolphin.android.apps.CpblCalendar.DELETE_NOTIFICATION";

    /**
     * set next available alarm
     *
     * @param context Context
     */
    public static void setNextAlarm(Context context) {
        //get next alarm from list
        AlarmHelper helper = new AlarmHelper(context);
        Game nextGame = helper.getNextAlarm();
        if (nextGame != null) {
            Log.v(TAG, "next game is #" + nextGame.Id +
                    " @ " + nextGame.StartTime.getTime().toString());
            Calendar alarm = nextGame.StartTime;
            alarm.add(Calendar.MINUTE, -PreferenceUtils.getAlarmNotifyTime(context));
            //Log.d(TAG, "alarm: " + alarm.getTime().toString());
            if (context.getResources().getBoolean(R.bool.demo_notification)) {//debug alarm
                alarm = CpblCalendarHelper.getNowTime();
                alarm.add(Calendar.SECOND, 15);
            }
            setAlarm(context, alarm, AlarmHelper.getAlarmIdKey(nextGame));
        } else {//try to cancel the alarm
            cancelAlarm(context, null);
            Log.v(TAG, "try to cancel the alarm that is not triggered");
        }
    }

    /**
     * get PendingIntent for AlarmManager
     *
     * @param context Context
     * @param key     game key
     * @return alarm intent
     */
    private static PendingIntent getAlarmIntent(Context context, String key) {
        Intent intent = AlarmProvider.getIntent(context);
        if (intent != null) {
            intent.setAction(ACTION_ALARM);
            if (key != null) {
                intent.putExtra(KEY_GAME, key);
            }
            return PendingIntent.getBroadcast(context, 2001, intent, 0);
        }
        return null;
    }

    /**
     * set alarm
     *
     * @param context   Context
     * @param alarmTime alarm time
     * @param key       game key
     */
    public static void setAlarm(Context context, Calendar alarmTime, String key) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent alermIntent = getAlarmIntent(context, key);
        if (alermIntent != null) {
            //[168]++ add different alarm wake mode
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmTime.getTimeInMillis(),
                        alermIntent);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                am.setExact(AlarmManager.RTC_WAKEUP, alarmTime.getTimeInMillis(),
                        alermIntent);
            } else {
                am.set(AlarmManager.RTC_WAKEUP, alarmTime.getTimeInMillis(),
                        alermIntent);
            }
            Log.v(TAG, "Alarm set! " + alarmTime.getTime().toString());
        }
    }

    /**
     * cancel alarm
     *
     * @param context Context
     * @param key     game key
     */
    public static void cancelAlarm(Context context, String key) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent alermIntent = getAlarmIntent(context, key);
        if (alermIntent != null) {
            am.cancel(alermIntent);
            //Log.v(TAG, "Alarm cancel!");
        }
    }
}
