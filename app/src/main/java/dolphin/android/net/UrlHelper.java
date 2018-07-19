package dolphin.android.net;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

/**
 * Created by dolphin on 2015/03/13.
 * <p/>
 * Use UrlConnection to replace org.apache.net
 */
public class UrlHelper {
    private final static String TAG = "UrlHelper";

    public final static int DEFAULT_NETWORK_TIMEOUT = 10 * 1000;

    public final static String ENCODE_UTF8 = "utf-8";
    public final static String ENCODE_BIG5 = "big5";

    private static synchronized String getBody(URL url, int timeoutMillis, String encode)
            throws IOException {
        InputStream in = null;
        try {
            URLConnection urlConnection = url.openConnection();
            urlConnection.setReadTimeout(timeoutMillis);
            in = new BufferedInputStream(urlConnection.getInputStream());
            ByteArrayOutputStream content = new ByteArrayOutputStream();

            // Read response into a buffered stream
            byte[] sBuffer = new byte[512];
            int readBytes = 0;
            while ((readBytes = in.read(sBuffer)) != -1) {
                content.write(sBuffer, 0, readBytes);
            }
            return new String(content.toByteArray(), encode);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                in.close();
            }
        }
        return null;
    }

    public /*static*/ synchronized String getUrlContent(String url, int timeoutMillis, String encode) {
        try {
            return getBody(new URL(url), timeoutMillis, encode);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public /*static*/ synchronized String getUrlContent(String url, String encode) {
        return getUrlContent(url, DEFAULT_NETWORK_TIMEOUT, encode);
    }

    public /*static*/ synchronized String getUrlContent(String url) {
        return getUrlContent(url, ENCODE_UTF8);
    }

    public /*static*/ synchronized boolean getRemoteFile(Context context, String url, String fileName) {
        return getRemoteFile(context, url, fileName, DEFAULT_NETWORK_TIMEOUT);
    }

    public /*static*/ synchronized boolean getRemoteFile(Context context, String url, String fileName,
                                                     HttpProgressListener listener) {
        return getRemoteFile(context, url, fileName, DEFAULT_NETWORK_TIMEOUT, listener);
    }

    public static synchronized boolean getRemoteFile(Context context, String url, String fileName,
                                                     int timeout) {
        return getRemoteFile(context, url, fileName, timeout, null);
    }

    public static synchronized boolean getRemoteFile(Context context, String url, String fileName,
                                                     int timeout, HttpProgressListener listener) {
        try {
            return getRemoteFile(context, new URL(url), fileName, timeout, listener);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @SuppressWarnings("deprecation")
    @SuppressLint("WorldWriteableFiles")
    public static synchronized boolean getRemoteFile(Context context,
                                                     URL aURL, String fileName,
                                                     int timeout, HttpProgressListener listener) {
        boolean result = false;
        try {
            URLConnection conn = aURL.openConnection();
            conn.setReadTimeout(timeout);
            conn.connect();
            // this will be useful so that you can show a typical 0-100% progress bar
            int fileLength = conn.getContentLength();
            //Log.v(TAG, String.format("file length: %d", fileLength));
            if (listener != null) {
                listener.onStart(fileLength);
            }

            BufferedInputStream bis =
                    new BufferedInputStream(conn.getInputStream());
            //Bitmap bum = BitmapFactory.decodeStream(bis);
            OutputStream outstream = null;
            if (fileName.startsWith("/")) {
                outstream = new FileOutputStream(new File(fileName));
            } else {
                outstream = context.openFileOutput(fileName, Context.MODE_WORLD_WRITEABLE);
            }
            if (outstream == null)
                throw new IOException("null stream");
            //result = bm.compress(Bitmap.CompressFormat.PNG, 100, outstream);
            byte data[] = new byte[1024];
            long total = 0;
            int count;
            while ((count = bis.read(data)) != -1) {
                total += count;
                // publishing the progress....
                if (listener != null) {
                    listener.onUpdate(count, total);
                }
                //publishProgress((int) (total * 100 / fileLength));
                outstream.write(data, 0, count);
            }
            Log.v(TAG, String.format("fileLength: %d, total recv: %d", fileLength, total));
            result = fileLength > 0 ? (fileLength == total) : total > 0;
            if (listener != null) {
                listener.onComplete(total);
            }

            bis.close();
            outstream.close();
            outstream = null;
            //bm.recycle();
            //bm = null;
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Exception: " + e.getMessage());
            result = false;
            if (listener != null) {
                listener.onComplete(0);
            }
        }
        return result;
    }
}
