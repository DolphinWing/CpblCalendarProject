package dolphin.android.net;

/**
 * Created by dolphin on 2015/03/07.
 */
public interface HttpProgressListener {
    public void onStart(long contentSize);
    public void onUpdate(int bytes, long totalBytes);
    public void onComplete(long totalBytes);
}
