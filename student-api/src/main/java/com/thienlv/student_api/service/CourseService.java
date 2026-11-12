package com.thienlv.student_api.service;

import com.thienlv.student_api.dto.CourseAverageScoreResponse;
import com.thienlv.student_api.dto.CourseRequest;
import com.thienlv.student_api.dto.CourseResponse;
import com.thienlv.student_api.dto.ScoreVsAverageResponse;
import com.thienlv.student_api.dto.TopScoreResponse;
import com.thienlv.student_api.entity.Course;
import com.thienlv.student_api.exception.DuplicateCourseNameException;
import com.thienlv.student_api.exception.NotFoundException;
import com.thienlv.student_api.mapper.CourseMapper;
import com.thienlv.student_api.repository.CourseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CourseService {
    private static final Logger log = LoggerFactory.getLogger(CourseService.class);

    private final CourseRepository courseRepository;
    private final CourseMapper courseMapper;

    public CourseService(CourseRepository courseRepository, CourseMapper courseMapper) {
        this.courseRepository = courseRepository;
        this.courseMapper = courseMapper;
    }

    public List<CourseResponse> getAll() {
        return courseRepository.findAll().stream()
                .map(courseMapper::toResponse)
                .toList();
    }

    public CourseResponse getById(Long id) {
        return courseRepository.findById(id)
                .map(courseMapper::toResponse)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy khóa học"));
    }

    public CourseResponse create(CourseRequest request) {
        String name = request.name().trim();
        if (courseRepository.existsByNameIgnoreCase(name)) {
            throw new DuplicateCourseNameException("Tên khóa học đã tồn tại");
        }

        Course newCourse = new Course(null, name, LocalDateTime.now());
        Course saved = courseRepository.save(newCourse);
        log.info("Đã tạo khóa học id={}", saved.getId());
        return courseMapper.toResponse(saved);
    }

    public CourseResponse update(Long id, CourseRequest request) {
        Course existing = courseRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy khóa học"));

        String name = request.name().trim();
        if (courseRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new DuplicateCourseNameException("Tên khóa học đã tồn tại");
        }

        existing.setName(name);
        Course saved = courseRepository.save(existing);
        return courseMapper.toResponse(saved);
    }

    public CourseAverageScoreResponse getAverageScore(Long id) {
        return courseRepository.findAverageScoreByCourseId(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy khóa học"));
    }

    public List<TopScoreResponse> getTopScores(Long id) {
        if (!courseRepository.existsById(id)) throw new NotFoundException("Không tìm thấy khóa học");
        return courseRepository.findTop3ScoresByCourseId(id).stream()
                .map(p -> new TopScoreResponse(p.getStudentId(), p.getStudentName(), p.getScore(), p.getRankNo()))
                .toList();
    }

    public List<ScoreVsAverageResponse> getScoreVsAverage(Long id) {
        if (!courseRepository.existsById(id)) throw new NotFoundException("Không tìm thấy khóa học");
        return courseRepository.findScoreVsAverageByCourseId(id).stream()
                .map(p -> new ScoreVsAverageResponse(p.getStudentId(), p.getStudentName(), p.getScore(), p.getAvgScore(), p.getDiff()))
                .toList();
    }

    public void deleteById(Long id) {
        if (!courseRepository.existsById(id)) throw new NotFoundException("Không tìm thấy khóa học");
        courseRepository.deleteById(id);
        log.info("Đã xóa khóa học id={}", id);
    }
}
