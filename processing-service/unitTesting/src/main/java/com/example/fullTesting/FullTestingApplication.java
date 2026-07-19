package com.example.fullTesting;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FullTestingApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(FullTestingApplication.class);
        app.setAdditionalProfiles("full");
        app.run(args);
    }

}
