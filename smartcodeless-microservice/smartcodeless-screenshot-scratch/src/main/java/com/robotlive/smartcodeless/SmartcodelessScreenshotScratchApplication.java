package com.robotlive.smartcodeless;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableDubbo
public class SmartcodelessScreenshotScratchApplication {
    public static void main(String[] args) {
        SpringApplication.run(SmartcodelessScreenshotScratchApplication.class, args);
    }
}