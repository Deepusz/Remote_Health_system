package com.RHMS.health_monitoring_system.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class EmailService {

    private final GmailApiService gmailApiService;
    private final String notificationEmail;
    private final String subject;
    private final String senderEmail;

    public EmailService(GmailApiService gmailApiService,
                       @Value("${app.fall-detection.notification-email}") String notificationEmail,
                       @Value("${app.fall-detection.subject}") String subject,
                       @Value("${app.fall-detection.sender-email}") String senderEmail) {
        this.gmailApiService = gmailApiService;
        this.notificationEmail = notificationEmail;
        this.subject = subject;
        this.senderEmail = senderEmail;
    }

    public Mono<Void> sendFallDetectionAlert(String heartRate, String spo2, String temperature, String entryId) {
        // Use Gmail API instead of SMTP
        return gmailApiService.sendFallDetectionAlert(heartRate, spo2, temperature, entryId, notificationEmail);
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
