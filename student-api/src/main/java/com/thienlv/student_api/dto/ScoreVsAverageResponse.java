package com.thienlv.student_api.dto;

public record ScoreVsAverageResponse(Long studentId, String studentName, Double score, Double avgScore, Double diff) {
}
