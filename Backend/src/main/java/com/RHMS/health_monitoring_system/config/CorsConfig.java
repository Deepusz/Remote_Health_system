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

                // allowed headers (strings)
                response.getHeaders().setAccessControlAllowHeaders(
                        List.of("Authorization", "Content-Type", "Accept", "Origin", "X-Requested-With")
                );

                response.getHeaders().setAccessControlAllowCredentials(true);

                // handle preflight
                if (exchange.getRequest().getMethod() == HttpMethod.OPTIONS) {
                    response.setStatusCode(HttpStatus.OK);
                    return Mono.empty();
                }
            }
            return chain.filter(exchange);
        };
    }
}
