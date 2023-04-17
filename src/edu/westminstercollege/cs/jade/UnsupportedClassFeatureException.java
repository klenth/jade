package edu.westminstercollege.cs.jade;

public class UnsupportedClassFeatureException extends Exception {

    public UnsupportedClassFeatureException() {
        super();
    }

    public UnsupportedClassFeatureException(String message) {
        super(message);
    }

    public UnsupportedClassFeatureException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnsupportedClassFeatureException(Throwable cause) {
        super(cause);
    }

    protected UnsupportedClassFeatureException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
