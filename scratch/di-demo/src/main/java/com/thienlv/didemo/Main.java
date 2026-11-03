package com.thienlv.didemo;

import java.time.LocalDate;
import java.time.LocalDateTime;

// Bước 3: tự tạo và lắp các object lại với nhau ở đây (StudentRepository -> StudentService),
//         rồi gọi thử một vài thao tác để kiểm tra.
public class Main {

    public static void main(String[] args) {
        StudentRepository repository =  new StudentRepository();
        StudentService studentService = new StudentService(repository);
        repository.save(new Student(1L, "Nguyen Van A", "a@gmail.com", LocalDate.of(2000,1,1), LocalDateTime.now()));
        System.out.println(studentService.listAll());
    }
}
