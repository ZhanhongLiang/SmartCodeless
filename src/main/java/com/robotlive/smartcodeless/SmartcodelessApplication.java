package com.robotlive.smartcodeless;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.robotlive.smartcodeless.mapper")
public class SmartcodelessApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartcodelessApplication.class, args);
    }

}
