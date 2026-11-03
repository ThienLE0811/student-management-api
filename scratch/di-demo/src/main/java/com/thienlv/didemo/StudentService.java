package com.thienlv.didemo;

import java.util.Map;

// Bước 1: thử tự `new StudentRepository()` ngay trong class này trước, chạy thử, ghi nhận xét vào
//         docs/tuan-3/buoi-01-02.md (Bước 1).
// Bước 2: sửa lại để nhận StudentRepository qua constructor thay vì tự new.
public class StudentService {
    StudentRepository  studentsRepository;
    public StudentService(StudentRepository studentsRepository) {
        this.studentsRepository = studentsRepository;
    }

    public Map<Long, Student> listAll() {
        return this.studentsRepository.findAll();
    }
}
