package dolphin.android.util;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

public class AssetUtils
{
    /**
     * read assets from resource
     * @param context
     * @param asset_name
     * @param encoding
     * @return
     */
    public static String read_asset_text(Context context, String asset_name,
            String encoding)
    {
        try {
            InputStreamReader sr =
                new InputStreamReader(context.getAssets().open(asset_name),
                        (encoding != null) ? encoding : "UTF8");
            //Log.i(TAG, asset_name + " " + sr.getEncoding());

            int len = 0;
            StringBuilder sb = new StringBuilder();

            while (true) {//read from buffer
                char[] buffer = new char[1024];
                len = sr.read(buffer);//, size, 512);
                //Log.d(TAG, String.format("%d", len));
                if (len > 0) {
                    sb.append(buffer);
                }
                else {
                    break;
                }
            }
            //Log.i(TAG, String.format("  length = %d", sb.length()));

            sr.close();
            return sb.toString().trim();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static void copyAssets(Context context, String asset_name, String outFolderPath)
            throws IOException
    {
        copyAssets(context, asset_name, new File(outFolderPath));
    }

    public static void copyAssets(Context context, String asset_name, File outFolder)
            throws IOException
    {
        if (outFolder == null)
            throw new IOException("Actuall I'm NullPointerException!");
        if (!outFolder.exists())
            throw new IOException("not exist: " + outFolder.getAbsolutePath());
        if (!outFolder.isDirectory())
            throw new IOException("not dir: " + outFolder.getAbsolutePath());

        //http://stackoverflow.com/a/4530294
        //AssetManager assetManager = context.getAssets();
        //String[] files = null;
        //try {
        //    files = assetManager.list("");
        //} catch (IOException e) {
        //    Log.e("tag", "Failed to get asset file list.", e);
        //}
        //for(String filename : files) {
        InputStream in = context.getAssets().open(asset_name);
        OutputStream out = new FileOutputStream(new File(outFolder, asset_name));
        //try {
        copyFile(in, out);
        in.close();
        in = null;
        out.flush();
        out.close();
        out = null;
        //} catch (IOException e) {
        //    e.printStackTrace();
        //}
        //}
    }

    private static void copyFile(InputStream in, OutputStream out) throws IOException
    {
        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }
}
