package com.thienlv.student_api.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record Student(
        Long id,
        String full_name,
        String email,
        LocalDate birth_date,
        LocalDateTime created_at
) {}
