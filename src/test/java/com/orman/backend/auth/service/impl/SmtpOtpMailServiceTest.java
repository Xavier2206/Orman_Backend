package com.orman.backend.auth.service.impl;

import com.orman.backend.auth.config.MailProperties;
import com.orman.backend.auth.exception.OtpDeliveryException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SmtpOtpMailServiceTest {

    @Test
    void sendsTheApprovedHtmlOtpContentUsingUtf8() throws Exception {
        JavaMailSender sender = mock(JavaMailSender.class);
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(sender.createMimeMessage()).thenReturn(mimeMessage);
        SmtpOtpMailService service = new SmtpOtpMailService(sender, new MailProperties("no-reply@example.test"));
        service.sendOtp("person@example.test", "004812", 300);

        ArgumentCaptor<MimeMessage> message = ArgumentCaptor.forClass(MimeMessage.class);
        verify(sender).send(message.capture());
        MimeMessage sent = message.getValue();
        sent.saveChanges();
        assertThat(sent.getAllRecipients()).extracting(Object::toString).containsExactly("person@example.test");
        assertThat(sent.getFrom()).extracting(Object::toString).containsExactly("no-reply@example.test");
        assertThat(sent.getSubject()).isEqualTo("ORMAN - Código de acceso");
        assertThat(sent.getContentType()).containsIgnoringCase("text/html").containsIgnoringCase("charset=UTF-8");
        assertThat(sent.getContent().toString()).contains("004812", "Código de acceso", "Copiar código", "El código expira en 5 minutos", "No compartas este código", "Verificación segura")
                .doesNotContain("digest", "password", "refresh", "token", "sid");
    }

    @Test
    void convertsSmtpFailureToSafeDeliveryException() {
        JavaMailSender sender = mock(JavaMailSender.class);
        when(sender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
        doThrow(new MailSendException("smtp unavailable")).when(sender).send(org.mockito.ArgumentMatchers.any(MimeMessage.class));
        SmtpOtpMailService service = new SmtpOtpMailService(sender, new MailProperties("no-reply@example.test"));
        assertThatThrownBy(() -> service.sendOtp("person@example.test", "004812", 300))
                .isInstanceOf(OtpDeliveryException.class)
                .hasMessage("No fue posible enviar el código de verificación.");
    }
}
