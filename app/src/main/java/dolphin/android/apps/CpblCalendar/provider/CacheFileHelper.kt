package dolphin.android.apps.CpblCalendar.provider

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import dolphin.android.apps.CpblCalendar3.R
import dolphin.android.util.FileUtils
import java.io.File
import java.util.*

internal class CacheFileHelper(private val context: Context) {

    companion object {
        /**
         * Get cache dir for app
         *
         * @param context Context
         * @return cache dir
         */
        @JvmStatic
        fun getCacheDir(context: Context?): File? {
            if (context == null) return null
            return if (ContextCompat.checkSelfPermission(context,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
                context.externalCacheDir
            else
                context.cacheDir
        }

        @JvmStatic
        fun putCache(context: Context?, fileName: String, list: ArrayList<Game>?): Boolean {
            if (context == null) {
                return false
            }
            //String fileName = String.format("%04d-%02d.json", year, month);
            var f = File(getCacheDir(context), fileName)
            if (list == null) {
                return f.delete()
            }
            //convert ArrayList<Game> object to JSON string
            //Log.d(TAG, "putCache " + f.getAbsolutePath());
            var r = FileUtils.writeStringToFile(f, Game.listToJson(context, list))
            if (!r) {
                f = File(context.cacheDir, fileName)
                //Log.d(TAG, "putCache " + f.getAbsolutePath());
                r = FileUtils.writeStringToFile(f, Game.listToJson(context, list))
            }
            return r
        }

        @JvmStatic
        fun getCache(context: Context?, fileName: String): ArrayList<Game>? {
            if (context != null) {
                //String fileName = String.format("%04d-%02d.json", year, month);
                var f = File(getCacheDir(context), fileName)
                //Log.d(TAG, "getCache " + f.getAbsolutePath());
                if (f.exists()) {
                    //convert JSON string to ArrayList<Game> object
                    return Game.listFromJson(context, FileUtils.readFileToString(f))
                }
                f = File(context.cacheDir, fileName)
                //Log.d(TAG, "getCache " + f.getAbsolutePath());
                if (f.exists()) {
                    //convert JSON string to ArrayList<Game> object
                    return Game.listFromJson(context, FileUtils.readFileToString(f))
                }
            }
            return null
        }
    }

    val canUseCache = context.resources.getBoolean(R.bool.feature_cache)

    private fun getCacheJsonFileName(year: Int, month: Int) =
            String.format(Locale.US, "%04d-%02d.json", year, month)

//    internal fun getCache(context: Context, year: Int, month: Int): ArrayList<Game>? {
//        return getCache(context, String.format(Locale.US, "%04d-%02d.json", year, month))
//    }

    fun getCache(year: Int, month: Int): ArrayList<Game>? {
        return if (canUseCache) getCache(context, getCacheJsonFileName(year, month)) else null
    }

//    fun putCache(context: Context, year: Int, month: Int, list: ArrayList<Game>): Boolean {
//        return putCache(context, String.format(Locale.US, "%04d-%02d.json", year, month), list)
//    }

    fun putCache(year: Int, month: Int, list: ArrayList<Game>): Boolean {
        return canUseCache && putCache(context, getCacheJsonFileName(year, month), list)
    }

//    fun hasCache(year: Int, month: Int): Boolean {
//        return getCache(year, month) != null
//    }
//
//    fun hasCache(context: Context, year: Int, month: Int): Boolean {
//        return getCache(context, year, month) != null
//    }

    private fun getCacheJsonFileName(year: Int, month: Int, kind: Int) =
            String.format(Locale.US, "%04d-%02d-%d.json", year, month, kind)

    fun putLocalCache(year: Int, month: Int, kind: Int, list: ArrayList<Game>?): Boolean {
        return putCache(context, getCacheJsonFileName(year, month, kind), list)
    }

    fun removeLocalCache(year: Int, month: Int, kind: Int): Boolean {
        return putLocalCache(year, month, kind, null)
    }

    fun getLocalCache(year: Int, month: Int, kind: Int): ArrayList<Game>? {
        return if (canUseCache) getCache(context, getCacheJsonFileName(year, month, kind)) else null
    }
}