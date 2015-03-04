package dolphin.android.apps.CpblCalendar;

import java.util.ArrayList;

import dolphin.android.apps.CpblCalendar.provider.CpblCalendarHelper;
import dolphin.android.apps.CpblCalendar.provider.Game;

/**
 * Created by dolphin on 2015/02/07.
 */
public interface OnQueryCallback {
    public void onQueryStart();

    public void onQueryStateChange(String msg);

    public void onQuerySuccess(CpblCalendarHelper helper, ArrayList<Game> gameArrayList);

    public void onQueryError();
}
