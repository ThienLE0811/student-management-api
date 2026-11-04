package com.thienlv.student_api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
public class HelloController {


    @GetMapping("/hello")
    public String hello(@RequestParam(required = false) String name) {
        if(Objects.isNull(name)) return "Hello, World!";
        return "Hello, " + name + "!";
    }
}
