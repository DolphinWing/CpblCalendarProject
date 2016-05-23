package dolphin.android.util;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import java.util.List;

public class PackageUtils {
    /**
     * get package info
     *
     * @param context
     * @param cls
     * @return
     */
    public static PackageInfo getPackageInfo(Context context, Class<?> cls) {
        try {
            ComponentName comp = new ComponentName(context, cls);
            return context.getPackageManager().getPackageInfo(
                    comp.getPackageName(), 0);
            //return pinfo;
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }
	
	//http://goo.gl/xtf7I
	public static void enablePackage(Context context, Class<?> cls) {
		ComponentName activity = new ComponentName(context, cls);
		final PackageManager pm = context.getPackageManager();

		pm.setComponentEnabledSetting(activity,
			PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
			PackageManager.DONT_KILL_APP);
	}
	
	public static void disablePackage(Context context, Class<?> cls) {
		ComponentName activity = new ComponentName(context, cls);
		final PackageManager pm = context.getPackageManager();

		pm.setComponentEnabledSetting(activity,
			PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
			PackageManager.DONT_KILL_APP);
	}

    /**
     * check if any activity can handle this intent
     *
     * @param context
     * @param intent
     * @return
     */
    public static boolean isCallable(Context context, Intent intent) {
        if (intent == null)
            return false;
        List<ResolveInfo> list = context.getPackageManager().queryIntentActivities(intent,
                PackageManager.MATCH_DEFAULT_ONLY);
        return list.size() > 0;
    }
}
