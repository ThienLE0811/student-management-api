package com.thienlv.student_api.controller;

import com.thienlv.student_api.dto.CourseAverageScoreResponse;
import com.thienlv.student_api.dto.ScoreVsAverageResponse;
import com.thienlv.student_api.dto.TopScoreResponse;
import com.thienlv.student_api.exception.NotFoundException;
import com.thienlv.student_api.service.CourseService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// @WebMvcTest chi load tang web (Controller + @RestControllerAdvice), CourseService mock bang @MockitoBean.
// Chi test 3 endpoint moi them o buoi 17 (average-score, top-scores, score-vs-average) -
// CRUD co san cua CourseController chua co test tu truoc, khong thuoc pham vi buoi nay.
@WebMvcTest(CourseController.class)
class CourseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CourseService courseService;

    @Test
    void getAverageScore_tra_ve_200_voi_diem_trung_binh() throws Exception {
        given(courseService.getAverageScore(1L))
                .willReturn(new CourseAverageScoreResponse(1L, "Mon hoc 1", 7.5));

        mockMvc.perform(get("/api/courses/{id}/average-score", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courseId").value(1))
                .andExpect(jsonPath("$.courseName").value("Mon hoc 1"))
                .andExpect(jsonPath("$.averageScore").value(7.5));
    }

    @Test
    void getAverageScore_tra_ve_null_khi_khoa_hoc_chua_ai_co_diem() throws Exception {
        // Khoa hoc ton tai nhung chua co enrollment nao -> AVG() tra NULL (xem buoi 17 LEFT JOIN)
        given(courseService.getAverageScore(1L))
                .willReturn(new CourseAverageScoreResponse(1L, "Mon hoc 1", null));

        mockMvc.perform(get("/api/courses/{id}/average-score", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageScore").doesNotExist());
    }

    @Test
    void getAverageScore_tra_ve_404_khi_khong_tim_thay_khoa_hoc() throws Exception {
        given(courseService.getAverageScore(99L))
                .willThrow(new NotFoundException("Khong tim thay khoa hoc"));

        mockMvc.perform(get("/api/courses/{id}/average-score", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Khong tim thay khoa hoc"));
    }

    @Test
    void getTopScores_tra_ve_200_voi_danh_sach_xep_hang() throws Exception {
        given(courseService.getTopScores(1L)).willReturn(List.of(
                new TopScoreResponse(10L, "Nguyen Van A", 9.5, 1L),
                new TopScoreResponse(11L, "Tran Thi B", 9.0, 2L),
                new TopScoreResponse(12L, "Le Van C", 8.5, 3L)
        ));

        mockMvc.perform(get("/api/courses/{id}/top-scores", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].studentId").value(10))
                .andExpect(jsonPath("$[0].rankNo").value(1))
                .andExpect(jsonPath("$[2].rankNo").value(3));
    }

    @Test
    void getTopScores_tra_ve_danh_sach_rong_khi_khoa_hoc_chua_ai_dang_ky() throws Exception {
        given(courseService.getTopScores(1L)).willReturn(List.of());

        mockMvc.perform(get("/api/courses/{id}/top-scores", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getTopScores_tra_ve_404_khi_khong_tim_thay_khoa_hoc() throws Exception {
        given(courseService.getTopScores(99L))
                .willThrow(new NotFoundException("Khong tim thay khoa hoc"));

        mockMvc.perform(get("/api/courses/{id}/top-scores", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Khong tim thay khoa hoc"));
    }

    @Test
    void getScoreVsAverage_tra_ve_200_voi_chenh_lech_tung_sinh_vien() throws Exception {
        given(courseService.getScoreVsAverage(1L)).willReturn(List.of(
                new ScoreVsAverageResponse(10L, "Nguyen Van A", 9.0, 7.0, 2.0),
                new ScoreVsAverageResponse(11L, "Tran Thi B", 5.0, 7.0, -2.0)
        ));

        mockMvc.perform(get("/api/courses/{id}/score-vs-average", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].avgScore").value(7.0))
                .andExpect(jsonPath("$[0].diff").value(2.0))
                .andExpect(jsonPath("$[1].diff").value(-2.0));
    }

    @Test
    void getScoreVsAverage_tra_ve_404_khi_khong_tim_thay_khoa_hoc() throws Exception {
        given(courseService.getScoreVsAverage(99L))
                .willThrow(new NotFoundException("Khong tim thay khoa hoc"));

        mockMvc.perform(get("/api/courses/{id}/score-vs-average", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Khong tim thay khoa hoc"));
    }
}
