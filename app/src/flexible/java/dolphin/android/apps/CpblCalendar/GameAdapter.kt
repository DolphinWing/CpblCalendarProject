package dolphin.android.apps.CpblCalendar

import android.content.Context
import android.graphics.PorterDuff
import android.os.Build
import android.text.Html
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import dolphin.android.apps.CpblCalendar.preference.PrefsHelper
import dolphin.android.apps.CpblCalendar.provider.CpblCalendarHelper
import dolphin.android.apps.CpblCalendar.provider.Game
import dolphin.android.apps.CpblCalendar.provider.TeamHelper
import dolphin.android.apps.CpblCalendar3.CpblApplication
import dolphin.android.apps.CpblCalendar3.R
import java.util.*

/**
 * Created by dolphin on 2014/7/5.
 *
 *
 * game adapter implementation
 */
@Suppress("PrivatePropertyName")
internal class GameAdapter(context: Context, objects: List<Game>?, application: CpblApplication) :
        BaseGameAdapter(context, objects) {
    //private final static String TAG = "GameAdapter";
    private val mTeamHelper: TeamHelper = TeamHelper(application)
    private var mDesktopMode = context.packageManager.hasSystemFeature("org.chromium.arc.device_management")

    private var ENABLE_CACHE_MODE = PrefsHelper(context).cacheModeEnabled
    private var ENABLE_BOTTOM_SHEET = FirebaseRemoteConfig.getInstance().getBoolean("enable_bottom_sheet_options")

    internal interface OnOptionClickListener {
        fun onOptionClicked(view: View, game: Game)
    }

    //private var mListener: OnOptionClickListener? = null
    internal var onOptionClickListener: OnOptionClickListener? = null

    override fun decorate(convertView: View, game: Game) {
        super.decorate(convertView, game)

        //more action
        val moreAction = convertView.findViewById<ImageView>(android.R.id.icon)
        if (moreAction != null) {
            if (ENABLE_BOTTOM_SHEET && !mDesktopMode) {
                moreAction.visibility = View.VISIBLE
                val control = convertView.findViewById<View>(R.id.item_control_pane)
                if (control != null) {
                    control.tag = game//for listener
                    control.setOnClickListener { view ->
                        onOptionClickListener?.onOptionClicked(view, view.tag as Game)
                    }
                } else {
                    moreAction.tag = game//for listener
                    moreAction.setOnClickListener { view ->
                        onOptionClickListener?.onOptionClicked(view, view.tag as Game)
                    }
                }
            } else {
                moreAction.visibility = View.INVISIBLE
            }
        }

        convertView.findViewById<TextView>(R.id.textView3)?.visibility =
                if (ENABLE_CACHE_MODE) View.INVISIBLE else View.VISIBLE

        convertView.findViewById<TextView>(R.id.textView4)?.visibility =
                if (ENABLE_CACHE_MODE) View.INVISIBLE else View.VISIBLE

        val timeText = convertView.findViewById<TextView>(R.id.textView1)
        val liveText = convertView.findViewById<TextView>(R.id.textView10)
        if (timeText != null) {
            var date_str = getGameDateStr(game)
            if (game.IsLive) {
                if (liveText != null) {//use live text field in design flavor
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        liveText.text = Html.fromHtml(game.LiveMessage.replace("&nbsp;&nbsp;",
                                "<br>"), Html.TO_HTML_PARAGRAPH_LINES_INDIVIDUAL)
                    } else {
                        liveText.text = Html.fromHtml(game.LiveMessage.replace("&nbsp;&nbsp;", "<br>"))
                    }
                    liveText.visibility = View.VISIBLE
                    timeText.text = date_str
                } else {
                    date_str = String.format("%s&nbsp;&nbsp;%s", date_str, game.LiveMessage)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        timeText.text = Html.fromHtml(date_str, Html.TO_HTML_PARAGRAPH_LINES_INDIVIDUAL)
                    } else {
                        timeText.text = Html.fromHtml(date_str)
                    }
                }
            } else {
                if (liveText != null) {//don't show
                    liveText.visibility = View.INVISIBLE
                }
                timeText.text = date_str
            }
        }

        //delay message
        convertView.findViewById<TextView>(R.id.textView8)?.apply {
            if (game.DelayMessage.isNullOrEmpty()) {
                visibility = View.GONE
            } else {
                visibility = View.VISIBLE
                text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    Html.fromHtml(game.DelayMessage, Html.TO_HTML_PARAGRAPH_LINES_INDIVIDUAL)
                } else {
                    Html.fromHtml(game.DelayMessage)
                }
            }
        }

        convertView.findViewById<TextView>(R.id.textView6)?.visibility =
                if (game.Channel != null) View.VISIBLE else View.GONE

        convertView.findViewById<View>(R.id.icon_alarm)?.apply {
            setOnClickListener(null)
            //setClickable(false);
            isFocusable = false
        }

        //team logo
        val year = game.StartTime.get(Calendar.YEAR)
        //Log.d("GameAdapter", String.format("year = %d", year));
        convertView.findViewById<ImageView>(android.R.id.icon1)?.apply {
            setImageResource(R.drawable.ic_baseball)
            setColorFilter(mTeamHelper.getLogoColorFilter(game.AwayTeam, year),
                    PorterDuff.Mode.SRC_IN)
            visibility = if (isShowLogo) View.VISIBLE else View.INVISIBLE
        }
        convertView.findViewById<ImageView>(android.R.id.icon2)?.apply {
            setImageResource(R.drawable.ic_baseball)
            setColorFilter(mTeamHelper.getLogoColorFilter(game.HomeTeam, year),
                    PorterDuff.Mode.SRC_IN)
            visibility = if (isShowLogo) View.VISIBLE else View.INVISIBLE
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val now = CpblCalendarHelper.getNowTime()
            var url: String? = game.Url//null;
            if (game.StartTime.after(now) && url!!.contains("box.html")) {
                url = null
            }
            if (url == null) {//no further information
                convertView.setOnContextClickListener { true }
            }
        }
    }

    override fun getLayoutResId(game: Game): Int = R.layout.listview_item_matchup

    override fun supportLongName(game: Game): Boolean = true

    override fun setAlarm(game: Game) {
        //do nothing
    }

    override fun cancelAlarm(game: Game) {
        //do nothing
    }
}
