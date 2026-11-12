package com.thienlv.student_api.controller;

import com.thienlv.student_api.dto.CourseAverageScoreResponse;
import com.thienlv.student_api.dto.CourseRequest;
import com.thienlv.student_api.dto.CourseResponse;
import com.thienlv.student_api.dto.ScoreVsAverageResponse;
import com.thienlv.student_api.dto.TopScoreResponse;
import com.thienlv.student_api.service.CourseService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
public class CourseController {
    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping("/api/courses")
    public List<CourseResponse> getListCourse() {
        return courseService.getAll();
    }

    @GetMapping("/api/courses/{id}")
    public CourseResponse getDetailCourse(@PathVariable Long id) {
        return courseService.getById(id);
    }

    @GetMapping("/api/courses/{id}/average-score")
    public CourseAverageScoreResponse getAverageScore(@PathVariable Long id) {
        return courseService.getAverageScore(id);
    }

    @GetMapping("/api/courses/{id}/top-scores")
    public List<TopScoreResponse> getTopScores(@PathVariable Long id) {
        return courseService.getTopScores(id);
    }

    @GetMapping("/api/courses/{id}/score-vs-average")
    public List<ScoreVsAverageResponse> getScoreVsAverage(@PathVariable Long id) {
        return courseService.getScoreVsAverage(id);
    }

    @PostMapping("/api/courses")
    public ResponseEntity<CourseResponse> createCourse(@Valid @RequestBody CourseRequest request) {
        CourseResponse response = courseService.create(request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/api/courses/{id}")
    public CourseResponse updateCourse(@PathVariable Long id, @Valid @RequestBody CourseRequest request) {
        return courseService.update(id, request);
    }

    @DeleteMapping("/api/courses/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable Long id) {
        courseService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
