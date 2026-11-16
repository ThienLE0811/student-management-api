package com.thienlv.student_api.dto;

import jakarta.validation.constraints.NotNull;

public record AddStudentToClassRequest(
        @NotNull
        Long studentId
) {
}
