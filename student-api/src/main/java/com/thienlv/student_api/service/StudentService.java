package com.thienlv.student_api.service;

import com.thienlv.student_api.dto.StudentRequest;
import com.thienlv.student_api.dto.StudentResponse;
import com.thienlv.student_api.entity.SchoolClass;
import com.thienlv.student_api.entity.Student;
import com.thienlv.student_api.exception.DuplicateEmailException;
import com.thienlv.student_api.exception.NotFoundException;
import com.thienlv.student_api.mapper.StudentMapper;
import com.thienlv.student_api.repository.ClassRepository;
import com.thienlv.student_api.repository.StudentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
public class StudentService {
    private static final Logger log = LoggerFactory.getLogger(StudentService.class);
    private static final int MAX_PAGE_SIZE = 100;

    private final StudentRepository studentRepository;
    private final StudentMapper studentMapper;
    private final ClassRepository classRepository;

    public StudentService(StudentRepository studentRepository, StudentMapper studentMapper, ClassRepository classRepository){
        this.studentRepository = studentRepository;
        this.studentMapper = studentMapper;
        this.classRepository = classRepository;
    }

    public Page<StudentResponse> getAll(String name, Integer minAge, Integer maxAge, Pageable pageable) {
        List<Student> students = studentRepository.findAll();
        LocalDate today = LocalDate.now();

        List<Student> filtered = students.stream()
                .filter(student -> name == null || Objects.equals(student.getFull_name(), name))
                .filter(student -> {
                    LocalDate dob = student.getBirth_date();
                    if (dob == null) return false;
                    // Tính số tuổi chính xác tới ngày hôm nay
                    int age = Period.between(dob, today).getYears();
                    // Nếu minAge có truyền -> kiểm tra age >= minAge; Nếu null -> luôn đúng
                    boolean matchMin = (minAge == null || age >= minAge);
                    // Nếu maxAge có truyền -> kiểm tra age <= maxAge; Nếu null -> luôn đúng
                    boolean matchMax = (maxAge == null || age <= maxAge);
                    return matchMin && matchMax;
                })
                .toList();

        Pageable limitedPageable = limitPageSize(pageable);
        List<Student> sorted = sortInMemory(filtered, limitedPageable.getSort());

        int total = sorted.size();
        int fromIndex = Math.min((int) limitedPageable.getOffset(), total);
        int toIndex = Math.min(fromIndex + limitedPageable.getPageSize(), total);
        List<Student> pageContent = sorted.subList(fromIndex, toIndex);

        Page<Student> studentPage = new PageImpl<>(pageContent, limitedPageable, total);
        return studentPage.map(studentMapper::toResponse);
    }

    private Pageable limitPageSize(Pageable pageable) {
        if (pageable.getPageSize() <= MAX_PAGE_SIZE) {
            return pageable;
        }
        return PageRequest.of(pageable.getPageNumber(), MAX_PAGE_SIZE, pageable.getSort());
    }

    private List<Student> sortInMemory(List<Student> students, Sort sort) {
        if (sort.isUnsorted()) {
            return students;
        }

        Comparator<Student> comparator = null;
        for (Sort.Order order : sort) {
            Comparator<Student> orderComparator = Comparator.comparing(student ->
                    (Comparable) new BeanWrapperImpl(student).getPropertyValue(order.getProperty()));
            if (order.isDescending()) {
                orderComparator = orderComparator.reversed();
            }
            comparator = (comparator == null) ? orderComparator : comparator.thenComparing(orderComparator);
        }

        return students.stream().sorted(comparator).toList();
    }

    public StudentResponse getById(Long id) {
        return studentRepository.findById(id).map(studentMapper::toResponse).orElseThrow(()-> new NotFoundException("Không tìm thấy người dùng"));
    }

    public StudentResponse create(StudentRequest request) {
        boolean sameEmail = studentRepository.findAll().stream().anyMatch(s -> s.getEmail().equals(request.email()));
        if(sameEmail)  throw new DuplicateEmailException("Email đã tồn tại");

        SchoolClass schoolClass = classRepository.findById(request.classId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy lớp học"));

        Student newStudent = new Student(null, request.fullName(), request.email(), request.birthDate(), LocalDateTime.now(), schoolClass);
        Student saved = studentRepository.save(newStudent);
        log.info("Đã tạo sinh viên id={}", saved.getId());
        return studentMapper.toResponse(saved);
    }

    public StudentResponse update(Long id, StudentRequest request) {
        Student student = studentRepository.findById(id).orElseThrow(()-> new NotFoundException("Không tìm thấy người dùng"));

        SchoolClass schoolClass = classRepository.findById(request.classId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy lớp học"));

        Student newStudent = new Student(id, request.fullName(), request.email(), request.birthDate(), student.getCreated_at(), schoolClass);
        Student saved = studentRepository.save(newStudent);
        return studentMapper.toResponse(saved);
    }


    public void deleteById(Long id) {
        if(!studentRepository.existsById(id)) throw new NotFoundException("Không tìm thấy người dùng");
        studentRepository.deleteById(id);
        log.info("Đã xóa sinh viên id={}", id);
    }

    public List<StudentResponse> getByClassId(Long classId) {
        if (!classRepository.existsById(classId)) throw new NotFoundException("Không tìm thấy lớp học");
        return studentRepository.findBySchoolClassId(classId).stream()
                .map(studentMapper::toResponse)
                .toList();
    }

    public List<StudentResponse> getWithoutCourse() {
        return studentRepository.findStudentsWithoutCourse().stream()
                .map(studentMapper::toResponse)
                .toList();
    }
}
