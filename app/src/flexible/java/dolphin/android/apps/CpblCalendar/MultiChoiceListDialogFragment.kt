package dolphin.android.apps.CpblCalendar

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import androidx.annotation.Keep
import androidx.fragment.app.DialogFragment
import android.util.SparseArray

import dolphin.android.apps.CpblCalendar.preference.PreferenceUtils
import dolphin.android.apps.CpblCalendar.preference.PrefsHelper
import dolphin.android.apps.CpblCalendar.provider.Team
import dolphin.android.apps.CpblCalendar3.R

/**
 * Created by jimmyhu on 2017/3/17.
 *
 *
 * https://www.101apps.co.za/articles/making-a-list-coding-multiple-choice-list-dialogs.html
 */
@Keep
class MultiChoiceListDialogFragment : DialogFragment, DialogInterface.OnMultiChoiceClickListener,
        DialogInterface.OnClickListener {
    //    private final static String TAG = "MultiChoiceDialog";
    private lateinit var mTeams: IntArray
    private lateinit var mTeamChecked: BooleanArray
    private lateinit var mPrefs: PrefsHelper
    private var mOnClickListener: OnClickListener? = null

    interface OnClickListener {
        fun onOkay()
        fun onCancel()
    }

    constructor() : super()

    @SuppressLint("ValidFragment")
    constructor(context: Context, listener: OnClickListener? = null) {
        mPrefs = PrefsHelper(context)
        val ids = context.resources.getStringArray(R.array.cpbl_team_id)
        mTeams = IntArray(ids.size)
        mTeamChecked = BooleanArray(ids.size)
        for (i in ids.indices) {
            mTeams[i] = Integer.parseInt(ids[i])
        }
        mOnClickListener = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val teams = mPrefs.favoriteTeams //PreferenceUtils.getFavoriteTeams(activity)
        for (i in mTeams.indices) {
            mTeamChecked[i] = teams.get(mTeams[i]) != null
        }
        val builder = AlertDialog.Builder(activity, R.style.Theme_MaterialComponents_Dialog_Alert)
                .setMultiChoiceItems(R.array.cpbl_team_name, mTeamChecked, this)
                .setPositiveButton(android.R.string.ok, this)
                .setNegativeButton(android.R.string.cancel, this)
                .setTitle(R.string.title_favorite_teams)
        return builder.create()
    }

    override fun onClick(dialogInterface: DialogInterface, which: Int, checked: Boolean) {
        mTeamChecked[which] = checked
    }

    override fun onClick(dialogInterface: DialogInterface, which: Int) {
        when (which) {
            DialogInterface.BUTTON_POSITIVE -> {
                //                Log.d(TAG, "BUTTON_POSITIVE");
                saveFavTeams()
                mOnClickListener?.onOkay()
            }
            DialogInterface.BUTTON_NEGATIVE ->
                //                Log.d(TAG, "BUTTON_NEGATIVE");
                mOnClickListener?.onCancel()
        }
    }

    private fun saveFavTeams() {
        val teams = SparseArray<Team>()
        for (i in mTeams.indices) {
            if (mTeamChecked[i]) {
                teams.put(mTeams[i], Team(activity, mTeams[i]))
            }
        }
        mPrefs.favoriteTeams = teams //PreferenceUtils.setFavoriteTeams(activity, teams)
    }
}
