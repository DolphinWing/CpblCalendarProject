package dolphin.android.apps.CpblCalendar;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.util.ArrayList;
import java.util.Calendar;

import dolphin.android.apps.CpblCalendar.preference.AlarmHelper;
import dolphin.android.apps.CpblCalendar.preference.PreferenceUtils;
import dolphin.android.apps.CpblCalendar.provider.AlarmProvider;
import dolphin.android.apps.CpblCalendar.provider.CpblCalendarHelper;
import dolphin.android.apps.CpblCalendar.provider.Game;

/**
 * Created by dolphin on 2013/8/31.
 * <p/>
 * Notification receiver and some helper methods
 */
public class NotifyReceiver extends BroadcastReceiver {
    private final static String TAG = "NotifyReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        //check notification config
        if (!PreferenceUtils.isEnableNotification(context)) {
            //cancelAlarm(context, null);
            return;//[57]dolphin++ bypass those alarms
        }

        String action = intent.getAction();
        if (action.equalsIgnoreCase(Intent.ACTION_BOOT_COMPLETED)) {
            //register next alarm on boot complete
            Log.i(TAG, Intent.ACTION_BOOT_COMPLETED);
        } else if (action.equals(AlarmProvider.ACTION_DELETE_NOTIFICATION) ||
                action.equals(AlarmProvider.ACTION_ALARM)) {
            Log.d(TAG, action);
            //return;
        } else {
            String key = intent.getStringExtra(AlarmProvider.KEY_GAME);
            showAlarm(context, key);
            //showAlarm should remove the notified alarm from the list
        }

        //AlarmProvider.setNextAlarm(context);//set alarm for next event
    }

    /**
     * show alarm when time arrived
     *
     * @param context Context
     * @param key     game key
     */
    public static void showAlarm(Context context, String key) {
        //Log.v(TAG, "showAlarm " + Calendar.getInstance().getTime().toString());
        //final boolean demoMode = context.getResources().getBoolean(R.bool.demo_notification);
        AlarmHelper helper = new AlarmHelper(context);
        ArrayList<Game> list = helper.getAlarmList();
        if (list != null && list.size() > 0) {//has alarm
            String contentText = "";
            String bigMsgText = "";
            //get data from the list and show notification
            Game game = list.get(0);
            //Log.d(TAG, "game " + game.Id + " @ " + game.StartTime.getTime().toString());
            //if this game is not the game we want
            if (!AlarmHelper.getAlarmIdKey(game).equalsIgnoreCase(key)) {
                //Log.w(TAG, " first game is " + AlarmHelper.getAlarmIdKey(game));
                Calendar now = CpblCalendarHelper.getNowTime();
                now.set(Calendar.SECOND, 0);
                now.set(Calendar.MILLISECOND, 0);
                now.add(Calendar.MINUTE, PreferenceUtils.getAlarmNotifyTime(context));
                //if the time has passed the game start time, ignore it
                if (now.compareTo(game.StartTime) <= 0) {
                    Log.w(TAG, "bypass the notification and reset alarm! " + key);
//                    AlarmProvider.cancelAlarm(context, key);
                    //return;//bypass the notification and reset alarm
                }//still show the dialog
            }
            contentText = context.getString(R.string.msg_content_text,
                    game.AwayTeam.getShortName(), game.HomeTeam.getShortName());
            if (game.Channel != null) {
                bigMsgText = context.getString(R.string.msg_big_text_content,
                        game.AwayTeam.getName(), game.HomeTeam.getName(),
                        game.Field, game.Channel);
            } else {//[51]dolphin++ some games have no broadcasts
                bigMsgText = context.getString(R.string.msg_big_text_content_no_broadcast,
                        game.AwayTeam.getName(), game.HomeTeam.getName(), game.Field);
            }
            //remove this game from the alarm list
            helper.removeGame(game);//list.remove(0);

            //check if the next alarm is around this time
            if (list.size() > 1) {
                Game nextGame = list.get(1);
                //Log.d(TAG, nextGame.StartTime.getTime().toString());
                if (nextGame.StartTime.compareTo(game.StartTime) == 0) {
                    contentText += context.getString(R.string.msg_content_text2,
                            nextGame.AwayTeam.getName(), nextGame.HomeTeam.getName());
                    bigMsgText += "\n";
                    if (nextGame.Channel != null) {
                        bigMsgText += context.getString(R.string.msg_big_text_content,
                                nextGame.AwayTeam.getName(), nextGame.HomeTeam.getName(),
                                nextGame.Field, nextGame.Channel);
                    } else {//[51]dolphin++ some games have no broadcasts
                        bigMsgText = context.getString(R.string.msg_big_text_content_no_broadcast,
                                nextGame.AwayTeam.getName(),
                                nextGame.HomeTeam.getName(), nextGame.Field);
                    }
                    //remove from the alarm list
                    helper.removeGame(nextGame);//list.remove(0);
                }
            }

            //[51]dolphin++ show dialog
            if (PreferenceUtils.isEnableNotifyDialog(context)) {
                showNotifyDialog(context, list);
            }

            showNotification(context, contentText, bigMsgText);
        } else {
            Log.w(TAG, "showAlarm no alarm " + key);
//            AlarmProvider.cancelAlarm(context, key);//cancel the event in manager
        }
    }

    /**
     * get PendingIntent for Notification click
     *
     * @param context Context
     * @return click intent
     */
    private static PendingIntent getClickIntent(Context context) {
        // Creates an Intent for the Activity
        Intent notifyIntent = new Intent();
        notifyIntent.setComponent(new ComponentName(context, SplashActivity.class));
        // Sets the Activity to start in a new, empty task
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            notifyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        } else {
            notifyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        // Creates the PendingIntent
        return PendingIntent.getActivity(
                context,
                2002,
                notifyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
        );
    }

    /**
     * get PendingIntent for Notification dismiss
     *
     * @param context Context
     * @return dismiss intent
     */
    private static PendingIntent getDeleteIntent(Context context) {
        Intent deleteIntent = new Intent();
        deleteIntent.setAction(AlarmProvider.ACTION_DELETE_NOTIFICATION);
        return PendingIntent.getBroadcast(context, 2003, deleteIntent, 0);
    }

    /**
     * show Notifications
     *
     * @param context     Context
     * @param contentText content
     * @param bigMsgText  big message text for rich notification
     */
    public static void showNotification(Context context,
                                         String contentText, String bigMsgText) {
        //Log.v(TAG, "showNotification " + Build.VERSION.SDK_INT);
        // Gets an instance of the NotificationManager service
        NotificationManager mNotifyMgr =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        int notifyFlags = /*Notification.DEFAULT_SOUND | */Notification.DEFAULT_LIGHTS;
        if (PreferenceUtils.isEnableNotifyVibrate(context)) {
            notifyFlags |= Notification.DEFAULT_VIBRATE;
        }
        Uri sound = PreferenceUtils.getNotificationUri(context);
        if (sound == null) {//[183]++
            notifyFlags |= Notification.DEFAULT_SOUND;
        }
//[63]--        //Rich Notifications
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
//            //http://developer.android.com/design/patterns/notifications.html
//            Notification.Builder builder =
//                    new Notification.Builder(context)
//                            .setSmallIcon(R.drawable.ic_launcher)
//                            .setContentTitle(context.getString(R.string.title_notification))
//                            .setContentText(contentText);
//            builder.setDefaults(notifyFlags);
//            //http://developer.android.com/reference/android/app/Notification.Builder.html#setLights(int, int, int)
//            //http://stackoverflow.com/a/10453489/2673859
//            //http://stackoverflow.com/a/3298102/2673859
//            builder.setLights(Color.GREEN, 1000, 3000);//[59]++ custom LED color if possible
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
//                builder.setStyle(new Notification.BigTextStyle().bigText(bigMsgText));
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
//                builder.setShowWhen(true);
//
//            switch (PreferenceUtils.getNotifyPendingAction(context)) {
//                case PreferenceUtils.PENDING_ACTION_DISMISS:
//                    builder.setAutoCancel(true);
//                    break;
//                case PreferenceUtils.PENDING_ACTION_ACTIVITY:
//                    builder.setContentIntent(getClickIntent(context));
//                    break;
//            }
//
//            // Builds the notification and issues it.
//            mNotifyMgr.notify(1001, builder.build());
//
//            //Tutorial on new Android Jelly Bean notification
//            //http://code4reference.com/2012/07/android-jelly-bean-notification/
//        } else {//Android 2.3 and before
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context)
                        .setSmallIcon(R.drawable.ic_stat_notify)
                        .setContentTitle(context.getString(R.string.title_notification))
                        .setContentText(contentText);
        builder.setStyle(new NotificationCompat.BigTextStyle().bigText(bigMsgText));
        builder.setDefaults(notifyFlags);
        builder.setLights(Color.GREEN, 1000, 3000);//[59]++ custom LED color if possible
        builder.setTicker(bigMsgText);//[63]dolphin++
        builder.setDeleteIntent(getDeleteIntent(context));//[122]dolphin++
        //http://stackoverflow.com/a/30865087/2673859
        if (sound != null) {//[183]++
            builder.setSound(sound);
        }
        Bitmap bm = BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher);
        builder.setLargeIcon(bm);

        switch (PreferenceUtils.getNotifyPendingAction(context)) {
            case PreferenceUtils.PENDING_ACTION_DISMISS:
                builder.setAutoCancel(true);
                break;
            case PreferenceUtils.PENDING_ACTION_ACTIVITY:
                builder.setContentIntent(getClickIntent(context));
                break;
        }

        mNotifyMgr.notify(1001, builder.build());
        //mNotifyMgr.notify(1001, builder.getNotification());
//        }
    }

    /**
     * show notification dialog
     *
     * @param context Content
     * @param list    game list
     */
    public static void showNotifyDialog(Context context, ArrayList<Game> list) {
        //Log.v(TAG, "showNotifyDialog");
        Intent intent = new Intent(context, NotifyDialog.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK/* | Intent.FLAG_ACTIVITY_CLEAR_TASK*/);
        if (list != null && list.size() > 0) {
            Game game = list.get(0);
            intent.putExtra(NotifyDialog.EXTRA_GAME1, Game.toPrefString(game));
            if (list.size() > 1) {
                Game nextGame = list.get(1);
                if (nextGame.StartTime.compareTo(game.StartTime) == 0) {
                    intent.putExtra(NotifyDialog.EXTRA_GAME2, Game.toPrefString(nextGame));
                }
            }
        }
        //a receiver can't start dialog, but can start activity
        context.startActivity(intent);
    }
}
