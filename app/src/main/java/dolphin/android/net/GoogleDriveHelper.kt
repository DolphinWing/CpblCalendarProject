package dolphin.android.net

import android.content.Context
import android.util.Log

import java.io.File

/**
 * Created by 97011424 on 2014/1/14.
 */
class GoogleDriveHelper(context: Context) {
    private val httpHelper = HttpHelper(context)

    private fun getUrl(id: String): String {
        return "https://drive.google.com/uc?export=download&id=$id"
    }

    @JvmOverloads
    fun download(id: String, dst: File, listener: HttpProgressListener? = null): Boolean {
        if (httpHelper.isNetworkAvailable && httpHelper.isNetworkConnected) {
            httpHelper.getRemoteFile(getUrl(id), dst.absolutePath, listener)
            return dst.exists()
        }
        return false
    }

//    @JvmOverloads
//    fun download(context: Context, id: String, dst: File,
//                 listener: HttpProgressListener? = null): Boolean {
//        if (!HttpHelper.checkNetworkConnected(context)) {
//            return false
//        }
//        //http://stackoverflow.com/a/11855448
//        //https://drive.google.com/uc?export=download&id={fileId}
//        val url = getUrl(id)
//        val r = HttpHelper.getRemoteFile(context, url, dst.absolutePath, listener)
//        Log.v("GoogleDriveHelper", "download " + if (r) "success" else "failed")
//        return dst.exists()
//    }
}
