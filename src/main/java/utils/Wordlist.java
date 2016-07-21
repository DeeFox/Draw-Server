package main.java.utils;

import java.util.Random;

/**
 * Created by DFX on 14.05.2016.
 */
public class Wordlist {
    private static Random rnd = new Random();

    public static String[] getRandomWords() {
        return new String[] {
                WORDS_EASY[rnd.nextInt(WORDS_EASY.length)].toUpperCase(),
                WORDS_MEDIUM[rnd.nextInt(WORDS_MEDIUM.length)].toUpperCase(),
                WORDS_HARD[rnd.nextInt(WORDS_HARD.length)].toUpperCase()
            };
    }

    public static final String[] WORDS_EASY = {
            "maennlich",
            "weiblich",
            "auto",
            "gluecklich",
            "traurig",
            "smiley",
            "laptop",
            "lampe",
            "bier",
            "lupe",
            "flasche",
            "schere",
            "handy",
            "deutschland",
            "wald",
            "sonne",
            "kerze",
            "tisch",
            "mensch",
            "buch"
    };
    public static final String[] WORDS_MEDIUM = {
            "mauer",
            "fahrrad",
            "wuetend",
            "winter",
            "sommer",
            "computer",
            "tastatur",
            "apfelsaft",
            "spielfigur",
            "casino",
            "telefon",
            "windows",
            "apple",
            "brunnen",
            "europa",
            "strand",
            "strom",
            "licht",
            "torwart",
            "tuersteher",
            "elfmeter",
            "handball",
            "hockey",
            "tennis",
            "bildschirm"
    };
    public static final String[] WORDS_HARD = {
            "wurmloch",
            "fruehling",
            "herbst",
            "jahreszeiten",
            "geschwindigkeit",
            "trinkwasser",
            "merkel",
            "putin",
            "obama",
            "poker",
            "todesstern",
            "starwars",
            "afrika",
            "wueste",
            "daft punk",
            "seekrank",
            "motor",
            "getriebe",
            "kolben",
            "schiedsrichter",
            "linienrichter",
            "geschichte",
            "mannschaft",
            "logik",
            "tischtennis",
            "joghurt",
            "dessert"
    };
}
