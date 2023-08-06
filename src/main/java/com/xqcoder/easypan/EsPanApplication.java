package com.xqcoder.easypan;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@EnableAsync
@SpringBootApplication(scanBasePackages = ("com.xqcoder.easypan"))
@MapperScan(basePackages = {"com.xqcoder.easypan.mappers"})
@EnableTransactionManagement
@EnableScheduling
public class EsPanApplication {

    public static void main(String[] args) {
        SpringApplication.run(EsPanApplication.class, args);
    }

}
