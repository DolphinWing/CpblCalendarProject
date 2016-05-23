package dolphin.android.os;

import java.lang.ref.WeakReference;

import android.os.Handler;
import android.view.View;
import dolphin.android.app.ABSActivity;

public class ABSActivityHandler extends Handler
{
	//Handlers and memory leaks in Android
	//http://stackoverflow.com/a/11336822

	private final WeakReference<ABSActivity> mActivity;

	//private ABSActivity mActivity;

	public ABSActivity getActivity()
	{
		return mActivity.get();
	}

	public ABSActivityHandler(ABSActivity activity)
	{
		mActivity = new WeakReference<ABSActivity>(activity);
	}

	/**
	 * find View in Activity by ID
	 * @param id
	 * @return
	 */
	public View findViewById(int id)
	{
		return getActivity().findViewById(id);
	}

	/**
	 * get resource string
	 * @param resId
	 * @return
	 */
	public String getString(int resId)
	{
		return getActivity().getString(resId);
	}

	/**
	 * get resource string
	 * @param resId
	 * @param formatArgs
	 * @return
	 */
	public String getString(int resId, Object... formatArgs)
	{
		return getActivity().getString(resId, formatArgs);
	}

	/**
	 * send Message to Activity Handler
	 * @param what
	 */
	public void send_message(int what)
	{
		getActivity().send_message(what, 0);
	}

	/**
	 * send Message to Activity Handler
	 * @param what
	 * @param delayMillis
	 */
	public void send_message(int what, int delayMillis)
	{
		getActivity().send_message(what, 0, 0, delayMillis);
	}

	/**
	 * send Message to Activity Handler
	 * @param what
	 * @param arg1
	 * @param arg2
	 */
	public void send_message(int what, int arg1, int arg2)
	{
		getActivity().send_message(what, arg1, arg2, 0);
	}

	/**
	 * send Message to Activity Handler
	 * @param what
	 * @param arg1
	 * @param arg2
	 * @param delayMillis
	 */
	public void send_message(int what, int arg1, int arg2, int delayMillis)
	{
		getActivity().send_message(what, arg1, arg2, delayMillis);
	}
}
