package dolphin.android.apps.CpblCalendar;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

//import android.support.v4.app.ActionBarDrawerToggle;

public class SplashActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = new Intent();
        intent.setClass(this, CalendarForPhoneActivity.class);
        startActivity(intent);
        this.finish();
    }

}
