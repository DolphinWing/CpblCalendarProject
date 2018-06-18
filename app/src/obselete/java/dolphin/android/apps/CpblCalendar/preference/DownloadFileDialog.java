package dolphin.android.apps.CpblCalendar.preference;

import android.app.ProgressDialog;
import android.content.Context;
import android.util.Log;

import java.io.File;

import dolphin.android.net.GoogleDriveHelper;
import dolphin.android.net.HttpProgressListener;

/**
 * Created by dolphin on 2015/03/08.
 * <p/>
 * download dialog
 */
public class DownloadFileDialog extends ProgressDialog implements Runnable, HttpProgressListener {
    private final static String TAG = "download";
    private final Context mContext;
    private Thread mThread;
    private String mId;
    private File mDst;
    private long mContentSize;
    private long mEstimateSize;

    private DownloadFileDialog(Context context) {
        super(context);
        mContext = context;
    }

    private void setMessage(int resId) {
        setMessage(mContext.getString(resId));
    }

    private void setDownloadTask(String id, File dst) {
        setDownloadTask(id, dst, 0);
    }

    private void setDownloadTask(String id, File dst, long estSize) {
        mThread = new Thread(this);
        mId = id;
        mDst = dst;
        mEstimateSize = estSize;
        setProgressStyle(mEstimateSize > 0 ? ProgressDialog.STYLE_HORIZONTAL
                : ProgressDialog.STYLE_SPINNER);
    }

    @Override
    public void run() {
        GoogleDriveHelper.download(mContext, mId, mDst, this);
    }

    @Override
    public void show() {
        super.show();
        mThread.start();
    }

    @Override
    public void onStart(long contentSize) {
        mContentSize = contentSize > 0 ? contentSize : mEstimateSize;
    }

    @Override
    public void onUpdate(int bytes, long totalBytes) {
        if (mContentSize > 0) {
            double p =  (double) totalBytes / mContentSize;
            int percentage = Math.min(100, (int)(p * 100));
            //Log.d(TAG, String.format("%.5f %% %d", p, percentage));
            setProgress(percentage);
        }
    }

    @Override
    public void onComplete(long totalBytes) {
        Log.v(TAG, "onComplete: " + totalBytes);
        dismiss();
    }

    public static class Builder {
        private final DownloadFileDialog mDialog;

        public Builder(Context context) {
            mDialog = new DownloadFileDialog(context);
            mDialog.setCancelable(false);
        }

        public Builder setDownloadTask(String url, File dst) {
            mDialog.setDownloadTask(url, dst);
            return this;
        }

        public Builder setDownloadTask(String url, File dst, long estSize) {
            mDialog.setDownloadTask(url, dst, estSize);
            return this;
        }

        public Builder setMessage(int resId) {
            mDialog.setMessage(resId);
            return this;
        }

        public Builder setMessage(String message) {
            mDialog.setMessage(message);
            return this;
        }

        public Builder setOnDismissListener(OnDismissListener listener) {
            mDialog.setOnDismissListener(listener);
            return this;
        }

        public DownloadFileDialog build() {
            return mDialog;
        }

        public DownloadFileDialog show() {
            mDialog.show();
            return mDialog;
        }
    }
}
