package com.thienlv.student_api_rewrite.controller;

import com.thienlv.student_api_rewrite.dto.StudentResponse;
import com.thienlv.student_api_rewrite.service.StudentService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService){
        this.studentService = studentService;
    }


    @GetMapping("/api/students")
    public List<StudentResponse> getListStudent(@RequestParam(required = false) String name, @RequestParam(required = false) Integer minAge, @RequestParam(required = false) Integer maxAge) {
        return studentService.getAll(name, minAge, maxAge);
    }

    @GetMapping("/api/students/{id}")
    public List<StudentResponse> getDetailStudent(@RequestParam(required = false) String name, @RequestParam(required = false) Integer minAge, @RequestParam(required = false) Integer maxAge) {
        return studentService.getAll(name, minAge, maxAge);
    }

    @PostMapping("/api/students")
    public List<StudentResponse> createStudent(@RequestParam(required = false) String name, @RequestParam(required = false) Integer minAge, @RequestParam(required = false) Integer maxAge) {
        return studentService.getAll(name, minAge, maxAge);
    }

    @PutMapping("/api/students/{id}")
    public List<StudentResponse> updateStudent(@RequestParam(required = false) String name, @RequestParam(required = false) Integer minAge, @RequestParam(required = false) Integer maxAge) {
        return studentService.getAll(name, minAge, maxAge);
    }

    @DeleteMapping("/api/students/{id}")
    public List<StudentResponse> deleteStudent(@RequestParam(required = false) String name, @RequestParam(required = false) Integer minAge, @RequestParam(required = false) Integer maxAge) {
        return studentService.getAll(name, minAge, maxAge);
    }
}
