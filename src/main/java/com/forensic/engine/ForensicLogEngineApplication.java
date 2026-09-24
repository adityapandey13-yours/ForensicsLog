package com.forensic.engine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class ForensicLogEngineApplication {
    public static void main(String[] args) {
        SpringApplication.run(ForensicLogEngineApplication.class, args);
    }
}
