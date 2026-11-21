package com.pcc.reporting;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PccReportingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PccReportingServiceApplication.class, args);
    }
}
