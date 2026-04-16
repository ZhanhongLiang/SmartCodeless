package com.robotlive.smartcodeless;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@MapperScan("com.robotlive.smartcodeless.mapper")
@EnableDubbo
public class SmartcodelessUserScratchApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartcodelessUserScratchApplication.class, args);
    }

}
