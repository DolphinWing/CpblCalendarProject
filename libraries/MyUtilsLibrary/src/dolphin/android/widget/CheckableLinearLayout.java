package dolphin.android.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Checkable;
import android.widget.CheckedTextView;
import android.widget.LinearLayout;

//Android: Checkable Linear Layout
//http://tokudu.com/2010/android-checkable-linear-layout/
public class CheckableLinearLayout extends LinearLayout implements Checkable
{
	private CheckedTextView _checkview;
	private CheckBox _checkbox;

	@SuppressLint("NewApi")
	public CheckableLinearLayout(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
	}

	public CheckableLinearLayout(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}

	public CheckableLinearLayout(Context context)
	{
		super(context);
	}

	@Override
	public boolean isChecked()
	{
		if (_checkbox != null)
			return _checkbox.isChecked();
		else if (_checkview != null)
			return _checkview.isChecked();
		return false;
	}

	@Override
	public void setChecked(boolean checked)
	{
		if (_checkbox != null) {
			_checkbox.setChecked(checked);
		}
		if (_checkview != null) {
			_checkview.setChecked(checked);
		}
	}

	@Override
	public void toggle()
	{
		if (_checkbox != null) {
			_checkbox.toggle();
		}
		if (_checkview != null) {
			_checkview.toggle();
		}
	}

	@Override
	protected void onFinishInflate()
	{
		super.onFinishInflate();
		// find checked text view or checkbox
		int childCount = getChildCount();
		for (int i = 0; i < childCount; ++i) {
			View v = getChildAt(i);
			if (v instanceof CheckBox) {
				_checkbox = (CheckBox) v;
			} else if (v instanceof CheckedTextView) {
				_checkview = (CheckedTextView) v;
			}
		}
	}
}
