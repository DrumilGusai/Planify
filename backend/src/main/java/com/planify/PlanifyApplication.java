package com.planify;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class PlanifyApplication {

    public static void main(String[] args) {
        SpringApplication.run(PlanifyApplication.class, args);
    }
}
