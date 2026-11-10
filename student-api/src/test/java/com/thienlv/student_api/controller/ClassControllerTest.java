package com.thienlv.student_api.controller;

import com.thienlv.student_api.dto.ClassRequest;
import com.thienlv.student_api.dto.ClassResponse;
import com.thienlv.student_api.exception.DuplicateClassNameException;
import com.thienlv.student_api.exception.NotFoundException;
import com.thienlv.student_api.service.ClassService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import tools.jackson.databind.ObjectMapper;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

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

    private ClassResponse sampleResponse(long id) {
        return new ClassResponse(id, "CNTT-K1");
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
        ClassRequest request = new ClassRequest("CNTT-K1");
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
        ClassRequest invalid = new ClassRequest("");

        mockMvc.perform(post("/api/classes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists());
    }

    @Test
    void createClass_tra_ve_400_khi_ten_qua_dai() throws Exception {
        String tenQuaDai = "A".repeat(101);
        ClassRequest invalid = new ClassRequest(tenQuaDai);

        mockMvc.perform(post("/api/classes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists());
    }

    @Test
    void createClass_tra_ve_409_khi_trung_ten() throws Exception {
        ClassRequest request = new ClassRequest("CNTT-K1");
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
        ClassRequest request = new ClassRequest("CNTT-K2");
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
        ClassRequest request = new ClassRequest("CNTT-K2");
        given(classService.update(anyLong(), any(ClassRequest.class)))
                .willReturn(new ClassResponse(1L, "CNTT-K2"));

        mockMvc.perform(put("/api/classes/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("CNTT-K2"));
    }

    @Test
    void updateClass_tra_ve_404_khi_khong_tim_thay() throws Exception {
        ClassRequest request = new ClassRequest("CNTT-K2");
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
}
