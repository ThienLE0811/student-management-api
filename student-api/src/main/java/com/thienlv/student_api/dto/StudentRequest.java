package com.thienlv.student_api.dto;

import java.time.LocalDate;

public record StudentRequest(String fullName, String email, LocalDate birthDate ) {
}
