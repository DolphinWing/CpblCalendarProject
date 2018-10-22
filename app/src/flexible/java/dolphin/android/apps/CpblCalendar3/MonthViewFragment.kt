package dolphin.android.apps.CpblCalendar3

import android.animation.Animator
import android.content.Context
import android.graphics.PorterDuff
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import dolphin.android.apps.CpblCalendar.Utils
import dolphin.android.apps.CpblCalendar.preference.PrefsHelper
import dolphin.android.apps.CpblCalendar.provider.CpblCalendarHelper
import dolphin.android.apps.CpblCalendar.provider.Game
import dolphin.android.apps.CpblCalendar.provider.TeamHelper
import dolphin.android.util.DateUtils
import dolphin.android.util.PackageUtils
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.common.SmoothScrollLinearLayoutManager
import eu.davidea.flexibleadapter.helpers.AnimatorHelper
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem
import eu.davidea.flexibleadapter.items.IFlexible
import eu.davidea.viewholders.FlexibleViewHolder
import java.text.SimpleDateFormat
import java.util.*

internal class MonthViewFragment : Fragment(), FlexibleAdapter.OnItemClickListener,
        FlexibleAdapter.OnItemLongClickListener {

    companion object {
        private const val TAG = "MonthViewFragment"
    }

    private var container: SwipeRefreshLayout? = null
    private lateinit var list: RecyclerView
    private lateinit var emptyView: View
    private var index = -1

    private lateinit var helper: TeamHelper
    private lateinit var viewModel: GameViewModel
    private lateinit var prefs: PrefsHelper
    private var year = 2018
    private var month = Calendar.MAY

    override fun setArguments(args: Bundle?) {
        super.setArguments(args)
        if (args?.containsKey("index") == true) {
            index = args.getInt("index", -1)
            Log.d(TAG, "fragment index = $index")
        }
        if (args?.containsKey("title") == true) {
            Log.d(TAG, "title: ${args.getString("title")}")
        }
        if (args?.getBoolean("refresh", false) == true) {
            year = args.getInt("year", 2018)
            month = args.getInt("month", 0)
            val fieldId = args.getString("field_id", "F00")
            val teamId = args.getInt("team_id", 0)
            startRefreshing(true, year, month, args.getBoolean("snackbar", true))
            Log.d(TAG, "refreshing $year/${month + 1} @$fieldId $teamId")
            viewModel.fetch(year, month)?.observe(this, androidx.lifecycle.Observer { resources ->
                when {
                    resources.progress == 98 ->
                        showSnackBar(getString(R.string.title_download_complete))
                    resources.progress < 90 ->
                        showSnackBar(resources.messages +
                                getString(R.string.title_download_from_cpbl, year, month + 1))
                    else -> updateAdapter(resources.dataList, helper, fieldId, teamId)
                }
            })
            //} else {
            //    startRefreshing(false)
        }
        if (args?.getBoolean("clear", false) == true) {
            Log.d(TAG, "clear adapter")
            updateAdapter(null, helper)
        }
    }

    private fun showSnackBar(message: String? = null, visible: Boolean = true) {
        (activity as? ListActivity)?.showSnackBar(message, visible)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //Log.d(TAG, "Fragment onCreate")
        viewModel = ViewModelProviders.of(activity!!,
                ViewModelProvider.AndroidViewModelFactory(activity!!.application))
                .get(GameViewModel::class.java)
        //cpblHelper = CpblCalendarHelper(activity!!)
        helper = TeamHelper(activity!!.application as CpblApplication)
        prefs = PrefsHelper(activity!!)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_recycler_view, container, false)
        this.container = view.findViewById(R.id.swipeRefreshLayout)
        this.container!!.isRefreshing = true
        this.container!!.setOnRefreshListener {
            //Log.d(TAG, "pull to refresh")
            (activity as? ListActivity)?.doWebQuery()
        }
        this.list = view.findViewById(android.R.id.list)
        this.list.layoutManager = SmoothScrollLinearLayoutManager(activity)
        this.emptyView = view.findViewById(R.id.empty_pane)
        return view //super.onCreateView(inflater, container, savedInstanceState)
    }

    private fun startRefreshing(enabled: Boolean, year: Int = 2018, monthOfJava: Int = 0,
                                showSnackbar: Boolean = true) {
        if (enabled) {
            this.container?.isEnabled = true
            this.container?.isRefreshing = true
            if (showSnackbar && activity is ListActivity) {
                (activity as ListActivity).showSnackBar(
                        getString(R.string.title_download_from_cpbl, year, monthOfJava + 1))
            }
        } else {
            if (showSnackbar && activity is ListActivity) {
                (activity as ListActivity).showSnackBar(visible = false)
            }
            this.container?.isRefreshing = false
            this.container?.isEnabled = prefs.pullToRefreshEnabled
        }
    }

    private val adapterList = ArrayList<MyItemView>()
    private fun updateAdapter(list: List<Game>? = null, helper: TeamHelper,
                              field: String = "F00", team: Int = 0) {
        //Log.d(TAG, "we have ${list?.size} games in $month (page=$index)")
        if (activity == null) return
        //val adapterList = ArrayList<MyItemView>()
        adapterList.clear()
        list?.forEach {
            if ((field == "F00" || it.FieldId == field) &&
                    (team == 0 || it.HomeTeam.id == team || it.AwayTeam.id == team)) {
                adapterList.add(MyItemView(activity!!, it, helper))
            }
        }
        //Log.d(TAG, "field = $field, ${adapterList.size} games")
        this.list.adapter = ItemAdapter(adapterList, this)
        this.list.setHasFixedSize(true)
        for (i in 0 until adapterList.size) {
            if (DateUtils.isToday(adapterList[i].game.StartTime)) {
                this.list.scrollToPosition(i)
                break//break when the upcoming game if found
            } else if (!adapterList[i].game.IsFinal) {
                this.list.scrollToPosition(i)
                break//break when the upcoming game if found
            }
        }
        emptyView.visibility = if (adapterList.isEmpty()) View.VISIBLE else View.GONE
        this.list.visibility = if (adapterList.isEmpty()) View.GONE else View.VISIBLE
        showSnackBar(visible = false)
        startRefreshing(false)
    }

    override fun onItemClick(view: View?, position: Int): Boolean {
        adapterList[position].game.Url?.let { url ->
            (activity as? ListActivity)?.let {
                CustomTabsHelper.launchUrl(it,
                        it.mCustomTabsSession, Uri.parse(url))
                return true
            }
        }
        //Log.d(TAG, "onItemClick $position ${game.Id}")
        return false
    }

    override fun onItemLongClick(position: Int) {
        val game = adapterList[position].game
        //Log.d(TAG, "onItemLongClick $position ${game.Id}")
        if (!game.IsFinal && !game.IsLive && activity != null) {
            AlertDialog.Builder(activity!!)
                    .setItems(arrayOf(getString(R.string.action_add_to_calendar),
                            getString(R.string.action_show_field_info))) { _, index ->
                        //Log.d(TAG, "selected $i")
                        when (index) {
                            0 -> addToCalendar(game)
                            1 -> showGameActivity(game)
                        }
                    }.show()
        }
    }

    private fun addToCalendar(game: Game) {
        val calIntent = Utils.createAddToCalendarIntent(activity, game)
        if (PackageUtils.isCallable(activity!!, calIntent)) {
            startActivity(calIntent)
        }
    }

    private fun showGameActivity(game: Game) {
        val url = CpblCalendarHelper.URL_FIELD_2017.replace("@field", game.FieldId)
        //Utils.startBrowserActivity(activity, url)
        (activity as? ListActivity)?.let {
            CustomTabsHelper.launchUrl(it, it.mCustomTabsSession, Uri.parse(url))
        }
    }

    internal class MyItemView(private val context: Context, val game: Game,
                              private val helper: TeamHelper) :
            AbstractFlexibleItem<MyItemView.ViewHolder>() {

        //private val appContext: Context
        //    get() = helper.application.applicationContext

        private fun getResString(resId: Int, vararg args: Any) = context.getString(resId, args)

        override fun bindViewHolder(adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>?,
                                    holder: ViewHolder?, position: Int,
                                    payloads: MutableList<Any>?) {
            val dateStr = SimpleDateFormat("MMM d (E) ", Locale.TAIWAN).format(game.StartTime.time)
            val timeStr = SimpleDateFormat("HH:mm", Locale.TAIWAN).format(game.StartTime.time)
            val year = game.StartTime.get(Calendar.YEAR)

            holder?.apply {
                if (DateUtils.isToday(game.StartTime) && helper.config().isHighlightToday) {
                    itemView.setBackgroundResource(R.drawable.item_highlight_background_holo_light)
                } else {
                    itemView.setBackgroundResource(R.drawable.selectable_background_holo_green)
                }
                gameId?.text = when (game.Kind) {
                    "01" -> game.Id.toString()
                    "02" -> context.getString(R.string.id_prefix_all_star, game.Id)
                    "03" -> context.getString(R.string.id_prefix_champion, game.Id)
                    "05" -> context.getString(R.string.id_prefix_challenge, game.Id)
                    "07" -> context.getString(R.string.id_prefix_warm_up, game.Id)
                    else -> game.Id.toString()
                }
                val displayTime = when {
                    game.IsFinal -> dateStr
                    game.IsLive -> "$dateStr&nbsp;&nbsp;${game.LiveMessage}"
                    else -> dateStr + timeStr
                }
                gameTime?.text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    Html.fromHtml(displayTime, Html.TO_HTML_PARAGRAPH_LINES_INDIVIDUAL)
                } else {
                    Html.fromHtml(displayTime)
                }
                gameField?.text = if (game.Source == Game.SOURCE_CPBL ||
                        game.Field?.contains(getResString(R.string.title_at)) == false) {
                    String.format("%s%s", getResString(R.string.title_at),
                            game.getFieldFullName(context))
                } else {
                    game.getFieldFullName(context)
                }
                teamAwayName?.text = game.AwayTeam.name
                teamAwayScore?.text = if (game.IsFinal || game.IsLive) {
                    game.AwayScore.toString()
                } else "-"
                teamAwayLogo?.apply {
                    setImageResource(R.drawable.ic_baseball)
                    setColorFilter(helper.getLogoColorFilter(game.AwayTeam, year),
                            PorterDuff.Mode.SRC_IN)
                }
                teamHomeName?.text = game.HomeTeam.name
                teamHomeScore?.text = if (game.IsFinal || game.IsLive) {
                    game.HomeScore.toString()
                } else "-"
                teamHomeLogo?.apply {
                    setImageResource(R.drawable.ic_baseball)
                    setColorFilter(helper.getLogoColorFilter(game.HomeTeam, year),
                            PorterDuff.Mode.SRC_IN)
                }
                extraMessage?.apply {
                    if (game.DelayMessage.isNullOrEmpty()) {
                        visibility = View.GONE
                    } else {
                        visibility = View.VISIBLE
                        text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            Html.fromHtml(game.DelayMessage,
                                    Html.TO_HTML_PARAGRAPH_LINES_INDIVIDUAL)
                        } else {
                            Html.fromHtml(game.DelayMessage)
                        }
                    }

                }
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
                                      adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>?)
                : ViewHolder = ViewHolder(view, adapter)

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

            override fun scrollAnimators(animators: MutableList<Animator>, position: Int,
                                         isForward: Boolean) {
                AnimatorHelper.alphaAnimator(animators, frontView, 0f)
            }
        }
    }

    internal class ItemAdapter(items: MutableList<MyItemView>?, listener: Any? = null)
        : FlexibleAdapter<MyItemView>(items, listener) {
        init {
            setAnimationOnForwardScrolling(true)
            setAnimationOnReverseScrolling(true)
        }
    }
}