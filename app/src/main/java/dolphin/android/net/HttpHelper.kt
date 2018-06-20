package dolphin.android.net

import android.content.Context
import android.net.ConnectivityManager

/**
 * Created by dolphin on 2013/6/3.
 */
open class HttpHelper(val context: Context) : UrlHelper() {
    companion object {
        private const val TAG = "HttpHelper"

        /**
         * HTTP status code when no server error has occurred.
         */
        const val HTTP_STATUS_OK = 200

        /**
         * check if network is available
         *
         * @param context Context
         * @return true if network available
         */
        @JvmStatic
        fun checkNetworkAvailable(context: Context): Boolean {
            val connMgr = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            return if (connMgr.activeNetworkInfo != null) {
                connMgr.activeNetworkInfo.isAvailable
            } else false
        }

        /**
         * check if network is connected
         *
         * @param context Context
         * @return true if network connected
         */
        @JvmStatic
        fun checkNetworkConnected(context: Context): Boolean {
            val connMgr = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            return if (connMgr.activeNetworkInfo != null) {
                connMgr.activeNetworkInfo.isConnected
            } else false
        }
    }

    val isNetworkAvailable: Boolean
        get() = (context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager)
                .activeNetworkInfo?.isAvailable ?: false

    val isNetworkConnected: Boolean
        get() = (context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager)
                .activeNetworkInfo?.isConnected ?: false

    fun getRemoteFile(url: String, path: String, listener: HttpProgressListener? = null): Boolean {
        return UrlHelper.getRemoteFile(context, url, path, DEFAULT_NETWORK_TIMEOUT, listener)
    }
}
