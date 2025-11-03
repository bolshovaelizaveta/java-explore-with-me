package ru.practicum.main;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"ru.practicum.client", "ru.practicum.main"})
public class EwmMainServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(EwmMainServiceApplication.class, args);
    }
}