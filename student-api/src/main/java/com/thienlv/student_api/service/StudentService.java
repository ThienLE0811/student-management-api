package com.thienlv.student_api.service;

import com.thienlv.student_api.dto.StudentRequest;
import com.thienlv.student_api.dto.StudentResponse;
import com.thienlv.student_api.entity.Student;
import com.thienlv.student_api.exception.DuplicateEmailException;
import com.thienlv.student_api.exception.NotFoundException;
import com.thienlv.student_api.mapper.StudentMapper;
import com.thienlv.student_api.repository.StudentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;
import java.util.Objects;

@Service
public class StudentService {
    private static final Logger log = LoggerFactory.getLogger(StudentService.class);

    private final StudentRepository studentRepository;
    private final StudentMapper studentMapper;

    public StudentService(StudentRepository studentRepository, StudentMapper studentMapper){
        this.studentRepository = studentRepository;
        this.studentMapper = studentMapper;
    }

    public List<StudentResponse> getAll(String name, Integer minAge, Integer maxAge) {
        List<Student> students = studentRepository.findAll();
        LocalDate today = LocalDate.now();

        return students.stream()
                .filter(student -> name == null || Objects.equals(student.getFull_name(), name))
                .filter(student -> {
                    LocalDate dob = student.getBirth_date();
                    if (dob == null) return false;
                    // Tính số tuổi chính xác tới ngày hôm nay
                    int age = Period.between(dob, today).getYears();
                    // Nếu minAge có truyền -> kiểm tra age >= minAge; Nếu null -> luôn đúng
                    boolean matchMin = (minAge == null || age >= minAge);
                    // Nếu maxAge có truyền -> kiểm tra age <= maxAge; Nếu null -> luôn đúng
                    boolean matchMax = (maxAge == null || age <= maxAge);
                    return matchMin && matchMax;
                })
                .map(studentMapper::toResponse).toList();
    }

    public StudentResponse getById(Long id) {
        return studentRepository.findById(id).map(studentMapper::toResponse).orElseThrow(()-> new NotFoundException("Không tìm thấy người dùng"));
    }

    public StudentResponse create(StudentRequest request) {
        boolean sameEmail = studentRepository.findAll().stream().anyMatch(s -> s.getEmail().equals(request.email()));
        if(sameEmail)  throw new DuplicateEmailException("Email đã tồn tại");
        Student newStudent = new Student(null, request.fullName(), request.email(), request.birthDate(), LocalDateTime.now());
        Student saved = studentRepository.save(newStudent);
        log.info("Đã tạo sinh viên id={}", saved.getId());
        return studentMapper.toResponse(saved);
    }

    public StudentResponse update(Long id, StudentRequest request) {
        Student student = studentRepository.findById(id).orElseThrow(()-> new NotFoundException("Không tìm thấy người dùng"));
        Student newStudent = new Student(id, request.fullName(), request.email(), request.birthDate(), student.getCreated_at());
        Student saved = studentRepository.save(newStudent);
        return studentMapper.toResponse(saved);
    }


    public void deleteById(Long id) {
        if(!studentRepository.existsById(id)) throw new NotFoundException("Không tìm thấy người dùng");
        studentRepository.deleteById(id);
        log.info("Đã xóa sinh viên id={}", id);
    }
}
