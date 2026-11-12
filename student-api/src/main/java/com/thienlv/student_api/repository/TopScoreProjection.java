package com.thienlv.student_api.repository;

public interface TopScoreProjection {
    Long getStudentId();
    String getStudentName();
    Double getScore();
    Long getRankNo();
}
