package dolphin.android.apps.CpblCalendar;

import android.app.Activity;
import android.support.v4.app.Fragment;

/**
 * Created by dolphin on 2013/7/15.
 * <p/>
 * https://code.google.com/p/android/issues/detail?id=23096#c5
 * http://stackoverflow.com/a/12681526
 */
public class EmptyFragmentWithCallbackOnResume extends Fragment {
    public final static String TAG = "EmptyFragmentWithCallbackOnResume";

    OnFragmentAttachedListener mListener = null;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentAttachedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentAttachedListener");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mListener != null) {
            mListener.OnFragmentAttached();
        }
    }

    public interface OnFragmentAttachedListener {
        void OnFragmentAttached();
    }
}
