package dolphin.android.apps.CpblCalendar3

import android.content.Intent
import dolphin.android.apps.CpblCalendar.CalendarForPhoneActivity
import dolphin.android.apps.CpblCalendar.preference.PreferenceUtils

class SplashActivity3 : SplashActivity() {
    override fun startNextActivity() {
        overridePendingTransition(0, 0)
        val intent: Intent = if (PreferenceUtils.isOnlyOldDrawerMenu(this)) {
            Intent(this, CalendarForPhoneActivity::class.java)
        } else if (PreferenceUtils.isCacheMode(this)) {
            Intent(this, CacheModeListActivity::class.java)
        } else {
            //Intent(this, HighlightActivity3::class.java)
            Intent(this, ListActivity::class.java)
        }
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
//        val intent = Intent(this, ListActivity::class.java).apply {
//            flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
//        }
        startActivity(intent)
        overridePendingTransition(0, 0)
        this.finish()
        overridePendingTransition(0, 0)
    }
}