package com.orman.backend;

import com.orman.backend.person.config.PersonaPhotoProperties;
import com.orman.backend.contract.config.ContratoArchivoProperties;
import com.orman.backend.payment.config.PaymentImageStorageProperties;
import com.orman.backend.property.config.PropiedadPortadaProperties;
import com.orman.backend.property.config.UnidadFotoProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties({PersonaPhotoProperties.class, PropiedadPortadaProperties.class,
        UnidadFotoProperties.class, ContratoArchivoProperties.class, PaymentImageStorageProperties.class})
@EnableScheduling
public class OrmanBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrmanBackendApplication.class, args);
    }

}
