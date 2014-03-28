package dolphin.android.app;

import android.os.Bundle;
import android.os.Message;
import android.util.Log;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;

import dolphin.android.libs.R;
import dolphin.android.os.ABSActivityHandler;

public class ABSActivity extends SherlockActivity
{
	private final static String TAG = "ABSActivity";
	private ActionBar mActionBar = null;

	/**
	 * get ActionBarSherlock supported ActionBar
	 * @return com.actionbarsherlock.app.ActionBar
	 */
	public ActionBar getSActionBar()
	{
		if (mActionBar == null)
			mActionBar = (ActionBar) getSupportActionBar();
		return mActionBar;
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		setTheme(R.style.Theme_Sherlock);
		super.onCreate(savedInstanceState);

		//This has to be called before setContentView and you must use the
		//class in com.actionbarsherlock.view and NOT android.view
		//requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

		//use ActionBarSherlock library
		mActionBar = (ActionBar) getSupportActionBar();
	}

	public Thread.UncaughtExceptionHandler onUncaughtExceptionHandler =
		new Thread.UncaughtExceptionHandler() {

			@Override
			public void uncaughtException(Thread arg0, Throwable arg1)
			{
				Log.e(TAG, "thread exception!" + arg1.getMessage());
			}
		};

	public void show_loading(boolean bShown)
	{
		setSupportProgressBarIndeterminateVisibility(bShown);
	}

	public void close_program()
	{
		this.finish();
	}

	private ABSActivityHandler mHandler = null;

	/**
	 * set Activity message Handler
	 * @param handler
	 */
	public void setMessageHandler(ABSActivityHandler handler)
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

}
