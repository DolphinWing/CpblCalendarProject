@file:Suppress("PackageName")

package dolphin.android.apps.CpblCalendar3

import android.content.Context
import android.graphics.PorterDuff
import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.support.v4.view.ViewPager
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.util.Log
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import dolphin.android.apps.CpblCalendar.provider.CpblCalendarHelper
import dolphin.android.apps.CpblCalendar.provider.Game
import dolphin.android.apps.CpblCalendar.provider.TeamHelper
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.common.SmoothScrollLinearLayoutManager
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem
import eu.davidea.flexibleadapter.items.IFlexible
import eu.davidea.viewholders.FlexibleViewHolder
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
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
    private lateinit var teamHelper: TeamHelper
    private val mAllGamesCache = SparseArray<ArrayList<Game>>()
    private var mYear: Int = 2018
    private var mMonth: Int = Calendar.MAY

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list)

        helper = CpblCalendarHelper(this)
        teamHelper = TeamHelper(application as CpblApplication)

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
        mAllGamesCache.get(mYear * 12 + mMonth)?.let { updateGameList(mMonth - 2, it) }
                ?: kotlin.run { doWebQuery() }
    }

    private fun doWebQuery() {
        val year = mYear
        val month = mMonth
        thread {
            Log.d(TAG, "start query $year/${month + 1}")
            val list = helper.query2016(mYear, month + 1, "01", "F00")
            Log.d(TAG, "list size = ${list.size}")
            mAllGamesCache.put(year * 12 + month, list)
            runOnUiThread { updateGameList(month - 2, list) }
        }
    }

    private fun updateGameList(index: Int, list: ArrayList<Game>? = null) {
        Log.d(TAG, "update index $index")
        mAdapter.getChildFragment(index)?.let {
            if (it is MonthViewFragment) {
                it.updateAdapter(list, teamHelper)
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
                    (activity as ListActivity).doWebQuery()
                }
            }
            this.list = view.findViewById(android.R.id.list)
            this.list.layoutManager = SmoothScrollLinearLayoutManager(activity)
            return view //super.onCreateView(inflater, container, savedInstanceState)
        }

        fun startUpdating() {
            this.container.isRefreshing = true
        }

        fun updateAdapter(list: ArrayList<Game>? = null, helper: TeamHelper) {
            Log.d(TAG, "we have ${list?.size} games in $month ($index)")
            if (activity == null) return
            val adapterList = ArrayList<MyItemView>()
            list?.forEach { adapterList.add(MyItemView(activity!!, it, helper)) }
            this.list.adapter = ItemAdapter(adapterList)
            this.list.setHasFixedSize(true)
            this.container.isRefreshing = false
        }
    }

    internal class MyItemView(private val context: Context, val game: Game,
                              private val helper: TeamHelper) :
            AbstractFlexibleItem<MyItemView.ViewHolder>() {
        override fun bindViewHolder(adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>?,
                                    holder: ViewHolder?, position: Int, payloads: MutableList<Any>?) {
            val date_str = SimpleDateFormat("MMM d (E) ", Locale.TAIWAN)
                    .format(game.StartTime.time)
            val time_str = SimpleDateFormat("HH:mm", Locale.TAIWAN)
                    .format(game.StartTime.time)
            val field = if (game.Source == Game.SOURCE_CPBL || !game.Field.contains(context.getString(R.string.title_at)))
                String.format("%s%s", context.getString(R.string.title_at), game.Field)
            else
                game.Field
            val year = game.StartTime.get(Calendar.YEAR)

            holder?.apply {
                gameId?.text = game.Id.toString()
                gameTime?.text = when {
                    game.IsFinal -> date_str
                    game.IsLive -> "LIVE"
                    else -> date_str + time_str
                }
                gameField?.text = field
                teamAwayName?.text = game.AwayTeam.name
                teamAwayScore?.text = if (game.IsFinal) game.AwayScore.toString() else "-"
                teamAwayLogo?.apply {
                    setImageResource(R.drawable.ic_baseball)
                    setColorFilter(helper.getLogoColorFilter(game.AwayTeam, year),
                            PorterDuff.Mode.SRC_IN)
                }
                teamHomeName?.text = game.HomeTeam.name
                teamHomeScore?.text = if (game.IsFinal) game.HomeScore.toString() else "-"
                teamHomeLogo?.apply {
                    setImageResource(R.drawable.ic_baseball)
                    setColorFilter(helper.getLogoColorFilter(game.HomeTeam, year),
                            PorterDuff.Mode.SRC_IN)
                }
                extraMessage?.text = ""
                liveChannel?.apply {
                    visibility = if (game.IsFinal || game.Channel.isNullOrEmpty())
                        View.GONE else View.VISIBLE
                    text = game.Channel
                }
            }
        }

        override fun hashCode(): Int {
            return game.StartTime.timeInMillis.hashCode() + game.Id.hashCode()
        }

        override fun getLayoutRes() = R.layout.recyclerview_item_matchup_card

        override fun equals(other: Any?) = if (other is MyItemView) other == this else false

        override fun createViewHolder(view: View?,
                                      adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>?): ViewHolder {
            return ViewHolder(view, adapter)
        }

        class ViewHolder(view: View?, adapter: FlexibleAdapter<out IFlexible<*>>?) :
                FlexibleViewHolder(view, adapter) {
            val gameId: TextView? = view?.findViewById(R.id.textView9)
            val gameTime: TextView? = view?.findViewById(R.id.textView1)
            val gameField: TextView? = view?.findViewById(R.id.textView7)
            val teamAwayLogo: ImageView? = view?.findViewById(android.R.id.icon1)
            val teamAwayName: TextView? = view?.findViewById(R.id.textView2)
            val teamAwayScore: TextView? = view?.findViewById(R.id.textView3)
            val teamHomeLogo: ImageView? = view?.findViewById(android.R.id.icon2)
            val teamHomeName: TextView? = view?.findViewById(R.id.textView5)
            val teamHomeScore: TextView? = view?.findViewById(R.id.textView4)
            val extraMessage: TextView? = view?.findViewById(R.id.textView8)
            val liveChannel: TextView? = view?.findViewById(R.id.textView6)

            init {
                view?.findViewById<View>(R.id.item_control_pane)?.visibility = View.INVISIBLE
            }
        }
    }

    internal class ItemAdapter(items: MutableList<MyItemView>?) : FlexibleAdapter<MyItemView>(items)
}
