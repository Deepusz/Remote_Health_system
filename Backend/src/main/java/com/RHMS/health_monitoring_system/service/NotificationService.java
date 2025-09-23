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
public class NotificationService {

    private final JavaMailSender mailSender;
    private final WhatsAppService whatsAppService;
    private final String notificationEmail;
    private final String subject;
    private final String senderEmail;

    public NotificationService(JavaMailSender mailSender,
                              WhatsAppService whatsAppService,
                              @Value("${app.fall-detection.notification-email}") String notificationEmail,
                              @Value("${app.fall-detection.subject}") String subject,
                              @Value("${app.fall-detection.sender-email}") String senderEmail) {
        this.mailSender = mailSender;
        this.whatsAppService = whatsAppService;
        this.notificationEmail = notificationEmail;
        this.subject = subject;
        this.senderEmail = senderEmail;
    }

    public Mono<Void> sendFallDetectionAlert(String heartRate, String spo2, String temperature, String entryId) {
        // Send both email and WhatsApp notifications
        return Mono.when(
            sendEmailNotification(heartRate, spo2, temperature, entryId),
            whatsAppService.sendFallDetectionAlert(heartRate, spo2, temperature, entryId)
        );
    }

    private Mono<Void> sendEmailNotification(String heartRate, String spo2, String temperature, String entryId) {
        return Mono.fromRunnable(() -> {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(senderEmail);
                message.setTo(notificationEmail);
                message.setSubject(subject);
                message.setText(buildFallDetectionEmailBody(heartRate, spo2, temperature, entryId));
                
                System.out.println("📧 Attempting to send email notification to: " + notificationEmail);
                mailSender.send(message);
                System.out.println("✅ Email notification sent successfully!");
            } catch (Exception e) {
                System.err.println("❌ Failed to send email notification: " + e.getMessage());
                System.err.println("Error type: " + e.getClass().getSimpleName());
            }
        })
        .subscribeOn(Schedulers.boundedElastic())
        .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                .filter(throwable -> {
                    String errorMessage = throwable.getMessage().toLowerCase();
                    return errorMessage.contains("timeout") || 
                           errorMessage.contains("connection") || 
                           errorMessage.contains("network");
                })
                .doBeforeRetry(retrySignal -> 
                    System.out.println("🔄 Retrying email send, attempt: " + retrySignal.totalRetries())))
        .onErrorResume(throwable -> {
            System.err.println("🚨 Email notification failed after all retries!");
            return Mono.empty();
        })
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
