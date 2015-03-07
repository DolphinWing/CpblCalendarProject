package dolphin.android.net;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Created by dolphin on 2013/6/3.
 */
public class HttpHelper {
    private static final String TAG = "HttpHelper";

    private final static int DEFAULT_NETWORK_TIMEOUT = 20000;

    public final static String ENCODE_UTF8 = HTTP.UTF_8;//"utf-8";
    public final static String ENCODE_BIG5 = "big5";

    /**
     * {@link org.apache.http.StatusLine} HTTP status code when no server error has occurred.
     */
    private static final int HTTP_STATUS_OK = 200;

    /**
     * Shared buffer used by {@link #getUrlContent(String)} when reading results
     * from an API request.
     */
    private static byte[] sBuffer = new byte[512];

    /**
     * User-agent string to use when making requests. Should be filled using
     * {@link #prepareUserAgent(android.content.Context)} before making any other calls.
     */
    private static String sUserAgent = null;

    /**
     * Prepare the internal User-Agent string for use. This requires a
     * {@link android.content.Context} to pull the package name and version number for this
     * application.
     */
    public static void prepareUserAgent(Context context) {
        try {
            // Read package name and version number from manifest
            PackageManager manager = context.getPackageManager();
            PackageInfo info = manager.getPackageInfo(context.getPackageName(), 0);
            sUserAgent = String.format("%s/%s (Linux; Android)", info.packageName, info.versionName);

        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Couldn't find package information in PackageManager", e);
        }
    }

    /**
     * Pull the raw text content of the given URL. This call blocks until the
     * operation has completed, and is synchronized because it uses a shared
     * buffer {@link #sBuffer}.
     *
     * @param url The exact URL to request.
     * @return The raw content returned by the server.
     * @throws Exception If any connection or server error occurs.
     */
    public static synchronized String getUrlContent(String url, int timeout,
                                                    String encode) throws Exception {
        // if (sUserAgent == null) {
        // throw new Exception("User-Agent string must be prepared");
        // }

        try {
            //How to set HttpResponse timeout for Android in Java
            //http://stackoverflow.com/questions/693997/how-to-set-httpresponse-timeout-for-android-in-java
            HttpParams httpParameters = new BasicHttpParams();
            // Set the timeout in milliseconds until a connection is established.
            HttpConnectionParams.setConnectionTimeout(httpParameters, timeout);
            // Set the default socket timeout (SO_TIMEOUT)
            // in milliseconds which is the timeout for waiting for data.
            HttpConnectionParams.setSoTimeout(httpParameters, timeout);

            // Create client and set our specific user-agent string
            HttpClient client = new DefaultHttpClient(httpParameters);
            HttpGet request = new HttpGet(url);// Uri.encode(url)
            request.setHeader(HTTP.USER_AGENT, sUserAgent);
            //request.setHeader("Content-Type", "text/xml; charset=" + encode);
            //request.setHeader("HTTP_REFERRER", url);
            request.setHeader("REFERER", url);//put referer(referrer) to header
            //request.setHeader("REFERRER", url);
            // return sUserAgent;

            // Return result from buffered stream
            return readFromResponse(client.execute(request), encode);
            //}
        } catch (IOException e) {
            // throw new Exception("Problem communicating with API", e);
            return "IOException: " + e.getMessage();
        }
    }

    private static String readFromResponse(HttpResponse response, String encode) throws Exception {
        // Check if server response is valid
        StatusLine status = response.getStatusLine();
        if (status.getStatusCode() != HTTP_STATUS_OK) {
            throw new Exception("Invalid response from server: " + status.toString());
        }

        // Pull content stream from response
        HttpEntity entity = response.getEntity();
        InputStream inputStream = entity.getContent();

        //if (ENCODE_UTF8 == encode) {
        ByteArrayOutputStream content = new ByteArrayOutputStream();

        // Read response into a buffered stream
        int readBytes = 0;
        while ((readBytes = inputStream.read(sBuffer)) != -1) {
            content.write(sBuffer, 0, readBytes);
        }
        return new String(content.toByteArray(), encode);
    }

    /**
     * get URL contents
     *
     * @param url
     * @param encode
     * @return
     * @throws Exception
     */
    public static synchronized String getUrlContent(String url, String encode)
            throws Exception {
        return getUrlContent(url, DEFAULT_NETWORK_TIMEOUT, encode);
    }

    public static synchronized String getUrlContent(String url, int timeout)
            throws Exception {
        return getUrlContent(url, timeout, ENCODE_UTF8);
    }

    public static synchronized String getUrlContent(String url)
            throws Exception {
        return getUrlContent(url, DEFAULT_NETWORK_TIMEOUT);
    }

    public static synchronized String postUrlContent(String url, List<NameValuePair> params, int timeout,
                                                     String encode) throws Exception {

        HttpParams httpParameters = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpParameters, timeout);
        HttpConnectionParams.setSoTimeout(httpParameters, timeout);

        HttpClient client = new DefaultHttpClient(httpParameters);
        HttpPost request = new HttpPost(url);
        //request.setHeader("User-Agent", sUserAgent);
        //request.setHeader("REFERER", url);//put referer(referrer) to header
        try {
            request.setEntity(new UrlEncodedFormEntity(params, encode));
            // Return result from buffered stream
            return readFromResponse(client.execute(request), encode);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * remove some ISO-8859 encoded HTML
     *
     * @param content
     * @return
     */
    public static String removeIso8859HTML(String content) {
        String res_content = content;
        // HTML ISO-8859-1 Reference
        // http://www.w3schools.com/tags/ref_entities.asp
        res_content = res_content.replace("&lt;", "<").replace("&#60;", "<");// less-than
        res_content = res_content.replace("&gt;", ">").replace("&#62;", ">");// greater-than
        res_content = res_content.replace("&quot;", "\"").replace("&#34;", "\"");// quotation
        // mark
        res_content = res_content.replace("&apos;", "'").replace("&#39;", "'");// apostrophe
        res_content = res_content.replace("&amp;", "&").replace("&#38;", "&");// ampersand
        res_content = res_content.replace("&nbsp;", " ").replace("&#160;", " ");// non-breaking
        // space
        res_content = res_content.replace("&deg;", "°").replace("&#176;", "°");// degree
        return res_content;
    }

    public static String removeHTML(String content) {
        String tmp = Pattern.compile("<!--[^-]*-->").matcher(content).replaceAll("").trim();
        return Pattern.compile("<[^>]*>").matcher(tmp).replaceAll("").trim();
    }

    /**
     * check if network is available
     *
     * @param context
     * @return
     */
    public static boolean checkNetworkAvailable(Context context) {
        ConnectivityManager connMgr =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connMgr != null && connMgr.getActiveNetworkInfo() != null)
            return connMgr.getActiveNetworkInfo().isAvailable();
        return false;
    }


    public static synchronized boolean getRemoteFile(Context context, String url, String fileName) {
        return getRemoteFile(context, url, fileName, DEFAULT_NETWORK_TIMEOUT);
    }

    public static synchronized boolean getRemoteFile(Context context, String url, String fileName,
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
