package com.example.springdocs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SpringDocApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringDocApplication.class, args);
        System.out.println("Spring Document Application Started!");
        System.out.println("Access at: http://localhost:8080/");
        System.out.println("Try: http://localhost:8080/perform-login?userId=user1 to simulate login");
    }

}
