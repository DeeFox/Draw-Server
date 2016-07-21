package main.java.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import main.java.db.DrawDB;
import main.java.db.UserDataStore;
import main.java.model.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.websocket.Session;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Random;

/**
 * Created by DFX on 12.06.2016.
 */
public class GameUtils {
    public static Random rnd = new Random(System.currentTimeMillis());
    static final Logger logger = LogManager.getLogger(GameUtils.class);

    public static int[] WORD_POINTS = new int[] {100, 200, 400};

    public static void newGame(String mode, JsonObject data, Session session) {
        if(!mode.equals("random") && !mode.equals("id")) {
            AnswerUtils.sendError(session, "Invalid mode to begin new game.");
            return;
        }

        Player thisPlayer = UserDataStore.getPlayerBySession(session);

        Player opponent = null;
        if(mode.equals("random")) {
            opponent = UserDataStore.getRandomOpponent(thisPlayer);
        }

        if(mode.equals("id")) {
            HashMap<String, String> parsed = null;
            try {
                parsed = JSONHelpers.parseRequiredFields(session, data, new String[]{"opponentid"});
            } catch (ParserException e) {
                AnswerUtils.sendError(session, e.getMessage());
                e.printStackTrace();
                return;
            }

            int opid;
            try {
                opid = Integer.parseInt(parsed.get("opponentid"));
            } catch(NumberFormatException e) {
                AnswerUtils.sendError(session, "Malformed Opponent-ID.");
                return;
            }

            try {
                opponent = UserDataStore.getPlayer(opid);
            } catch (InternalServerException e) {
                AnswerUtils.sendInternalServerException(session, e);
            }
        }

        // Check if we found a suitable opponent
        if(opponent == null) {
            AnswerUtils.sendError(session, "Player not found.");
            return;
        }

        // Check if the player wants to play with himself
        if(opponent.equals(thisPlayer)) {
            AnswerUtils.sendError(session, "You can not play against yourself. Sorry!");
            return;
        }

        // Who starts?
        boolean p1Begins = rnd.nextBoolean();

        try {
            startMatch(thisPlayer, opponent, p1Begins);
        } catch (InternalServerException e) {
            AnswerUtils.sendInternalServerException(session, e);
        }

        // Send updated games list to both players
        GameUtils.sendGamesList(thisPlayer);
        GameUtils.sendGamesList(opponent);
    }

    public static void sendGamesList(Player p) {
        if(UserDataStore.isOnline(p.getId())) {
            Match[] playerMatches;
            try {
                playerMatches = DrawDB.getHandler().getAllMatchesForPlayer(p.getId());
            } catch (InternalServerException e) {
                e.printStackTrace();
                return;
            }

            JsonObject answer = new JsonObject();
            answer.addProperty("action", "gameslist");
            answer.addProperty("score", p.getScore());

            JsonArray games = new JsonArray();
            for(Match m : playerMatches) {
                JsonObject game = new JsonObject();
                game.addProperty("gameid", m.getId());
                Player opponent = m.getOpponentOf(p);
                game.addProperty("opponent", opponent.getName());
                game.addProperty("opponentid", opponent.getPublicId());
                game.addProperty("opponentscore", opponent.getScore());
                game.addProperty("gamestate", m.getStateStr(p));
                game.addProperty("streak", m.getStreak());
                game.addProperty("gamepoints", m.getTotalPoints());
                game.addProperty("lastactiontime", (m.getLastTimeAction().getTime() / 1000));
                games.add(game);
            }

            answer.add("games", games);
            
            AnswerUtils.sendMessageToPlayer(p, answer.toString());
        } else {
            logger.debug("Player " + p.getPublicId() + " not online.");
        }
    }

    public static void makeTurn(String gameid, Session session) {
        Pair<Match, Player> res = AuthUtils.authenticatePlayerTurn(gameid, session);
        if(res == null) {
            return;
        }
        Player thisPlayer = res.getSecond();
        Match m = res.getFirst();

        if(m.isPlayerDrawTurn(thisPlayer)) {
            // Drawing turn
            Round r = m.getRound();
            if(r.hasChoosenWord()) {
                GameUtils.readyToDraw(session, r, m);
            } else {
                GameUtils.sendWordList(session, r, m);
            }
        } else {
            GameUtils.readyToStreamChunks(session, m);
            // Guessing turn
            // IF ALREADY PAINTED
            //GameUtils.sendGuessWord(r, session);
        }
    }

    private static void readyToDraw(Session session, Round r, Match m) {
        try {
            r.clearDrawData();
        } catch(InternalServerException e) {
            AnswerUtils.sendInternalServerException(session, e);
            return;
        }

        JsonObject answer = new JsonObject();
        answer.addProperty("action", "readyfordrawing");
        answer.addProperty("gameid", m.getId());
        answer.addProperty("word", r.getChosenWord());

        AnswerUtils.sendMessageToSession(session, answer.toString());
    }

    private static void readyToStreamChunks(Session session, Match m) {
        JsonObject answer = new JsonObject();
        answer.addProperty("action", "readyfordrawdatarequest");
        answer.addProperty("gameid", m.getId());
        answer.addProperty("chunks", m.getRound().getChunkCount());

        AnswerUtils.sendMessageToSession(session, answer.toString());
    }

    private static void sendWordList(Session session, Round r, Match m) {
        String[] words = r.getWords();
        JsonObject answer = new JsonObject();
        answer.addProperty("action", "chooseword");
        answer.addProperty("gameid", m.getId());

        JsonObject[] wordOs = new JsonObject[3];
        for(int i = 0; i < 3; i++) {
            JsonObject w = new JsonObject();
            w.addProperty("word", words[i]);
            w.addProperty("points", WORD_POINTS[i]);
            wordOs[i] = w;
        }

        JsonObject wordList = new JsonObject();
        wordList.add("easy", wordOs[0]);
        wordList.add("medium", wordOs[1]);
        wordList.add("hard", wordOs[2]);

        answer.add("words", wordList);

        AnswerUtils.sendMessageToSession(session, answer.toString());
    }

    public static void chooseWord(String gameid, String word, Session session) {
        Pair<Match, Player> res = AuthUtils.authenticatePlayerTurn(gameid, session);
        if(res == null) {
            return;
        }
        Player thisPlayer = res.getSecond();
        Match m = res.getFirst();

        if(!m.isPlayerDrawTurn(thisPlayer)) {
            AnswerUtils.sendError(session, "It's not your draw turn right now!");
            return;
        }

        if(!(word.equals("easy") || word.equals("medium") || word.equals("hard"))) {
            AnswerUtils.sendError(session, "Malformed word identifier! Possible identifiers are: easy, medium, hard");
            return;
        }

        Round r = m.getRound();
        if(r == null) {
            AnswerUtils.sendInternalServerException(session, new InternalServerException("Game has no round associated!"));
            return;
        }

        // Set and update DB
        r.setChoosenWord(word);
        try {
            r.update();
        } catch (InternalServerException e) {
            AnswerUtils.sendInternalServerException(session, e);
            return;
        }

        try {
            m.setLastTimeAction();
            m.update();
        } catch (InternalServerException e) {
            AnswerUtils.sendInternalServerException(session, e);
            return;
        }

        // Continue by sending ready to draw
        GameUtils.readyToDraw(session, r, m);
    }


    private static void sendGuessWord(Round r, Session session) {
        JsonObject answer = new JsonObject();
        answer.addProperty("action", "guessword");
        answer.addProperty("chars", r.getAlphabet());
        answer.addProperty("blanks", r.getGapWord());
        answer.addProperty("points", r.getCalculatedPoints());

        AnswerUtils.sendMessageToSession(session, answer.toString());
    }

    public static void stopDrawData(String gameid, Session session) {
        Pair<Match, Player> res = AuthUtils.authenticatePlayerTurn(gameid, session);
        if(res == null) {
            return;
        }
        Match m = res.getFirst();

        GameUtils.sendGuessWord(m.getRound(), session);
    }

    public static void drawingData(String gameid, JsonObject data, Session session) {
        Pair<Match, Player> res = AuthUtils.authenticatePlayerTurn(gameid, session);
        if(res == null) {
            return;
        }
        Player thisPlayer = res.getSecond();
        Match m = res.getFirst();

        if(!m.isPlayerDrawTurn(thisPlayer)) {
            AnswerUtils.sendError(session, "It's not your draw turn right now!");
            return;
        }

        Round r = m.getRound();
        if(!r.hasChoosenWord()) {
            AnswerUtils.sendError(session, "You have to choose a word first!");
            return;
        }

        Pair<Integer, Integer> validateResults;
        try {
            validateResults = DrawData.validateDrawData(data, r.getId(), r);
        } catch (DrawDataValidationException e) {
            AnswerUtils.sendError(session, "Parsing Error: " + e.getMessage());
            return;
        } catch (InternalServerException e) {
            AnswerUtils.sendInternalServerException(session, e);
            return;
        }

        // Set chunkCount
        int chunkCount = validateResults.getFirst();
        int dataPerChunk = validateResults.getSecond();
        r.setChunkCount(chunkCount);
        r.setDataPerChunk(dataPerChunk);
        try {
            DrawDB.getHandler().roundsDao.update(r);
        } catch (SQLException e) {
            AnswerUtils.sendInternalServerException(session, e);
            return;
        }

        // Set gamestate
        try {
            m.playerFinishedDrawing(thisPlayer);
        } catch (InternalServerException e) {
            AnswerUtils.sendInternalServerException(session, e);
        }

        sendGamesListToBothPlayers(m);
    }

    private static void sendGamesListToBothPlayers(Match m) {
        sendGamesList(m.getPlayer1());
        sendGamesList(m.getPlayer2());
    }

    public static void getDrawData(String gameid, String chunk, Session session) {
        Pair<Match, Player> res = AuthUtils.authenticatePlayerTurn(gameid, session);
        if(res == null) {
            return;
        }
        Player thisPlayer = res.getSecond();
        Match m = res.getFirst();

        if(!m.isPlayerGuessTurn(thisPlayer)) {
            AnswerUtils.sendError(session, "It's not your guessing turn right now!");
            return;
        }

        Round r = m.getRound();

        int chunkId;

        try {
            chunkId = Integer.parseInt(chunk);
        } catch (NumberFormatException e) {
            AnswerUtils.sendError(session, "Invalid chunk id!");
            return;
        }

        if(chunkId < 0 || chunkId >= r.getChunkCount()) {
            AnswerUtils.sendError(session, "Chunk id needs to be within 0 and (chunks-1)!");
            return;
        }

        boolean update = r.chunkRetrieved(chunkId);
        if(update) {
            try {
                r.update();
            } catch (InternalServerException e) {
                AnswerUtils.sendInternalServerException(session, e);
            }
        }

        DrawData.sendChunk(m, r, chunkId, session);
    }

    public static void guess(String gameid, String word, Session session) {
        Pair<Match, Player> res = AuthUtils.authenticatePlayerTurn(gameid, session);
        if(res == null) {
            return;
        }
        Player thisPlayer = res.getSecond();
        Match m = res.getFirst();

        if(!m.isPlayerGuessTurn(thisPlayer)) {
            AnswerUtils.sendError(session, "It's not your guessing turn right now!");
            return;
        }

        Round r = m.getRound();

        word = word.trim().toUpperCase();
        int calculatedScore = r.getCalculatedPoints();
        if(r.getChosenWord().equals(word)) {
            try {
                GameUtils.sendGuessResult(true, calculatedScore, session);

                GameUtils.addScores(m, calculatedScore);

                m.roundFinished(calculatedScore);
            } catch (InternalServerException e) {
                AnswerUtils.sendInternalServerException(session, e);
            }

            GameUtils.sendGamesListToBothPlayers(m);
        } else {
            GameUtils.sendGuessResult(false, calculatedScore, session);
        }
    }

    private static void addScores(Match m, int calculatedPoints) throws InternalServerException {
        m.getPlayer1().addScore(calculatedPoints);
        m.getPlayer2().addScore(calculatedPoints);
    }

    private static void sendGuessResult(boolean b, int calculatedPoints, Session session) {
        JsonObject answer = new JsonObject();
        answer.addProperty("action", "guessresult");
        answer.addProperty("result", (b) ? "correct" : "wrong");
        answer.addProperty("points", calculatedPoints);

        AnswerUtils.sendMessageToSession(session, answer.toString());
    }

    public static void startMatch(Player p1, Player p2, boolean p1Begins) throws InternalServerException {
        Match m = new Match(p1, p2, p1Begins);
        Player pDrawing = (p1Begins) ? p1 : p2;
        Round r = Round.createBlankRound(pDrawing);

        try {
            DrawDB.getHandler().roundsDao.create(r);
        } catch (SQLException e) {
            throw new InternalServerException("Error while creating Round!", e);
        }

        m.setRoundId(r.getId());

        try {
            DrawDB.getHandler().matchesDao.create(m);
        } catch (SQLException e) {
            throw new InternalServerException("Error while creating Match!", e);
        }
    }
}
