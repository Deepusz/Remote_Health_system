package com.RHMS.health_monitoring_system.service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

@Service
public class GmailApiService {

    private static final String APPLICATION_NAME = "Remote Health Monitoring System";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";
    private static final List<String> SCOPES = Collections.singletonList(GmailScopes.GMAIL_SEND);
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";

    private final String clientId;
    private final String clientSecret;
    private final String refreshToken;
    private final String userEmail;

    public GmailApiService(
            @Value("${gmail.api.client-id}") String clientId,
            @Value("${gmail.api.client-secret}") String clientSecret,
            @Value("${gmail.api.refresh-token}") String refreshToken,
            @Value("${gmail.api.user-email}") String userEmail) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.refreshToken = refreshToken;
        this.userEmail = userEmail;
    }

    public Mono<Void> sendFallDetectionAlert(String heartRate, String spo2, String temperature, String entryId, String recipientEmail) {
        return Mono.fromRunnable(() -> {
            try {
                System.out.println("Attempting to send Gmail API fall detection alert to: " + recipientEmail);
                
                Gmail service = getGmailService();
                Message message = createMessage(recipientEmail, heartRate, spo2, temperature, entryId);
                
                service.users().messages().send("me", message).execute();
                
                System.out.println("✅ Gmail API fall detection alert sent successfully to: " + recipientEmail);
            } catch (Exception e) {
                System.err.println("❌ Failed to send Gmail API fall detection email: " + e.getMessage());
                System.err.println("Error type: " + e.getClass().getSimpleName());
                if (e.getCause() != null) {
                    System.err.println("Caused by: " + e.getCause().getMessage());
                }
            }
        })
        .subscribeOn(Schedulers.boundedElastic())
        .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                .filter(throwable -> {
                    String errorMessage = throwable.getMessage().toLowerCase();
                    return errorMessage.contains("timeout") || 
                           errorMessage.contains("connection") || 
                           errorMessage.contains("network") ||
                           errorMessage.contains("unreachable");
                })
                .doBeforeRetry(retrySignal -> 
                    System.out.println("Retrying Gmail API send, attempt: " + retrySignal.totalRetries() + 
                                     ", error: " + retrySignal.failure().getMessage())))
        .onErrorResume(throwable -> {
            System.err.println("🚨 Gmail API email sending failed after all retries. Fall detection alert could not be sent!");
            System.err.println("Final error: " + throwable.getMessage());
            return Mono.empty();
        })
        .then();
    }

    private Gmail getGmailService() throws GeneralSecurityException, IOException {
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        
        // Create credentials from refresh token
        Credential credential = new Credential.Builder(
                GoogleNetHttpTransport.newTrustedTransport().getJsonFactory(),
                GoogleNetHttpTransport.newTrustedTransport().getJsonFactory(),
                new com.google.api.client.auth.oauth2.ClientParametersAuthentication(clientId, clientSecret),
                GmailScopes.GMAIL_SEND)
                .setRefreshListeners(Collections.singletonList(credential1 -> {
                    // Handle token refresh if needed
                }))
                .build();
        
        credential.setRefreshToken(refreshToken);
        
        return new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    private Message createMessage(String recipientEmail, String heartRate, String spo2, String temperature, String entryId) 
            throws MessagingException, IOException {
        
        String subject = "🚨 FALL DETECTED - Emergency Alert";
        String bodyText = buildFallDetectionEmailBody(heartRate, spo2, temperature, entryId);
        
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);
        
        MimeMessage email = new MimeMessage(session);
        email.setFrom(new InternetAddress(userEmail));
        email.addRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(recipientEmail));
        email.setSubject(subject);
        email.setText(bodyText);
        
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        email.writeTo(buffer);
        byte[] bytes = buffer.toByteArray();
        String encodedEmail = com.google.api.client.util.Base64.encodeBase64URLSafeString(bytes);
        
        Message message = new Message();
        message.setRaw(encodedEmail);
        
        return message;
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
