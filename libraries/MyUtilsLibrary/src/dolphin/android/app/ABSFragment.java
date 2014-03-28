package dolphin.android.app;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;

import com.actionbarsherlock.app.SherlockFragment;

import dolphin.android.os.ABSFragmentHandler;

public class ABSFragment extends SherlockFragment
{
	private final static String TAG = "ABSFragment";

	// http://code.google.com/p/android/issues/detail?id=19917
	@Override
	public void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);
		if (outState.isEmpty()) {
			outState.putBoolean("bug:fix", true);
		}
	}

	private ABSFragmentHandler mHandler = null;

	/**
	 * set Activity message Handler
	 * @param handler
	 */
	public void setMessageHandler(ABSFragmentHandler handler)
	{
		mHandler = handler;
	}

	/**
	 * send Message to Handler
	 * @param what
	 */
	public void send_message(int what)
	{
		send_message(what, 0);
	}

	/**
	 * send Message to Handler
	 * @param what
	 * @param delayMillis
	 */
	public void send_message(int what, int delayMillis)
	{
		send_message(what, 0, 0, delayMillis);
	}

	/**
	 * send Message to Handler
	 * @param what
	 * @param arg1
	 * @param arg2
	 */
	public void send_message(int what, int arg1, int arg2)
	{
		send_message(what, arg1, arg2, 0);
	}

	/**
	 * send Message to Handler
	 * @param what
	 * @param arg1
	 * @param arg2
	 * @param delayMillis
	 */
	public void send_message(int what, int arg1, int arg2, int delayMillis)
	{
		if (mHandler != null) {
			Message msg = mHandler.obtainMessage(what, arg1, arg2);
			if (msg != null)
				if (delayMillis > 0) {
					mHandler.sendMessageDelayed(msg, delayMillis);
				}
				else {
					mHandler.sendMessage(msg);
				}
			else
				Log.e(TAG, "no Message");
		} else {
			Log.e(TAG, "no Handler");
		}
	}

	@SuppressWarnings("deprecation")
	public Cursor managedQuery(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder)
	{
		return getActivity().managedQuery(uri, projection, selection,
			selectionArgs, sortOrder);
	}
}
