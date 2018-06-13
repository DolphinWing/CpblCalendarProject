package dolphin.android.apps.CpblCalendar;

import android.annotation.TargetApi;
import android.app.Fragment;
import android.content.Context;
import android.os.Build;

/**
 * Created by dolphin on 2013/7/15.
 * <p/>
 * https://code.google.com/p/android/issues/detail?id=23096#c5
 * http://stackoverflow.com/a/12681526
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class EmptyFragmentWithCallbackOnResume extends Fragment {
    public final static String TAG = "EmptyFragmentWithCallbackOnResume";

    private OnFragmentAttachedListener mListener = null;

    @Override
    public void onAttach(Context activity) {
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

    interface OnFragmentAttachedListener {
        void OnFragmentAttached();
    }
}
