package dolphin.android.apps.CpblCalendar3

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import dolphin.android.apps.CpblCalendar.CalendarActivity
import dolphin.android.apps.CpblCalendar.GameListFragment
import dolphin.android.apps.CpblCalendar.OnQueryCallback
import dolphin.android.apps.CpblCalendar.preference.PreferenceUtils
import dolphin.android.apps.CpblCalendar.provider.CpblCalendarHelper
import dolphin.android.apps.CpblCalendar.provider.Game
import java.util.*

class CacheModeListActivity : CalendarActivity() {
    companion object {
        private const val TAG = "CacheModeListActivity"
    }

    override fun getActivity() = this

    private lateinit var mDrawerLayout: androidx.drawerlayout.widget.DrawerLayout
    private lateinit var mDrawerList: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendar_phone)

        mDrawerLayout = findViewById(R.id.drawer_layout)
        mDrawerList = findViewById(R.id.left_drawer)
        findViewById<Toolbar>(R.id.toolbar)?.let { setSupportActionBar(it) }
        initQueryPane()
        mSpinnerYear.setSelection(0)
        mSpinnerMonth.setSelection(CpblCalendarHelper.getNowTime().get(Calendar.MONTH))
        findViewById<androidx.swiperefreshlayout.widget.SwipeRefreshLayout>(R.id.swipeRefreshLayout)?.isEnabled = false
        findViewById<View>(R.id.bottom_sheet)?.visibility = View.GONE
        findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.button_floating_action)?.visibility = View.GONE

        if (intent?.getBooleanExtra("cache_init", false) == true) {
            Handler().postDelayed({ runDownloadCache() }, 500)
        } else {
            performButtonQuery()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.splash, menu)
        return true //super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val visible = !IsQuery() && !mDrawerLayout.isDrawerOpen(mDrawerList)
        menu?.findItem(R.id.action_settings)?.isVisible = visible
        menu?.findItem(R.id.action_go_to_cpbl)?.isVisible = false
        menu?.findItem(R.id.action_refresh)?.isVisible = false
        menu?.findItem(R.id.action_cache_mode)?.title = getString(R.string.action_leave_cache_mode)
        menu?.findItem(R.id.action_leader_board)?.isVisible = false
        menu?.findItem(R.id.action_search)?.isVisible = visible
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.action_cache_mode -> {
                PreferenceUtils.setCacheMode(this@CacheModeListActivity, false)
                startActivity(Intent(this@CacheModeListActivity, ListActivity::class.java))
                finish()
                return true
            }
            R.id.action_settings -> {
                startActivityForResult(Intent(this, SettingsActivity3::class.java), 0)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onLoading(is_load: Boolean) {
        super.onLoading(is_load)
        mSpinnerYear?.isEnabled = false
        progressText?.visibility = if (is_load) View.VISIBLE else View.GONE
        if (mSnackbar != null && !is_load) {
            mSnackbar.dismiss()
            mSnackbar = null
        }
    }

    override fun getOnQueryCallback() = object : OnQueryCallback {
        override fun onQueryStart() {
            mDrawerLayout.closeDrawer(mDrawerList)
            onLoading(true)
            invalidateOptionsMenu()
        }

        override fun onQueryStateChange(msg: String?) {
            progressText?.text = msg
            showSnackbar(msg)
        }

        override fun onQuerySuccess(helper: CpblCalendarHelper?,
                                    gameArrayList: ArrayList<Game>?, year: Int, month: Int) {
            mDrawerLayout.closeDrawer(mDrawerList)
            updateGameListFragment(gameArrayList, year, month)
            onLoading(false)
            invalidateOptionsMenu()
        }

        override fun onQueryError(year: Int, month: Int) {
            updateGameListFragment(ArrayList(), year, month)
            Toast.makeText(activity, R.string.query_error, Toast.LENGTH_SHORT).show()
            onLoading(false)
            invalidateOptionsMenu()
        }

    }

    override fun OnFragmentAttached() {
        //
    }

    private fun updateGameListFragment(list: ArrayList<Game>?, year: Int, month: Int) {
        val transaction = fragmentManager.beginTransaction()
        val frag1 = fragmentManager.findFragmentById(R.id.main_content_frame) as GameListFragment?
        frag1?.updateAdapter(list, year.toString(), month.toString())
        try {
            transaction.commitAllowingStateLoss()
        } catch (e: IllegalStateException) {
            Log.e(TAG, "IllegalStateException: " + e.message)
        }
    }
}