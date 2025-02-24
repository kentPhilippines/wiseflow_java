package com.wiseflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class WiseflowApplication {
    public static void main(String[] args) {
        SpringApplication.run(WiseflowApplication.class, args);
    }
}