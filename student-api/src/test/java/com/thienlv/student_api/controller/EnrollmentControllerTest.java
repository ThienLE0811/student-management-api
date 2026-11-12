package com.thienlv.student_api.controller;

import com.thienlv.student_api.dto.EnrollmentRequest;
import com.thienlv.student_api.dto.EnrollmentResponse;
import com.thienlv.student_api.exception.DuplicateEnrollmentException;
import com.thienlv.student_api.exception.NotFoundException;
import com.thienlv.student_api.service.EnrollmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import tools.jackson.databind.ObjectMapper;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// @WebMvcTest chi load tang web (Controller + @RestControllerAdvice), EnrollmentService mock bang @MockitoBean
@WebMvcTest(EnrollmentController.class)
class EnrollmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EnrollmentService enrollmentService;

    private EnrollmentResponse sampleResponse(long id) {
        return new EnrollmentResponse(id, 1L, 2L, null, LocalDateTime.of(2024, 1, 10, 8, 30));
    }

    @Test
    void createEnrollment_tra_ve_201_va_location_header() throws Exception {
        EnrollmentRequest request = new EnrollmentRequest(1L, 2L, null);
        given(enrollmentService.create(any(EnrollmentRequest.class))).willReturn(sampleResponse(1L));

        mockMvc.perform(post("/api/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/api/enrollments/1")))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.studentId").value(1))
                .andExpect(jsonPath("$.courseId").value(2));
    }

    @Test
    void createEnrollment_tra_ve_400_khi_thieu_studentId() throws Exception {
        // studentId null -> vi pham @NotNull
        EnrollmentRequest invalid = new EnrollmentRequest(null, 2L, null);

        mockMvc.perform(post("/api/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.studentId").exists());
    }

    @Test
    void createEnrollment_tra_ve_400_khi_thieu_courseId() throws Exception {
        // courseId null -> vi pham @NotNull
        EnrollmentRequest invalid = new EnrollmentRequest(1L, null, null);

        mockMvc.perform(post("/api/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.courseId").exists());
    }

    @Test
    void createEnrollment_tra_ve_400_khi_score_ngoai_khoang_0_10() throws Exception {
        // score > 10 -> vi pham @DecimalMax
        EnrollmentRequest invalid = new EnrollmentRequest(1L, 2L, 10.5);

        mockMvc.perform(post("/api/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.score").exists());
    }

    @Test
    void createEnrollment_tra_ve_404_khi_khong_tim_thay_sinh_vien() throws Exception {
        EnrollmentRequest request = new EnrollmentRequest(99L, 2L, null);
        given(enrollmentService.create(any(EnrollmentRequest.class)))
                .willThrow(new NotFoundException("Khong tim thay sinh vien"));

        mockMvc.perform(post("/api/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Khong tim thay sinh vien"));
    }

    @Test
    void createEnrollment_tra_ve_404_khi_khong_tim_thay_khoa_hoc() throws Exception {
        EnrollmentRequest request = new EnrollmentRequest(1L, 99L, null);
        given(enrollmentService.create(any(EnrollmentRequest.class)))
                .willThrow(new NotFoundException("Khong tim thay khoa hoc"));

        mockMvc.perform(post("/api/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Khong tim thay khoa hoc"));
    }

    @Test
    void createEnrollment_tra_ve_409_khi_dang_ky_trung() throws Exception {
        EnrollmentRequest request = new EnrollmentRequest(1L, 2L, null);
        given(enrollmentService.create(any(EnrollmentRequest.class)))
                .willThrow(new DuplicateEnrollmentException("Sinh vien da dang ky khoa hoc nay"));

        mockMvc.perform(post("/api/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Sinh vien da dang ky khoa hoc nay"));
    }
}
