package com.RHMS.health_monitoring_system.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ThingSpeakService {

    private final WebClient webClient;
    private final String writeKey;
    private final String readKey;
    private final String channelId;
    private final String frontendApiToken;
    private final NotificationService notificationService;

    public ThingSpeakService(WebClient webClient,
                             @Value("${thingspeak.write-key}") String writeKey,
                             @Value("${thingspeak.read-key}") String readKey,
                             @Value("${thingspeak.channel-id}") String channelId,
                             @Value("${app.frontend.api-token}") String frontendApiToken,
                             NotificationService notificationService) {
        this.webClient = webClient;
        this.writeKey = writeKey;
        this.readKey = readKey;
        this.channelId = channelId;
        this.frontendApiToken = frontendApiToken;
        this.notificationService = notificationService;
    }

    public boolean isAuthorized(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) return false;
        String token = authorizationHeader.substring(7);
        return token.equals(frontendApiToken);
    }

    public Mono<String> update(Map<String, Object> fields, String status) {
        // Accept keys like "field1", "field2" etc. Limit to first 8 fields.
        var pairs = fields.entrySet().stream()
                .filter(e -> e.getKey().matches("field[1-8]"))
                .limit(8)
                .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "=" +
                          URLEncoder.encode(String.valueOf(e.getValue()), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));

        String qs = "api_key=" + URLEncoder.encode(writeKey, StandardCharsets.UTF_8);
        if (!pairs.isEmpty()) qs += "&" + pairs;
        if (status != null && !status.isBlank()) {
            qs += "&status=" + URLEncoder.encode(status, StandardCharsets.UTF_8);
        }

        String url = "/update?" + qs;

        return webClient.get()
                .uri(url)
                .accept(MediaType.TEXT_PLAIN)
                .retrieve()
                .bodyToMono(String.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                        .filter(throwable -> !(throwable instanceof IllegalArgumentException))
                        .doBeforeRetry(retrySignal -> 
                            System.out.println("Retrying ThingSpeak update, attempt: " + retrySignal.totalRetries())));
    }

    public Mono<Object> readFeeds(int results, Integer field) {
        String url;
        if (field != null) {
            url = String.format("/channels/%s/fields/%d.json?results=%d&api_key=%s", channelId, field, results, readKey);
        } else {
            url = String.format("/channels/%s/feeds.json?results=%d&api_key=%s", channelId, results, readKey);
        }

        return webClient.get()
                .uri(url)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(Object.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                        .filter(throwable -> !(throwable instanceof IllegalArgumentException))
                        .doBeforeRetry(retrySignal -> 
                            System.out.println("Retrying ThingSpeak read, attempt: " + retrySignal.totalRetries())))
                .flatMap(response -> {
                    // Check for fall detection in the response
                    checkForFallDetection(response);
                    return Mono.just(response);
                });
    }

    private void checkForFallDetection(Object response) {
        try {
            if (response instanceof Map<?, ?> responseMap) {
                Object feedsObj = responseMap.get("feeds");
                if (feedsObj instanceof List<?> feeds) {
                    for (Object feedObj : feeds) {
                        if (feedObj instanceof Map<?, ?> feed) {
                            String field4 = (String) feed.get("field4");
                            if ("1".equals(field4)) {
                                // Fall detected! Send notifications (both email and WhatsApp)
                                String entryId = String.valueOf(feed.get("entry_id"));
                                String heartRate = (String) feed.get("field1");
                                String spo2 = (String) feed.get("field2");
                                String temperature = (String) feed.get("field3");
                                
                                System.out.println("🚨 FALL DETECTED! Entry ID: " + entryId + 
                                                 ", Heart Rate: " + heartRate + 
                                                 ", SpO2: " + spo2 + 
                                                 ", Temperature: " + temperature);
                                
                                // Send both email and WhatsApp notifications asynchronously
                                notificationService.sendFallDetectionAlert(heartRate, spo2, temperature, entryId)
                                    .subscribe(
                                        null,
                                        error -> System.err.println("Failed to send fall detection notifications: " + error.getMessage())
                                    );
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error checking for fall detection: " + e.getMessage());
            e.printStackTrace();
        }
    }
}