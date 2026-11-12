package com.thienlv.student_api.mapper;

import com.thienlv.student_api.dto.CourseResponse;
import com.thienlv.student_api.entity.Course;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CourseMapper {

    CourseResponse toResponse(Course course);
}
