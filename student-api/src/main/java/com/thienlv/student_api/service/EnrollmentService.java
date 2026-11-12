package com.thienlv.student_api.service;

import com.thienlv.student_api.dto.EnrollmentRequest;
import com.thienlv.student_api.dto.EnrollmentResponse;
import com.thienlv.student_api.entity.Course;
import com.thienlv.student_api.entity.Enrollment;
import com.thienlv.student_api.entity.Student;
import com.thienlv.student_api.exception.DuplicateEnrollmentException;
import com.thienlv.student_api.exception.NotFoundException;
import com.thienlv.student_api.mapper.EnrollmentMapper;
import com.thienlv.student_api.repository.CourseRepository;
import com.thienlv.student_api.repository.EnrollmentRepository;
import com.thienlv.student_api.repository.StudentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class EnrollmentService {
    private static final Logger log = LoggerFactory.getLogger(EnrollmentService.class);

    private final EnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentMapper enrollmentMapper;

    public EnrollmentService(EnrollmentRepository enrollmentRepository, StudentRepository studentRepository,
                              CourseRepository courseRepository, EnrollmentMapper enrollmentMapper) {
        this.enrollmentRepository = enrollmentRepository;
        this.studentRepository = studentRepository;
        this.courseRepository = courseRepository;
        this.enrollmentMapper = enrollmentMapper;
    }

    public EnrollmentResponse create(EnrollmentRequest request) {
        Student student = studentRepository.findById(request.studentId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy sinh viên"));

        Course course = courseRepository.findById(request.courseId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy khóa học"));

        if (enrollmentRepository.existsByStudentIdAndCourseId(student.getId(), course.getId())) {
            throw new DuplicateEnrollmentException("Sinh viên đã đăng ký khóa học này");
        }

        Enrollment enrollment = new Enrollment(null, student, course, request.score(), LocalDateTime.now());
        Enrollment saved = enrollmentRepository.save(enrollment);
        log.info("Đã tạo đăng ký id={} (student={}, course={})", saved.getId(), student.getId(), course.getId());
        return enrollmentMapper.toResponse(saved);
    }
}
