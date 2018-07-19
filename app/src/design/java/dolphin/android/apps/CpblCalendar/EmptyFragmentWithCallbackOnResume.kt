package dolphin.android.apps.CpblCalendar

import android.annotation.TargetApi
import android.app.Fragment
import android.content.Context
import android.os.Build

/**
 * Created by dolphin on 2013/7/15.
 *
 *
 * https://code.google.com/p/android/issues/detail?id=23096#c5
 * http://stackoverflow.com/a/12681526
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
class EmptyFragmentWithCallbackOnResume : Fragment() {

    private var mListener: OnFragmentAttachedListener? = null

    override fun onAttach(activity: Context) {
        super.onAttach(activity)
        try {
            mListener = activity as OnFragmentAttachedListener
        } catch (e: ClassCastException) {
            throw ClassCastException(activity.toString() + " must implement OnFragmentAttachedListener")
        }

    }

    override fun onResume() {
        super.onResume()
        if (mListener != null) {
            mListener!!.OnFragmentAttached()
        }
    }

    internal interface OnFragmentAttachedListener {
        fun OnFragmentAttached()
    }

    companion object {
        val TAG = "EmptyFragmentWithCallbackOnResume"
    }
}
