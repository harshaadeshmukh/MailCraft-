package com.example.emailGenerator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class EmailGeneratorApplication {
    public static void main(String[] args) {
        SpringApplication.run(EmailGeneratorApplication.class, args);
    }
}