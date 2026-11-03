package com.thienlv.didemo;

import java.util.HashMap;
import java.util.Map;

// Bước 1: lưu Student trong Map<Long, Student>, tự viết các hàm cần (save, findAll, findById...)
public class StudentRepository {

    // TODO
    Map<Long, Student> students  = new HashMap<>();

    public void save(Student student) {
        this.students.put(student.id(), student);
    }


    public Map<Long, Student> findAll() {
        return students;
    }
}
