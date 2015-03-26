package dolphin.android.apps.CpblCalendar.provider;

import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by jimmyhu on 2015/1/5.
 * ASP.NET helper to get rid of __VIEWSTATE & __EVENTVALIDATION
 */
public class AspNetHelper {
    private final static String TAG = "AspNetHelper";
    private String mUrl;
    private String mViewState;
    private String mValidation;
    private String mEncoding = HTTP.UTF_8;

    public AspNetHelper(String url) {
        this(url, HTTP.UTF_8);
    }

    public AspNetHelper(String url, String encoding) {
        mUrl = url;
        mEncoding = encoding;
        mValidation = "";
        mViewState = "";
        //make first request to get view state and validation
        makeRequest("", "");
    }

    public String makeRequest(String name, String value) {
        HttpRequestBase httpRequest;
        String response = null;

        boolean isPost = !mValidation.isEmpty() && !mViewState.isEmpty();
        if (isPost) {
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            if (name != null && !name.isEmpty()) {
                params.add(new BasicNameValuePair(name, value));
                params.add(new BasicNameValuePair("__EVENTTARGET", name));
                params.add(new BasicNameValuePair("__EVENTARGUMENT", ""));
            } else {
                params.add(new BasicNameValuePair("__EVENTTARGET", ""));
                params.add(new BasicNameValuePair("__EVENTARGUMENT", ""));
            }
            params.add(new BasicNameValuePair("__EVENTVALIDATION", mValidation));
            params.add(new BasicNameValuePair("__LASTFOCUS", ""));
            params.add(new BasicNameValuePair("__VIEWSTATE", mViewState));

            /* make HTTP POST request */
            httpRequest = new HttpPost(mUrl);
            try {
                ((HttpPost) httpRequest).setEntity(new UrlEncodedFormEntity(params, mEncoding));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        } else {
            /* make HTTP GET request */
            httpRequest = new HttpGet(mUrl);
            //httpRequest.setHeader("REFERER", mUrl);
            //httpRequest.setHeader("User-Agent", sUserAgent);
        }

        try {
            //http://stackoverflow.com/a/1565243
            HttpParams httpParameters = new BasicHttpParams();
            HttpConnectionParams.setConnectionTimeout(httpParameters, 5000);
            HttpConnectionParams.setSoTimeout(httpParameters, 10000);
            /* get HTTP response */
            HttpResponse httpResponse = new DefaultHttpClient(httpParameters).execute(httpRequest);

            /* if status == 200, ok */
            //Log.d(TAG, String.format("status = %d", httpResponse.getStatusLine().getStatusCode()));
            if (httpResponse.getStatusLine().getStatusCode() == 200) {
                /* get response string */
                response = EntityUtils.toString(httpResponse.getEntity());
            } else {
                Log.e(TAG, String.format("HttpPost getStatusCode = %d",
                        httpResponse.getStatusLine().getStatusCode()));
                response = null;
            }
        } catch (Exception e) {
            response = null;
            Log.e(TAG, "query: " + e.getMessage());
        }

        parseResponseForNextUse(response);
        return response;
    }

    private void parseResponseForNextUse(String response) {
        if (response != null && !response.isEmpty()) {
            //Log.d(TAG, "response " + response.length());

            //save the validation for next request
            if (response.contains("__EVENTVALIDATION")) {
                mValidation = response.substring(response.lastIndexOf("__EVENTVALIDATION"));
                mValidation = mValidation.substring(mValidation.indexOf("value=\""),
                        mValidation.indexOf("\" />"));
                mValidation = mValidation.substring("value=\"".length(), mValidation.length());
                //Log.d(TAG, mValidation);
            }

            //save the view state for next request
            if (response.contains("__VIEWSTATE")) {
                mViewState = response.substring(response.lastIndexOf("__VIEWSTATE"));
                mViewState = mViewState.substring(mViewState.indexOf("value=\""),
                        mViewState.indexOf("\" />"));
                mViewState = mViewState.substring("value=\"".length(), mViewState.length());
                //Log.d(TAG, mViewState);
            }
        }
    }

    public String makeUrlRequest(String name, String value) {
        URL url;
        try {
            url = new URL(mUrl);
        } catch (MalformedURLException e) {
            Log.e(TAG, "MalformedURLException: " + e.getMessage());
            return null;
        }

        HttpURLConnection conn;
        try {
            conn = (HttpURLConnection) url.openConnection();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "openConnection: " + e.getMessage());
            return null;
        }

        String response = null;
        boolean isPost = !mValidation.isEmpty() && !mViewState.isEmpty();
        if (isPost) {//add POST header
            try {
                conn.setRequestMethod("POST");
            } catch (ProtocolException e) {
                e.printStackTrace();
            }

            if (name != null && !name.isEmpty()) {
                conn.setRequestProperty(name, value);
                conn.setRequestProperty("__EVENTTARGET", name);
                conn.setRequestProperty("__EVENTARGUMENT", "");
            } else {
                conn.setRequestProperty("__EVENTTARGET", "");
                conn.setRequestProperty("__EVENTARGUMENT", "");
            }
            conn.setRequestProperty("__EVENTVALIDATION", mValidation);
            conn.setRequestProperty("__LASTFOCUS", "");
            conn.setRequestProperty("__VIEWSTATE", mViewState);
        }

        InputStream in = null;
        try {
            conn.setReadTimeout(10000);
            in = new BufferedInputStream(conn.getInputStream());
            ByteArrayOutputStream content = new ByteArrayOutputStream();

            // Read response into a buffered stream
            byte[] sBuffer = new byte[512];
            int readBytes = 0;
            while ((readBytes = in.read(sBuffer)) != -1) {
                content.write(sBuffer, 0, readBytes);
            }
            response = new String(content.toByteArray(), mEncoding);
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "IOException: " + e.getMessage());
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException e) {
                Log.e(TAG, "close stream exception");
            }
        }

        parseResponseForNextUse(response);
        return response;
    }
}
