package com.thienlv.student_api_rewrite.dto;

import java.time.LocalDate;

public record StudentRequest(
        String fullName,
        String email,
        LocalDate birthDate
) {
}
