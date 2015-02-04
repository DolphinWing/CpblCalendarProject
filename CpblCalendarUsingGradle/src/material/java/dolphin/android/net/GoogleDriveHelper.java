package dolphin.android.net;

import android.content.Context;

import java.io.File;

/**
 * Created by 97011424 on 2014/1/14.
 */
public class GoogleDriveHelper {
    public static String getUrl(String id) {
        return "https://drive.google.com/uc?export=download&id=" + id;
    }

    /**
     * download file from Google Drive
     *
     * @param context
     * @param id
     * @param dst
     */
    public static boolean download(Context context, String id, File dst) {
        if (!HttpHelper.checkNetworkAvailable(context))
            return false;
        //http://stackoverflow.com/a/11855448
        //https://drive.google.com/uc?export=download&id={fileId}
        String url = getUrl(id);
        boolean r = HttpHelper.getRemoteFile(context, url, dst.getAbsolutePath());
        //Log.v(TAG, "download " + (r ? "success" : "failed"));
        return r;
    }
}
