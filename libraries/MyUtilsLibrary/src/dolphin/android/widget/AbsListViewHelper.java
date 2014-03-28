package dolphin.android.widget;

import android.os.Build;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListAdapter;

/**
 * This helper class is mainly to solve the pre-ICS Android 
 * has different AbsListView definition in SDK
 *  
 * @author Jimmy Hu <Jimmy.Hu@quantatw.com>
 *
 */
public class AbsListViewHelper
{
	private static final String TAG = "AbsListViewHelper";

	private AbsListView mListView = null;
	SparseBooleanArray mCheckedArray = new SparseBooleanArray();
	boolean mSelectionMode = false;

	public AbsListViewHelper(AbsListView listView)
	{
		mListView = listView;
	}

	/**
	 * Sets the adapter that provides the data and the views to represent the data in this widget.
	 * @param adapter
	 */
	public void setAdapter(ListAdapter adapter)
	{
		//http://stackoverflow.com/a/6643799
		((AdapterView<ListAdapter>) mListView).setAdapter(adapter);
		mCheckedArray.clear();
		mCheckedArray = new SparseBooleanArray();
		setSelectionMode(false);//[29]jimmy++ restart
	}

	/**
	 * Defines the choice behavior for the List.
	 * @param choiceMode
	 */
	public void setChoiceMode(int choiceMode)
	{
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			mListView.setChoiceMode(choiceMode);
		} else {
			Log.w(TAG, "setChoiceMode is not working on SDK = "
				+ Build.VERSION.SDK_INT);
		}
	}

	/**
	 * Gets the data associated with the specified position in the list.
	 * @param position
	 * @return
	 */
	public Object getItemAtPosition(int position)
	{
		return mListView.getItemAtPosition(position);
	}

	/**
	 * Returns the set of checked items in the list. 
	 * @return 
	 */
	public SparseBooleanArray getCheckedItemPositions()
	{
		//SparseBooleanArray checked = null;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			return mListView.getCheckedItemPositions();
		} //else {
			//checked = new SparseBooleanArray();
		//}
		return mCheckedArray;
	}

	/**
	 * Sets the checked state of the specified position.
	 * @param position
	 * @param checked
	 */
	public void setItemChecked(int position, boolean checked)
	{
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			mListView.setItemChecked(position, checked);
		} else {
			if (checked) {
				mCheckedArray.put(position, true);
			} else {
				mCheckedArray.delete(position);
			}

			int fp = mListView.getFirstVisiblePosition();//jimmy++ fix invalid selection
			//Log.d(TAG, "position: " + position + " " + fp);
			View v = mListView.getChildAt(position - fp);//related to first position
			//Log.d(TAG,
			//	String.format("setItemChecked: %d %s", position, checked));
			//Log.d(TAG, v.toString());
			if (v instanceof CheckableLinearLayout) {
				((CheckableLinearLayout) v).setChecked(checked);
			}
		}
	}

	/**
	 * Returns the checked state of the specified position.
	 * @param position
	 * @return
	 */
	public boolean isItemChecked(int position)
	{
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			return mListView.isItemChecked(position);
		}
		return mCheckedArray.get(position);
	}

	/**
	 * Set the selection state of this view. 
	 * @param enabled
	 */
	public void setSelectionMode(boolean enabled)
	{
		mSelectionMode = enabled;
		//Log.i(TAG, "mSelectionMode: " + mSelectionMode);
	}

	/**
	 * Set the enabled state of this view. 
	 * @param enabled
	 */
	public void setEnabled(boolean enabled)
	{
		mListView.setEnabled(enabled);
	}

	/**
	 * Returns the enabled status for this view.
	 * @return
	 */
	public boolean isEnabled()
	{
		return mListView.isEnabled();
	}

	/**
	 * Sets the view to show if the adapter is empty 
	 * @param emptyView
	 */
	public void setEmptyView(View emptyView)
	{
		mListView.setEmptyView(emptyView);
	}

	AdapterView.OnItemClickListener mOnItemClickListener;

	/**
	 * Register a callback to be invoked when an item in this AdapterView has been clicked.
	 * @param listener
	 */
	public void setOnItemClickListener(AdapterView.OnItemClickListener listener)
	{
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			mListView.setOnItemClickListener(listener);
		} else {//use override listener to hack some private actions
			mOnItemClickListener = listener;
			mListView.setOnItemClickListener(onItemClickListener);
		}
	}

	private AdapterView.OnItemClickListener onItemClickListener =
		new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1,
					int position, long arg3)
			{
				//Log.d(TAG, "onItemClick " + mSelectionMode + " " + position);
				if (mSelectionMode) {
					//Log.d(TAG, "isItemChecked " + isItemChecked(position));
					setItemChecked(position, !isItemChecked(position));
				}
				if (mOnItemClickListener != null) {
					//Log.d(TAG, "  call original OnItemClickListener");
					mOnItemClickListener
							.onItemClick(arg0, arg1, position, arg3);
				}
			}
		};

	AdapterView.OnItemLongClickListener mOnItemLongClickListener;

	/**
	 * Register a callback to be invoked when an item in this AdapterView has been clicked and held
	 * @param listener
	 */
	public void setOnItemLongClickListener(
			AdapterView.OnItemLongClickListener listener)
	{
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			mListView.setOnItemLongClickListener(listener);
		} else {//use override listener to hack some private actions
			mOnItemLongClickListener = listener;
			mListView.setOnItemLongClickListener(onItemLongClickListener);
		}
	}

	private AdapterView.OnItemLongClickListener onItemLongClickListener =
		new AdapterView.OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
					int position, long arg3)
			{
				//Log.d(TAG, "onItemLongClick " + position);
				if (mOnItemLongClickListener != null) {
					//Log.d(TAG, "  call original onItemLongClickListener");
					return mOnItemLongClickListener.onItemLongClick(arg0, arg1,
						position, arg3);
				}
				return false;
			}
		};

	/**
	 * Sets the tag associated with this view. 
	 * @param tag
	 */
	public void setTag(Object tag)
	{
		mListView.setTag(tag);
	}

	/**
	 * 
	 * @return The number of items owned by the Adapter associated with this AdapterView. 
	 */
	public int getCount()
	{
		return mListView.getCount();
	}

	/**
	 * Returns the number of children in the group.
	 * @returna positive integer representing the number of children in the group 
	 */
	public int getChildCount()
	{
		return mListView.getChildCount();
	}
}
