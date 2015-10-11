package dolphin.android.apps.CpblCalendar;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.AppCompatCheckedTextView;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.AppCompatRadioButton;
import android.support.v7.widget.AppCompatSpinner;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import dolphin.android.apps.CpblCalendar.preference.AdvancedFragment;
import dolphin.android.apps.CpblCalendar.preference.DisplayFragment;
import dolphin.android.apps.CpblCalendar.preference.GeneralFragment;
import dolphin.android.apps.CpblCalendar.preference.NotificationFragment;
import dolphin.android.apps.CpblCalendar.preference.PreferenceActivity;

/**
 * Created by dolphin on 2015/03/08.
 * <p/>
 * http://stackoverflow.com/a/27455363
 */
public class SettingsActivity extends PreferenceActivity {

    //This API was added due to a newly discovered vulnerability.
    // Please see http://ibm.co/1bAA8kF or http://ibm.co/IDm2Es
    @TargetApi(Build.VERSION_CODES.KITKAT)
    //http://stackoverflow.com/a/20494759/2673859
    @Override
    protected boolean isValidFragment(String fragmentName) {
        return GeneralFragment.class.getName().equals(fragmentName)
                || DisplayFragment.class.getName().equals(fragmentName)
                || NotificationFragment.class.getName().equals(fragmentName)
                || AdvancedFragment.class.getName().equals(fragmentName);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        LinearLayout root = (LinearLayout) findViewById(android.R.id.list).getParent().getParent().getParent();
        Toolbar bar = (Toolbar) LayoutInflater.from(this).inflate(R.layout.settings_toolbar, root, false);
        root.addView(bar, 0); // insert at top
        bar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(String name, Context context, AttributeSet attrs) {
        // Allow super to try and create a view first
        final View result = super.onCreateView(name, context, attrs);
        if (result != null) {
            return result;
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            // If we're running pre-L, we need to 'inject' our tint aware Views in place of the
            // standard framework versions
            switch (name) {
                case "EditText":
                    return new AppCompatEditText(this, attrs);
                case "Spinner":
                    return new AppCompatSpinner(this, attrs);
                case "CheckBox":
                    return new AppCompatCheckBox(this, attrs);
                case "RadioButton":
                    return new AppCompatRadioButton(this, attrs);
                case "CheckedTextView":
                    return new AppCompatCheckedTextView(this, attrs);
            }
        }

        return null;
    }
}
