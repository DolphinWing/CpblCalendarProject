package dolphin.android.util;

import java.util.Calendar;

/**
 * Created by dolphin on 2017/05/20.
 * <p>
 * Quick access to some comparisons.
 */

public class DateUtils {
    public static boolean sameDay(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    public static boolean isToday(Calendar calendar) {
        return sameDay(Calendar.getInstance(), calendar);
    }
}
