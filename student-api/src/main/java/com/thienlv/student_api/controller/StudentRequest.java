package com.thienlv.student_api.controller;

import java.time.LocalDate;

public record StudentRequest( String fullName, String email, LocalDate birthDate ) {
}

