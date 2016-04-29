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
class SupprtV4Utils {
    public static File getCacheDir(Context context) {
        return (ContextCompat.checkSelfPermission(context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
                ? context.getExternalCacheDir() : context.getCacheDir();
        //return context.getExternalCacheDir();
    }
}
