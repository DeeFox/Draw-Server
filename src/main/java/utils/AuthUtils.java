package main.java.utils;

import com.google.gson.JsonObject;
import main.java.db.DrawDB;
import main.java.db.UserDataStore;
import main.java.model.AuthenticationFailedException;
import main.java.model.InternalServerException;
import main.java.model.Match;
import main.java.model.Player;

import javax.websocket.Session;

/**
 * Created by DFX on 18.05.2016.
 */
public class AuthUtils {

    public static void register(String username, Session session) {
        int userid;

        try {
            userid = UserDataStore.getNextUserID();
        } catch (InternalServerException e) {
            AnswerUtils.sendInternalServerException(session, e);
            return;
        }

        username = username.replaceAll("[^A-Za-z0-9]", "");

        String usersecret = null;
        try {
            usersecret = UserDataStore.registerUser(username, userid);
        } catch (InternalServerException e) {
            AnswerUtils.sendInternalServerException(session, e);
            return;
        }

        JsonObject answer = new JsonObject();
        answer.addProperty("action", "registerdata");
        answer.addProperty("username", username);
        answer.addProperty("userid", userid);
        answer.addProperty("usersecret", usersecret);
        AnswerUtils.sendMessageToSession(session, answer.toString());
    }

    public static void authenticate(Session session) throws AuthenticationFailedException, InternalServerException {
        Player p = UserDataStore.getPlayerBySession(session);

        if(p == null) {
            throw new AuthenticationFailedException();
        }
    }

    public static void login(Session session, String userid, String usersecret) throws InternalServerException {
        int uid;
        try {
            uid = Integer.valueOf(userid);
        } catch(NumberFormatException e) {
            AnswerUtils.sendError(session, "Malformed input: userid");
            return;
        }

        String username;
        try {
            username = UserDataStore.checkLogin(uid, usersecret);
        } catch (InternalServerException e) {
            throw e;
        }

        if(username == null) {
            AnswerUtils.sendError(session, "Invalid authentication credentials!");
            return;
        }

        // Set User online
        Player p = UserDataStore.getPlayer(uid);
        UserDataStore.setPlayerOnline(p.getId(), session);

        JsonObject answer = new JsonObject();
        answer.addProperty("action", "authenticated");
        answer.addProperty("username", username);
        AnswerUtils.sendMessageToSession(session, answer.toString());
    }

    public static Pair<Match, Player> authenticatePlayerTurn(String gameid, Session session) {
        int gid;
        try {
            gid = Integer.parseInt(gameid);
        } catch (NumberFormatException e) {
            AnswerUtils.sendError(session, "Malformed Game-ID!");
            return null;
        }

        Player thisPlayer = UserDataStore.getPlayerBySession(session);

        Match m;
        try {
            m = DrawDB.getHandler().getMatch(gid);
        } catch (InternalServerException e) {
            AnswerUtils.sendInternalServerException(session, e);
            return null;
        }
        if(m == null) {
            AnswerUtils.sendError(session, "Game not found!");
            return null;
        }

        if(!(m.getPlayer1().equals(thisPlayer)) && !(m.getPlayer2().equals(thisPlayer))) {
            AnswerUtils.sendError(session, "Not your game!");
            return null;
        }

        if(!m.isPlayerTurn(thisPlayer)) {
            AnswerUtils.sendError(session, "It's not your turn right now!");
            return null;
        }
        return new Pair<>(m, thisPlayer);
    }
}
