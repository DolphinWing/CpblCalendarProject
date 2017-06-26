package dolphin.android.net;

/**
 * Created by dolphin on 2015/03/07.
 */
public interface HttpProgressListener {
    void onStart(long contentSize);
    void onUpdate(int bytes, long totalBytes);
    void onComplete(long totalBytes);
}
