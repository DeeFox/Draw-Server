package main.java;

import com.google.gson.JsonObject;
import javafx.util.Pair;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;

import main.java.db.UserDataStore;
import main.java.model.AuthenticationFailedException;
import main.java.model.InternalServerException;
import main.java.model.ParserException;
import main.java.model.Player;
import main.java.utils.AnswerUtils;
import main.java.utils.AuthUtils;
import main.java.utils.GameUtils;
import main.java.utils.JSONHelpers;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.SocketTimeoutException;
import java.util.HashMap;

/**
 * Created by DFX on 14.05.2016.
 */

@ServerEndpoint(value = "/")
public class DrawServerEndpoint {
    private static final Logger logger = LogManager.getLogger(DrawServerEndpoint.class);

    @OnClose
    public void onClose(Session sess, CloseReason reason) {
        Player p = UserDataStore.getPlayerBySession(sess);

        if(p != null) {
            UserDataStore.setPlayerOffline(p.getId());
            logger.info("Client '" + p.getName() + "' disconnected (" + reason.getReasonPhrase() + ")" + sess.getId());
        } else {
            logger.info("Client disconnected (" + reason.getReasonPhrase() + ")" + sess.getId());
        }
    }

    @OnOpen
    public void onConnect(Session session) {
        logger.info("New connection from: " + session.getId());
    }

    @OnError
    public void onError(Session sess, Throwable cause) {
        if(cause instanceof SocketTimeoutException) {
            AnswerUtils.sendError(sess, "You were timed out by the server. Avoid this by sending ping-packets regularly.");
        } else {
            AnswerUtils.sendInternalServerException(sess, cause);
        }
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        logger.info("[INP] (" + session.getId() + ") " + message);

        Pair<JsonObject,String> data = JSONHelpers.parseMessage(message, session);

        if(data == null)
            return;

        handleMessage(data, session);
    }

    private void handleMessage(Pair<JsonObject,String> data, Session session) {
        HashMap<String, String> parsed;
        try {
            switch (data.getValue()) {
                case "ping":
                    AnswerUtils.sendPong(session);
                    break;
                case "register":
                    parsed = JSONHelpers.parseRequiredFields(session, data.getKey(), new String[]{"name"});
                    AuthUtils.register(parsed.get("name"), session);
                    break;
                case "auth":
                    parsed = JSONHelpers.parseRequiredFields(session, data.getKey(), new String[]{"userid", "usersecret"});
                    AuthUtils.login(session, parsed.get("userid"), parsed.get("usersecret"));
                    break;
                case "games":
                    AuthUtils.authenticate(session);
                    Player p = UserDataStore.getPlayerBySession(session);
                    GameUtils.sendGamesList(p);
                    break;
                case "startnew":
                    AuthUtils.authenticate(session);

                    // Create new Game
                    parsed = JSONHelpers.parseRequiredFields(session, data.getKey(), new String[]{"mode"});
                    GameUtils.newGame(parsed.get("mode"), data.getKey(), session);
                    break;
                case "doturn":
                    AuthUtils.authenticate(session);

                    parsed = JSONHelpers.parseRequiredFields(session, data.getKey(), new String[]{"gameid"});
                    GameUtils.makeTurn(parsed.get("gameid"), session);
                    break;

                case "wordchosen":
                    AuthUtils.authenticate(session);

                    parsed = JSONHelpers.parseRequiredFields(session, data.getKey(), new String[]{"gameid", "word"});
                    GameUtils.chooseWord(parsed.get("gameid"), parsed.get("word"), session);
                    break;

                case "drawingdata":
                    AuthUtils.authenticate(session);

                    parsed = JSONHelpers.parseRequiredFields(session, data.getKey(), new String[]{"gameid"});
                    GameUtils.drawingData(parsed.get("gameid"), data.getKey(), session);
                    break;

                case "getdrawdata":
                    AuthUtils.authenticate(session);

                    parsed = JSONHelpers.parseRequiredFields(session, data.getKey(), new String[]{"gameid", "chunk"});
                    GameUtils.getDrawData(parsed.get("gameid"), parsed.get("chunk"), session);
                    break;

                case "stopdrawdata":
                    AuthUtils.authenticate(session);

                    parsed = JSONHelpers.parseRequiredFields(session, data.getKey(), new String[]{"gameid"});
                    GameUtils.stopDrawData(parsed.get("gameid"), session);
                    break;

                case "guess":
                    AuthUtils.authenticate(session);

                    parsed = JSONHelpers.parseRequiredFields(session, data.getKey(), new String[]{"gameid", "word"});
                    GameUtils.guess(parsed.get("gameid"), parsed.get("word"), session);
                    break;

                // Debug stuff
                case "msgtoall":
                    parsed = JSONHelpers.parseRequiredFields(session, data.getKey(), new String[]{"msg", "secret"});
                    if(parsed.get("secret").equals("15121991")) {
                        AnswerUtils.sendDebugMessageToAll(parsed.get("msg"));
                        break;
                    }
                default:
                    AnswerUtils.sendError(session, "No such action available.");
                    break;
            }
        } catch(AuthenticationFailedException e) {
            AnswerUtils.sendError(session, "Not authenticated to do this.");
        } catch(InternalServerException e) {
            AnswerUtils.sendInternalServerException(session, e);
        } catch(ParserException e) {
            AnswerUtils.sendError(session, e.getMessage());
            e.printStackTrace();
        }
    }
}
