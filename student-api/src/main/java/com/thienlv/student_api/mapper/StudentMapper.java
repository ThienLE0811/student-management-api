package com.thienlv.student_api.mapper;

import com.thienlv.student_api.dto.StudentResponse;
import com.thienlv.student_api.entity.Student;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDate;
import java.time.Period;

@Mapper(componentModel = "spring")
public interface StudentMapper {

    @Mapping(target = "fullName", source = "full_name")
    @Mapping(target = "birthDate", source = "birth_date")
    @Mapping(target = "createdAt", source = "created_at")
    @Mapping(target = "age", expression = "java(calculateAge(student.birth_date()))")
    StudentResponse toResponse(Student student);

    default int calculateAge(LocalDate birthDate) {
        return Period.between(birthDate, LocalDate.now()).getYears();
    }
}
