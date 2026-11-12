package com.thienlv.student_api.repository;

import com.thienlv.student_api.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface StudentRepository extends JpaRepository<Student, Long> {

    @Query("SELECT s FROM Student s WHERE LOWER(s.full_name) LIKE LOWER(CONCAT('%', :fullName, '%'))")
    List<Student> findByFullNameContainingIgnoreCase(@Param("fullName") String fullName);

    @Query("SELECT s FROM Student s WHERE s.birth_date BETWEEN :from AND :to")
    List<Student> findByBirthDateBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT s FROM Student s WHERE s.schoolClass.id = :classId")
    List<Student> findBySchoolClassId(@Param("classId") Long classId);

    @Query("SELECT s FROM Student s WHERE NOT EXISTS (SELECT 1 FROM Enrollment e WHERE e.student = s)")
    List<Student> findStudentsWithoutCourse();
}
