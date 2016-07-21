package main.java.db;

import main.java.model.InternalServerException;
import main.java.model.Player;
import main.java.utils.Misc;

import javax.websocket.Session;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Random;
import java.security.MessageDigest;

/**
 * Created by DFX on 18.05.2016.
 */
public class UserDataStore {
    public static Random rnd = new Random();

    private static HashMap<Integer, Session> playerSessions = new HashMap<>();

    public static Session[] getSessions() {
        return playerSessions.values().toArray(new Session[playerSessions.values().size()]);
    }


    public static int getNextUserID() throws InternalServerException {
        boolean found = false;
        int userid = -1;
        while(!found) {
            userid = 100000 + rnd.nextInt(999999);
            try {
                found = DrawDB.getHandler().isUserIDAvailable(userid);
            } catch (InternalServerException e) {
                throw new InternalServerException("Next userid could not be generated!", e);
            }
        }
        return userid;
    }

    public static String registerUser(String username, int userid) throws InternalServerException {
        // Generate Secret
        String usersecret;
        String userhash;
        try {
            usersecret = sha256(System.currentTimeMillis() + "");
            userhash = sha256(usersecret);
        } catch (InternalServerException e) {
            throw new InternalServerException("Error while hashing!", e);
        }

        try {
            DrawDB.getHandler().addUser(username, userid, userhash);
        } catch (InternalServerException e) {
            throw new InternalServerException("User could not be added!", e);
        }

        return usersecret;
    }

    private static String sha256(String in) throws InternalServerException {
        String output;
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            messageDigest.update( in.getBytes());
            output = Misc.bytesToHex(messageDigest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new InternalServerException("SHA-256 module not found!");
        }
        return output;
    }

    public static void setPlayerOnline(int pid, Session s) {
        if(playerSessions.containsValue(s)) {
            playerSessions.values().removeIf(s::equals);
        }
        playerSessions.put(pid, s);
    }

    public static String checkLogin(int userid, String usersecret) throws InternalServerException {
        String hash;
        try {
            hash = sha256(usersecret);
        } catch (InternalServerException e) {
            throw new InternalServerException("Error while hashing!", e);
        }

        return DrawDB.getHandler().checkLogin(userid, hash);
    }

    public static Player getPlayer(int publicId) throws InternalServerException {
        // get player from DB
        if(DrawDB.getHandler().doesPlayerExist(publicId)) {
            return DrawDB.getHandler().getPlayerByPublicId(publicId);
        }
        return null;
    }

    public static Player getPlayerById(int internalId) throws InternalServerException {
        try {
            return DrawDB.getHandler().getPlayerById(internalId);
        } catch (InternalServerException e) {
            throw new InternalServerException("Error getting player", e);
        }
    }

    public static Player getPlayerBySession(Session session) {
        if(playerSessions.containsValue(session)) {
            for(Integer pid : playerSessions.keySet()) {
                if(playerSessions.get(pid).equals(session)) {
                    try {
                        return getPlayerById(pid);
                    } catch (InternalServerException e) {
                        e.printStackTrace();
                        return null;
                    }
                }
            }
        }
        return null;
    }

    public static Session getSessionForPlayerId(int pid) {
        return playerSessions.get(pid);
    }

    public static Player getRandomOpponent(Player player) {
        try {
            Player[] players = DrawDB.getHandler().getActivePlayers(player);
            int size = players.length;
            if(size > 0) {
                return players[rnd.nextInt(size)];
            }
        } catch (InternalServerException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void setPlayerOffline(int pid) {
        playerSessions.remove(pid);
    }

    public static boolean isOnline(int pid) {
        return (playerSessions.containsKey(pid));
    }
}
