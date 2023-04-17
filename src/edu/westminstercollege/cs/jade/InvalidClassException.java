package edu.westminstercollege.cs.jade;

public class InvalidClassException extends Exception {

    public InvalidClassException() {
        super();
    }

    public InvalidClassException(String message) {
        super(message);
    }

    public InvalidClassException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidClassException(Throwable cause) {
        super(cause);
    }

    protected InvalidClassException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
