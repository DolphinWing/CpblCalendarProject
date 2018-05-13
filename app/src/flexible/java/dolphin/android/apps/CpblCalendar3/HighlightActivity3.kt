package dolphin.android.apps.CpblCalendar3

import android.content.Intent
import dolphin.android.apps.CpblCalendar.provider.Game
import java.util.*

class HighlightActivity3 : HighlightActivity() {
    override fun startGameListActivity(list: ArrayList<Game>?) {
        //super.startGameListActivity(list)
        val intent = Intent(this, ListActivity::class.java)
        intent.putParcelableArrayListExtra(KEY_CACHE, list)
        //Log.d(TAG, String.format("list %d", mCacheGames.size()));
        startActivity(intent)
    }
}