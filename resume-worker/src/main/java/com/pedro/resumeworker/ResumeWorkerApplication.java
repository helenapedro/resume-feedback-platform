package com.pedro.resumeworker;

import com.pedro.resumeworker.ai.config.AiJobRetryProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(AiJobRetryProperties.class)
public class ResumeWorkerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ResumeWorkerApplication.class, args);
    }

}
