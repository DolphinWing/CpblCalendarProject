package dolphin.android.apps.CpblCalendar.provider;

import android.content.Context;

import java.io.File;

/**
 * Created by dolphin on 2016/02/21.
 * To support official build only uses support v4 22.2.0
 */
public class SupprtV4Utils {
    public static File getCacheDir(Context context) {
        return context.getExternalCacheDir();
    }
}
