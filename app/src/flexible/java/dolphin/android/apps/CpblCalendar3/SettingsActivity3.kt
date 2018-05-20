package dolphin.android.apps.CpblCalendar3

import android.os.Bundle
import android.support.v7.widget.Toolbar
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import dolphin.android.apps.CpblCalendar.preference.AdvancedFragment
import dolphin.android.apps.CpblCalendar.preference.GeneralFragment

class SettingsActivity3 : android.preference.PreferenceActivity() {
    override fun isValidFragment(fragmentName: String?): Boolean {
        return (GeneralFragment::class.java.name == fragmentName
                || DisplayFragment3::class.java.name == fragmentName
                //|| NotificationFragment::class.java.name == fragmentName
                || AdvancedFragment::class.java.name == fragmentName)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        val root = findViewById<View>(android.R.id.list)
                .parent.parent.parent as LinearLayout
        val bar = LayoutInflater.from(this)
                .inflate(R.layout.settings_toolbar, root, false) as Toolbar
        root.addView(bar, 0) // insert at top
        bar.setNavigationOnClickListener { onBackPressed() }
    }

    override fun onBuildHeaders(target: MutableList<Header>?) {
        //super.onBuildHeaders(target)
        loadHeadersFromResource(R.xml.settings3_header, target)
    }
}