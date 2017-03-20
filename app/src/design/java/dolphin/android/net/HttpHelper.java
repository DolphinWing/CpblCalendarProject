package dolphin.android.net;

import android.content.Context;
import android.net.ConnectivityManager;

/**
 * Created by dolphin on 2013/6/3.
 */
public class HttpHelper extends UrlHelper {
    private static final String TAG = "HttpHelper";

    /**
     * HTTP status code when no server error has occurred.
     */
    public static final int HTTP_STATUS_OK = 200;

    /**
     * check if network is available
     *
     * @param context Context
     * @return true if network available
     */
    public static boolean checkNetworkAvailable(Context context) {
        ConnectivityManager connMgr =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connMgr != null && connMgr.getActiveNetworkInfo() != null) {
            return connMgr.getActiveNetworkInfo().isAvailable();
        }
        return false;
    }

    /**
     * check if network is connected
     *
     * @param context Context
     * @return true if network connected
     */
    public static boolean checkNetworkConnected(Context context) {
        ConnectivityManager connMgr =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connMgr != null && connMgr.getActiveNetworkInfo() != null) {
            return connMgr.getActiveNetworkInfo().isConnected();
        }
        return false;
    }

}
