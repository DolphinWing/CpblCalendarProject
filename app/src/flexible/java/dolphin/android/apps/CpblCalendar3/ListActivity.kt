@file:Suppress("PackageName")

package dolphin.android.apps.CpblCalendar3

import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.support.v4.view.PagerAdapter
import android.support.v4.view.ViewPager
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.util.Log
import android.util.SparseArray
import android.view.*
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.Spinner
import android.widget.TextView
import dolphin.android.apps.CpblCalendar.GameListFragment
import dolphin.android.apps.CpblCalendar.provider.CpblCalendarHelper
import dolphin.android.apps.CpblCalendar.provider.Game
import java.lang.ref.WeakReference
import java.text.DateFormatSymbols
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.thread

class ListActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "ListActivity"
    }

    private lateinit var mFilterListPane: View
    private lateinit var mFilterControlPane: View
    private lateinit var mFilterControlBg: View
    private lateinit var mAdapter: SimplePageAdapter

    private lateinit var mSpinnerYear: Spinner
    private lateinit var mSpinnerField: Spinner
    private lateinit var mSpinnerTeam: Spinner

    private lateinit var helper: CpblCalendarHelper
    private val mAllGamesCache = SparseArray<ArrayList<Game>>()
    private var mYear: Int = 2018
    private var mMonth: Int = 4

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list)

        helper = CpblCalendarHelper(this)

        findViewById<Toolbar>(R.id.toolbar)?.apply { setSupportActionBar(this) }
        val tabLayout: TabLayout = findViewById(R.id.tab_layout)
        val pager: ViewPager = findViewById(R.id.viewpager)

        val months = ArrayList(Arrays.asList(*DateFormatSymbols(Locale.TAIWAN).months))
        months.removeAt(months.size - 1)
        months.removeAt(0)
        months.removeAt(0)
        months.forEach { tabLayout.addTab(tabLayout.newTab().setText(it)) }
        mAdapter = SimplePageAdapter(this, months)
        pager.adapter = mAdapter
        pager.addOnPageChangeListener(TabLayout.TabLayoutOnPageChangeListener(tabLayout))
        pager.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                mMonth = position + 2
                Log.d(TAG, "selected month = ${mMonth + 1}")
                doQueryAction()
            }
        })
        pager.currentItem = mMonth - 2
        tabLayout.addOnTabSelectedListener(TabLayout.ViewPagerOnTabSelectedListener(pager))

        mFilterListPane = findViewById(R.id.filter_list_pane)
        mFilterControlBg = findViewById(R.id.filter_control_background)
        mFilterControlBg.setOnClickListener { filterPaneVisible = false }
        mFilterControlPane = findViewById(R.id.filter_control_pane)
        findViewById<View>(R.id.filter_control_button)?.setOnClickListener {
            Log.d(TAG, "visible: $filterPaneVisible")
            filterPaneVisible = !filterPaneVisible
        }
        filterPaneVisible = false //mFilterControlPane.visibility == View.VISIBLE

        mSpinnerYear = findViewById(R.id.spinner3)
        mSpinnerYear.adapter = CpblCalendarHelper.buildYearAdapter(this, mYear)
        mSpinnerField = findViewById(R.id.spinner1)
        mSpinnerTeam = findViewById(R.id.spinner2)

        findViewById<View>(android.R.id.button1)?.setOnClickListener {
            filterPaneVisible = false
            doQueryAction()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_list, menu)
        return true //super.onCreateOptionsMenu(menu)
    }

    private var filterPaneVisible: Boolean = false
        set(value) {
            runOnUiThread { showFilterPane(value) }
            field = value
        }

    private fun showFilterPane(visible: Boolean) {
        mFilterControlPane.apply {
            //FIXME: change elevation before animation
            if (visible) {
                this.animate()
                        .translationY(0f)
                        .withStartAction {
                            //visibility = View.VISIBLE
                            mFilterControlBg.visibility = View.VISIBLE
                            mFilterListPane.visibility = View.INVISIBLE
                        }
                        //.withEndAction { }
                        .setInterpolator(AccelerateInterpolator())
                        .start()

            } else {
                this.animate()
                        .translationY(-bottom.toFloat())
                        .setInterpolator(DecelerateInterpolator())
                        //.withStartAction { }
                        .withEndAction {
                            //visibility = View.GONE
                            mFilterListPane.visibility = View.VISIBLE
                            mFilterControlBg.visibility = View.GONE
                        }
                        .start()
            }
        }
    }

    private fun doQueryAction() {
        mAdapter.getChildFragment(mMonth - 2)?.let {
            if (it is MonthViewFragment) {
                it.startUpdating()
            }
        }
        mAllGamesCache.get(mYear * 12 + mMonth)?.let { updateGameList(it) }
                ?: kotlin.run { doWebQuery() }
    }

    private fun doWebQuery() {
        thread {
            val list = helper.query2016(mYear, mMonth, "01", "F00")
            Log.d(TAG, "list size = ${list.size}")
            mAllGamesCache.put(mYear * 12 + mMonth, list)
            runOnUiThread { updateGameList(list) }
        }
    }

    private fun updateGameList(list: ArrayList<Game>? = null) {
        mAdapter.getChildFragment(mMonth - 2)?.let {
            if (it is MonthViewFragment) {
                it.updateAdapter(list)
            }
        }
    }

    class SimplePageAdapter(a: AppCompatActivity, private var months: ArrayList<String>) :
            WizardPager.PagerAdapter(a.supportFragmentManager) {
        override fun getItem(position: Int): Fragment {
            return MonthViewFragment().apply {
                arguments = Bundle().apply {
                    putInt("index", position)
                    putString("title", months[position])
                }
            }
        }

        override fun getCount() = months.size
    }

    internal class MonthViewFragment : Fragment() {
        private lateinit var container: SwipeRefreshLayout
        private lateinit var list: RecyclerView
        var index = -1
        var month: String? = null

        override fun setArguments(args: Bundle?) {
            super.setArguments(args)
            index = args?.getInt("index", -1) ?: -1
            month = args?.getString("title")
        }

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            val view = inflater.inflate(R.layout.fragment_recycler_view, container, false)
            this.container = view.findViewById(R.id.swipeRefreshLayout)
            this.container.isRefreshing = true
            this.container.setOnRefreshListener {
                if (activity is ListActivity) {
                    (activity as ListActivity).doQueryAction()
                }
            }
            this.list = view.findViewById(android.R.id.list)
            return view //super.onCreateView(inflater, container, savedInstanceState)
        }

        fun startUpdating() {
            this.container.isRefreshing = true
        }

        fun updateAdapter(list: ArrayList<Game>? = null) {
            Log.d(TAG, "we have ${list?.size} games in $month ($index)")
            this.container.isRefreshing = false
        }
    }
}
