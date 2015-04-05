package dolphin.android.apps.CpblCalendar;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
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
}
