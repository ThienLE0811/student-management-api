package com.thienlv.student_api.exception;

public class ClassFullException extends RuntimeException {
    public ClassFullException(String message) {
        super(message);
    }
}
