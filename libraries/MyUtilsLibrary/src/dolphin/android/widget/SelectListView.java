package dolphin.android.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.widget.ListView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

// Gmail-like ListView with checkboxes (and using the ActionBar)
// http://stackoverflow.com/a/9768693
public class SelectListView extends ListView
{
	final static String TAG = "SelectListView";
	private SherlockFragmentActivity mActivity;

	public SherlockFragmentActivity getActivity()
	{
		return mActivity;
	}

	public ActionMode mActionMode;

	public SelectListView(Context context)
	{
		this(context, null, 0);
	}

	public SelectListView(Context context, AttributeSet attrs)
	{
		this(context, attrs, 0);
	}

	public SelectListView(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
		setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

		if (isInEditMode()) {
			//can't use Context in Eclipse http://goo.gl/ZqTvW
		} else {
			mActivity = (SherlockFragmentActivity) context;
		}
	}

	@Override
	public boolean performItemClick(View view, int position, long id)
	{
		Log.d(TAG, "performItemClick " + position);
		OnItemClickListener mOnItemClickListener = getOnItemClickListener();
		if (mOnItemClickListener != null) {
			playSoundEffect(SoundEffectConstants.CLICK);
			if (view != null)
				view.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_CLICKED);
			mOnItemClickListener.onItemClick(this, view, position, id);
			return true;
		}
		Log.w(TAG, "mOnItemClickListener is null");
		return false;
	}

	boolean mSelectionMode = false;

	public boolean isSelectionMode()
	{
		return mSelectionMode;
	}

	int mStartPosition;

	@Override
	public boolean onTouchEvent(MotionEvent ev)
	{
		//Log.d(TAG, "onTouch");
		final int action = ev.getAction();
		final int x = (int) ev.getX();
		final int y = (int) ev.getY();

		if (action == MotionEvent.ACTION_DOWN && x < getWidth() / 6) {
			mSelectionMode = true;
			mStartPosition = pointToPosition(x, y);
		}
		//Log.d(TAG, "mSelectionMode " + mSelectionMode);
		if (!mSelectionMode)
			return super.onTouchEvent(ev);
		switch (action) {
			case MotionEvent.ACTION_DOWN:
				break;
			case MotionEvent.ACTION_MOVE:
				if (pointToPosition(x, y) != mStartPosition)
					mSelectionMode = false;
				break;
			case MotionEvent.ACTION_CANCEL:
			case MotionEvent.ACTION_UP:
			default:
				mSelectionMode = false;
				int mItemPosition = pointToPosition(x, y);
				if (mStartPosition != ListView.INVALID_POSITION)
					setItemChecked(mItemPosition, !isItemChecked(mItemPosition));
		}

		return true;
	}

	@Override
	public void setItemChecked(int position, boolean value)
	{
		super.setItemChecked(position, value);
		((CheckableLinearLayout) getChildAt(position)).setChecked(value);
		// boolean r = getAdapter().hasStableIds();
		int checkedCount = getCheckedItemCount();//getCheckItemIds().length;

		if (checkedCount == 0) {
			if (mActionMode != null)
				mActionMode.finish();
			return;
		}
		if (mActionMode == null)
			mActionMode = mActivity.startActionMode(getActionModeCallback());

		mActionMode.setTitle(checkedCount + " selected");
	}

	private ActionMode.Callback mCallback = null;

	public void setActionModeCallback(ActionMode.Callback callback)
	{
		mCallback = callback;
	}

	public ActionMode.Callback getActionModeCallback()
	{
		if (mCallback == null)
			mCallback = new ModeCallback(this);
		return mCallback;
	}

	public static class ModeCallback implements ActionMode.Callback
	{
		private SelectListView mListView = null;

		public ModeCallback(SelectListView view)
		{
			mListView = view;
		}

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu)
		{
			return true;
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu)
		{
			return true;
		}

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item)
		{
			Toast.makeText(mListView.getActivity(), "Deleted items",
				Toast.LENGTH_SHORT).show();
			mode.finish();
			return true;
		}

		@Override
		public void onDestroyActionMode(ActionMode mode)
		{
			mListView.mActionMode = null;
			mListView.clearChecked();
		}

	}

	public void clearChecked()
	{
		SparseBooleanArray CItem = getCheckedItemPositions();
		for (int i = 0; i < CItem.size(); i++)
			if (CItem.valueAt(i))
				super.setItemChecked(CItem.keyAt(i), false);
	}

	public void setAllItemsChecked(boolean checked)
	{
		for (int i = 0; i < getCount(); i++) {
			super.setItemChecked(i, checked);
		}
	}
}
