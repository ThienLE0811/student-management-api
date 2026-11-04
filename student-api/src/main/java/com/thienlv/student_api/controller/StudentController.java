package com.thienlv.student_api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;
import java.util.Objects;

@RestController
public class StudentController {
    private static final List<Student> STUDENTS = List.of(
            new Student(1L, "Nguyễn Văn An", "an.nguyenvan@gmail.com", LocalDate.of(2004, 3, 15), LocalDateTime.of(2024, 1, 10, 8, 30, 0)),
            new Student(2L, "Trần Thị Bích", "bich.tran@gmail.com", LocalDate.of(2004, 7, 22), LocalDateTime.of(2024, 1, 10, 9, 15, 0)),
            new Student(3L, "Lê Hoàng Cường", "cuong.le@gmail.com", LocalDate.of(2003, 11, 5), LocalDateTime.of(2024, 1, 11, 10, 0, 0)),
            new Student(4L, "Phạm Minh Đức", "duc.pham@gmail.com", LocalDate.of(2004, 1, 18), LocalDateTime.of(2024, 1, 11, 14, 20, 0)),
            new Student(5L, "Hoàng Thu Giang", "giang.hoang@gmail.com", LocalDate.of(2004, 9, 30), LocalDateTime.of(2024, 1, 12, 11, 45, 0)),
            new Student(6L, "Vũ Hải Nam", "nam.vu@gmail.com", LocalDate.of(2003, 5, 12), LocalDateTime.of(2024, 1, 12, 16, 10, 0)),
            new Student(7L, "Đặng Ngọc Mai", "mai.dang@gmail.com", LocalDate.of(2004, 12, 8), LocalDateTime.of(2024, 1, 13, 8, 0, 0)),
            new Student(8L, "Bùi Quốc Huy", "huy.bui@gmail.com", LocalDate.of(2004, 4, 25), LocalDateTime.of(2024, 1, 13, 13, 30, 0)),
            new Student(9L, "Đỗ Phương Linh", "linh.do@gmail.com", LocalDate.of(2003, 8, 19), LocalDateTime.of(2024, 1, 14, 15, 50, 0)),
            new Student(10L, "Ngô Thành Long", "long.ngo@gmail.com", LocalDate.of(2004, 6, 2), LocalDateTime.of(2024, 1, 14, 17, 0, 0))
    );


    @GetMapping("/api/students")
    public List<Student> getListStudent(@RequestParam(required = false) String name, @RequestParam(required = false) Integer minAge, @RequestParam(required = false) Integer maxAge) {
        LocalDate today = LocalDate.now();

        return STUDENTS.stream()
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
                .toList();
    }


    @GetMapping("/api/students/{id}")
    public Student getDetailStudent(@PathVariable Long id) {

        return STUDENTS.stream().filter(student -> Objects.equals(student.id(), id)).findFirst().orElseThrow();
    }
}
