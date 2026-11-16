package com.thienlv.student_api.controller;

import com.thienlv.student_api.dto.AddStudentToClassRequest;
import com.thienlv.student_api.dto.ClassRequest;
import com.thienlv.student_api.dto.ClassResponse;
import com.thienlv.student_api.dto.StudentResponse;
import com.thienlv.student_api.exception.ClassFullException;
import com.thienlv.student_api.exception.DuplicateClassNameException;
import com.thienlv.student_api.exception.NotFoundException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import com.thienlv.student_api.service.ClassService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ClassController.class)
class ClassControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ClassService classService;

    @MockitoBean
    private StudentService studentService;

    private ClassResponse sampleResponse(long id) {
        return new ClassResponse(id, "CNTT-K1", 30, 0, List.of());
    }

    @Test
    void getListClass_tra_ve_danh_sach_tu_service() throws Exception {
        given(classService.getAll()).willReturn(List.of(sampleResponse(1L), sampleResponse(2L)));

        mockMvc.perform(get("/api/classes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("CNTT-K1"));
    }

    @Test
    void getDetailClass_tra_ve_200_khi_tim_thay() throws Exception {
        given(classService.getById(1L)).willReturn(sampleResponse(1L));

        mockMvc.perform(get("/api/classes/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("CNTT-K1"));
    }

    @Test
    void getDetailClass_tra_ve_404_khi_khong_tim_thay() throws Exception {
        given(classService.getById(99L)).willThrow(new NotFoundException("Khong tim thay lop hoc"));

        mockMvc.perform(get("/api/classes/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Khong tim thay lop hoc"));
    }

    @Test
    void createClass_tra_ve_201_va_location_header() throws Exception {
        ClassRequest request = new ClassRequest("CNTT-K1", 30);
        given(classService.create(any(ClassRequest.class))).willReturn(sampleResponse(1L));

        mockMvc.perform(post("/api/classes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/api/classes/1")))
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void createClass_tra_ve_400_khi_ten_rong() throws Exception {
        ClassRequest invalid = new ClassRequest("", 30);

        mockMvc.perform(post("/api/classes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists());
    }

    @Test
    void createClass_tra_ve_400_khi_ten_qua_dai() throws Exception {
        String tenQuaDai = "A".repeat(101);
        ClassRequest invalid = new ClassRequest(tenQuaDai, 30);

        mockMvc.perform(post("/api/classes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists());
    }

    @Test
    void createClass_tra_ve_409_khi_trung_ten() throws Exception {
        ClassRequest request = new ClassRequest("CNTT-K1", 30);
        given(classService.create(any(ClassRequest.class)))
                .willThrow(new DuplicateClassNameException("Ten lop da ton tai"));

        mockMvc.perform(post("/api/classes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Ten lop da ton tai"));
    }

    @Test
    void updateClass_tra_ve_409_khi_trung_ten_lop_khac() throws Exception {
        ClassRequest request = new ClassRequest("CNTT-K2", 30);
        given(classService.update(eq(1L), any(ClassRequest.class)))
                .willThrow(new DuplicateClassNameException("Ten lop da ton tai"));

        mockMvc.perform(put("/api/classes/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Ten lop da ton tai"));
    }

    @Test
    void updateClass_tra_ve_200_voi_du_lieu_moi() throws Exception {
        ClassRequest request = new ClassRequest("CNTT-K2", 30);
        given(classService.update(anyLong(), any(ClassRequest.class)))
                .willReturn(new ClassResponse(1L, "CNTT-K2", 30, 0, List.of()));

        mockMvc.perform(put("/api/classes/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("CNTT-K2"));
    }

    @Test
    void updateClass_tra_ve_404_khi_khong_tim_thay() throws Exception {
        ClassRequest request = new ClassRequest("CNTT-K2", 30);
        given(classService.update(eq(99L), any(ClassRequest.class)))
                .willThrow(new NotFoundException("Khong tim thay lop hoc"));

        mockMvc.perform(put("/api/classes/{id}", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteById_tra_ve_204_khi_xoa_thanh_cong() throws Exception {
        mockMvc.perform(delete("/api/classes/{id}", 1))
                .andExpect(status().isNoContent());

        verify(classService).deleteById(1L);
    }

    @Test
    void deleteById_tra_ve_404_khi_khong_tim_thay() throws Exception {
        doThrow(new NotFoundException("Khong tim thay lop hoc"))
                .when(classService).deleteById(99L);

        mockMvc.perform(delete("/api/classes/{id}", 99L))
                .andExpect(status().isNotFound());
    }

    @Test
    void getStudentsByClass_tra_ve_danh_sach_sinh_vien_cua_lop() throws Exception {
        StudentResponse student = new StudentResponse(1L, "Nguyen Van A", "a@gmail.com",
                LocalDate.of(2004, 3, 15), 21, LocalDateTime.of(2024, 1, 10, 8, 30), 1L);
        given(studentService.getByClassId(1L)).willReturn(List.of(student));

        mockMvc.perform(get("/api/classes/{id}/students", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].classId").value(1));
    }

    @Test
    void getStudentsByClass_tra_ve_404_khi_khong_tim_thay_lop() throws Exception {
        given(studentService.getByClassId(99L)).willThrow(new NotFoundException("Khong tim thay lop hoc"));

        mockMvc.perform(get("/api/classes/{id}/students", 99L))
                .andExpect(status().isNotFound());
    }

    @Test
    void addStudentToClass_tra_ve_200_khi_con_cho() throws Exception {
        StudentResponse response = new StudentResponse(1L, "Nguyen Van A", "a@gmail.com",
                LocalDate.of(2004, 3, 15), 21, LocalDateTime.of(2024, 1, 10, 8, 30), 1L);
        given(studentService.addToClass(1L, 1L)).willReturn(response);

        mockMvc.perform(post("/api/classes/{id}/students", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddStudentToClassRequest(1L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.classId").value(1));
    }

    @Test
    void addStudentToClass_tra_ve_404_khi_khong_tim_thay_lop() throws Exception {
        given(studentService.addToClass(99L, 1L)).willThrow(new NotFoundException("Khong tim thay lop hoc"));

        mockMvc.perform(post("/api/classes/{id}/students", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddStudentToClassRequest(1L))))
                .andExpect(status().isNotFound());
    }

    @Test
    void addStudentToClass_tra_ve_404_khi_khong_tim_thay_sinh_vien() throws Exception {
        given(studentService.addToClass(1L, 99L)).willThrow(new NotFoundException("Khong tim thay nguoi dung"));

        mockMvc.perform(post("/api/classes/{id}/students", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddStudentToClassRequest(99L))))
                .andExpect(status().isNotFound());
    }

    @Test
    void addStudentToClass_tra_ve_409_khi_lop_het_cho() throws Exception {
        given(studentService.addToClass(1L, 1L)).willThrow(new ClassFullException("Lop da het cho"));

        mockMvc.perform(post("/api/classes/{id}/students", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddStudentToClassRequest(1L))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Lop da het cho"));
    }

    @Test
    void addStudentToClass_tra_ve_409_khi_co_xung_dot_optimistic_lock() throws Exception {
        given(studentService.addToClass(1L, 1L))
                .willThrow(new ObjectOptimisticLockingFailureException(com.thienlv.student_api.entity.SchoolClass.class, 1L));

        mockMvc.perform(post("/api/classes/{id}/students", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddStudentToClassRequest(1L))))
                .andExpect(status().isConflict());
    }

    @Test
    void addStudentToClass_tra_ve_400_khi_thieu_studentId() throws Exception {
        mockMvc.perform(post("/api/classes/{id}/students", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddStudentToClassRequest(null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.studentId").exists());
    }
}
