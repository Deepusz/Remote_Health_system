package com.RHMS.health_monitoring_system.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.cors.reactive.CorsUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import reactor.core.publisher.Mono;

import java.util.List;

@Configuration
public class CorsConfig {

    // Use the exact origin of your Vercel frontend
    private static final String VERCEL_ORIGIN = "https://remote-health-system.vercel.app";

    @Bean
    public WebFilter corsFilter() {
        return (ServerWebExchange exchange, org.springframework.web.server.WebFilterChain chain) -> {
            if (CorsUtils.isCorsRequest(exchange.getRequest())) {
                ServerHttpResponse response = exchange.getResponse();
                
                // Debug logging
                String origin = exchange.getRequest().getHeaders().getFirst("Origin");
                String method = exchange.getRequest().getMethod().toString();
                System.out.println("CORS Request - Origin: " + origin + ", Method: " + method);

                // origin
                response.getHeaders().setAccessControlAllowOrigin(VERCEL_ORIGIN);

                // methods expect HttpMethod instances
                response.getHeaders().setAccessControlAllowMethods(
                        List.of(
                                HttpMethod.GET,
                                HttpMethod.POST,
                                HttpMethod.PUT,
                                HttpMethod.DELETE,
                                HttpMethod.OPTIONS
                        )
                );

                // allowed headers (strings) - including all headers from your request
                response.getHeaders().setAccessControlAllowHeaders(
                        List.of(
                                "Authorization", 
                                "Content-Type", 
                                "Accept", 
                                "Origin", 
                                "X-Requested-With",
                                "Referer",
                                "User-Agent",
                                "sec-ch-ua",
                                "sec-ch-ua-mobile",
                                "sec-ch-ua-platform"
                        )
                );

                // expose headers for client access
                response.getHeaders().setAccessControlExposeHeaders(
                        List.of("Authorization", "Content-Type")
                );

                response.getHeaders().setAccessControlAllowCredentials(true);
                
                // set max age for preflight cache
                response.getHeaders().setAccessControlMaxAge(3600L);

                // handle preflight
                if (exchange.getRequest().getMethod() == HttpMethod.OPTIONS) {
                    System.out.println("Handling CORS preflight request");
                    response.setStatusCode(HttpStatus.OK);
                    return Mono.empty();
                }
            }
            return chain.filter(exchange);
        };
    }
}
