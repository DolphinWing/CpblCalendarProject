package dolphin.android.apps.CpblCalendar.provider;

import android.util.Log;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobCreator;

/**
 * Created by jimmyhu on 2016/5/4.
 * <p/>
 * https://github.com/evernote/android-job
 */
public class NotifyJobCreator implements JobCreator {
    private final static String TAG = "NotifyJobCreator";

    @Override
    public Job create(String tag) {
        Log.d(TAG, "create job: " + tag);
        switch (tag) {
            case NotifyJob.TAG:
                return new NotifyJob();
        }
        return null;
    }
}
