package com.thienlv.student_api.mapper;

import com.thienlv.student_api.dto.EnrollmentResponse;
import com.thienlv.student_api.entity.Enrollment;
import org.mapstruct.Mapping;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface EnrollmentMapper {

    @Mapping(target = "studentId", source = "student.id")
    @Mapping(target = "courseId", source = "course.id")
    EnrollmentResponse toResponse(Enrollment enrollment);
}
