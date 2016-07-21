package main.java.model;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.awt.geom.Point2D;

/**
 * Created by DFX on 27.06.2016.
 */

@DatabaseTable(tableName = "drawdata")
public class DataElement {

    public enum ElementType {
        RLINE("rline"),
        BLINE("bline"),
        EMOJI("emoji"),
        SHAPE("shape");

        private String name;

        ElementType(String name) {
            this.name = name;
        }

        public static ElementType getByName(String name) {
            for(ElementType e : ElementType.values()) {
                if(e.getName().equals(name)) {
                    return e;
                }
            }
            return null;
        }

        public String getName() {
            return name;
        }
    }

    public enum ShapeType {
        CIRCLE("circle"),
        RECT("rect"),
        TRI("tri");

        private String name;

        ShapeType(String name) {
            this.name = name;
        }

        public static ShapeType getByName(String name) {
            for(ShapeType e : ShapeType.values()) {
                if(e.getName().equals(name)) {
                    return e;
                }
            }
            return null;
        }

        public String getName() {
            return name;
        }
    }

    public static final String ROUND_ID_FIELD = "rid";
    public static final String N_FIELD = "n";
    public static final String TYPE_FIELD = "type";
    public static final String LINE_ID_FIELD = "lid";
    public static final String COLOR_FIELD = "color";
    public static final String THICK_FIELD = "thick";
    public static final String X1_FIELD = "x1";
    public static final String Y1_FIELD = "y1";
    public static final String X2_FIELD = "x2";
    public static final String Y2_FIELD = "y2";
    public static final String X3_FIELD = "x3";
    public static final String Y3_FIELD = "y3";
    public static final String X4_FIELD = "x4";
    public static final String Y4_FIELD = "y4";
    public static final String EMOJI1_FIELD = "e1";
    public static final String EMOJI2_FIELD = "e2";
    public static final String SHAPE_FIELD = "shape";

    @DatabaseField(generatedId = true)
    private int id;

    @DatabaseField(columnName = ROUND_ID_FIELD)
    private int roundId;

    @DatabaseField(columnName = N_FIELD)
    private int n;

    @DatabaseField(columnName = TYPE_FIELD)
    private ElementType type;

    @DatabaseField(columnName = LINE_ID_FIELD)
    private int lineId;

    @DatabaseField(columnName = COLOR_FIELD)
    private String color;

    @DatabaseField(columnName = THICK_FIELD)
    private float thickness;

    @DatabaseField(columnName = X1_FIELD)
    private float x1;
    @DatabaseField(columnName = Y1_FIELD)
    private float y1;
    @DatabaseField(columnName = X2_FIELD)
    private float x2;
    @DatabaseField(columnName = Y2_FIELD)
    private float y2;
    @DatabaseField(columnName = X3_FIELD)
    private float x3;
    @DatabaseField(columnName = Y3_FIELD)
    private float y3;
    @DatabaseField(columnName = X4_FIELD)
    private float x4;
    @DatabaseField(columnName = Y4_FIELD)
    private float y4;

    @DatabaseField(columnName = EMOJI1_FIELD)
    private int emoji1;

    @DatabaseField(columnName = EMOJI2_FIELD)
    private int emoji2;

    @DatabaseField(columnName = SHAPE_FIELD)
    private ShapeType shape;

    // Constructor for ORMLite
    public DataElement() {

    }

    // Constructor for RLINE
    public DataElement(int rid, ElementType type, int n, int ei, float lastF1, float lastF2, float f1, float f2, String color, float thickness) {
        this.roundId = rid;
        this.type = type;
        this.n = n;
        this.lineId = ei;
        this.x1 = lastF1;
        this.y1 = lastF2;
        this.x2 = f1;
        this.y2 = f2;
        this.color = color;
        this.thickness = thickness;
    }

    // Constructor for BLINE
    public DataElement(int rid, ElementType type, int n, int ei, float[] pts, String color, float thickness) {
        this(rid, type, n, ei, pts[0], pts[1], pts[2], pts[3], color, thickness);

        // Additional points
        this.x3 = pts[4];
        this.y3 = pts[5];
        this.x4 = pts[6];
        this.y4 = pts[7];
    }

    // Constructor for EMOJI
    public DataElement(int rid, ElementType emoji, int n, int c1, int c2, Point2D.Float point, float rotation, float size) {
        this.roundId = rid;
        this.type = emoji;
        this.n = n;
        this.emoji1 = c1;
        this.emoji2 = c2;
        this.x1 = point.x;
        this.y1 = point.y;
        this.x4 = rotation;
        this.y4 = size;
    }

    // Constructor for Circle Shape
    public DataElement(int rid, ElementType shape, int n, Point2D.Float point, float radius, ShapeType circle, String color) {
        this.roundId = rid;
        this.type = shape;
        this.n = n;
        this.x1 = point.x;
        this.y1 = point.y;
        this.y4 = radius;
        this.shape = circle;
        this.color = color;
    }

    // Constructor for Shape Types Rect and Tri
    public DataElement(int rid, ElementType shape, int n, Point2D.Float point, Point2D.Float dim, ShapeType rectTri, float rotation, String color) {
        this.roundId = rid;
        this.type = shape;
        this.n = n;
        this.x1 = point.x;
        this.y1 = point.y;
        this.x2 = dim.x;
        this.y2 = dim.y;
        this.shape = rectTri;
        this.y4 = rotation;
        this.color = color;
    }

    public int getId() {
        return id;
    }

    public int getRoundId() {
        return roundId;
    }

    public int getN() {
        return n;
    }

    public ElementType getType() {
        return type;
    }

    public int getLineId() {
        return lineId;
    }

    public String getColor() {
        return color;
    }

    public float getThickness() {
        return thickness;
    }

    public ShapeType getShape() {
        return shape;
    }

    public float getX1() {
        return x1;
    }

    public float getY1() {
        return y1;
    }

    public float getX2() {
        return x2;
    }

    public float getY2() {
        return y2;
    }

    public float getX3() {
        return x3;
    }

    public float getY3() {
        return y3;
    }

    public float getX4() {
        return x4;
    }

    public float getY4() {
        return y4;
    }

    public int getEmoji1() {
        return emoji1;
    }

    public int getEmoji2() {
        return emoji2;
    }
}
