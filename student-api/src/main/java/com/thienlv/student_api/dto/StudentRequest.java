package com.thienlv.student_api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record StudentRequest(
        @NotBlank
        @Size(max = 100)
        String fullName,

        @NotBlank
        @Size(max = 100)
        @Email
        String email,

        @Past
        LocalDate birthDate
) {
}
