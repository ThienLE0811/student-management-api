package com.thienlv.student_api.service;

import com.thienlv.student_api.dto.StudentRequest;
import com.thienlv.student_api.dto.StudentResponse;
import com.thienlv.student_api.entity.Student;
import com.thienlv.student_api.repository.StudentRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class StudentService {
    private final StudentRepository studentRepository;
    public StudentService(StudentRepository studentRepository){
        this.studentRepository = studentRepository;
    }

    private StudentResponse toResponse(Student student) {
        int age = Period.between(student.birth_date(), LocalDate.now()).getYears();
        return new StudentResponse(
                student.id(),
                student.full_name(),
                student.email(),
                student.birth_date(),
                age,
                student.created_at()
        );
    }

    public List<StudentResponse> getAll(String name, Integer minAge, Integer maxAge) {
        List<Student> students = studentRepository.findAll();
        LocalDate today = LocalDate.now();

        return students.stream()
                .filter(student -> name == null || Objects.equals(student.full_name(), name))
                .filter(student -> {
                    // Lấy ngày sinh (giả sử record là student.dob() hoặc class là student.getDob())
                    LocalDate dob = student.birth_date();
                    if (dob == null) return false;
                    // Tính số tuổi chính xác tới ngày hôm nay
                    int age = Period.between(dob, today).getYears();
                    // Nếu minAge có truyền -> kiểm tra age >= minAge; Nếu null -> luôn đúng
                    boolean matchMin = (minAge == null || age >= minAge);
                    // Nếu maxAge có truyền -> kiểm tra age <= maxAge; Nếu null -> luôn đúng
                    boolean matchMax = (maxAge == null || age <= maxAge);
                    return matchMin && matchMax;
                })
                .map(this::toResponse).toList();
                //.map(s-> this.toResponse(s)).toList();
    }

    public Optional<StudentResponse> getById(Long id) {
        return studentRepository.findById(id).map(this::toResponse);
    }

    public StudentResponse create(StudentRequest request) {
        boolean sameEmail = studentRepository.findAll().stream().anyMatch(s -> s.email().equals(request.email()));
        if(sameEmail)  throw new IllegalArgumentException("Email đã tồn tại");
        Student newStudent = new Student(null, request.fullName(), request.email(), request.birthDate(), LocalDateTime.now());
        Student saved = studentRepository.save(newStudent);
        return  toResponse(saved);
    }

    public Optional<StudentResponse> update(Long id, StudentRequest request) {
        Optional<Student> student = studentRepository.findById(id);
        if (student.isPresent()) {
            Student newStudent = new Student(id, request.fullName(), request.email(), request.birthDate(), student.get().created_at());
            Student saved = studentRepository.save(newStudent);
            return Optional.of(toResponse(saved));
        }
        return Optional.empty();
    }


    public boolean deleteById(Long id) {
        return studentRepository.deleteById(id);
    }
}
