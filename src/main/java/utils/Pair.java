package main.java.utils;

/**
 * Created by DFX on 25.06.2016.
 */
public class Pair<F, S> {
    private final F first; //first member of pair
    private final S second; //second member of pair

    public Pair(F first, S second) {
        this.first = first;
        this.second = second;
    }

    public F getFirst() {
        return first;
    }

    public S getSecond() {
        return second;
    }
}
