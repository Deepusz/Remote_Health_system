package com.RHMS.health_monitoring_system.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final String notificationEmail;
    private final String subject;

    public EmailService(JavaMailSender mailSender,
                       @Value("${app.fall-detection.notification-email}") String notificationEmail,
                       @Value("${app.fall-detection.subject}") String subject) {
        this.mailSender = mailSender;
        this.notificationEmail = notificationEmail;
        this.subject = subject;
    }

    public Mono<Void> sendFallDetectionAlert(String heartRate, String spo2, String temperature, String entryId) {
        return Mono.fromRunnable(() -> {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(notificationEmail);
                message.setSubject(subject);
                message.setText(buildFallDetectionEmailBody(heartRate, spo2, temperature, entryId));
                
                mailSender.send(message);
                System.out.println("Fall detection alert email sent successfully to: " + notificationEmail);
            } catch (Exception e) {
                System.err.println("Failed to send fall detection email: " + e.getMessage());
                e.printStackTrace();
            }
        })
        .subscribeOn(Schedulers.boundedElastic())
        .then();
    }

    private String buildFallDetectionEmailBody(String heartRate, String spo2, String temperature, String entryId) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        
        return String.format("""
            🚨 EMERGENCY ALERT - FALL DETECTED 🚨
            
            A fall has been detected by the Remote Health Monitoring System!
            
            📊 Current Health Metrics:
            • Heart Rate: %s bpm
            • SpO₂ (Oxygen Level): %s%%
            • Temperature: %s°C
            • Fall Detection Status: ACTIVATED (Field4 = 1)
            
            📍 Device Information:
            • Entry ID: %s
            • Detection Time: %s
            • Channel: Remote Health Monitoring System
            
            ⚠️ IMMEDIATE ACTION REQUIRED:
            Please check on the person wearing the device immediately.
            If no response or signs of distress, consider calling emergency services.
            
            This is an automated alert from your health monitoring system.
            """, 
            heartRate != null ? heartRate : "N/A",
            spo2 != null ? spo2 : "N/A", 
            temperature != null ? temperature : "N/A",
            entryId != null ? entryId : "N/A",
            timestamp
        );
    }
}
