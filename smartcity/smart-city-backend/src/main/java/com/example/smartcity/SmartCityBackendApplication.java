package com.example.smartcity;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching
@MapperScan
@SpringBootApplication
public class SmartCityBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartCityBackendApplication.class, args);
    }

}
