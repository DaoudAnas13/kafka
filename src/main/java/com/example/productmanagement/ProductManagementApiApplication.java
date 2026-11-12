package com.example.productmanagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class ProductManagementApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProductManagementApiApplication.class, args);
    }
}