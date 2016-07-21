package main.java.db;

import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import com.j256.ormlite.dao.Dao;
import main.java.model.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.util.List;

/**
 * Created by DFX on 10.06.2016.
 */
public class DrawDB {
    //private static final String JDBC_DRIVER = "org.h2.Driver"; //org.h2.Driver
    private static final String DB_URL = "jdbc:h2:./drawdb2";
    static final Logger logger = LogManager.getLogger(DrawDB.class);

    private static DrawDB reference;

    public Dao<Player, Integer> playersDao;
    public Dao<Match, Integer> matchesDao;
    public Dao<Round, Integer> roundsDao;
    public Dao<DataElement, Integer> dataElementDao;

    public DrawDB() {
        DrawDB.setReference(this);
    }

    private static void setReference(DrawDB ref) {
        reference = ref;
    }

    public static DrawDB getHandler() {
        return reference;
    }

    public boolean init() {
        ConnectionSource connectionSource;

        try {
            connectionSource = new JdbcConnectionSource(DB_URL);
            playersDao = DaoManager.createDao(connectionSource, Player.class);
            TableUtils.createTableIfNotExists(connectionSource, Player.class);

            matchesDao = DaoManager.createDao(connectionSource, Match.class);
            TableUtils.createTableIfNotExists(connectionSource, Match.class);

            roundsDao = DaoManager.createDao(connectionSource, Round.class);
            TableUtils.createTableIfNotExists(connectionSource, Round.class);

            dataElementDao = DaoManager.createDao(connectionSource, DataElement.class);
            TableUtils.createTableIfNotExists(connectionSource, DataElement.class);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return true;
    }

    public boolean isUserIDAvailable(int userid) throws InternalServerException {
        QueryBuilder<Player, Integer> statementBuilder = playersDao.queryBuilder();
        try {
            statementBuilder.where().eq(Player.PUB_ID_FIELD_NAME, userid);
            List<Player> players = playersDao.query(statementBuilder.prepare());
            if(players.size() > 0) {
                return false;
            }
        } catch (SQLException e) {
            throw new InternalServerException("SQL error in userid availability check!", e);
        }
        return true;
    }

    public void addUser(String username, int userid, String userhash) throws InternalServerException {
        Player newUser = new Player();
        newUser.setName(username);
        newUser.setPublicId(userid);
        newUser.setSecret(userhash);
        newUser.setLastOnline(new java.util.Date());

        try {
            playersDao.create(newUser);
        } catch (SQLException e) {
            throw new InternalServerException("SQL error in user creation!", e);
        }
    }

    public boolean doesPlayerExist(int uid) throws InternalServerException {
        return !isUserIDAvailable(uid);
    }

    public String checkLogin(int userid, String hash) throws InternalServerException {
        QueryBuilder<Player, Integer> statementBuilder = playersDao.queryBuilder();
        try {
            statementBuilder.where().eq(Player.PUB_ID_FIELD_NAME, userid).and().eq(Player.SECRET_FIELD_NAME, hash);
            List<Player> players = playersDao.query(statementBuilder.prepare());
            if(players.size() > 0) {
                // Get Player and set last online to now
                Player thePlayer = players.get(0);
                thePlayer.setLastOnline(new java.util.Date());
                playersDao.update(thePlayer);

                return thePlayer.getName();
            }
        } catch (SQLException e) {
            throw new InternalServerException("SQL error in userid availability check!", e);
        }
        return null;
    }

    public Match[] getAllMatchesForPlayer(int id) throws InternalServerException {
        QueryBuilder<Match, Integer> statementBuilder = matchesDao.queryBuilder();
        try {
            statementBuilder.where().eq(Match.PLAYER1_FIELD_NAME, id).or().eq(Match.PLAYER2_FIELD_NAME, id);
            List<Match> matches = matchesDao.query(statementBuilder.prepare());
            return matches.toArray(new Match[matches.size()]);
        } catch (SQLException e) {
            throw new InternalServerException("SQL error in player fetch!", e);
        }
    }

    public Player getPlayerById(int id) throws InternalServerException {
        QueryBuilder<Player, Integer> statementBuilder = playersDao.queryBuilder();
        try {
            statementBuilder.where().eq("id", id);
            List<Player> players = playersDao.query(statementBuilder.prepare());
            if(players.size() > 0) {
                return players.get(0);
            } else {
                return null;
            }
        } catch (SQLException e) {
            throw new InternalServerException("SQL error in player fetch!", e);
        }
    }

    public Player getPlayerByPublicId(int id) throws InternalServerException {
        QueryBuilder<Player, Integer> statementBuilder = playersDao.queryBuilder();
        try {
            statementBuilder.where().eq(Player.PUB_ID_FIELD_NAME, id);
            List<Player> players = playersDao.query(statementBuilder.prepare());
            if(players.size() > 0) {
                return players.get(0);
            } else {
                return null;
            }
        } catch (SQLException e) {
            throw new InternalServerException("SQL error in player fetch!", e);
        }
    }

    public Player[] getActivePlayers(Player player) throws InternalServerException {
        QueryBuilder<Player, Integer> statementBuilder = playersDao.queryBuilder();
        try {
            statementBuilder.where().not().eq(Player.PUB_ID_FIELD_NAME, player.getPublicId());
            statementBuilder.orderBy(Player.LASTONLINE_FIELD_NAME, false);
            statementBuilder.limit(50L);
            List<Player> players = playersDao.query(statementBuilder.prepare());
            return players.toArray(new Player[players.size()]);
        } catch (SQLException e) {
            throw new InternalServerException("SQL error in player fetch!", e);
        }
    }

    public Match getMatch(int id) throws InternalServerException {
        QueryBuilder<Match, Integer> statementBuilder = matchesDao.queryBuilder();
        try {
            statementBuilder.where().eq("id", id);
            List<Match> matches = matchesDao.query(statementBuilder.prepare());
            if(matches.size() > 0) {
                return matches.get(0);
            }
        } catch (SQLException e) {
            throw new InternalServerException("SQL error in player fetch!", e);
        }
        return null;
    }

    public Round getRoundById(int roundId) throws InternalServerException {
        QueryBuilder<Round, Integer> statementBuilder = roundsDao.queryBuilder();
        try {
            statementBuilder.where().eq("id", roundId);
            List<Round> rounds = roundsDao.query(statementBuilder.prepare());
            if(rounds.size() > 0) {
                return rounds.get(0);
            }
        } catch (SQLException e) {
            throw new InternalServerException("SQL error in round fetch!", e);
        }
        return null;
    }

    public void clearDrawDataForRound(int rid) throws InternalServerException {
        DeleteBuilder<DataElement, Integer> delBuilder = dataElementDao.deleteBuilder();
        try {
            delBuilder.where().eq(DataElement.ROUND_ID_FIELD, rid);
            int deleted = delBuilder.delete();
            logger.info("Cleared round " + rid + " by deleting " + deleted + " rows!");
        } catch (SQLException e) {
            throw new InternalServerException("SQL error in round fetch!", e);
        }
    }
}
