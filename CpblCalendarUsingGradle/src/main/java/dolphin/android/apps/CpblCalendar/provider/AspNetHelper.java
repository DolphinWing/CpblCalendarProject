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
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private String mLastResponse;

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
            mLastResponse = response;

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

    /**
     * make POST/GET request by UrlConnection
     * http://www.cnblogs.com/menlsh/archive/2013/05/22/3091983.html
     *
     * @param name  changed field
     * @param value changed data
     * @return html content
     */
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
            conn.setConnectTimeout(3000);
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "openConnection: " + e.getMessage());
            return null;
        }

        String response = null;
        if (!mValidation.isEmpty() && !mViewState.isEmpty()) {//add POST header
            Map<String, String> params = new HashMap<>();

            if (name != null && !name.isEmpty()) {
                params.put(name, value);
                params.put("__EVENTTARGET", name);
                params.put("__EVENTARGUMENT", "");
            } else {
                params.put("__EVENTTARGET", "");
                params.put("__EVENTARGUMENT", "");
            }
            params.put("__EVENTVALIDATION", mValidation);
            params.put("__LASTFOCUS", "");
            params.put("__VIEWSTATE", mViewState);

            try {
                conn.setRequestMethod("POST");
                //Log.d(TAG, "post " + name + ", " + value);
            } catch (ProtocolException e) {
                e.printStackTrace();
            }

            conn.setDoInput(true);
            conn.setDoOutput(true);//set enable output for POST
            conn.setUseCaches(false);//don't use cache for POST

            byte[] data = getRequestData(params, mEncoding).toString().getBytes();
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("Content-Length", String.valueOf(data.length));

            int responseCode = 0;
            OutputStream outputStream = null;
            try {//write POST data
                outputStream = conn.getOutputStream();
                outputStream.write(data);
                responseCode = conn.getResponseCode();
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "write IOException: " + e.getMessage());
            } finally {
                try {
                    if (outputStream != null) {
                        outputStream.close();
                    }
                } catch (IOException e) {
                    Log.e(TAG, "close out stream exception");
                }
            }

            //Log.d(TAG, "response code = " + responseCode);
            if (responseCode != HttpURLConnection.HTTP_OK) {
                Log.e(TAG, String.format("POST failed (%d)", responseCode));
                return null;
            }
        }

        InputStream inputStream = null;
        try {//read POST response
            conn.setReadTimeout(5000);
            inputStream = new BufferedInputStream(conn.getInputStream());
            ByteArrayOutputStream content = new ByteArrayOutputStream();

            // Read response into a buffered stream
            byte[] sBuffer = new byte[512];
            int readBytes = 0;
            while ((readBytes = inputStream.read(sBuffer)) != -1) {
                content.write(sBuffer, 0, readBytes);
            }
            response = new String(content.toByteArray(), mEncoding);
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "read IOException: " + e.getMessage());
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                Log.e(TAG, "close in stream exception");
            }
        }

        //Log.d(TAG, String.format("response length = %d", response.length()));
        parseResponseForNextUse(response);
        return response;
    }

    /**
     * encode POST request data
     *
     * @param params post field
     * @param encode text encoding
     * @return encoded request data
     */
    public static StringBuffer getRequestData(Map<String, String> params, String encode) {
        StringBuffer stringBuffer = new StringBuffer();        //store the request
        try {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                stringBuffer.append(entry.getKey())
                        .append("=")
                        .append(URLEncoder.encode(entry.getValue(), encode))
                        .append("&");
            }
            stringBuffer.deleteCharAt(stringBuffer.length() - 1);    //delete last "&"
        } catch (Exception e) {
            e.printStackTrace();
        }
        return stringBuffer;
    }

    /**
     * get last response data
     *
     * @return response HTML
     */
    public String getLastResponse() {
        return mLastResponse;
    }
}
