package com.thienlv.student_api.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record StudentResponse(Long id, String fullName, String email, LocalDate birthDate, int age, LocalDateTime createdAt, Long classId) {
}
