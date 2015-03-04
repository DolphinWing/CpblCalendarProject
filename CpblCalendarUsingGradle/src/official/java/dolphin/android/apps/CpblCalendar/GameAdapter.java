package dolphin.android.apps.CpblCalendar;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Html;
import android.text.SpannableString;
import android.text.format.DateUtils;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import dolphin.android.apps.CpblCalendar.preference.AlarmHelper;
import dolphin.android.apps.CpblCalendar.preference.PreferenceUtils;
import dolphin.android.apps.CpblCalendar.provider.CpblCalendarHelper;
import dolphin.android.apps.CpblCalendar.provider.Game;

/**
 * Created by dolphin on 2014/7/5.
 *
 * game adapter implementation
 */
public class GameAdapter extends BaseGameAdapter {

    public GameAdapter(Context context, List<Game> objects) {
        super(context, objects);
    }

    @Override
    protected int getLayoutResId(Game game) {
        boolean bIsLongName = game.Kind.equalsIgnoreCase("09");
        return bIsLongName ? R.layout.listview_item_matchup_09 : R.layout.listview_item_matchup;
    }

    @Override
    protected boolean supportLongName(Game game) {
        return game.Kind.equalsIgnoreCase("09");
    }
}
