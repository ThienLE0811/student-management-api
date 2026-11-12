package com.thienlv.student_api.repository;

import com.thienlv.student_api.entity.SchoolClass;
import com.thienlv.student_api.entity.Student;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// @DataJpaTest mac dinh thay datasource bang embedded DB -> Replace.NONE de
// dung that Postgres tu Testcontainers (qua @ServiceConnection) thay vi H2,
// vi query dung syntax/semantic Postgres that (vd Hibernate dialect).
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class StudentRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @DynamicPropertySource
    static void ddlAuto(DynamicPropertyRegistry registry) {
        // Container rong, chua co schema -> can Hibernate tu sinh bang theo entity.
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private ClassRepository classRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private CourseRepository courseRepository;

    private SchoolClass schoolClass;

    @BeforeEach
    void setUp() {
        schoolClass = classRepository.save(new SchoolClass(null, "10A1", LocalDateTime.now()));
    }

    private Student newStudent(String fullName, String email, LocalDate birthDate) {
        return studentRepository.save(new Student(null, fullName, email, birthDate, LocalDateTime.now(), schoolClass));
    }

    @Test
    void findByFullNameContainingIgnoreCase_khong_phan_biet_hoa_thuong_va_khop_mot_phan() {
        newStudent("Nguyen Van A", "a@test.com", LocalDate.of(2005, 1, 1));
        newStudent("Tran Thi B", "b@test.com", LocalDate.of(2005, 2, 2));

        List<Student> result = studentRepository.findByFullNameContainingIgnoreCase("nguyen");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEmail()).isEqualTo("a@test.com");
    }

    @Test
    void findByFullNameContainingIgnoreCase_tra_ve_rong_khi_khong_khop() {
        newStudent("Nguyen Van A", "a@test.com", LocalDate.of(2005, 1, 1));

        List<Student> result = studentRepository.findByFullNameContainingIgnoreCase("khong-ton-tai");

        assertThat(result).isEmpty();
    }

    @Test
    void findByBirthDateBetween_loc_dung_khoang_bao_gom_ca_bien() {
        newStudent("A", "a@test.com", LocalDate.of(2005, 1, 1));
        newStudent("B", "b@test.com", LocalDate.of(2005, 6, 15));
        newStudent("C", "c@test.com", LocalDate.of(2006, 1, 1));

        List<Student> result = studentRepository.findByBirthDateBetween(
                LocalDate.of(2005, 1, 1), LocalDate.of(2005, 12, 31));

        assertThat(result).extracting(Student::getEmail)
                .containsExactlyInAnyOrder("a@test.com", "b@test.com");
    }

    @Test
    void findBySchoolClassId_chi_tra_ve_sinh_vien_cua_dung_lop() {
        SchoolClass otherClass = classRepository.save(new SchoolClass(null, "10A2", LocalDateTime.now()));
        newStudent("A", "a@test.com", LocalDate.of(2005, 1, 1));
        studentRepository.save(new Student(null, "D", "d@test.com", LocalDate.of(2005, 1, 1), LocalDateTime.now(), otherClass));

        List<Student> result = studentRepository.findBySchoolClassId(schoolClass.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEmail()).isEqualTo("a@test.com");
    }

    @Test
    void findStudentsWithoutCourse_chi_tra_ve_sinh_vien_chua_dang_ky_khoa_hoc_nao() {
        Student withCourse = newStudent("A", "a@test.com", LocalDate.of(2005, 1, 1));
        Student withoutCourse = newStudent("B", "b@test.com", LocalDate.of(2005, 1, 1));
        var course = courseRepository.save(new com.thienlv.student_api.entity.Course(null, "Toan", LocalDateTime.now()));
        enrollmentRepository.save(new com.thienlv.student_api.entity.Enrollment(null, withCourse, course, null, LocalDateTime.now()));

        List<Student> result = studentRepository.findStudentsWithoutCourse();

        assertThat(result).extracting(Student::getEmail).containsExactly(withoutCourse.getEmail());
    }
}
