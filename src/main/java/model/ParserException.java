package main.java.model;

/**
 * Created by DFX on 24.06.2016.
 */
public class ParserException extends Exception {
    public ParserException(String message, Throwable e){
        super(message, e);
    }
    public ParserException(String message){
        super(message);
    }
}
