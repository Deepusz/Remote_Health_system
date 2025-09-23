package com.RHMS.health_monitoring_system.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
public class WhatsAppService {

    private final WebClient webClient;
    private final String whatsappPhoneNumber;
    private final String apiKey;

    public WhatsAppService(WebClient.Builder webClientBuilder,
                          @Value("${whatsapp.phone-number:}") String whatsappPhoneNumber,
                          @Value("${whatsapp.api-key:}") String apiKey) {
        this.webClient = webClientBuilder.build();
        this.whatsappPhoneNumber = whatsappPhoneNumber;
        this.apiKey = apiKey;
    }

    public Mono<Void> sendFallDetectionAlert(String heartRate, String spo2, String temperature, String entryId) {
        if (whatsappPhoneNumber.isEmpty() || apiKey.isEmpty()) {
            System.out.println("⚠️ WhatsApp not configured. Skipping WhatsApp notification.");
            return Mono.empty();
        }

        String message = buildFallDetectionWhatsAppMessage(heartRate, spo2, temperature, entryId);
        
        return Mono.fromRunnable(() -> {
            try {
                System.out.println("📱 Attempting to send WhatsApp fall detection alert to: " + whatsappPhoneNumber);
                
                // Using CallMeBot API (free WhatsApp API)
                String url = String.format("https://api.callmebot.com/whatsapp.php?phone=%s&text=%s&apikey=%s",
                        whatsappPhoneNumber,
                        java.net.URLEncoder.encode(message, java.nio.charset.StandardCharsets.UTF_8),
                        apiKey);
                
                // Make HTTP request
                String response = webClient.get()
                        .uri(url)
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();
                
                System.out.println("✅ WhatsApp fall detection alert sent successfully!");
                System.out.println("Response: " + response);
                
            } catch (Exception e) {
                System.err.println("❌ Failed to send WhatsApp notification: " + e.getMessage());
                e.printStackTrace();
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
                    System.out.println("🔄 Retrying WhatsApp send, attempt: " + retrySignal.totalRetries())))
        .onErrorResume(throwable -> {
            System.err.println("🚨 WhatsApp notification failed after all retries!");
            System.err.println("Final error: " + throwable.getMessage());
            return Mono.empty();
        })
        .then();
    }

    private String buildFallDetectionWhatsAppMessage(String heartRate, String spo2, String temperature, String entryId) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        
        return String.format("""
            🚨 *FALL DETECTED* 🚨
            
            A fall has been detected by the Remote Health Monitoring System!
            
            📊 *Current Health Metrics:*
            • Heart Rate: %s bpm
            • SpO₂ (Oxygen Level): %s%%
            • Temperature: %s°C
            • Fall Detection Status: ACTIVATED
            
            📍 *Device Information:*
            • Entry ID: %s
            • Detection Time: %s
            
            ⚠️ *IMMEDIATE ACTION REQUIRED:*
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
