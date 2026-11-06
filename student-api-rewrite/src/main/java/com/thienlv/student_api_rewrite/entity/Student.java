package com.thienlv.student_api_rewrite.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record Student(
        Long id,
        String email,
        String full_name,
        LocalDate birth_date,
        LocalDateTime created_at
) {
}
