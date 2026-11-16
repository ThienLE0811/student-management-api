package com.thienlv.student_api.repository;

import com.thienlv.student_api.entity.SchoolClass;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ClassRepository extends JpaRepository<SchoolClass, Long> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    @EntityGraph(attributePaths = "students")
    @Query("SELECT c FROM SchoolClass c")
    List<SchoolClass> findAllWithStudents();
}
