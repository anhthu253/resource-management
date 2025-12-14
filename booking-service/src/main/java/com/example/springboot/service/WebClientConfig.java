package com.example.springboot.service;

import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@ConfigurationProperties(prefix = "services.resource")
@Setter
public class WebClientConfig {
    private String url;

    @Bean("resourceWebClient")
    public WebClient resourceWebClient() {
        return WebClient.builder()
                .baseUrl(url)
                .build();
    }
}