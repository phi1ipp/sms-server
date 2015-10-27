package com.grigorio.smsserver

import com.grigorio.smsserver.config.ApplicationConfig
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.ComponentScan
import org.springframework.scheduling.annotation.EnableScheduling

@ComponentScan(basePackages = ['com.grigorio.smsserver.service', 'com.grigorio.smsserver.controller'])
//@EnableScheduling
@SpringBootApplication
@EnableConfigurationProperties(ApplicationConfig.class)
class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args)
    }
}
