package dolphin.android.apps.CpblCalendar.preference;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import dolphin.android.apps.CpblCalendar.provider.CpblCalendarHelper;
import dolphin.android.apps.CpblCalendar.provider.Game;

/**
 * Created by dolphin on 2013/8/31.
 * <p/>
 * Hleper class to get/set alarm from preference
 */
public class AlarmHelper {
    public final static String TAG = "AlarmHelper";
    private final static boolean DEBUG_LOG = false;

    private final Context mContext;
    private final dolphin.android.preference.PreferenceUtils pHelper;
    //private SparseArray<Game> mAlarmArray = null;

    private final static String KEY_ALARM_LIST = "_alarm_list";

    private final static String KEY_JOB_MAP = "_job_map";

    /**
     * get key for Alarm ID
     *
     * @param game game
     * @return alarm id
     */
    public static String getAlarmIdKey(Game game) {
        //[122]dolphin++ new ker pair for better mapping
        return String.format(Locale.US, "%d-%d-%s", game.StartTime.get(Calendar.YEAR),
                game.Id, game.Kind);
    }

    /**
     * get key for Alarm data
     *
     * @param game_key alarm key
     * @return alarm data
     */
    private static String getAlarmDataKey(String game_key) {
        return "_alarm_" + game_key;
    }

    public AlarmHelper(Context context) {
        mContext = context;
        pHelper = new dolphin.android.preference.PreferenceUtils(mContext);
        //mAlarmArray = new SparseArray<Game>();
    }

    /**
     * add a game to alarm list
     *
     * @param game game
     */
    public void addGame(Game game) {
        if (game == null) {
            throw new NullPointerException("no game");
        }

        String key = getAlarmIdKey(game);
        if (DEBUG_LOG) {
            Log.v(TAG, "addGame key: " + key);
        }
        Set<String> keySet = pHelper.getStringSet(KEY_ALARM_LIST);
        if (keySet != null) {
            if (keySet.contains(key)) {
                Log.e(TAG, "already in alarm list");
                return;
            }
        } else {
            keySet = new HashSet<String>();
        }
        keySet.add(key);

        pHelper.putStringSet(KEY_ALARM_LIST, keySet);
        pHelper.putString(getAlarmDataKey(key), Game.toPrefString(game));
        //mAlarmArray.put(game.Id, game);//put to memory
    }

    /**
     * remove a game from alarm list
     *
     * @param game game
     */
    public void removeGame(Game game) {
        removeGame(getAlarmIdKey(game));
    }

    /**
     * remove a game from alarm list
     *
     * @param key game key
     */
    public void removeGame(String key) {
        if (DEBUG_LOG) {
            Log.v(TAG, "removeGame key: " + key);
        }
        Set<String> keySet = pHelper.getStringSet(KEY_ALARM_LIST);
        if (keySet != null && keySet.contains(key)) {
            keySet.remove(key);
            pHelper.putStringSet(KEY_ALARM_LIST, keySet);
            pHelper.remove(getAlarmDataKey(key));
            //mAlarmArray.remove(Integer.decode(key.split("-")[1]));
        }
    }

    /**
     * update a game data in preference
     *
     * @param game game
     */
    public void updateGame(Game game) {
        String key = getAlarmIdKey(game);
        //Log.d(TAG, "updateGame key: " + key);
        Set<String> keySet = pHelper.getStringSet(KEY_ALARM_LIST);
        if (keySet != null) {
            if (keySet.contains(key)) {
                pHelper.putString(getAlarmDataKey(key), Game.toPrefString(game));
            } else {//check game id, for old key pair
                for (String aKeySet : keySet) {
                    //remove the original key and data
                    if (Integer.decode(aKeySet.split("-")[1]) == game.Id) {
                        removeGame(key);//remove old entry
                        addGame(game);//put as new entry
                        break;
                    }
                }
            }
        }
    }

    /**
     * get entire alarm list
     *
     * @return game list
     */
    public ArrayList<Game> getAlarmList() {
        Set<String> keySet = pHelper.getStringSet(KEY_ALARM_LIST);
        if (keySet == null) {
            Log.e(TAG, String.format("%s does not have data", KEY_ALARM_LIST));
            return null;
        }

        List<String> keys = new LinkedList<String>(keySet);
        Collections.sort(keys, new Comparator<String>() {
            @Override
            public int compare(String s1, String s2) {
                //compare game start time first
                Game g1 = Game.fromPrefString(mContext, pHelper.getString(getAlarmDataKey(s1)));
                Game g2 = Game.fromPrefString(mContext, pHelper.getString(getAlarmDataKey(s2)));
                if (g1 != null && g2 != null) {
                    if (g1.StartTime.compareTo(g2.StartTime) != 0) {//different day game
                        return g1.StartTime.compareTo(g2.StartTime);
                    }
                    //return Integer.compare(g1.Id, g2.Id);//same day game
                    return g1.Id - g2.Id;
                }
                //compare id
                String[] str1 = s1.split("-");
                String[] str2 = s2.split("-");
                if (str1[0].compareTo(str2[0]) == 0) {//same year
                    return Integer.decode(str1[1]).compareTo(Integer.decode(str2[1]));
                }
                //not possible to get here, we should remove last year's game
                return Long.decode(str1[0]).compareTo(Long.decode(str2[0]));
            }
        });

        //mAlarmArray.clear();
        ArrayList<Game> list = new ArrayList<>();
        //Log.d(TAG, String.format("getAlarmList list size = %d", keys.size()));
        Calendar now = CpblCalendarHelper.getNowTime();//Calendar.getInstance();
        if (PreferenceUtils.getAlarmNotifyTime(mContext) == 0) {
            now.add(Calendar.MINUTE, -1);//check one minute before
        }
        for (String key : keys) {
            Game g = Game.fromPrefString(mContext, pHelper.getString(getAlarmDataKey(key)));
            if (DEBUG_LOG) {
                Log.d(TAG, " " + g.Id + " @ " + g.StartTime.getTimeInMillis());
            }
            //mAlarmArray.put(g.Id, g);//put to memory
            if (g.StartTime.after(now)) {
                list.add(g);
            } else {//should remove from the list
                Log.w(TAG, String.format("game %d StartTime is passed.", g.Id));
                if (g.StartTime.get(Calendar.YEAR) < now.get(Calendar.YEAR) ||
                        (now.getTimeInMillis() - g.StartTime.getTimeInMillis() > 86400)) {
                    removeGame(g);//remove last year's game
                }
            }
        }

        return list;
    }

    /**
     * check if this game has set alarm
     *
     * @param game game
     * @return true if having set alarm
     */
    public boolean hasAlarm(Game game) {
        String key = getAlarmIdKey(game);
        //Log.d(TAG, "hasAlarm key: " + key);
        Set<String> keySet = pHelper.getStringSet(KEY_ALARM_LIST);
        if (keySet != null) {
            if (keySet.contains(key)) {
                if (DEBUG_LOG) {
                    Log.d(TAG, "hasAlarm key: " + key);
                }
                return true;
            } else {//check game id, for some old key pair
                //[122]dolphin-- now always use year-id-kind as key
                for (String aKeySet : keySet) {
                    //key = StartTime-Id
                    if (Integer.decode(aKeySet.split("-")[1]) == game.Id) {
                        Log.w(TAG, "try to update " + aKeySet);
                        updateGame(game);//update the delayed info
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * get next alarm from the list
     *
     * @return next game with alarm
     */
    public Game getNextAlarm() {
        ArrayList<Game> list = getAlarmList();
        if (list != null) {//not empty list
            Calendar now = CpblCalendarHelper.getNowTime();//Calendar.getInstance();
            for (Game game : list) {
                //Log.d(TAG, " " + game.Id + " " + game.StartTime.getTime().toString());
                if (game.StartTime.after(now)) {
                    return game;
                }
                removeGame(game);//remove the game from alarm list
            }
            //return list.get(0);
        }
        return null;
    }

    /**
     * clear all alarms
     */
    public void clear() {
        ArrayList<Game> list = getAlarmList();
        if (list != null) {//not empty list
            for (Game game : list) {
                removeGame(game);//remove the game from alarm list
            }
        }
    }

    /**
     * store Job ID for Evernote JobManager
     *
     * @param key   Game key
     * @param jobId Job ID
     */
    public void addJobId(String key, int jobId) {
        //remove duplicated data
        for (int oldId = getJobId(key); oldId > 0; oldId = getJobId(key)) {
            if (DEBUG_LOG) {
                Log.d(TAG, "remove old id: " + oldId);
            }
            removeJobId(oldId);
        }

        String pattern = String.format(Locale.US, "%s:%d", key, jobId);
        String jobMap = pHelper.getString(KEY_JOB_MAP, "");
        if (DEBUG_LOG) {
            Log.d(TAG, "map>>> " + jobMap);
        }
        if (!jobMap.contains(pattern)) {
            jobMap = jobMap.concat(";").concat(pattern);
            jobMap = jobMap.replaceAll("[;]+", ";");
            if (DEBUG_LOG) {
                Log.d(TAG, "new map>>> " + jobMap);
            }
            pHelper.putString(KEY_JOB_MAP, jobMap);
        }
    }

    /**
     * get Job ID for Evernote JobManager from SharedPreference
     *
     * @param key Game key
     * @return Job ID
     */
    public int getJobId(String key) {
        String jobMap = pHelper.getString(KEY_JOB_MAP, "");
        if (DEBUG_LOG) {
            Log.d(TAG, "map>>> " + jobMap);
        }
        String[] maps = jobMap.split(";");
        for (String job : maps) {
            String[] map = job.split(":");
            if (map.length > 1 && map[0].equals(key)) {
                if (DEBUG_LOG) {
                    Log.d(TAG, "found: " + job);
                }
                return Integer.parseInt(map[1]);
            }
        }
        return -1;
    }

    /**
     * remove Job ID from SharedPreference
     *
     * @param jobId Job ID
     */
    public void removeJobId(int jobId) {
        String jobMap = pHelper.getString(KEY_JOB_MAP, "");
        if (DEBUG_LOG) {
            Log.d(TAG, "map>>> " + jobMap);
        }
        if (jobId > 0) {
            String[] maps = jobMap.split(";");
            for (String job : maps) {
                String[] map = job.split(":");
                if (map.length > 1 && Integer.parseInt(map[1]) == jobId) {
                    if (DEBUG_LOG) {
                        Log.d(TAG, "found: " + job);
                    }
                    jobMap = jobMap.replace(job, "");
                    jobMap = jobMap.replaceAll("[;]+", ";");
                    if (DEBUG_LOG) {
                        Log.d(TAG, "new map>>> " + jobMap);
                    }
                    pHelper.putString(KEY_JOB_MAP, jobMap);
                }
            }
        } else if (jobId == 0) {
            Log.v(TAG, "clear all");
            pHelper.putString(KEY_JOB_MAP, "");
        }
    }
}
