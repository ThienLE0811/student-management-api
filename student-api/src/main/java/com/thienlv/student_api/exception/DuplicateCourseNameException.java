package com.thienlv.student_api.exception;

public class DuplicateCourseNameException extends RuntimeException {
    public DuplicateCourseNameException(String message) {
        super(message);
    }
}
