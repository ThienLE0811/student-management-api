package com.thienlv.student_api.mapper;

import com.thienlv.student_api.dto.ClassResponse;
import com.thienlv.student_api.entity.SchoolClass;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ClassMapper {

    ClassResponse toResponse(SchoolClass schoolClass);
}
