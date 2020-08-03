package com.usststore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import tk.mybatis.spring.annotation.MapperScan;

@SpringBootApplication
@EnableDiscoveryClient
@MapperScan("com.usststore.item.mapper")
public class UsItemApplication {
    public static void main(String[] args) {
        SpringApplication.run(UsItemApplication.class);
    }
}
