package com.example.smartnotes.exception;

public class AiFeatureException extends RuntimeException {

    public AiFeatureException(String message) {
        super(message);
    }

    public AiFeatureException(String message, Throwable cause) {
        super(message, cause);
    }
}
