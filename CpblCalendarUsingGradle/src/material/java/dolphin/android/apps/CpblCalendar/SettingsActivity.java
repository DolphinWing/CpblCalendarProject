package dolphin.android.apps.CpblCalendar;

import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import dolphin.android.apps.CpblCalendar.preference.PreferenceActivity;

/**
 * Created by dolphin on 2015/03/08.
 *
 * http://stackoverflow.com/a/27455363
 */
public class SettingsActivity extends PreferenceActivity {
//    @Override
//    protected void onApplyThemeResource(Resources.Theme theme, int resid, boolean first) {
//        theme.applyStyle(R.style.Theme_Holo_green_Settings, true);
//        super.onApplyThemeResource(theme, resid, first);
//    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        LinearLayout root = (LinearLayout)findViewById(android.R.id.list).getParent().getParent().getParent();
        Toolbar bar = (Toolbar) LayoutInflater.from(this).inflate(R.layout.settings_toolbar, root, false);
        root.addView(bar, 0); // insert at top
        bar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
}
