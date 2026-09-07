package com.billim;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BillimApplication {
    public static void main(String[] args) {
        SpringApplication.run(BillimApplication.class, args);
    }
}