package com.thienlv.didemospring;


import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
public class StudentRunner implements CommandLineRunner {
    StudentService studentService;
    StudentRepository studentRepository;
    NotificationService notificationService;
    StudentRunner(
            StudentService studentService,
            StudentRepository  studentRepository ,
            @Qualifier("smsNotificationService") NotificationService notificationService
    ) {
        this.studentService = studentService;
        this.studentRepository = studentRepository;
        this.notificationService = notificationService;
    }

    @Override
    public void run(String... args) {
        studentRepository.save(new Student(1L, "Nguyen Van A", "a@gmail.com", LocalDate.of(2000,1,1), LocalDateTime.now()));
        notificationService.notify("say hi");
        System.out.println(studentService.listAll());
    }
}
