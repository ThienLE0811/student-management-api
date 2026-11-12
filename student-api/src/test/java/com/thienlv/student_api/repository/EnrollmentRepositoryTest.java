package com.thienlv.student_api.repository;

import com.thienlv.student_api.entity.Course;
import com.thienlv.student_api.entity.Enrollment;
import com.thienlv.student_api.entity.SchoolClass;
import com.thienlv.student_api.entity.Student;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class EnrollmentRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @DynamicPropertySource
    static void ddlAuto(DynamicPropertyRegistry registry) {
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private ClassRepository classRepository;

    private Student student;
    private Course course;

    @BeforeEach
    void setUp() {
        SchoolClass schoolClass = classRepository.save(new SchoolClass(null, "10A1", LocalDateTime.now()));
        student = studentRepository.save(new Student(null, "Nguyen Van A", "a@test.com",
                LocalDate.of(2005, 1, 1), LocalDateTime.now(), schoolClass));
        course = courseRepository.save(new Course(null, "Toan", LocalDateTime.now()));
    }

    @Test
    void existsByStudentIdAndCourseId_tra_ve_false_khi_chua_dang_ky() {
        boolean exists = enrollmentRepository.existsByStudentIdAndCourseId(student.getId(), course.getId());

        assertThat(exists).isFalse();
    }

    @Test
    void existsByStudentIdAndCourseId_tra_ve_true_sau_khi_da_dang_ky() {
        enrollmentRepository.save(new Enrollment(null, student, course, null, LocalDateTime.now()));

        boolean exists = enrollmentRepository.existsByStudentIdAndCourseId(student.getId(), course.getId());

        assertThat(exists).isTrue();
    }

    @Test
    void dang_ky_trung_sinh_vien_va_khoa_hoc_vi_pham_unique_constraint_o_DB() {
        enrollmentRepository.saveAndFlush(new Enrollment(null, student, course, 8.0, LocalDateTime.now()));

        Enrollment duplicate = new Enrollment(null, student, course, 9.0, LocalDateTime.now());

        // Kiem tra rang buoc unique_constraint(student_id, course_id) o tang DB that
        // (khong phai logic existsByStudentIdAndCourseId o service) - bao ve truong hop
        // 2 request chay dong thoi vuot qua check exists truoc khi insert (race condition).
        assertThatThrownBy(() -> enrollmentRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
