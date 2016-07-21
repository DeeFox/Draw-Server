package main.java.model;

/**
 * Created by DFX on 10.06.2016.
 */
public class InternalServerException extends Exception {
    public InternalServerException(String message, Throwable e){
        super(message, e);
    }
    public InternalServerException(String message){
        super(message);
    }
    public InternalServerException(){
    }
}
