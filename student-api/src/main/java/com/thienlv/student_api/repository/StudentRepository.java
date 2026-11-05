package com.thienlv.student_api.repository;

import com.thienlv.student_api.entity.Student;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class StudentRepository {
    private static final Map<Long, Student> STUDENTS = new LinkedHashMap<>(Map.of(
            1L, new Student(1L, "Nguyễn Văn An", "an.nguyenvan@gmail.com", LocalDate.of(2004, 3, 15), LocalDateTime.of(2024, 1, 10, 8, 30, 0)),
            2L, new Student(2L, "Trần Thị Bích", "bich.tran@gmail.com", LocalDate.of(2004, 7, 22), LocalDateTime.of(2024, 1, 10, 9, 15, 0)),
            3L, new Student(3L, "Lê Hoàng Cường", "cuong.le@gmail.com", LocalDate.of(2003, 11, 5), LocalDateTime.of(2024, 1, 11, 10, 0, 0)),
            4L, new Student(4L, "Phạm Minh Đức", "duc.pham@gmail.com", LocalDate.of(2004, 1, 18), LocalDateTime.of(2024, 1, 11, 14, 20, 0)),
            5L, new Student(5L, "Hoàng Thu Giang", "giang.hoang@gmail.com", LocalDate.of(2004, 9, 30), LocalDateTime.of(2024, 1, 12, 11, 45, 0)),
            6L, new Student(6L, "Vũ Hải Nam", "nam.vu@gmail.com", LocalDate.of(2003, 5, 12), LocalDateTime.of(2024, 1, 12, 16, 10, 0)),
            7L, new Student(7L, "Đặng Ngọc Mai", "mai.dang@gmail.com", LocalDate.of(2004, 12, 8), LocalDateTime.of(2024, 1, 13, 8, 0, 0)),
            8L, new Student(8L, "Bùi Quốc Huy", "huy.bui@gmail.com", LocalDate.of(2004, 4, 25), LocalDateTime.of(2024, 1, 13, 13, 30, 0)),
            9L, new Student(9L, "Đỗ Phương Linh", "linh.do@gmail.com", LocalDate.of(2003, 8, 19), LocalDateTime.of(2024, 1, 14, 15, 50, 0)),
            10L, new Student(10L, "Ngô Thành Long", "long.ngo@gmail.com", LocalDate.of(2004, 6, 2), LocalDateTime.of(2024, 1, 14, 17, 0, 0))
    ));

    private static final AtomicLong ID_GEN = new AtomicLong(
            STUDENTS.keySet().stream().mapToLong(Long::longValue).max().orElse(0)
    );

    public List<Student> findAll() {
        return STUDENTS.values().stream().toList();
    }

    public Optional<Student> findById(Long id) {
        return Optional.ofNullable(STUDENTS.get(id));
    }

    public Student save(Student student) {
        if(student.id() == null) {
            Long id = ID_GEN.incrementAndGet();
            Student newStudent = new Student(id, student.full_name(), student.email(), student.birth_date(), student.created_at());
            STUDENTS.put(id,newStudent);
            return newStudent;
        }
        else {
            STUDENTS.put(student.id(),student);
            return student;
        }
    }

    public boolean deleteById(Long id) {
        Student studentRemove =  STUDENTS.remove(id);
        return Objects.nonNull(studentRemove);
    }

}
