package dolphin.android.apps.CpblCalendar;

import java.util.ArrayList;

import dolphin.android.apps.CpblCalendar.provider.CpblCalendarHelper;
import dolphin.android.apps.CpblCalendar.provider.Game;

/**
 * Created by dolphin on 2015/02/07.
 * <p>
 * Query action callback
 */
public interface OnQueryCallback {
    void onQueryStart();

    void onQueryStateChange(String msg);

    void onQuerySuccess(CpblCalendarHelper helper, ArrayList<Game> gameArrayList, int year, int month);

    void onQueryError(int year, int month);
}
