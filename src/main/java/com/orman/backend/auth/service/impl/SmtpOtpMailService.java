package com.orman.backend.auth.service.impl;

import com.orman.backend.auth.config.MailProperties;
import com.orman.backend.auth.exception.OtpDeliveryException;
import com.orman.backend.auth.service.OtpMailService;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class SmtpOtpMailService implements OtpMailService {

    private final JavaMailSender mailSender;
    private final MailProperties properties;

    @Override
    public void sendOtp(String destination, String otp, long expirationSeconds) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setFrom(properties.from());
            helper.setTo(destination);
            helper.setSubject("ORMAN - Código de acceso");
            helper.setText(buildOtpEmail(otp, expirationSeconds), true);
            mailSender.send(message);
        } catch (org.springframework.mail.MailException | MessagingException exception) {
            throw new OtpDeliveryException();
        }
    }

    private String buildOtpEmail(String otp, long expirationSeconds) {
        long minutes = expirationSeconds / 60;
        return """
                <!doctype html>
                <html lang="es">
                <head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1.0"></head>
                <body style="margin:0;padding:0;background-color:#f5f7fa;font-family:Arial,Helvetica,sans-serif;color:#1f2937;">
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="background-color:#f5f7fa;padding:24px 12px;">
                    <tr><td align="center">
                      <table role="presentation" width="600" cellspacing="0" cellpadding="0" border="0" style="width:100%%;max-width:600px;background-color:#ffffff;border:1px solid #d9e1e8;">
                        <tr><td style="padding:28px 32px 10px;background-color:#000f1f;text-align:center;"><div style="font-size:24px;line-height:28px;font-weight:bold;letter-spacing:3px;color:#d4a94e;">ORMAN</div></td></tr>
                        <tr><td style="padding:8px 32px 30px;background-color:#000f1f;text-align:center;"><div style="font-size:27px;line-height:34px;font-weight:bold;color:#e7edf3;">Código de acceso</div></td></tr>
                        <tr><td style="padding:24px 32px;background-color:#062238;text-align:center;border-top:1px solid #0a2b45;border-bottom:3px solid #d4a94e;"><div style="font-size:23px;line-height:26px;color:#d4a94e;">◆</div><div style="padding-top:8px;font-size:15px;line-height:22px;font-weight:bold;color:#e7edf3;">Verificación segura</div><div style="padding-top:3px;font-size:13px;line-height:20px;color:#aebbc7;">Protege el acceso a tu cuenta ORMAN</div></td></tr>
                        <tr><td style="padding:34px 32px 14px;background-color:#ffffff;text-align:center;font-size:16px;line-height:25px;color:#1f2937;">Usa este código para confirmar tu inicio de sesión:</td></tr>
                        <tr><td style="padding:14px 32px 12px;background-color:#ffffff;text-align:center;"><table role="presentation" align="center" cellspacing="0" cellpadding="0" border="0"><tr><td style="padding:17px 24px;background-color:#ffffff;border:2px solid #d4a94e;border-radius:7px;font-size:35px;line-height:40px;font-weight:bold;letter-spacing:8px;color:#000f1f;">%s</td></tr></table></td></tr>
                        <tr><td style="padding:8px 32px 18px;background-color:#ffffff;text-align:center;"><table role="presentation" align="center" cellspacing="0" cellpadding="0" border="0"><tr><td style="min-width:180px;padding:13px 20px;background-color:#d4a94e;border-radius:6px;font-size:15px;line-height:18px;font-weight:bold;color:#000f1f;text-align:center;">Copiar código</td></tr></table></td></tr>
                        <tr><td style="padding:2px 32px 30px;background-color:#ffffff;text-align:center;font-size:14px;line-height:22px;color:#4b5563;">El código expira en %d minutos.</td></tr>
                        <tr><td style="padding:0 32px 30px;background-color:#ffffff;"><table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="background-color:#eaf0f7;border-left:4px solid #d4a94e;"><tr><td style="padding:20px 20px 8px;font-size:16px;line-height:23px;font-weight:bold;color:#000f1f;">No compartas este código con nadie</td></tr><tr><td style="padding:0 20px 20px;font-size:14px;line-height:22px;color:#374151;">• ORMAN nunca te pedirá este código por teléfono, chat o correo.<br>• Si no intentaste iniciar sesión, puedes ignorar este mensaje.</td></tr></table></td></tr>
                        <tr><td style="padding:20px 32px;background-color:#f3f4f6;border-top:1px solid #d9e1e8;text-align:center;font-size:12px;line-height:18px;color:#6b7280;">Este mensaje fue enviado automáticamente por ORMAN.<br>Por tu seguridad, no respondas a este correo.</td></tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(otp, minutes);
    }
}
