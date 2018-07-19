package dolphin.android.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri

class PackageUtils {
    companion object {
        /**
         * get package info
         *
         * @param context
         * @param cls
         * @return
         */
        @JvmStatic
        fun getPackageInfo(context: Context, cls: Class<*>): PackageInfo? {
            return try {
                val comp = ComponentName(context, cls)
                context.packageManager.getPackageInfo(
                        comp.packageName, 0)
                //return pinfo;
            } catch (e: PackageManager.NameNotFoundException) {
                null
            }

        }

        //http://goo.gl/xtf7I
        @JvmStatic
        fun enablePackage(context: Context, cls: Class<*>) {
            val activity = ComponentName(context, cls)
            val pm = context.packageManager

            pm.setComponentEnabledSetting(activity,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP)
        }

        @JvmStatic
        fun disablePackage(context: Context, cls: Class<*>) {
            val activity = ComponentName(context, cls)
            val pm = context.packageManager

            pm.setComponentEnabledSetting(activity,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP)
        }

        /**
         * check if any activity can handle this intent
         *
         * @param context
         * @param intent
         * @return
         */
        @JvmStatic
        fun isCallable(context: Context, intent: Intent?): Boolean {
            if (intent == null)
                return false
            val list = context.packageManager.queryIntentActivities(intent,
                    PackageManager.MATCH_DEFAULT_ONLY)
            return list.size > 0
        }

        @JvmStatic
        fun startGooglePlayApp(context: Context, packageName: String) {
            //http://stackoverflow.com/a/11753070/2673859
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW,
                        Uri.parse("market://details?id=$packageName")))
            } catch (anfe: android.content.ActivityNotFoundException) {
                context.startActivity(Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=$packageName")))
            }

        }
    }
}
