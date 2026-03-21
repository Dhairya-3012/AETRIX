package com.aetrix;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class AetrixApplication {

    public static void main(String[] args) {
        SpringApplication.run(AetrixApplication.class, args);
    }
}
