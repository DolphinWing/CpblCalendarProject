package dolphin.android.apps.CpblCalendar;

import android.content.Intent;
import android.os.Bundle;

import com.actionbarsherlock.app.SherlockActivity;

//import android.support.v4.app.ActionBarDrawerToggle;

public class SplashActivity extends SherlockActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_test);

        //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
        //    // http://goo.gl/cmG1V , solve android.os.NetworkOnMainThreadException
        //    StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
        //            .detectDiskReads()
        //            .detectDiskWrites()
        //            .detectNetwork() // or .detectAll() for all detectable problems
        //            .penaltyLog()
        //            .build());
        //}

        //CpblCalendarHelper helper = new CpblCalendarHelper();
        //helper.query(2013, 5);

        Intent intent = new Intent();
        final boolean is_debug = getResources().getBoolean(R.bool.pref_engineer_mode);
        final boolean is_tablet = getResources().getBoolean(R.bool.config_tablet);
        //final boolean is_land = getResources().getBoolean(R.bool.config_landscape);
        if (is_debug) {
            intent.setClass(this, CalendarForPhoneActivity.class);
        } else if (is_tablet/* && is_land*/) {
            intent.setClass(this, CalendarForTabletActivity.class);
        } else {
            intent.setClass(this, CalendarForPhoneActivity.class);
        }
        startActivity(intent);
        this.finish();
    }

}
