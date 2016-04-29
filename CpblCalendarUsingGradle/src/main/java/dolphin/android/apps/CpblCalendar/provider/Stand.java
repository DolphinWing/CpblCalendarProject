package dolphin.android.apps.CpblCalendar.provider;

/**
 * Created by dolphin on 2014/4/4.
 */
public class Stand {
    private final Team mTeam;
    private final int mWins;
    private final int mLoses;
    private final int mTies;
    private final float mWinningRate;
    private final float mGameBehind;

    public Stand(Team team, int win, int lose, int tie, float rate, float behind) {
        mTeam = team;
        mWins = win;
        mLoses = lose;
        mTies = tie;
        mWinningRate = rate;
        mGameBehind = behind;
    }

    public Team getTeam() {
        return mTeam;
    }

    public int getGamesWon() {
        return mWins;
    }

    public int getGamesLost() {
        return mLoses;
    }

    public int getGamesTied() {
        return mTies;
    }

    public float getWinningRate() {
        return mWinningRate;
    }

    public float getGamesBehind() {
        return mGameBehind;
    }
}
