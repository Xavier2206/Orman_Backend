package com.orman.backend;

import com.orman.backend.person.config.PersonaPhotoProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableConfigurationProperties(PersonaPhotoProperties.class)
public class OrmanBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrmanBackendApplication.class, args);
    }

}
