package com.thienlv.didemospring;

import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;

@Repository
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