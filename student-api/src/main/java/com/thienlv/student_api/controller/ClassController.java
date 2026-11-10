package com.thienlv.student_api.controller;

import com.thienlv.student_api.dto.ClassRequest;
import com.thienlv.student_api.dto.ClassResponse;
import com.thienlv.student_api.service.ClassService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
public class ClassController {
    private final ClassService classService;

    public ClassController(ClassService classService) {
        this.classService = classService;
    }

    @GetMapping("/api/classes")
    public List<ClassResponse> getListClass() {
        return classService.getAll();
    }

    @GetMapping("/api/classes/{id}")
    public ClassResponse getDetailClass(@PathVariable Long id) {
        return classService.getById(id);
    }

    @PostMapping("/api/classes")
    public ResponseEntity<ClassResponse> createClass(@Valid @RequestBody ClassRequest request) {
        ClassResponse response = classService.create(request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/api/classes/{id}")
    public ClassResponse updateClass(@PathVariable Long id, @Valid @RequestBody ClassRequest request) {
        return classService.update(id, request);
    }

    @DeleteMapping("/api/classes/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable Long id) {
        classService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
