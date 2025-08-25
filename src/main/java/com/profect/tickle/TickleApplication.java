package com.profect.tickle;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@ConfigurationPropertiesScan(basePackages = "com.profect.tickle")
public class TickleApplication {

    public static void main(String[] args) {
        SpringApplication.run(TickleApplication.class, args);
    }

}
