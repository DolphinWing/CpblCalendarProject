package se.pixelcoding.wikitab.preferences;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.os.Parcelable;
import android.preference.DialogPreference;
import android.util.AttributeSet;

// MultiSelectListPreference is very broken so lets reverse engineer it, 
// see http://code.google.com/p/android/issues/detail?id=15966
public class FixedMultiSelectListPreference extends DialogPreference {
	private CharSequence[] entries;
    private CharSequence[] entryValues;
    private Set<String> values;
	
	private boolean checked[];

	public FixedMultiSelectListPreference(Context context, AttributeSet attrs) {
		super(context, attrs);

        TypedArray a = context.obtainStyledAttributes(attrs,
                new int[] { android.R.attr.entries, android.R.attr.entryValues }, 0, 0);
        entries = a.getTextArray(0);
        entryValues = a.getTextArray(1);
        a.recycle();
	}

	public FixedMultiSelectListPreference(Context context) {
		super(context, null);
	}
	
    public void setEntries(CharSequence[] entries) {
        this.entries = entries;
    }

    public void setEntries(int entriesResId) {
        setEntries(getContext().getResources().getTextArray(entriesResId));
    }

    public CharSequence[] getEntries() {
        return entries;
    }

    public void setEntryValues(CharSequence[] entryValues) {
    	this.entryValues = entryValues;
    }

    public void setEntryValues(int entryValuesResId) {
        setEntryValues(getContext().getResources().getTextArray(entryValuesResId));
    }

    public CharSequence[] getEntryValues() {
        return entryValues;
    }
    
    public void setValues(Set<String> values) {
    	this.values = values;
    	
    	persistStringSet(values);
    }
    
    public Set<String> getValues() {
    	return values;
    }
	
	@Override
	protected void onPrepareDialogBuilder(Builder builder) {
		super.onPrepareDialogBuilder(builder);
		
		if (entries == null || entryValues == null) {
			throw new IllegalStateException(
					"FixedMultiSelectListPreference requires an entries array and an entryValues array.");
		}
		
		checked = new boolean[entryValues.length];
		List<CharSequence> list = Arrays.asList(entryValues);
		
		if (values != null) {
			for (String value : values) {
				int index = list.indexOf(value);
				
				if (index != -1) {
					checked[index] = true;
				}
			}
		}
		
		
		
		builder.setMultiChoiceItems(entries, checked, new OnMultiChoiceClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which, boolean isChecked) {
				checked[which] = isChecked;
			}
		});
	}
    
	@Override
	protected void onDialogClosed(boolean positiveResult) {
		super.onDialogClosed(positiveResult);
		
		if (positiveResult && entryValues != null) {
			Set<String> newValues = new HashSet<String>();
			for (int i = 0; i < entryValues.length; ++i) {
				if (checked[i]) {
					newValues.add(entryValues[i].toString());
				}
			}
			
			if (callChangeListener(newValues)) {
				setValues(newValues);
			}
		}
	}

    public Set<String> getCheckedValues() {
        if (entryValues != null) {
            Set<String> newValues = new HashSet<String>();
            for (int i = 0; i < entryValues.length; ++i) {
                if (checked[i]) {
                    newValues.add(entryValues[i].toString());
                }
            }

            return newValues;
        }

        return null;
    }

	@Override
	protected Object onGetDefaultValue(TypedArray a, int index) {
		CharSequence[] array = a.getTextArray(index);
		
		Set<String> set = new HashSet<String>();
		
		for (CharSequence item : array) {
			set.add(item.toString());
		}

		return set;
	}

	@Override
	protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
		@SuppressWarnings("unchecked")
		Set<String> defaultValues = (Set<String>) defaultValue;
		
		setValues((restorePersistedValue ? getPersistedStringSet(values) : defaultValues));
	}
	
	public Set<String> getPersistedStringSet(Set<String> defaultReturnValue) {
		String key = getKey();
		
		return getSharedPreferences().getStringSet(key, defaultReturnValue);
	}
	
	public boolean persistStringSet(Set<String> values) {
		if (shouldPersist()) {
			// Shouldn't store null
			if (values == getPersistedStringSet(null)) {
				// It's already there, so the same as persisting
				return true;
			}
		}
		
        SharedPreferences.Editor editor = getEditor();
        editor.putStringSet(getKey(), values);
        // Default class does fancy stuff here
        editor.apply();
        
        return true;
	}
	
	@Override
    protected Parcelable onSaveInstanceState() {
		if (isPersistent()) {
			return super.onSaveInstanceState();
		} else {
			throw new IllegalStateException("Must always be persistent");
		}
	}

}
