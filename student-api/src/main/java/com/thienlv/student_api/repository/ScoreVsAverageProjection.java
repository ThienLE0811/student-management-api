package com.thienlv.student_api.repository;

public interface ScoreVsAverageProjection {
    Long getStudentId();
    String getStudentName();
    Double getScore();
    Double getAvgScore();
    Double getDiff();
}
