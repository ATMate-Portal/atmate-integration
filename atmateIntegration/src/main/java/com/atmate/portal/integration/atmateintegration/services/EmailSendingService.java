package com.atmate.portal.integration.atmateintegration.services;

import com.atmate.portal.integration.atmateintegration.utils.interfaces.EmailSendingInterface;
import jakarta.mail.MessagingException; // Para Spring Boot 3.x
import jakarta.mail.internet.MimeMessage;   // Para Spring Boot 3.x
// Se estiver a usar Spring Boot 2.x, seria:
// import javax.mail.MessagingException;
// import javax.mail.internet.MimeMessage;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailSendingService implements EmailSendingInterface {


    @Autowired
    private JavaMailSender javaMailSender;

    @Value("${spring.mail.from:noreply@exemplo.com}") // Usa o 'from' das propriedades ou um default
    private String defaultFromAddress;

    @Override
    public boolean sendEmail(String to, String subject, String body, boolean isHtml) {
        if (to == null || to.trim().isEmpty()) {
            log.error("Destinatário do email não pode ser nulo ou vazio.");
            return false;
        }

        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            // O 'true' no construtor do MimeMessageHelper habilita o suporte multipart,
            // útil se quiser enviar anexos no futuro.
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(defaultFromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, isHtml); // O segundo parâmetro indica se o texto é HTML

            log.info("A tentar enviar email para: {} com assunto: {}", to, subject);
            javaMailSender.send(mimeMessage);
            log.info("Email enviado com sucesso para: {}", to);
            return true;

        } catch (MessagingException e) {
            log.error("Erro de MessagingException ao enviar email para {}: {}", to, e.getMessage(), e);
            return false;
        } catch (MailException e) { // Captura outras exceções do Spring Mail
            log.error("Erro de MailException ao enviar email para {}: {}", to, e.getMessage(), e);
            return false;
        } catch (Exception e) { // Captura genérica para outros erros inesperados
            log.error("Erro inesperado ao enviar email para {}: {}", to, e.getMessage(), e);
            return false;
        }
    }
}
