package com.wanted.preonboarding.ticket.application.common.service;

import com.wanted.preonboarding.ticket.application.exception.ServiceFailedException;
import com.wanted.preonboarding.ticket.domain.dto.request.SendNotification;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import static com.wanted.preonboarding.ticket.application.common.template.MailTemplate.RESERVATION_CANCELLED_TITLE;
import static com.wanted.preonboarding.ticket.application.common.template.MailTemplate.createReservationCancelledContent;
import static com.wanted.preonboarding.ticket.application.exception.ExceptionStatus.FAIL_TO_SEND_EMAIL;
import static jakarta.mail.Message.RecipientType.TO;
@Service
@RequiredArgsConstructor
public class MailService {

    public static final String ENCODING_CHARSET = "UTF-8";
    public static final String MAILER_SUBTYPE = "html";
    public static final String MAILER = "no-reply@wantedticket.com";
    public static final String TITLE_SEPARATOR = " - ";

    private final JavaMailSender emailSender;

    private void sendMail(
            final String recipient,
            final String subject,
            final String content
    ) {
        MimeMessage message = emailSender.createMimeMessage();
        try {
            message.setFrom(MAILER);
            message.addRecipient(TO, new InternetAddress(recipient));
            message.setSubject(subject, ENCODING_CHARSET);
            message.setText(content, ENCODING_CHARSET, MAILER_SUBTYPE);
        } catch (MessagingException e) {
            throw new ServiceFailedException(FAIL_TO_SEND_EMAIL);
        }
        emailSender.send(message);
    }

    @Async
    public void sendNotificationMail(SendNotification notification) {
        String title = RESERVATION_CANCELLED_TITLE + TITLE_SEPARATOR + notification.getPerformanceName();
        String content = createReservationCancelledContent(notification);
        for (String email : notification.getEmailList()) {
            sendMail(email, title, content);
        }
    }
}