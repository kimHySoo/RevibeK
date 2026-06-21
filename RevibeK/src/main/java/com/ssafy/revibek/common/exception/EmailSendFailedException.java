package com.ssafy.revibek.common.exception;

public class EmailSendFailedException extends RuntimeException {

    public EmailSendFailedException(String message) {
        super(message);
    }
}
