package main.java.utils;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.util.Pair;
import main.java.model.ParserException;

import javax.websocket.Session;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Created by DFX on 14.05.2016.
 */
public class JSONHelpers {
    private static Gson gson = new Gson();

    public static Pair<JsonObject,String> parseMessage(String message, Session sess) {
        JsonObject json;
        String mode;
        try {
            json = gson.fromJson(message, JsonObject.class);
            JsonElement jElemMode = json.get("action");
            mode = jElemMode.getAsString();
        } catch(Exception e) {
            AnswerUtils.sendError(sess, "Malformed JSON input!");
            return null;
        }
        return new Pair<>(json, mode);
    }

    public static HashMap<String, String> parseRequiredFields(Session sess, JsonObject json, String[] fieldKeys) throws ParserException {
        HashMap<String, String> fields = new HashMap<>();

        for(String k : fieldKeys) {
            String field;
            try {
                JsonElement jelem = json.get(k);
                field = jelem.getAsString();
                if(field.length() == 0) {
                    throw new ParserException("Crucial fields for this method missing! Needed: " + Arrays.toString(fieldKeys));
                }
                fields.put(k, field);
            } catch(Exception e) {
                throw new ParserException("Crucial fields for this method missing! Needed: " + Arrays.toString(fieldKeys), e);
            }
        }
        return fields;
    }
}
