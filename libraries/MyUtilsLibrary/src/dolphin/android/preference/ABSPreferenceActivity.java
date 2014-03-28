package dolphin.android.preference;

import java.io.IOException;
import java.io.InputStreamReader;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.util.Log;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockPreferenceActivity;

import dolphin.android.libs.R;

public class ABSPreferenceActivity extends SherlockPreferenceActivity
{
	private final static String TAG = "ABSActivity";
	private ActionBar mActionBar = null;

	public ActionBar getSActionBar()
	{
		return mActionBar;
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		//setTheme(R.style.Theme_Sherlock);
		super.onCreate(savedInstanceState);

		//This has to be called before setContentView and you must use the
		//class in com.actionbarsherlock.view and NOT android.view
		//requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

		//use ActionBarSherlock library
		mActionBar = (ActionBar) getSupportActionBar();
	}

	public static PackageInfo getPackageInfo(Context context, Class<?> cls)
	{
		try {
			ComponentName comp = new ComponentName(context, cls);
			return context.getPackageManager().getPackageInfo(
				comp.getPackageName(), 0);
			//return pinfo;
		} catch (PackageManager.NameNotFoundException e) {
			return null;
		}
	}

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
			Log.i(TAG, asset_name + " " + sr.getEncoding());

			int len = 0;
			StringBuilder sb = new StringBuilder();

			while (true) {//read from buffer
				char[] buffer = new char[512];
				len = sr.read(buffer);//, size, 512);
				//Log.d(TAG, String.format("%d", len));
				if (len > 0) {
					sb.append(buffer);
				}
				else {
					break;
				}
			}
			Log.i(TAG, String.format("  length = %d", sb.length()));

			sr.close();
			return sb.toString();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

	/**
	 * get default SharedPreferences
	 * @return
	 */
	public SharedPreferences getSharedPreferences()
	{
		return getSharedPreferences(this);
	}

	/**
	 * get default SharedPreferences
	 * @param context
	 * @return
	 */
	public static SharedPreferences getSharedPreferences(Context context)
	{
		return PreferenceManager.getDefaultSharedPreferences(context);
	}

	/**
	 * get String from Preference
	 * @param resKey
	 * @param defKey
	 * @return
	 */
	public String getPreferenceString(int resKey, int defKey)
	{
		return getPreferenceString(resKey, getString(defKey));
	}

	/**
	 * get String from Preference
	 * @param resKey
	 * @param defaultValue
	 * @return
	 */
	public String getPreferenceString(int resKey, String defaultValue)
	{
		return getPreferenceString(getString(resKey), defaultValue);
	}

	/**
	 * get String from Preference
	 * @param key
	 * @param defaultValue
	 * @return
	 */
	public String getPreferenceString(String key, String defaultValue)
	{
		return getSharedPreferences().getString(key, defaultValue);
	}

	/**
	 * get int from Preference
	 * @param resKey
	 * @param defValue
	 * @return
	 */
	public int getPreferenceInteger(int resKey, int defValue)
	{
		return getPreferenceInteger(getString(resKey), defValue);
	}

	/**
	 * get int from Preference
	 * @param key
	 * @param defValue
	 * @return
	 */
	public int getPreferenceInteger(String key, int defValue)
	{
		return getSharedPreferences().getInt(key, defValue);
	}

	/**
	 * get boolean from Preference
	 * @param resKey
	 * @param defKey
	 * @return
	 */
	public boolean getPreferenceBoolean(int resKey, int defKey)
	{
		return getPreferenceBoolean(resKey, getResources().getBoolean(defKey));
	}

	/**
	 * get boolean from Preference
	 * @param resKey
	 * @param defaultValue
	 * @return
	 */
	public boolean getPreferenceBoolean(int resKey, boolean defaultValue)
	{
		return getPreferenceBoolean(getString(resKey), defaultValue);
	}

	/**
	 * get boolean from Preference
	 * @param key
	 * @param defaultValue
	 * @return
	 */
	public boolean getPreferenceBoolean(String key, boolean defaultValue)
	{
		return getSharedPreferences().getBoolean(key, defaultValue);
	}

	/**
	 * put String to Preference
	 * @param resKey
	 * @param value
	 */
	public void putPreference(int resKey, String value)
	{
		putPreference(getString(resKey), value);
	}

	/**
	 * put String to Preference
	 * @param key
	 * @param value
	 */
	public void putPreference(String key, String value)
	{
		getSharedPreferences().edit().putString(key, value).commit();
	}

	/**
	 * put boolean to Preference
	 * @param resKey
	 * @param value
	 */
	public void putPreference(int resKey, boolean value)
	{
		putPreference(getString(resKey), value);
	}

	/**
	 * put boolean to Preference
	 * @param key
	 * @param value
	 */
	public void putPreference(String key, boolean value)
	{
		getSharedPreferences().edit().putBoolean(key, value).commit();
	}

	/**
	 * put Integer to Preference
	 * @param key
	 * @param value
	 */
	public void putPreference(String key, int value)
	{
		getSharedPreferences().edit().putInt(key, value).commit();
	}

	@SuppressWarnings("deprecation")
	public Preference findPreferenceById(int keyId)
	{
		return findPreference(getString(keyId));
	}
}
