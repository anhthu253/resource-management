package com.example.springboot.service;

import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.HttpProtocol;
import reactor.netty.http.client.HttpClient;

@Configuration
@ConfigurationProperties(prefix = "services.resource")
@Setter
public class WebClientConfig {
    private String url;
    @Bean("resourceWebClient")
    public WebClient resourceWebClient() {
        HttpClient httpClient = HttpClient.create()
                .protocol(HttpProtocol.HTTP11);
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl(url)
                .build();
    }
}