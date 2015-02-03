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
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by jimmyhu on 2015/1/5.
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
            /* get HTTP response */
            HttpResponse httpResponse = new DefaultHttpClient().execute(httpRequest);

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
        return response;
    }
}
