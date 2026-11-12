package com.thienlv.student_api.controller;

import com.thienlv.student_api.dto.StudentRequest;
import com.thienlv.student_api.dto.StudentResponse;
import com.thienlv.student_api.service.StudentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.*;

@RestController
public class StudentController {
    private final StudentService studentService;
    public StudentController(StudentService studentService ){
        this.studentService = studentService;
    }

    @GetMapping("/api/students")
    public List<StudentResponse> getListStudent(@RequestParam(required = false) String name, @RequestParam(required = false) Integer minAge, @RequestParam(required = false) Integer maxAge) {
        return studentService.getAll(name, minAge, maxAge);
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


}





















