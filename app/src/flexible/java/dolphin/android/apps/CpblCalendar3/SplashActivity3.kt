package dolphin.android.apps.CpblCalendar3

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.util.Log
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import dolphin.android.apps.CpblCalendar.CalendarForPhoneActivity
import dolphin.android.apps.CpblCalendar.Utils
import dolphin.android.apps.CpblCalendar.preference.PreferenceUtils
import dolphin.android.util.PackageUtils

class SplashActivity3 : AppCompatActivity() {
    companion object {
        private const val TAG = "SplashActivity"
    }

    private lateinit var config: FirebaseRemoteConfig

    //http://gunhansancar.com/change-language-programmatically-in-android/
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(Utils.onAttach(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash3)

        //http://stackoverflow.com/a/31016761/2673859
        //check google play service and authentication
        val googleAPI = GoogleApiAvailability.getInstance()
        val result = googleAPI.isGooglePlayServicesAvailable(this)
        if (result != ConnectionResult.SUCCESS) {
            Log.e(TAG, googleAPI.getErrorString(result))
            val textView = findViewById<TextView>(android.R.id.message)
            if (textView != null) {
                textView.text = googleAPI.getErrorString(result)
            }
            if (googleAPI.isUserResolvableError(result)) {
                googleAPI.getErrorDialog(this, result, 0) {
                    finish()//no update, no start
                }
            }
            return //don't show progress bar
        }

        findViewById<View>(android.R.id.progress).animate()
                .alpha(1.0f)
                .setDuration(500)
                .setInterpolator(DecelerateInterpolator())
                .withStartAction { prepareRemoteConfig() }
                .withEndAction { }
                .start()
    }

    private fun prepareRemoteConfig() {
        config = FirebaseRemoteConfig.getInstance()
        config.apply {
            setConfigSettings(FirebaseRemoteConfigSettings.Builder()
                    .setDeveloperModeEnabled(BuildConfig.DEBUG)
                    .build())
            setDefaults(R.xml.remote_config_defaults)
        }
        fetchFirebaseRemoteConfig()
    }

    private fun fetchFirebaseRemoteConfig() {
        config.fetch(if (BuildConfig.DEBUG) 60 else 43200)
                .addOnCompleteListener {
                    if (it.isSuccessful) {
                        config.activateFetched()
                        if (PreferenceUtils.isCacheMode(applicationContext)) {
                            checkLatestAppVersion()
                            return@addOnCompleteListener
                        }
                        animateToNextActivity()
                    }
                }
    }

    private fun checkLatestAppVersion() {
        val info = PackageUtils.getPackageInfo(this, SplashActivity::class.java)
        val versionCode = info?.versionCode ?: 0
        if (versionCode > config.getLong("latest_version_code")) {
            animateToNextActivity()
            return
        }

        val dialog = AlertDialog.Builder(this)
                .setTitle(R.string.new_version_available_title)
                .setMessage(R.string.new_version_available_message)
                .setPositiveButton(R.string.new_version_available_go) { _, _ ->
                    PackageUtils.startGooglePlayApp(this@SplashActivity3, packageName)
                }
                .setNegativeButton(R.string.new_version_available_later) { _, _ ->
                    animateToNextActivity()
                }
                .show()

        var summary: String? = config.getString("latest_version_summary")
        summary = if (summary != null && !summary.isEmpty())
            summary
        else
            getString(R.string.new_version_available_message)
        val textView = dialog.findViewById<TextView>(android.R.id.message)
        if (textView != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                textView.text = Html.fromHtml(summary, Html.TO_HTML_PARAGRAPH_LINES_INDIVIDUAL)
            } else {
                textView.text = Html.fromHtml(summary)
            }
        }
    }

    private fun animateToNextActivity() {
        val progressBar: ProgressBar = findViewById(android.R.id.progress)
        ProgressBarAnimation(progressBar, 50, 99)
                .withStartAction {
                    progressBar.isIndeterminate = false
                    progressBar.max = 100
                }
                .withEndAction { progressBar.progress = 100; startNextActivity() }
                .animate()
    }

    private fun startNextActivity() {
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