package dolphin.android.apps.CpblCalendar.provider;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobManager;
import com.evernote.android.job.JobRequest;
import com.evernote.android.job.util.support.PersistableBundleCompat;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Set;

import dolphin.android.apps.CpblCalendar.CpblApplication;
import dolphin.android.apps.CpblCalendar.NotifyReceiver;
import dolphin.android.apps.CpblCalendar.R;
import dolphin.android.apps.CpblCalendar.preference.AlarmHelper;
import dolphin.android.apps.CpblCalendar.preference.PreferenceUtils;

/**
 * Created by dolphin on 2016/04/28.
 * <p/>
 * To fix Alarm Intent in new structure
 */
public class AlarmProvider {
    private final static String TAG = "AlarmProvider";
    private final static boolean DEBUG_LOG = false;

    public final static String KEY_GAME = "_game";

    public final static String ACTION_ALARM = "dolphin.android.apps.CpblCalendar.ALARM";
    public final static String ACTION_DELETE_NOTIFICATION =
            "dolphin.android.apps.CpblCalendar.DELETE_NOTIFICATION";

    /**
     * initialize JobManager
     *
     * @param context Context
     */
    public static void registerJob(Context context) {
        if (DEBUG_LOG) {
            Log.d(TAG, "onRegisterJob");
        }
        JobManager.create(context).addJobCreator(new NotifyJobCreator());
    }

    /**
     * set next available alarm
     *
     * @param application CpblApplication
     */
    @Deprecated
    public static void setNextAlarm(CpblApplication application) {
        Context context = application.getBaseContext();
        //get next alarm from list
        AlarmHelper helper = new AlarmHelper(context);
        Game nextGame = helper.getNextAlarm();
        if (nextGame != null) {
            Log.v(TAG, "next game is #" + nextGame.Id +
                    " @ " + nextGame.StartTime.getTime().toString());
            Calendar alarm = nextGame.StartTime;
            alarm.add(Calendar.MINUTE, -PreferenceUtils.getAlarmNotifyTime(context));
            Log.d(TAG, "alarm: " + alarm.getTime().toString());
            if (context.getResources().getBoolean(R.bool.demo_notification)) {//debug alarm
                alarm = CpblCalendarHelper.getNowTime();
                alarm.add(Calendar.SECOND, 15 * helper.getAlarmList().size());
                Log.d(TAG, "demo alarm: " + alarm.getTime().toString());
            }
            setAlarm(application, alarm, AlarmHelper.getAlarmIdKey(nextGame));
        } else {//try to cancel the alarm
            cancelAlarm(application, null);
            Log.v(TAG, "try to cancel the alarm that is not triggered");
        }
    }

    /**
     * set alarm
     *
     * @param application CpblApplication
     * @param alarmTime   alarm time
     * @param key         game key
     */
    public static int setAlarm(CpblApplication application, Calendar alarmTime, String key) {
        Context context = application.getBaseContext();
        AlarmHelper helper = new AlarmHelper(context);

        long now = CpblCalendarHelper.getNowTime().getTimeInMillis();
        long offset = alarmTime.getTimeInMillis() - now;
        if (DEBUG_LOG) {
            Log.d(TAG, String.format("exact offset from now: %d ms", offset));
        }
        PersistableBundleCompat extras = new PersistableBundleCompat();
        extras.putString(KEY_GAME, key);
        int jobId = new JobRequest.Builder(NotifyJob.TAG)
                .setBackoffCriteria(5_000L, JobRequest.BackoffPolicy.EXPONENTIAL)
                .setExact(offset) //alarmTime.getTimeInMillis()
                //.setExact(20_000L)
                .setExtras(extras)
                .setPersisted(true)
                //.setUpdateCurrent(true)
                .build()
                .schedule();
        if (DEBUG_LOG) {
            Log.d(TAG, "jobId = " + jobId);
        }
        helper.addJobId(key, jobId);
        return jobId;
    }

    /**
     * cancel alarm
     *
     * @param application CpblApplication
     * @param key         game key
     */
    public static void cancelAlarm(CpblApplication application, String key) {
        Context context = application.getBaseContext();
//        Log.d(TAG, "JobRequest");
//        Set<JobRequest> jobRequests = JobManager.instance().getAllJobRequests();
//        for (JobRequest request : jobRequests) {
//            Log.d(TAG, "  id: " + request.getJobId());
//        }
//        Log.d(TAG, "Job");
//        Set<Job> jobs = JobManager.instance().getAllJobs();
//        for (Job job : jobs) {
//            Log.d(TAG, job.toString());
//            Log.d(TAG, "  finished?: " + job.isFinished());
//        }

        AlarmHelper helper = new AlarmHelper(context);
        if (key != null && !key.isEmpty()) {
            int jobId = helper.getJobId(key);
            if (DEBUG_LOG) {
                Log.d(TAG, "found jobId: " + jobId);
            }
            if (jobId > 0) {
                JobManager.instance().cancel(jobId);
                helper.removeJobId(jobId);
            }
        } else {
            if (DEBUG_LOG) {
                Log.d(TAG, "cancel all");
            }
            JobManager.instance().cancelAll();
            helper.removeJobId(0);
        }
    }
}
