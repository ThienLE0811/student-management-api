package com.thienlv.student_api_rewrite.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record StudentResponse (
        Long id,
        String fullName,
        String email,
        LocalDate birthDate,
        int age,
        LocalDateTime createdAt
) {
}
