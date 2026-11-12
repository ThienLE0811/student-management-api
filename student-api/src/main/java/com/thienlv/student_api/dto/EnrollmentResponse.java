package com.thienlv.student_api.dto;

import java.time.LocalDateTime;

public record EnrollmentResponse(Long id, Long studentId, Long courseId, Double score, LocalDateTime createdAt) {
}
