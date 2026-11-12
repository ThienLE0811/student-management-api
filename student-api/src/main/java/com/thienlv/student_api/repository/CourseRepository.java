package com.thienlv.student_api.repository;

import com.thienlv.student_api.dto.CourseAverageScoreResponse;
import com.thienlv.student_api.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    @Query("SELECT new com.thienlv.student_api.dto.CourseAverageScoreResponse(c.id, c.name, AVG(e.score)) " +
            "FROM Course c LEFT JOIN Enrollment e ON e.course = c " +
            "WHERE c.id = :courseId " +
            "GROUP BY c.id, c.name")
    Optional<CourseAverageScoreResponse> findAverageScoreByCourseId(@Param("courseId") Long courseId);

    // Window function (ROW_NUMBER) không có trong JPQL -> dùng native query.
    @Query(value = "SELECT student_id, student_name, score, rank_no FROM (" +
            "    SELECT e.student_id AS student_id, s.full_name AS student_name, e.score AS score, " +
            "           ROW_NUMBER() OVER (ORDER BY e.score DESC) AS rank_no " +
            "    FROM enrollments e " +
            "    JOIN students s ON s.id = e.student_id " +
            "    WHERE e.course_id = :courseId" +
            ") ranked " +
            "WHERE rank_no <= 3 " +
            "ORDER BY rank_no",
            nativeQuery = true)
    List<TopScoreProjection> findTop3ScoresByCourseId(@Param("courseId") Long courseId);

    // AVG(...) OVER (...) không có trong JPQL -> dùng native query. Đã lọc
    // theo 1 course_id nên AVG() OVER () (không PARTITION BY) là đủ — toàn
    // bộ dòng trong kết quả vốn đã cùng 1 khóa.
    @Query(value = "SELECT e.student_id AS student_id, s.full_name AS student_name, e.score AS score, " +
            "       AVG(e.score) OVER () AS avg_score, " +
            "       e.score - AVG(e.score) OVER () AS diff " +
            "FROM enrollments e " +
            "JOIN students s ON s.id = e.student_id " +
            "WHERE e.course_id = :courseId " +
            "ORDER BY e.student_id",
            nativeQuery = true)
    List<ScoreVsAverageProjection> findScoreVsAverageByCourseId(@Param("courseId") Long courseId);
}
