package com.thienlv.student_api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class StudentApiApplication {

	private static final Logger log = LoggerFactory.getLogger(StudentApiApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(StudentApiApplication.class, args);
	}

	@Bean
	public CommandLineRunner printEnvironmentLabel(@Value("${app.environment-label}") String environmentLabel) {
		return args -> log.info("Profile dang chay: {}", environmentLabel);
	}

}
