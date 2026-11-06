package com.thienlv.student_api_rewrite.service;

import com.thienlv.student_api_rewrite.dto.StudentResponse;
import com.thienlv.student_api_rewrite.entity.Student;
import com.thienlv.student_api_rewrite.repository.StudentRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Component
public class StudentService {

    private final StudentRepository studentRepository;

    public StudentService(StudentRepository studentRepository){
        this.studentRepository = studentRepository;
    }

    // Lỗ hổng: chưa viết được logic các hàm và vẫn phải xem lại
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

    public List<StudentResponse> getAll(String name,Integer minAge,Integer maxAge) {
        List<Student> students =  studentRepository.findAll();

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
        Optional<Student> student = studentRepository.findById(id);
        return student.map(this::toResponse);
    }


}
