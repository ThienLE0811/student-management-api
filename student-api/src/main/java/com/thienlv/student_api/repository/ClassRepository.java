package com.thienlv.student_api.repository;

import com.thienlv.student_api.entity.SchoolClass;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClassRepository extends JpaRepository<SchoolClass, Long> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    @EntityGraph(attributePaths = "students")
    @Query("SELECT c FROM SchoolClass c")
    List<SchoolClass> findAllWithStudents();

    // Buoi 22 - ban doi chieu voi optimistic locking (@Version). Khoa dong
    // ngay luc doc (SELECT ... FOR UPDATE) thay vi phat hien xung dot luc
    // commit. Dung rieng cho StudentService.addToClassPessimistic, khong
    // dung cho endpoint addToClass hien tai (dang dung optimistic).
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM SchoolClass c WHERE c.id = :id")
    Optional<SchoolClass> findByIdForUpdate(@Param("id") Long id);
}
