package com.thienlv.student_api.controller;

import com.thienlv.student_api.dto.StudentCourseResponse;
import com.thienlv.student_api.dto.StudentRequest;
import com.thienlv.student_api.dto.StudentResponse;
import com.thienlv.student_api.service.EnrollmentService;
import com.thienlv.student_api.service.StudentService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.*;

@RestController
public class StudentController {
    private final StudentService studentService;
    private final EnrollmentService enrollmentService;

    public StudentController(StudentService studentService, EnrollmentService enrollmentService){
        this.studentService = studentService;
        this.enrollmentService = enrollmentService;
    }

    @GetMapping("/api/students")
    public Page<StudentResponse> getListStudent(@RequestParam(required = false) String name,
                                                 @RequestParam(required = false) Integer minAge,
                                                 @RequestParam(required = false) Integer maxAge,
                                                 @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return studentService.getAll(name, minAge, maxAge, pageable);
    }


    @GetMapping("/api/students/{id}")
    public StudentResponse getDetailStudent(@PathVariable Long id) {
        return studentService.getById(id);
    }

    @GetMapping("/api/students/without-course")
    public List<StudentResponse> getStudentsWithoutCourse() {
        return studentService.getWithoutCourse();
    }

    @PostMapping("/api/students")
    public ResponseEntity<StudentResponse > createStudent(@Valid @RequestBody StudentRequest request) {
        StudentResponse response = studentService.create(request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()   // lấy URI hiện tại, tức /api/students
                .path("/{id}")          // nối thêm /{id}
                .buildAndExpand(response.id())  // thay {id}
                .toUri();               // ra kiểu java.net.URI
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/api/students/{id}")
    public StudentResponse updateStudent(@PathVariable Long id, @Valid @RequestBody StudentRequest request) {
        return studentService.update(id, request);
    }

    @DeleteMapping("/api/students/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable Long id) {
        studentService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/api/students/{id}/class/{classId}")
    public StudentResponse transferClass(@PathVariable Long id, @PathVariable Long classId) {
        return studentService.transferClass(id, classId);
    }

    @GetMapping("/api/students/{id}/courses")
    public List<StudentCourseResponse> getCoursesOfStudent(@PathVariable Long id) {
        return enrollmentService.getCoursesByStudentId(id);
    }


}





















