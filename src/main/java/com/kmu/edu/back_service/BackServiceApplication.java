package com.kmu.edu.back_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableAsync
@EnableTransactionManagement
public class BackServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackServiceApplication.class, args);

        System.out.println(" ____    _    ____    ____ ");
        System.out.println("/  _ \\  / \\  /  _ \\  /   _\\");
        System.out.println("| / \\|  | |  | / \\|  |  /  ");
        System.out.println("| |-||  | |  | \\_/|  |  \\__");
        System.out.println("\\_/ \\|  \\_/  \\____/  \\____/");
        System.out.println("后端服务启动成功！");
        System.out.println("API文档文件: API文档.md");

    }

}
