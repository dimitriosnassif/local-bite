package com.localbite.backend;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class LocalbiteBackendApplication {

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    public static void main(String[] args) {
        SpringApplication.run(LocalbiteBackendApplication.class, args);
    }

    @Bean
    public ApplicationRunner logActiveProfile() {
        return args -> {
            System.out.println("✅ Active Spring profile: " + activeProfile);
        };
    }
}