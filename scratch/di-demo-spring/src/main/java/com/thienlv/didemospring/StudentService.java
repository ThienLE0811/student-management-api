package com.thienlv.didemospring;

import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class StudentService {
    StudentRepository  studentsRepository;
    public StudentService(StudentRepository studentsRepository) {
        this.studentsRepository = studentsRepository;
    }

    public Map<Long, Student> listAll() {
        return this.studentsRepository.findAll();
    }
}