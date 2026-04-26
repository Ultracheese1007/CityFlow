package com.cityflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@EnableAspectJAutoProxy(exposeProxy = true)
@SpringBootApplication
public class CityFlowApplication {
    public static void main(String[] args) {
        SpringApplication.run(CityFlowApplication.class, args);
    }
}