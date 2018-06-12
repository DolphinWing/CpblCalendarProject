package dolphin.android.apps.CpblCalendar3

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import dolphin.android.apps.CpblCalendar.provider.Game
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem
import eu.davidea.flexibleadapter.items.IFlexible
import eu.davidea.viewholders.FlexibleViewHolder
import android.content.Context
import android.graphics.PorterDuff
import android.os.Build
import android.text.Html
import android.text.format.DateUtils
import dolphin.android.apps.CpblCalendar.provider.TeamHelper
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class HighlightCardAdapter(private val items: MutableList<HighlightCardAdapter.CardItem>?,
                           listener: Any? = null)
    : FlexibleAdapter<HighlightCardAdapter.CardItem>(items, listener) {

    companion object {
        const val TYPE_UPDATE_CARD = -3
        const val TYPE_ANNOUNCEMENT = -2
        const val TYPE_MORE_CARD = -1

        @JvmStatic
        fun createCardItems(application: CpblApplication, list: ArrayList<Game>?,
                            listener: OnCardClickListener? = null): ArrayList<CardItem>? {
            val context = application.applicationContext
            val gameList = ArrayList<CardItem>()
            list?.forEach {
                when {
                    it.IsLive -> gameList.add(LiveCardItem(context, it, listener))
                    it.IsFinal -> gameList.add(ResultCardItem(context, listener = listener,
                            helper = TeamHelper(application), game = it))
                    it.Id == TYPE_ANNOUNCEMENT -> gameList.add(AnnounceCardItem(it))
                    it.Id == TYPE_UPDATE_CARD -> gameList.add(UpdateCardItem(it, listener))
                    else -> gameList.add(UpcomingCardItem(context, it, listener))
                }
            }
            gameList.add(CardItem(Game(TYPE_MORE_CARD), listener))
            return gameList
        }
    }

    constructor(application: CpblApplication, list: ArrayList<Game>, listener: Any? = null)
            : this(createCardItems(application, list, listener as? OnCardClickListener), listener)

    init {
        setAnimationDuration(300)
    }

    interface OnCardClickListener {
        fun onOptionClick(view: View, game: Game, position: Int)
    }

//    fun removeAt(index: Int) {
//        items?.removeAt(index)
//        notifyDataSetChanged()
//    }

    open class CardItem(val game: Game, private val listener: OnCardClickListener? = null)
        : AbstractFlexibleItem<CardViewHolder>() {
        override fun hashCode() = game.hashCode()

        override fun equals(other: Any?) = if (other is CardItem) other.game == game else false

        override fun createViewHolder(view: View?,
                                      adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>?)
                : CardViewHolder = CardViewHolder(view, adapter, listener)

        override fun getLayoutRes() = R.layout.recyclerview_item3_more

        override fun bindViewHolder(adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>?,
                                    holder: CardViewHolder?, position: Int, list: MutableList<Any>?) {
            holder?.apply(game, position)
        }
    }

    open class CardViewHolder(view: View?, private val adapter: FlexibleAdapter<out IFlexible<*>>?,
                              private val listener: OnCardClickListener? = null)
        : FlexibleViewHolder(view, adapter) {

        private val option1: View? = view?.findViewById(R.id.card_option1)
        private val option2: View? = view?.findViewById(R.id.card_option2)
        private val option3: View? = view?.findViewById(R.id.card_option3)

        open fun apply(game: Game, position: Int) {
            option1?.setOnClickListener { listener?.onOptionClick(option1, game, position) }
            option2?.setOnClickListener { listener?.onOptionClick(option2, game, position) }
            option3?.setOnClickListener {
                adapter?.removeItem(position)
                adapter?.notifyDataSetChanged()
                listener?.onOptionClick(option3, game, position)
            }
        }
    }

    open class UpcomingCardItem(private val context: Context, game: Game,
                                private val listener: OnCardClickListener? = null) : CardItem(game) {

        override fun getLayoutRes() = R.layout.recyclerview_item3_matchup_upcoming

        override fun createViewHolder(view: View?,
                                      adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>?)
                : CardViewHolder = UpcomingCard(context, view, adapter, listener)

        open class UpcomingCard(private val context: Context, view: View?,
                                adapter: FlexibleAdapter<out IFlexible<*>>?,
                                listener: OnCardClickListener? = null)
            : CardViewHolder(view, adapter, listener) {
            val textTime: TextView? = view?.findViewById(R.id.textView1)
            private val textId: TextView? = view?.findViewById(R.id.textView9)
            val awayTeamName: TextView? = view?.findViewById(R.id.textView2)
            val homeTeamName: TextView? = view?.findViewById(R.id.textView5)
            val textField: TextView? = view?.findViewById(R.id.textView7)
            private val textChannel: TextView? = view?.findViewById(R.id.textView6)

            override fun apply(game: Game, position: Int) {
                super.apply(game, position)
                textId?.text = game.Id.toString()
                awayTeamName?.text = game.AwayTeam?.shortName
                homeTeamName?.text = game.HomeTeam?.shortName
                textField?.text = String.format("%s%s", context.getString(R.string.title_at), game.Field)
                textChannel?.apply {
                    visibility = if (game.Channel.isNullOrEmpty()) View.GONE else View.VISIBLE
                    text = game.Channel
                }
                val diff = DateUtils.getRelativeTimeSpanString(game.StartTime.timeInMillis,
                        System.currentTimeMillis(), DateUtils.DAY_IN_MILLIS).toString()
                textTime?.text = if (game.isToday) {
                    "$diff ${SimpleDateFormat("HH:mm", Locale.TAIWAN).format(game.StartTime.time)}"
                } else diff
            }
        }
    }

    open class LiveCardItem(private val context: Context, game: Game,
                            private val listener: OnCardClickListener? = null)
        : UpcomingCardItem(context, game, listener) {

        override fun getLayoutRes() = R.layout.recyclerview_item3_matchup

        override fun createViewHolder(view: View?,
                                      adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>?)
                : CardViewHolder = LiveCardHolder(context, view, adapter, listener)

        open class LiveCardHolder(private val context: Context, view: View?,
                                  adapter: FlexibleAdapter<out IFlexible<*>>?,
                                  listener: OnCardClickListener? = null)
            : UpcomingCard(context, view, adapter, listener) {

            private val awayTeamScore: TextView? = view?.findViewById(R.id.textView3)
            private val homeTeamScore: TextView? = view?.findViewById(R.id.textView4)

            override fun apply(game: Game, position: Int) {
                super.apply(game, position)
                if (!game.LiveMessage.isNullOrEmpty()) {
                    textTime?.text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        Html.fromHtml(game.LiveMessage, Html.TO_HTML_PARAGRAPH_LINES_INDIVIDUAL)
                    } else {
                        Html.fromHtml(game.LiveMessage)
                    }
                }
                awayTeamScore?.text = game.AwayScore.toString()
                homeTeamScore?.text = game.HomeScore.toString()
                textField?.text = game.getFieldFullName(context)
            }
        }
    }

    class ResultCardItem(private val context: Context, private val helper: TeamHelper, game: Game,
                         private val listener: OnCardClickListener? = null)
        : LiveCardItem(context, game, listener) {

        override fun getLayoutRes() = R.layout.recyclerview_item3_matchup_final

        override fun createViewHolder(view: View?,
                                      adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>?)
                : CardViewHolder = ResultCardHolder(context, helper, view, adapter, listener)

        class ResultCardHolder(context: Context, private val helper: TeamHelper, view: View?,
                               adapter: FlexibleAdapter<out IFlexible<*>>?,
                               listener: OnCardClickListener? = null)
            : LiveCardHolder(context, view, adapter, listener) {

            private val awayTeamLogo: ImageView? = view?.findViewById(android.R.id.icon1)
            private val homeTeamLogo: ImageView? = view?.findViewById(android.R.id.icon2)

            override fun apply(game: Game, position: Int) {
                super.apply(game, position)

                textTime?.text = SimpleDateFormat("MMM d (E)",
                        Locale.TAIWAN).format(game.StartTime.time)

                awayTeamName?.text = game.AwayTeam?.name
                homeTeamName?.text = game.HomeTeam?.name

                val year = game.StartTime.get(Calendar.YEAR)
                awayTeamLogo?.setColorFilter(helper.getLogoColorFilter(game.AwayTeam, year),
                        PorterDuff.Mode.SRC_IN)
                homeTeamLogo?.setColorFilter(helper.getLogoColorFilter(game.HomeTeam, year),
                        PorterDuff.Mode.SRC_IN)
            }
        }
    }

    open class UpdateCardItem(game: Game, private val listener: OnCardClickListener? = null)
        : CardItem(game, listener) {
        //constructor(message: String) : this(Game(-3).apply { LiveMessage = message })

        override fun getLayoutRes() = R.layout.recyclerview_item3_update_info

        override fun createViewHolder(view: View?,
                                      adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>?)
                : CardViewHolder = UpdateCardHolder(view, adapter, listener)

        class UpdateCardHolder(view: View?, adapter: FlexibleAdapter<out IFlexible<*>>?,
                               listener: OnCardClickListener? = null)
            : CardViewHolder(view, adapter, listener) {
            val summary: TextView? = view?.findViewById(R.id.textView10)

            override fun apply(game: Game, position: Int) {
                super.apply(game, position)
                summary?.text = game.LiveMessage
            }
        }
    }

    class AnnounceCardItem(game: Game) : UpdateCardItem(game) {
        override fun getLayoutRes() = R.layout.recyclerview_item3_announce
    }
}