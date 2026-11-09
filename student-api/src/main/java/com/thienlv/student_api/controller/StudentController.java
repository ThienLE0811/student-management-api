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
    public ResponseEntity<StudentResponse> getDetailStudent(@PathVariable Long id) {
        Optional<StudentResponse> student = studentService.getById(id);

        return ResponseEntity.of(student);
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
    public ResponseEntity<StudentResponse> updateStudent(@PathVariable Long id, @Valid @RequestBody StudentRequest request) {
        Optional<StudentResponse> response = studentService.update(id, request);
        return response.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());

//        if(response.isEmpty()) return ResponseEntity.notFound().build();
//
//        return ResponseEntity.ok(response.get());
    }

    @DeleteMapping("/api/students/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable Long id) {
        boolean isDeleted = studentService.deleteById(id);
        if (isDeleted) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }


}





















