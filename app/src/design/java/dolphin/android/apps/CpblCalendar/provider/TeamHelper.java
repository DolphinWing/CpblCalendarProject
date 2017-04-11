package dolphin.android.apps.CpblCalendar.provider;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.support.v7.graphics.Palette;

import dolphin.android.apps.CpblCalendar.CpblApplication;

/**
 * Created by jimmyhu on 2017/3/17.
 * <p>
 * A helper class to handle Team logo & palette.
 */

public class TeamHelper {
    private final CpblApplication mApplication;

    public TeamHelper(CpblApplication application) {
        mApplication = application;
    }

    private CpblApplication getApplication() {
        return mApplication;
    }

    public int getLogoColorFilter(Team team, int year) {
        int logoId = Team.getTeamLogo(team.getId(), year);
        Palette palette = getApplication().getTeamLogoPalette(logoId);
        if (palette == null) {
            Bitmap bitmap = BitmapFactory.decodeResource(getApplication().getResources(), logoId);
            palette = Palette.from(bitmap).generate();
            getApplication().setTeamLogoPalette(logoId, palette);//save to cache
        }
        int color = Color.BLACK;
        if (!validateColor(color) && palette.getDominantSwatch() != null) {
            color = palette.getDominantColor(color);
        }
        if (!validateColor(color) && palette.getVibrantSwatch() != null) {
            color = palette.getVibrantColor(color);
        }
        if (!validateColor(color) && palette.getMutedSwatch() != null) {
            color = palette.getMutedColor(color);
        }
        if (!validateColor(color)) {
            color = Color.BLACK;//still use black
        }
        return color;
    }

    private boolean validateColor(int color) {
        switch (color) {
            case Color.BLACK:
            case Color.WHITE:
            case Color.TRANSPARENT:
                return false;
        }
        return true;
    }
}
