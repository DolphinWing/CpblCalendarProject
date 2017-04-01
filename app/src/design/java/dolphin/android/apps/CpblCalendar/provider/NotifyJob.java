package dolphin.android.apps.CpblCalendar.provider;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.evernote.android.job.Job;
import com.evernote.android.job.util.support.PersistableBundleCompat;

import dolphin.android.apps.CpblCalendar.NotifyReceiver;
import dolphin.android.apps.CpblCalendar.preference.AlarmHelper;

/**
 * Created by jimmyhu on 2016/5/4.
 * <p/>
 * https://github.com/evernote/android-job
 */
class NotifyJob extends Job {
    public static final String TAG = "NotifyJob";
    private final static boolean DEBUG_LOG = false;

    @NonNull
    @Override
    protected Result onRunJob(Params params) {
        int jobId = params.getId();
        if (DEBUG_LOG) {
            Log.d(TAG, "onRunJob: " + jobId);
        }
        Context context = getContext();

        AlarmHelper helper = new AlarmHelper(context);
        helper.removeJobId(jobId);

        String key = "";
        PersistableBundleCompat extras = params.getExtras();
        if (extras.containsKey(AlarmProvider.KEY_GAME)) {
            key = extras.getString(AlarmProvider.KEY_GAME, "");
        }
        if (DEBUG_LOG) {
            Log.d(TAG, "key: " + key);
        }
        if (key.isEmpty()) {
            return Result.FAILURE;
        }

        NotifyReceiver.showAlarm(context, key);
        helper.removeGame(key);

        // run your job
        return Result.SUCCESS;
    }

}
