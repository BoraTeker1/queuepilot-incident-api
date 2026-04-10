package com.example.queuepilot.incident_api.exception;

public class InvalidStateException extends RuntimeException{
    public InvalidStateException(String message) {
        super(message);
    }
}
