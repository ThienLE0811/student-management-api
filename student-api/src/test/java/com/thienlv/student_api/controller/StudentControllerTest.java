package com.thienlv.student_api.controller;

import com.thienlv.student_api.dto.StudentRequest;
import com.thienlv.student_api.dto.StudentResponse;
import com.thienlv.student_api.exception.DuplicateEmailException;
import com.thienlv.student_api.exception.NotFoundException;
import com.thienlv.student_api.service.StudentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import tools.jackson.databind.ObjectMapper;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// @WebMvcTest chỉ load tầng web (Controller + @RestControllerAdvice + Filter),
// không load Service/Repository thật -> StudentService phải mock bằng @MockitoBean
@WebMvcTest(StudentController.class)
class StudentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private StudentService studentService;

    private StudentResponse sampleResponse(long id) {
        return new StudentResponse(id, "Nguyen Van A", "a@gmail.com",
                LocalDate.of(2004, 3, 15), 21, LocalDateTime.of(2024, 1, 10, 8, 30));
    }

    @Test
    void getListStudent_tra_ve_danh_sach_tu_service() throws Exception {
        given(studentService.getAll(null, null, null))
                .willReturn(List.of(sampleResponse(1L), sampleResponse(2L)));

        mockMvc.perform(get("/api/students"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].email").value("a@gmail.com"));
    }

    @Test
    void getListStudent_truyen_query_param_xuong_service() throws Exception {
        given(studentService.getAll("An", 18, 25))
                .willReturn(List.of(sampleResponse(1L)));

        mockMvc.perform(get("/api/students")
                        .param("name", "An")
                        .param("minAge", "18")
                        .param("maxAge", "25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getDetailStudent_tra_ve_200_khi_tim_thay() throws Exception {
        given(studentService.getById(1L)).willReturn(sampleResponse(1L));

        mockMvc.perform(get("/api/students/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.fullName").value("Nguyen Van A"));
    }

    @Test
    void getDetailStudent_tra_ve_404_khi_khong_tim_thay() throws Exception {
        given(studentService.getById(99L)).willThrow(new NotFoundException("Khong tim thay nguoi dung"));

        mockMvc.perform(get("/api/students/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Khong tim thay nguoi dung"));
    }

    @Test
    void createStudent_tra_ve_201_va_location_header() throws Exception {
        StudentRequest request = new StudentRequest("Nguyen Van A", "a@gmail.com", LocalDate.of(2004, 3, 15));
        given(studentService.create(any(StudentRequest.class))).willReturn(sampleResponse(1L));

        mockMvc.perform(post("/api/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/api/students/1")))
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void createStudent_tra_ve_400_khi_validation_fail() throws Exception {
        // fullName blank, email sai dinh dang -> vi pham @NotBlank/@Email
        StudentRequest invalid = new StudentRequest("", "khong-phai-email", LocalDate.of(2004, 3, 15));

        mockMvc.perform(post("/api/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.fullName").exists())
                .andExpect(jsonPath("$.errors.email").exists());
    }

    @Test
    void createStudent_tra_ve_400_khi_ten_qua_dai() throws Exception {
        // fullName > 100 ky tu -> vi pham @Size(max = 100)
        String tenQuaDai = "A".repeat(101);
        StudentRequest invalid = new StudentRequest(tenQuaDai, "a@gmail.com", LocalDate.of(2004, 3, 15));

        mockMvc.perform(post("/api/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.fullName").exists());
    }

    @Test
    void createStudent_tra_ve_400_khi_ngay_sinh_trong_tuong_lai() throws Exception {
        // birthDate trong tuong lai -> vi pham @Past
        StudentRequest invalid = new StudentRequest("Nguyen Van A", "a@gmail.com", LocalDate.now().plusDays(1));

        mockMvc.perform(post("/api/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.birthDate").exists());
    }

    @Test
    void createStudent_sai_dinh_dang_ngay_sinh_hien_tai_tra_ve_500() throws Exception {
        // Luu y: day la hanh vi HIEN TAI (bug), khong phai hanh vi mong muon.
        // Loi parse JSON (HttpMessageNotReadableException) khong khop handler nao
        // ngoai @ExceptionHandler(Exception.class) -> roi vao nhanh 500 thay vi 400.
        String jsonSaiDinhDang = """
                {"fullName":"Nguyen Van A","email":"a@gmail.com","birthDate":"khong-phai-ngay"}
                """;

        mockMvc.perform(post("/api/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonSaiDinhDang))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void createStudent_tra_ve_409_khi_trung_email() throws Exception {
        StudentRequest request = new StudentRequest("Nguyen Van A", "a@gmail.com", LocalDate.of(2004, 3, 15));
        given(studentService.create(any(StudentRequest.class)))
                .willThrow(new DuplicateEmailException("Email da ton tai"));

        mockMvc.perform(post("/api/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Email da ton tai"));
    }

    @Test
    void updateStudent_tra_ve_200_voi_du_lieu_moi() throws Exception {
        StudentRequest request = new StudentRequest("Nguyen Van B", "b@gmail.com", LocalDate.of(2004, 3, 15));
        given(studentService.update(anyLong(), any(StudentRequest.class)))
                .willReturn(new StudentResponse(1L, "Nguyen Van B", "b@gmail.com",
                        LocalDate.of(2004, 3, 15), 21, LocalDateTime.of(2024, 1, 10, 8, 30)));

        mockMvc.perform(put("/api/students/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Nguyen Van B"));
    }

    @Test
    void updateStudent_tra_ve_404_khi_khong_tim_thay() throws Exception {
        StudentRequest request = new StudentRequest("Nguyen Van B", "b@gmail.com", LocalDate.of(2004, 3, 15));
        given(studentService.update(org.mockito.ArgumentMatchers.eq(99L), any(StudentRequest.class)))
                .willThrow(new NotFoundException("Khong tim thay nguoi dung"));

        mockMvc.perform(put("/api/students/{id}", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteById_tra_ve_204_khi_xoa_thanh_cong() throws Exception {
        mockMvc.perform(delete("/api/students/{id}", 1))
                .andExpect(status().isNoContent());

        verify(studentService).deleteById(1L);
    }

    @Test
    void deleteById_tra_ve_404_khi_khong_tim_thay() throws Exception {
        org.mockito.Mockito.doThrow(new NotFoundException("Khong tim thay nguoi dung"))
                .when(studentService).deleteById(99L);

        mockMvc.perform(delete("/api/students/{id}", 99L))
                .andExpect(status().isNotFound());
    }
}
