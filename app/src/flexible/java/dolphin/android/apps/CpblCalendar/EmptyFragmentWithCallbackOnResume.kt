package dolphin.android.apps.CpblCalendar

import android.content.Context
import androidx.fragment.app.Fragment

/**
 * https://code.google.com/p/android/issues/detail?id=23096#c5
 * http://stackoverflow.com/a/12681526
 */
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
        mListener?.onFragmentAttached()
    }

    internal interface OnFragmentAttachedListener {
        fun onFragmentAttached()
    }

    companion object {
        val TAG = "EmptyFragmentWithCallbackOnResume"
    }
}
