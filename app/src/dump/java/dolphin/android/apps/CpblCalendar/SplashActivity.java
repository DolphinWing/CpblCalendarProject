package dolphin.android.apps.CpblCalendar;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class SplashActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = new Intent();
        intent.setClass(this, TestActivity.class);
        startActivity(intent);
        this.finish();
    }

}
