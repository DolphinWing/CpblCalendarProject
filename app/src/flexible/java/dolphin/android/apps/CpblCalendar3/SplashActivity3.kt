package dolphin.android.apps.CpblCalendar3

import android.content.Intent

class SplashActivity3 : SplashActivity() {
    override fun startNextActivity() {
        overridePendingTransition(0, 0)
//        val intent: Intent = if (PreferenceUtils.isCacheMode(this)) {
//            Intent(this, ListActivity::class.java)
//        } else {
//            Intent(this, HighlightActivity::class.java)
//        }
//        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
        val intent = Intent(this, ListActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        overridePendingTransition(0, 0)
        this.finish()
        overridePendingTransition(0, 0)
    }
}