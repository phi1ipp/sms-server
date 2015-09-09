package com.grigorio.smsserver

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.scheduling.annotation.EnableScheduling

@ComponentScan(basePackages = ['com.grigorio.smsserver.service', 'com.grigorio.smsserver.controller'])
@EnableScheduling
@SpringBootApplication
class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args)
    }
}
