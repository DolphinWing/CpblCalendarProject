package dolphin.android.apps.CpblCalendar.provider

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.palette.graphics.Palette

import dolphin.android.apps.CpblCalendar3.CpblApplication

/**
 * Created by jimmyhu on 2017/3/17.
 *
 *
 * A helper class to handle Team logo & palette.
 */

class TeamHelper(private val application: CpblApplication) {

    fun getLogoColorFilter(team: Team, year: Int): Int {
        val logoId = Team.getTeamLogo(team.id, year)
        var palette: Palette? = application.getTeamLogoPalette(logoId)
        if (palette == null) {
            val bitmap = BitmapFactory.decodeResource(application.resources, logoId)
            palette = Palette.from(bitmap).generate()
            application.setTeamLogoPalette(logoId, palette)//save to cache
        }
        var color = Color.BLACK
        if (!validateColor(color) && palette.dominantSwatch != null) {
            color = palette.getDominantColor(color)
        }
        if (!validateColor(color) && palette.vibrantSwatch != null) {
            color = palette.getVibrantColor(color)
        }
        if (!validateColor(color) && palette.mutedSwatch != null) {
            color = palette.getMutedColor(color)
        }
        if (!validateColor(color)) {
            color = Color.BLACK//still use black
        }
        return color
    }

    private fun validateColor(color: Int): Boolean {
        when (color) {
            Color.BLACK, Color.WHITE, Color.TRANSPARENT -> return false
        }
        return true
    }
}
