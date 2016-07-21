package main.java.model;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import main.java.db.DrawDB;

import java.sql.SQLException;
import java.util.Date;

/**
 * Created by DFX on 18.05.2016.
 */
@DatabaseTable(tableName = "matches")
public class Match {
    public static final String TOTALPOINTS_FIELD_NAME = "totalpoints";
    public static final String STATE_FIELD_NAME = "state";
    public static final String STREAK_FIELD_NAME = "streak";
    public static final String LASTACTION_FIELD_NAME = "lastaction";
    public static final String PLAYER1_FIELD_NAME = "player1";
    public static final String PLAYER2_FIELD_NAME = "player2";
    public static final String ROUND_ID_NAME = "round_id";

    public enum MState {
        P1DRAWS, P1GUESSES, P2DRAWS, P2GUESSES;
        
        public static MState flipPlayer(MState s) {
            switch(s) {
                case P1DRAWS:
                    return P2DRAWS;
                case P2DRAWS:
                    return P1DRAWS;
                case P1GUESSES:
                    return P2GUESSES;
                case P2GUESSES:
                default:
                    return P1GUESSES;
            }
        }
    }

    @DatabaseField(generatedId = true)
    private int id;

    @DatabaseField(columnName = PLAYER1_FIELD_NAME)
    private int p1;

    @DatabaseField(columnName = PLAYER2_FIELD_NAME)
    private int p2;

    @DatabaseField(columnName = STATE_FIELD_NAME)
    private MState state;

    @DatabaseField(columnName = TOTALPOINTS_FIELD_NAME)
    private int totalPoints;

    @DatabaseField(columnName = STREAK_FIELD_NAME)
    private int streak;

    @DatabaseField(columnName = LASTACTION_FIELD_NAME)
    private Date lastTimeAction;

    @DatabaseField(columnName = ROUND_ID_NAME)
    private int roundId;

    // Empty Constructor for ORMLite
    public Match() {
    }

    public Match(Player p1, Player p2, boolean p1Begins) {
        this.p1 = p1.getId();
        this.p2 = p2.getId();

        if(p1Begins) {
            this.state = MState.P1DRAWS;
        } else {
            this.state = MState.P2DRAWS;
        }

        this.totalPoints = 0;
        this.streak = 0;
        this.lastTimeAction = new Date();
    }

    public void setRoundId(int id) {
        this.roundId = id;
    }

    public Player getPlayer1() {
        try {
            return DrawDB.getHandler().getPlayerById(this.p1);
        } catch (InternalServerException e) {
            e.printStackTrace();
            return null;
        }
    }

    public Player getPlayer2() {
        try {
            return DrawDB.getHandler().getPlayerById(this.p2);
        } catch (InternalServerException e) {
            e.printStackTrace();
            return null;
        }
    }


    public Player getOpponentOf(Player p) {
        return (p.equals(getPlayer1())) ? getPlayer2() : getPlayer1();
    }

    public Date getLastTimeAction() {
        return lastTimeAction;
    }

    public String getStateStr(Player p) {
        MState mst = (p.equals(getPlayer1())) ? this.state : MState.flipPlayer(this.state);

        switch(mst) {
            case P1DRAWS:
                return "yourturn_draw";
            case P1GUESSES:
                return "yourturn_guess";
            case P2DRAWS:
                return "opponentturn_draw";
            case P2GUESSES:
                return "opponentturn_guess";
            default:
                return "";
        }
    }

    public boolean isPlayerDrawTurn(Player p) {
        MState mst = (p.equals(getPlayer1())) ? this.state : MState.flipPlayer(this.state);
        return (mst.equals(MState.P1DRAWS));
    }

    public boolean isPlayerTurn(Player p) {
        MState mst = (p.equals(getPlayer1())) ? this.state : MState.flipPlayer(this.state);
        return (mst.equals(MState.P1DRAWS) || mst.equals(MState.P1GUESSES));
    }

    public boolean isPlayerGuessTurn(Player p) {
        MState mst = (p.equals(getPlayer1())) ? this.state : MState.flipPlayer(this.state);
        return (mst.equals(MState.P1GUESSES));
    }

    public int getId() {
        return id;
    }

    public int getTotalPoints() {
        return totalPoints;
    }

    public int getStreak() {
        return streak;
    }

    public Round getRound() {
        try {
            return DrawDB.getHandler().getRoundById(roundId);
        } catch(InternalServerException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void playerFinishedDrawing(Player p) throws InternalServerException {
        this.state = MState.P1GUESSES;
        if(p.equals(getPlayer1())) {
            this.state = MState.flipPlayer(this.state);
        }
        this.setLastTimeAction();
        update();
    }

    public void roundFinished(int calculatedPoints) throws InternalServerException {
        streak = streak + 1;
        totalPoints = totalPoints + calculatedPoints;


        Round newRound;
        if(this.state.equals(MState.P1GUESSES)) {
            this.state = MState.P1DRAWS;
            newRound = Round.createBlankRound(getPlayer1());
        } else {
            this.state = MState.P2DRAWS;
            newRound = Round.createBlankRound(getPlayer1());
        }
        newRound.create();

        this.roundId = newRound.getId();
        this.setLastTimeAction();

        update();
    }

    public void setLastTimeAction() throws InternalServerException {
        this.lastTimeAction = new Date();
    }

    public void update() throws InternalServerException {
        try {
            DrawDB.getHandler().matchesDao.update(this);
        } catch (SQLException e) {
            throw new InternalServerException("Error while updating Match!", e);
        }
    }
}
