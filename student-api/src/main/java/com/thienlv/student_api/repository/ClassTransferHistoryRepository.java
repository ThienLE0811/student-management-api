package com.thienlv.student_api.repository;

import com.thienlv.student_api.entity.ClassTransferHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClassTransferHistoryRepository extends JpaRepository<ClassTransferHistory, Long> {
}
