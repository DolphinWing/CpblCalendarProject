package dolphin.android.apps.CpblCalendar;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.util.SparseArray;

import dolphin.android.apps.CpblCalendar.preference.PreferenceUtils;
import dolphin.android.apps.CpblCalendar.provider.Team;
import dolphin.android.apps.CpblCalendar3.R;

/**
 * Created by jimmyhu on 2017/3/17.
 * <p>
 * https://www.101apps.co.za/articles/making-a-list-coding-multiple-choice-list-dialogs.html
 */

public class MultiChoiceListDialogFragment extends DialogFragment
        implements DialogInterface.OnMultiChoiceClickListener, DialogInterface.OnClickListener {
//    private final static String TAG = "MultiChoiceDialog";
    private int[] mTeams;
    private boolean[] mTeamChecked;
    private OnClickListener mOnClickListener;

    public interface OnClickListener {
        void onOkay();
        void onCancel();
    }

    public MultiChoiceListDialogFragment() {
        super();//
    }

    @SuppressLint("ValidFragment")
    public MultiChoiceListDialogFragment(Context context, OnClickListener listener) {
        String[] ids = context.getResources().getStringArray(R.array.cpbl_team_id);
        mTeams = new int[ids.length];
        mTeamChecked = new boolean[ids.length];
        for (int i = 0; i < ids.length; i++) {
            mTeams[i] = Integer.parseInt(ids[i]);
        }
        mOnClickListener = listener;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        SparseArray<Team> teams = PreferenceUtils.getFavoriteTeams(getActivity());
        for (int i = 0; i < mTeams.length; i++) {
            mTeamChecked[i] = teams.get(mTeams[i]) != null;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setMultiChoiceItems(R.array.cpbl_team_name, mTeamChecked, this)
                .setPositiveButton(android.R.string.ok, this)
                .setNegativeButton(android.R.string.cancel, this)
                .setTitle(R.string.title_favorite_teams);
        return builder.create();
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int which, boolean checked) {
        mTeamChecked[which] = checked;
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int which) {
        switch (which) {
            case DialogInterface.BUTTON_POSITIVE:
//                Log.d(TAG, "BUTTON_POSITIVE");
                saveFavTeams();
                if (mOnClickListener != null) {
                    mOnClickListener.onOkay();
                }
                break;
            case DialogInterface.BUTTON_NEGATIVE:
//                Log.d(TAG, "BUTTON_NEGATIVE");
                if (mOnClickListener != null) {
                    mOnClickListener.onCancel();
                }
                break;
        }
    }

    private void saveFavTeams() {
        SparseArray<Team> teams = new SparseArray<>();
        for (int i = 0; i < mTeams.length; i++) {
            if (mTeamChecked[i]) {
                teams.put(mTeams[i], new Team(getActivity(), mTeams[i]));
            }
        }
        PreferenceUtils.setFavoriteTeams(getActivity(), teams);
    }
}
