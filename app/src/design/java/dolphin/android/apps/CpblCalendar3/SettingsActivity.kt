package dolphin.android.apps.CpblCalendar3

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.widget.*
import dolphin.android.apps.CpblCalendar.preference.PreferenceActivity

/**
 * Created by dolphin on 2015/03/08.
 *
 *
 * http://stackoverflow.com/a/27455363
 */
class SettingsActivity : PreferenceActivity() {

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        val root = findViewById<View>(android.R.id.list)
                .parent.parent.parent as LinearLayout
        val bar = LayoutInflater.from(this)
                .inflate(R.layout.settings_toolbar, root, false) as Toolbar
        root.addView(bar, 0) // insert at top
        bar.setNavigationOnClickListener { finish() }
    }

    override fun onCreateView(name: String, context: Context, attrs: AttributeSet): View? {
        // Allow super to try and create a view first
        val result = super.onCreateView(name, context, attrs)
        if (result != null) {
            return result
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            // If we're running pre-L, we need to 'inject' our tint aware Views in place of the
            // standard framework versions
            when (name) {
                "EditText" -> return AppCompatEditText(this, attrs)
                "Spinner" -> return AppCompatSpinner(this, attrs)
                "CheckBox" -> return AppCompatCheckBox(this, attrs)
                "RadioButton" -> return AppCompatRadioButton(this, attrs)
                "CheckedTextView" -> return AppCompatCheckedTextView(this, attrs)
            }
        }

        return null
    }
}
