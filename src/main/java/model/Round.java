package main.java.model;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import main.java.db.DrawDB;
import main.java.utils.GameUtils;
import main.java.utils.Wordlist;

import java.sql.SQLException;
import java.util.*;

/**
 * Created by DFX on 12.06.2016.
 */

@DatabaseTable(tableName = "rounds")
public class Round {
    public static Random rnd = new Random(System.currentTimeMillis());

    private static final int NUM_CHARS_ALPHABET = 20;

    public static final String DRAWING_PLAYER_FIELD = "drawing_player";
    public static final String DRAWING_ID_FIELD = "drawing_id";
    public static final String WORD_EASY_FIELD = "word_easy";
    public static final String WORD_MEDIUM_FIELD = "word_medium";
    public static final String WORD_HARD_FIELD = "word_hard";
    public static final String CHOSEN_WORD_FIELD = "chosen_word";
    public static final String WORD_POINTS_FIELD = "word_points";
    public static final String ALPHABET_FIELD = "alphabet";
    public static final String GAPWORD_FIELD = "gapword";
    public static final String CHUNKS_FIELD = "chunks";
    public static final String CHUNKS_RETRIEVED_FIELD = "chunks_retrieved";
    public static final String DATA_PER_CHUNK_FIELD = "data_per_chunk";
    public static final String BGCOLOR_FIELD = "bgcolor";

    @DatabaseField(generatedId = true)
    private int id;

    @DatabaseField(columnName = DRAWING_PLAYER_FIELD)
    private int drawingPlayer;

    @DatabaseField(columnName = DRAWING_ID_FIELD)
    private String drawingId;

    @DatabaseField(columnName = WORD_EASY_FIELD)
    private String wordEasy;

    @DatabaseField(columnName = WORD_MEDIUM_FIELD)
    private String wordMedium;

    @DatabaseField(columnName = WORD_HARD_FIELD)
    private String wordHard;

    @DatabaseField(columnName = CHOSEN_WORD_FIELD)
    private String chosenWord = "";

    @DatabaseField(columnName = WORD_POINTS_FIELD)
    private int wordPoints = 0;

    @DatabaseField(columnName = ALPHABET_FIELD)
    private String alphabet;

    @DatabaseField(columnName = GAPWORD_FIELD)
    private String gapWord;

    @DatabaseField(columnName = CHUNKS_FIELD)
    private int chunks = 0;

    @DatabaseField(columnName = CHUNKS_RETRIEVED_FIELD)
    private int chunksRetrieved = 0;

    @DatabaseField(columnName = DATA_PER_CHUNK_FIELD)
    private int dataPerChunk;

    @DatabaseField(columnName = BGCOLOR_FIELD)
    private String bgColor = "";

    public static Round createBlankRound(Player p) {
        Round res = new Round();

        // Make some nice words!
        String[] words = Wordlist.getRandomWords();
        res.setWords(words);

        // Set Player
        res.setPlayer(p.getId());

        // Set a unique drawing ID
        int secs = (int) (System.currentTimeMillis() / 1000);
        String id = p.getPublicId() + "_" + secs + "_" + rnd.nextInt(2000);
        res.setDrawingId(id);

        return res;
    }

    // Empty Constructor for ORMLite
    public Round() {
    }

    public boolean hasChoosenWord() {
        return chosenWord.length() > 0;
    }

    public String[] getWords() {
        return new String[] {wordEasy, wordMedium, wordHard};
    }

    public int getId() {
        return id;
    }

    public void setWords(String[] words) {
        this.wordEasy = words[0];
        this.wordMedium = words[1];
        this.wordHard = words[2];
    }

    public void setPlayer(int player) {
        this.drawingPlayer = player;
    }

    public void setDrawingId(String drawingId) {
        this.drawingId = drawingId;
    }

    public void clearDrawData() throws InternalServerException {
        DrawDB.getHandler().clearDrawDataForRound(this.id);
    }

    public void setChoosenWord(String input) {
        switch(input) {
            case "easy":
                this.chosenWord = this.wordEasy;
                this.wordPoints = GameUtils.WORD_POINTS[0];
                break;
            case "medium":
                this.chosenWord = this.wordMedium;
                this.wordPoints = GameUtils.WORD_POINTS[1];
                break;
            case "hard":
                this.chosenWord = this.wordHard;
                this.wordPoints = GameUtils.WORD_POINTS[2];
                break;
            default:
                return;
        }

        generateAlphabet();
        generateGapWord();
    }

    private void generateGapWord() {
        char[] wordChars = this.chosenWord.toCharArray();
        String result = "";
        for(char c : wordChars) {
            if(Character.isAlphabetic(c)) {
                result += "#";
            } else {
                result += c;
            }
        }
        this.gapWord = result;
    }

    private void generateAlphabet() {
        char[] wordChars = this.chosenWord.toCharArray();
        char[] allChars = new char[NUM_CHARS_ALPHABET];
        for(int i = 0; i < NUM_CHARS_ALPHABET; i++) {
            if(i < wordChars.length) {
                allChars[i] = wordChars[i];
            } else {
                allChars[i] = (char) (rnd.nextInt(26) + 'A');
            }
        }
        // Shuffle the Chars
        List<Character> charList = new ArrayList<>();
        for(int i = 0; i < NUM_CHARS_ALPHABET; i++)
        {
            charList.add(allChars[i]);
        }

        Collections.shuffle(charList);

        // Array to String
        String result = "";
        for(Character c : charList) {
            result += c;
        }

        this.alphabet = result;
    }

    public String getAlphabet() {
        return alphabet;
    }

    public String getGapWord() {
        return gapWord;
    }

    public String getChosenWord() {
        return chosenWord;
    }

    public int getChunkCount() {
        return chunks;
    }

    public int getDataPerChunk() {
        return dataPerChunk;
    }

    public boolean chunkRetrieved(int i) {
        if(i >= chunksRetrieved) {
            chunksRetrieved = (i + 1);
            return true;
        }
        return false;
    }

    public void setChunkCount(int chunkCount) {
        this.chunks = chunkCount;
    }

    public void setDataPerChunk(int setDataPerChunk) {
        this.dataPerChunk = setDataPerChunk;
    }

    public int getCalculatedPoints() {
        float perc = chunksRetrieved / (chunks * 1.0f);
        return wordPoints - (int) ((wordPoints/2.0f) * perc);
    }

    public void create() throws InternalServerException {
        try {
            DrawDB.getHandler().roundsDao.create(this);
        } catch (SQLException e) {
            throw new InternalServerException("Error creating Round!", e);
        }
    }

    public void update() throws InternalServerException {
        try {
            DrawDB.getHandler().roundsDao.update(this);
        } catch (SQLException e) {
            throw new InternalServerException("Error updating Round!", e);
        }
    }

    public String getBgColor() {
        return bgColor;
    }

    public void setBgColor(String col) {
        this.bgColor = col;
    }
}
