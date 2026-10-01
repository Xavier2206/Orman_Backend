package com.orman.backend;

import com.google.firebase.messaging.FirebaseMessaging;
import com.orman.backend.push.config.FirebaseProperties;
import org.assertj.core.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class OrmanBackendApplicationTests {

    @Autowired private ApplicationContext applicationContext;
    @Autowired private FirebaseProperties firebaseProperties;

    @Test
    void contextLoads() {
        Assertions.assertThat(firebaseProperties.isEnabled()).isFalse();
        Assertions.assertThat(applicationContext.getBeansOfType(FirebaseMessaging.class)).isEmpty();
    }

}
