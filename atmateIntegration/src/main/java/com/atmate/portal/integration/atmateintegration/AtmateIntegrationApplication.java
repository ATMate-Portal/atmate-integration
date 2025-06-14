package com.atmate.portal.integration.atmateintegration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AtmateIntegrationApplication {

    public static void main(String[] args) {
        SpringApplication.run(AtmateIntegrationApplication.class, args);
    }

}
