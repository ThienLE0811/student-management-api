package com.thienlv.student_api.service;

import com.thienlv.student_api.dto.ClassRequest;
import com.thienlv.student_api.dto.ClassResponse;
import com.thienlv.student_api.entity.SchoolClass;
import com.thienlv.student_api.exception.DuplicateClassNameException;
import com.thienlv.student_api.exception.NotFoundException;
import com.thienlv.student_api.mapper.ClassMapper;
import com.thienlv.student_api.repository.ClassRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClassService {
    private static final Logger log = LoggerFactory.getLogger(ClassService.class);

    private final ClassRepository classRepository;
    private final ClassMapper classMapper;

    public ClassService(ClassRepository classRepository, ClassMapper classMapper) {
        this.classRepository = classRepository;
        this.classMapper = classMapper;
    }

    public List<ClassResponse> getAll() {
        return classRepository.findAll().stream()
                .map(classMapper::toResponse)
                .toList();
    }

    public ClassResponse getById(Long id) {
        return classRepository.findById(id)
                .map(classMapper::toResponse)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy lớp học"));
    }

    public ClassResponse create(ClassRequest request) {
        String name = request.name().trim();
        boolean sameName = classRepository.findAll().stream()
                .anyMatch(c -> c.name().trim().equalsIgnoreCase(name));
        if (sameName) throw new DuplicateClassNameException("Tên lớp đã tồn tại");

        SchoolClass newClass = new SchoolClass(null, name);
        SchoolClass saved = classRepository.save(newClass);
        log.info("Đã tạo lớp học id={}", saved.id());
        return classMapper.toResponse(saved);
    }

    public ClassResponse update(Long id, ClassRequest request) {
        classRepository.findById(id).orElseThrow(() -> new NotFoundException("Không tìm thấy lớp học"));

        String name = request.name().trim();
        boolean sameName = classRepository.findAll().stream()
                .anyMatch(c -> !c.id().equals(id) && c.name().trim().equalsIgnoreCase(name));
        if (sameName) throw new DuplicateClassNameException("Tên lớp đã tồn tại");

        SchoolClass updated = new SchoolClass(id, name);
        SchoolClass saved = classRepository.save(updated);
        return classMapper.toResponse(saved);
    }

    public void deleteById(Long id) {
        boolean isDeleted = classRepository.deleteById(id);
        if (!isDeleted) throw new NotFoundException("Không tìm thấy lớp học");
        log.info("Đã xóa lớp học id={}", id);
    }
}
