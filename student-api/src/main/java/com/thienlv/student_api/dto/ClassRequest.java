package com.thienlv.student_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ClassRequest(
        @NotBlank
        @Size(max = 100)
        String name,

        @NotNull
        @Positive
        Integer capacity
) {
}
