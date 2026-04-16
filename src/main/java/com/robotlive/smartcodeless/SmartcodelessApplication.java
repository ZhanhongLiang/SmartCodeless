package com.robotlive.smartcodeless;

import dev.langchain4j.community.store.embedding.redis.spring.RedisEmbeddingStoreAutoConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching
@SpringBootApplication(exclude = {RedisEmbeddingStoreAutoConfiguration.class})
@MapperScan("com.robotlive.smartcodeless.mapper")
public class SmartcodelessApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartcodelessApplication.class, args);
    }

}
