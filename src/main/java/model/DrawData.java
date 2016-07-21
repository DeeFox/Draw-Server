package main.java.model;

import com.google.gson.*;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.QueryBuilder;
import main.java.db.DrawDB;
import main.java.utils.AnswerUtils;
import main.java.utils.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.websocket.Session;

import java.awt.geom.Point2D;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by DFX on 18.06.2016.
 */
public class DrawData {
    private static final String HEX_PATTERN = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$";
    private static Pattern pattern = Pattern.compile(HEX_PATTERN);
    static final Logger logger = LogManager.getLogger(DrawData.class);

    private static String getValidHexColor(JsonElement e, String defaultHex) {
        if(e.isJsonPrimitive()) {
            JsonPrimitive ep = e.getAsJsonPrimitive();
            if(ep.isString()) {
                String hex = ep.getAsString();
                Matcher res = pattern.matcher(hex);
                boolean valid = res.matches();
                if(valid) {
                    return hex;
                }
            }
        }
        return defaultHex;
    }

    private static int validateN(JsonObject jo) throws DrawDataValidationException {
        if(!jo.has("n")) {
            throw new DrawDataValidationException("Every data element needs a valid value in the 'n' field!");
        }
        JsonElement je = jo.get("n");
        if(!je.isJsonPrimitive()) {
            throw new DrawDataValidationException("The 'n' field has to be a valid int!");
        }
        JsonPrimitive jp = je.getAsJsonPrimitive();
        if(!jp.isNumber()) {
            throw new DrawDataValidationException("The 'n' field has to be a valid int!");
        }
        try {
            return jp.getAsInt();
        } catch(NumberFormatException e) {
            throw new DrawDataValidationException("The 'n' field has to be a valid int!");
        }
    }

    public static Pair<Integer, Integer> validateDrawData(JsonObject data, int rid, Round r) throws DrawDataValidationException, InternalServerException {
        // Validate BGCOLOR
        String bgColor;
        if(!data.has("bgcolor")) {
            throw new DrawDataValidationException("No background color set!");
        }
        JsonElement hexE = data.get("bgcolor");
        bgColor = getValidHexColor(hexE, "#FFFFFF");

        // Data structure validation
        if(!data.has("data")) {
            throw new DrawDataValidationException("No data elements sent!");
        }
        JsonElement dataE = data.get("data");
        if(!dataE.isJsonArray()) {
            throw new DrawDataValidationException("Invalid 'data'-attribute. Needs to be an array!");
        }
        JsonArray dataArray = dataE.getAsJsonArray();
        int dataNum = dataArray.size();
        if(dataNum < 1) {
            throw new DrawDataValidationException("Data elements array is empty!");
        }

        ArrayList<DataElement> allElements = new ArrayList<>();

        for(int i = 0; i < dataNum; i++) {
            JsonElement je = dataArray.get(i);
            if(!je.isJsonObject()) {
                throw new DrawDataValidationException("Data elements have to be JSON objects!");
            }

            JsonObject jo = je.getAsJsonObject();

            validateElement(jo,allElements, rid);
        }

        // Save the stuff
        Dao<DataElement, Integer> dEDao = DrawDB.getHandler().dataElementDao;
        for(DataElement datE : allElements) {
            try {
                dEDao.create(datE);
            } catch (SQLException e) {
                throw new InternalServerException("Error while saving Draw Data to DB!", e);
            }
        }
        logger.info("Just wrote " + allElements.size() + " rows for round " + rid + " to DB!");

        int chunkCount;
        int dataCount = allElements.size();
        chunkCount = dataCount;
        if(dataCount > 20) {
            chunkCount = 20;
        }
        if(dataCount > 1000) {
            int tsd = (int) (dataCount / 1000.0f);
            chunkCount = 20 * tsd;
        }

        int elemsPerChunk = (int) Math.ceil(dataCount / (chunkCount * 1.0f));

        chunkCount = (int) Math.ceil(dataCount / (elemsPerChunk * 1.0f));

        r.setBgColor(bgColor);
        r.update();

        return new Pair(chunkCount, elemsPerChunk);
    }

    private static void validateElement(JsonObject jo, ArrayList<DataElement> result, int rid) throws DrawDataValidationException {
        int n = validateN(jo);

        if(!jo.has("type")) {
            throw new DrawDataValidationException("Data elements have to have valid field 'type' (#" + n + ")!)");
        }
        JsonElement je = jo.get("type");
        if(!je.isJsonPrimitive()) {
            throw new DrawDataValidationException("Data elements have to have valid field 'type' (#" + n + ")!)");
        }
        JsonPrimitive jp = je.getAsJsonPrimitive();
        if(!jp.isString()) {
            throw new DrawDataValidationException("Data elements have to have valid field 'type' (#" + n + ")!)");
        }
        String type = jp.getAsString();
        DataElement.ElementType et = DataElement.ElementType.getByName(type);
        if(et == null) {
            throw new DrawDataValidationException("Invalid type given (#" + n + ")!");
        }

        switch(et) {
            case RLINE:
                validateRawLine(jo, n, result, rid);
                break;
            case BLINE:
                validateBezierLine(jo, n, result, rid);
                break;
            case EMOJI:
                validateEmoji(jo, n, result, rid);
                break;
            case SHAPE:
                validateShape(jo, n, result, rid);
                break;
        }
    }

    private static void validateShape(JsonObject jo, int n, ArrayList<DataElement> result, int rid) throws DrawDataValidationException {
        String color = validateCol(jo, n);

        if(!jo.has("shape")) {
            throw new DrawDataValidationException("No shape type set (#" + n + ")!");
        }
        JsonElement je = jo.get("shape");
        if(!je.isJsonPrimitive()) {
            throw new DrawDataValidationException("Shape elements have to have valid field 'shape' (#" + n + ")!)");
        }
        JsonPrimitive jp = je.getAsJsonPrimitive();
        if(!jp.isString()) {
            throw new DrawDataValidationException("Shape elements have to have valid field 'shape' (#" + n + ")!)");
        }
        String type = jp.getAsString();
        DataElement.ShapeType stype = DataElement.ShapeType.getByName(type);
        if(stype == null) {
            throw new DrawDataValidationException("Invalid shape type given (#" + n + ")!");
        }

        if(!jo.has("pt")) {
            throw new DrawDataValidationException("No position for shape set (#" + n + ")!");
        }
        Point2D.Float point = validatePt(jo.get("pt"), n);

        if(stype.equals(DataElement.ShapeType.CIRCLE)) {
            if(!jo.has("r")) {
                throw new DrawDataValidationException("No radius for circle shape set (#" + n + ")!");
            }
            float radius = validateFloat(jo.get("r"), "r", n);

            DataElement de = new DataElement(rid, DataElement.ElementType.SHAPE, n, point, radius, DataElement.ShapeType.CIRCLE, color);
            result.add(de);
            return;
        }

        if(stype.equals(DataElement.ShapeType.RECT) || stype.equals(DataElement.ShapeType.TRI)) {
            if(!jo.has("dim")) {
                throw new DrawDataValidationException("No dimension for rect/tri shape set (#" + n + ")!");
            }
            Point2D.Float dim = validatePt(jo.get("dim"), "dim", n);

            if(!jo.has("rot")) {
                throw new DrawDataValidationException("No rotation for rect/tri shape set (#" + n + ")!");
            }
            float rotation = validateFloat(jo.get("rot"), "rot", n);

            DataElement de = new DataElement(rid, DataElement.ElementType.SHAPE, n, point, dim, stype, rotation, color);
            result.add(de);
            return;
        }
    }

    private static void validateEmoji(JsonObject jo, int n, ArrayList<DataElement> result, int rid) throws DrawDataValidationException {
        if(!jo.has("c1") || !jo.has("c2")) {
            throw new DrawDataValidationException("No emoji codepoints set (#" + n + ")!");
        }
        JsonElement c1e = jo.get("c1");
        JsonElement c2e = jo.get("c2");
        if(!c1e.isJsonPrimitive() || !c2e.isJsonPrimitive()) {
            throw new DrawDataValidationException("Invalid emoji codepoints set (#" + n + ")!");
        }
        JsonPrimitive c1p = c1e.getAsJsonPrimitive();
        JsonPrimitive c2p = c2e.getAsJsonPrimitive();
        if(!c1p.isNumber() || !c2p.isNumber()) {
            throw new DrawDataValidationException("Invalid emoji codepoints set (#" + n + ")!");
        }
        int c1;
        int c2;
        try {
            c1 = c1p.getAsInt();
            c2 = c2p.getAsInt();
        } catch(NumberFormatException e) {
            throw new DrawDataValidationException("Invalid emoji codepoints set (#" + n + ")!");
        }

        if(!jo.has("rot")) {
            throw new DrawDataValidationException("No rotation for emoji set (#" + n + ")!");
        }
        float rotation = validateFloat(jo.get("rot"), "rot", n);

        if(!jo.has("size")) {
            throw new DrawDataValidationException("No size for emoji set (#" + n + ")!");
        }
        float size = validateFloat(jo.get("size"), "size", n);

        if(!jo.has("pt")) {
            throw new DrawDataValidationException("No position for emoji set (#" + n + ")!");
        }
        Point2D.Float point = validatePt(jo.get("pt"), n);

        DataElement de = new DataElement(rid, DataElement.ElementType.EMOJI, n, c1, c2, point, rotation, size);
        result.add(de);
    }

    private static void validateBezierLine(JsonObject jo, int n, ArrayList<DataElement> result, int rid) throws DrawDataValidationException {
        String color = validateCol(jo, n);
        float thickness = validateThickness(jo, n);
        if(!jo.has("pts")) {
            throw new DrawDataValidationException("No pts set (#" + n + ")!");
        }
        JsonElement je = jo.get("pts");
        if(!je.isJsonArray()) {
            throw new DrawDataValidationException("Field pts has to be an array (#" + n + ")!");
        }
        JsonArray ja = je.getAsJsonArray();
        if(ja.size() == 0 ) {
            throw new DrawDataValidationException("Field pts needs at least 1 entry (#" + n + ")!");
        }

        for(int i = 0; i < ja.size(); i++) {
            JsonElement jje = ja.get(i);
            if(!jje.isJsonArray()) {
                throw new DrawDataValidationException("Every entry in pts needs to be an array of size 8 (#" + n + ")!");
            }
            JsonArray jja = jje.getAsJsonArray();
            if(jja.size() != 8) {
                throw new DrawDataValidationException("Every entry in pts needs to be an array of size 8 (#" + n + ")!");
            }
            float[] pts = new float[8];
            for(int j = 0; j < 8; j++) {
                pts[j] = validateFloat(jja.get(j), "pts", n);
            }

            // Create Line
            DataElement de = new DataElement(rid, DataElement.ElementType.BLINE, n, i, pts, color, thickness);
            result.add(de);
        }
    }

    private static void validateRawLine(JsonObject jo, int n, ArrayList<DataElement> result, int rid) throws DrawDataValidationException {
        String color = validateCol(jo, n);
        float thickness = validateThickness(jo, n);

        if(!jo.has("pts")) {
            throw new DrawDataValidationException("No pts set (#" + n + ")!");
        }
        JsonElement je = jo.get("pts");
        if(!je.isJsonArray()) {
            throw new DrawDataValidationException("Field pts has to be an array (#" + n + ")!");
        }
        JsonArray ja = je.getAsJsonArray();
        if(ja.size() == 0 || ja.size() < 4) {
            throw new DrawDataValidationException("Field pts needs at least 4 entries (#" + n + ")!");
        }
        if(ja.size() % 2 != 0) {
            throw new DrawDataValidationException("Field pts has to have an even number of entries (#" + n + ")!");
        }

        JsonElement e1 = ja.get(0);
        JsonElement e2 = ja.get(1);
        float lastF1 = validateFloat(e1, "pts", n);
        float lastF2 = validateFloat(e2, "pts", n);
        for(int i = 1; i < (ja.size() / 2); i++) {
            e1 = ja.get(i * 2);
            e2 = ja.get(i * 2 + 1);
            float f1 = validateFloat(e1, "pts", n);
            float f2 = validateFloat(e2, "pts", n);

            // Create Line
            DataElement de = new DataElement(rid, DataElement.ElementType.RLINE, n, i, lastF1, lastF2, f1, f2, color, thickness);
            result.add(de);

            lastF1 = f1;
            lastF2 = f2;
        }
    }

    private static String validateCol(JsonObject jo, int n) throws DrawDataValidationException {
        if(!jo.has("col")) {
            throw new DrawDataValidationException("No color set (#" + n + ")!");
        }
        String color = getValidHexColor(jo.get("col"), "");
        if(color.equals("")) {
            throw new DrawDataValidationException("Invalid color (#" + n + ")!");
        }
        return color;
    }

    private static float validateThickness(JsonObject jo, int n) throws DrawDataValidationException {
        if(!jo.has("thick")) {
            throw new DrawDataValidationException("No thickness set (#" + n + ")!");
        }
        return validateFloat(jo.get("thick"), "thick", n);
    }

    private static float validateFloat(JsonElement je, String field, int n) throws DrawDataValidationException {
        if(!je.isJsonPrimitive()) {
            throw new DrawDataValidationException("Entries in field " + field + " have to be valid floats (#" + n + ")!)");
        }
        JsonPrimitive jp = je.getAsJsonPrimitive();
        if(!jp.isNumber()) {
            throw new DrawDataValidationException("Entries in field " + field + " have to be valid floats (#" + n + ")!)");
        }
        try {
            return jp.getAsFloat();
        } catch(NumberFormatException e) {
            throw new DrawDataValidationException("Entries in field " + field + " have to be valid floats (#" + n + ")!)");
        }
    }

    private static Point2D.Float validatePt(JsonElement pt, int n) throws DrawDataValidationException {
        return validatePt(pt, "pt", n);
    }

    private static Point2D.Float validatePt(JsonElement pt, String field, int n) throws DrawDataValidationException {
        if(!pt.isJsonArray()) {
            throw new DrawDataValidationException("Value in field " + field + " has to be an array with 2 entries (#" + n + ")!)");
        }
        JsonArray ja = pt.getAsJsonArray();
        if(ja.size() != 2) {
            throw new DrawDataValidationException("Value in field " + field + " has to be an array with 2 entries (#" + n + ")!)");
        }
        float x = validateFloat(ja.get(0), "pt", n);
        float y = validateFloat(ja.get(1), "pt", n);

        return new Point2D.Float(x, y);
    }

    public static void sendChunk(Match m, Round r, int chunkId, Session session) {
        int dataPerChunk = r.getDataPerChunk();

        Dao<DataElement, Integer> dataElementDao = DrawDB.getHandler().dataElementDao;
        QueryBuilder<DataElement, Integer> statementBuilder = dataElementDao.queryBuilder();

        List<DataElement> elements;
        try {
            statementBuilder.where().eq(DataElement.ROUND_ID_FIELD, r.getId());
            statementBuilder.orderBy(DataElement.N_FIELD, true);
            statementBuilder.orderBy(DataElement.LINE_ID_FIELD, true);
            statementBuilder.limit((long) dataPerChunk);
            statementBuilder.offset((long) chunkId * dataPerChunk);

            elements = dataElementDao.query(statementBuilder.prepare());
        } catch(SQLException e) {
            AnswerUtils.sendInternalServerException(session, e);
            return;
        }

        if(elements.size() == 0) {
            AnswerUtils.sendInternalServerException(session, new InternalServerException("No data found for rid " + r.getId()));
            return;
        }

        // Prepare the JSON
        JsonObject answer = new JsonObject();
        answer.addProperty("action", "chunkeddrawdata");
        answer.addProperty("gameid", m.getId());
        answer.addProperty("chunk", chunkId);
        answer.addProperty("total_chunks", r.getChunkCount());
        answer.addProperty("bgcolor", r.getBgColor());
        int lastN = elements.get(0).getN();
        DataElement.ElementType lastType = elements.get(0).getType();
        List<DataElement> lineCache = new ArrayList<>();
        JsonArray data = new JsonArray();
        for(DataElement de : elements) {
            int n = de.getN();

            switch(de.getType()) {
                case RLINE:
                case BLINE:
                    if(n == lastN && lastType.equals(de.getType())) {
                        lineCache.add(de);
                    } else {
                        if(lineCache.size() > 0) {
                            data.add(lineCacheToJson(lineCache));
                            lineCache.clear();
                            lineCache.add(de);
                        }
                    }
                    break;
                case EMOJI:
                    if(lineCache.size() > 0) {
                        data.add(lineCacheToJson(lineCache));
                        lineCache.clear();
                    }
                    data.add(emojiToJson(de));
                    break;
                case SHAPE:
                    if(lineCache.size() > 0) {
                        data.add(lineCacheToJson(lineCache));
                        lineCache.clear();
                    }
                    data.add(shapeToJson(de));
                    break;
            }

            lastN = n;
        }
        if(lineCache.size() > 0) {
            data.add(lineCacheToJson(lineCache));
            lineCache.clear();
        }

        answer.add("data", data);

        AnswerUtils.sendMessageToSession(session, answer.toString());
    }

    private static JsonElement shapeToJson(DataElement de) {
        JsonObject result = new JsonObject();
        result.addProperty("n", de.getN());
        result.addProperty("type", "shape");
        result.addProperty("shape", de.getShape().getName());

        JsonArray ptArr = new JsonArray();
        ptArr.add(de.getX1());
        ptArr.add(de.getY1());
        result.add("pt", ptArr);

        if(de.getShape().equals(DataElement.ShapeType.CIRCLE)) {
            result.addProperty("r", de.getY4());
        } else {
            JsonArray dimArr = new JsonArray();
            dimArr.add(de.getX2());
            dimArr.add(de.getY2());
            result.add("dim", dimArr);

            result.addProperty("rot", de.getY4());
        }

        result.addProperty("col", de.getColor());

        return result;
    }

    private static JsonElement emojiToJson(DataElement de) {
        JsonObject result = new JsonObject();
        result.addProperty("n", de.getN());
        result.addProperty("type", "emoji");
        result.addProperty("c1", de.getEmoji1());
        result.addProperty("c2", de.getEmoji2());

        JsonArray ptArr = new JsonArray();
        ptArr.add(de.getX1());
        ptArr.add(de.getY1());
        result.add("pt", ptArr);

        result.addProperty("size", de.getY4());
        result.addProperty("rot", de.getX4());

        return result;
    }

    private static JsonObject lineCacheToJson(List<DataElement> lineCache) {
        DataElement firstElement = lineCache.get(0);

        JsonObject result = new JsonObject();
        result.addProperty("n", firstElement.getN());

        if(firstElement.getType().equals(DataElement.ElementType.RLINE)) {
            result.addProperty("type", "rline");

            JsonArray pts = new JsonArray();
            pts.add(firstElement.getX1());
            pts.add(firstElement.getY1());
            for (DataElement e : lineCache) {
                pts.add(e.getX2());
                pts.add(e.getY2());
            }

            result.add("pts", pts);
        } else {
            result.addProperty("type", "bline");
            JsonArray pts = new JsonArray();

            for (DataElement e : lineCache) {
                JsonArray inner = new JsonArray();
                inner.add(e.getX1());
                inner.add(e.getY1());
                inner.add(e.getX2());
                inner.add(e.getY2());
                inner.add(e.getX3());
                inner.add(e.getY3());
                inner.add(e.getX4());
                inner.add(e.getY4());
                pts.add(inner);
            }

            result.add("pts", pts);
        }

        result.addProperty("col", firstElement.getColor());
        result.addProperty("thick", firstElement.getThickness());

        return result;
    }
}
