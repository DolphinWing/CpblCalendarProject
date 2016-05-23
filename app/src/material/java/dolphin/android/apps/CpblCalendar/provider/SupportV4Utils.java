package dolphin.android.apps.CpblCalendar.provider;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.content.ContextCompat;

import java.io.File;

/**
 * Created by dolphin on 2016/02/21.
 * To support official build only uses support v4 22.2.0
 */
public class SupportV4Utils {
    /**
     * Returns proper cache directory
     * Starting in M, we need to check external storage permission.
     *
     * @param context Context
     * @return cache directory
     */
    public static File getCacheDir(Context context) {
        return (ContextCompat.checkSelfPermission(context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
                ? context.getExternalCacheDir() : context.getCacheDir();
        //return context.getExternalCacheDir();
    }

    /**
     * Returns a color associated with a particular resource ID
     * Starting in M, the returned color will be styled for the specified Context's theme.
     * <p/>
     * http://stackoverflow.com/a/31590927/2673859
     *
     * @param context Context
     * @param colorId resource id
     * @return color
     */
    public static int getColor(Context context, int colorId) {
        return ContextCompat.getColor(context, colorId);
    }
}
