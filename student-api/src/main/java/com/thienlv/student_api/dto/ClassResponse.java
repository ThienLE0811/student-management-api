package com.thienlv.student_api.dto;

import java.util.List;

public record ClassResponse(Long id, String name, Integer capacity, Integer enrolledCount, List<StudentResponse> students) {
}
