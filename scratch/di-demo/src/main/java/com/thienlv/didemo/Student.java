package com.thienlv.didemo;

import  java.time.LocalDate;
import java.time.LocalDateTime;

// TODO: record Student — tự chọn field (vd id, fullName)
public record Student(
      Long id,
      String full_name,
      String email,
      LocalDate birth_date,
      LocalDateTime created_at
) {
}
