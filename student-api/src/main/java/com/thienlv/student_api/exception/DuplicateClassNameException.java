package com.thienlv.student_api.exception;

public class DuplicateClassNameException extends RuntimeException {
    public DuplicateClassNameException(String message) {
        super(message);
    }
}
