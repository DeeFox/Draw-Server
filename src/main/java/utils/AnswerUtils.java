package main.java.utils;

import com.google.gson.JsonObject;
import main.java.db.UserDataStore;
import main.java.model.Player;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.websocket.RemoteEndpoint.Async;
import javax.websocket.Session;
import java.io.IOException;

/**
 * Created by DFX on 14.05.2016.
 */
public class AnswerUtils {
    private static final Logger logger = LogManager.getLogger(AnswerUtils.class);

    public static void sendError(Session sess, String err) {
        JsonObject answer = new JsonObject();
        answer.addProperty("action", "error");
        answer.addProperty("errmsg", err);

        sendMessageToSession(sess, answer.toString(), true);
    }

    private static void sendInternalServerException(Session sess) {
        JsonObject answer = new JsonObject();
        answer.addProperty("action", "error");
        answer.addProperty("errmsg", "Internal Server Exception! Pleaser contact Server Admin!");

        sendMessageToSession(sess, answer.toString(), true);
    }

    public static void sendInternalServerException(Session sess, Throwable e) {
        logger.error("Internal Server Exception!", e);
        sendInternalServerException(sess);
    }

    public static boolean sendMessageToSession(Session sess, String msg) {
        return sendMessageToSession(sess, msg, true);
    }

    public static boolean sendMessageToPlayer(Player p, String msg) {
        Session sess = UserDataStore.getSessionForPlayerId(p.getId());
        return (sess == null) ? false : sendMessageToSession(sess, msg);
    }

    private static boolean sendMessageToSession(Session sess, String msg, boolean log) {
        // ignore message if target session is null
        if(sess == null)
            return true;

        try {
            if(log)
                logger.info("[OUT] (" + sess.getId() + ") " + msg);

            Async as = sess.getAsyncRemote();
            as.sendText(msg);
            return true;
        } catch(Exception e) {
            logger.warn("!! Error sending message to " + sess.getId(), e);

            // Close the session
            try {
                sess.close();
            } catch (IOException e1) {
                // DO NUTHIN'
            }
            return false;
        }
    }

    public static void sendPong(Session session) {
        JsonObject answer = new JsonObject();
        answer.addProperty("action", "pong");

        sendMessageToSession(session, answer.toString(), true);
    }

    public static void sendDebugMessageToAll(String msg) {
        Session[] sesss = UserDataStore.getSessions();
        JsonObject jmsg = new JsonObject();
        jmsg.addProperty("action", "info");
        jmsg.addProperty("message", msg);
        for(Session s : sesss) {
            AnswerUtils.sendMessageToSession(s, jmsg.toString());
        }
    }
}
