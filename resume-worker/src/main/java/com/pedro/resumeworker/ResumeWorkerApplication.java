package com.pedro.resumeworker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ResumeWorkerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ResumeWorkerApplication.class, args);
    }

}
