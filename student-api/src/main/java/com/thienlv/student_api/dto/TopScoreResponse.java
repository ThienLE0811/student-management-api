package com.thienlv.student_api.dto;

public record TopScoreResponse(Long studentId, String studentName, Double score, Long rankNo) {
}
