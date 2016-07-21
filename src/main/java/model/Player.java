package main.java.model;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import main.java.db.DrawDB;

import java.sql.SQLException;
import java.util.Date;

/**
 * Created by DFX on 18.05.2016.
 */

@DatabaseTable(tableName = "players")
public class Player {
    // for QueryBuilder to be able to find the fields
    public static final String PUB_ID_FIELD_NAME = "publicid";
    public static final String NAME_FIELD_NAME = "name";
    public static final String SCORE_FIELD_NAME = "score";
    public static final String SECRET_FIELD_NAME = "secret";
    public static final String LASTONLINE_FIELD_NAME = "lastonline";

    @DatabaseField(generatedId = true)
    private int id;

    @DatabaseField(columnName = PUB_ID_FIELD_NAME)
    private int publicId;

    @DatabaseField(columnName = NAME_FIELD_NAME, canBeNull = false)
    private String name;

    @DatabaseField(columnName = SCORE_FIELD_NAME)
    private int score;

    @DatabaseField(columnName = SECRET_FIELD_NAME)
    private String secret;

    @DatabaseField(columnName = LASTONLINE_FIELD_NAME)
    private Date lastOnline;

    public Player() {

    }

    public int getId() {
        return this.id;
    }

    public int getPublicId() {
        return this.publicId;
    }

    public String getName() {
        return name;
    }

    public int getScore() {
        return score;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPublicId(int publicId) {
        this.publicId = publicId;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public void setLastOnline(Date lastOnline) {
        this.lastOnline = lastOnline;
    }

    @Override
    public boolean equals(Object o) {
        if(!(o instanceof Player)) {
            return false;
        }
        Player p = (Player) o;
        return (getId() == p.getId());
    }

    public void addScore(int calculatedPoints) throws InternalServerException {
        this.score += calculatedPoints;
        update();
    }

    public void update() throws InternalServerException {
        try {
            DrawDB.getHandler().playersDao.update(this);
        } catch (SQLException e) {
            throw new InternalServerException("Error while updating Player!", e);
        }
    }
}
