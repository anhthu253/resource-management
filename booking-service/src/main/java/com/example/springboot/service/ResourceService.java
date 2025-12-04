package com.example.springboot.service;

import com.example.springboot.dto.ResourceDto;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
@Service
public class ResourceService {
    private final WebClient resourceWebClient;
    public ResourceService(WebClient resourceWebClient) {
        this.resourceWebClient = resourceWebClient;
    }
    public Mono<List<ResourceDto>> getAllResources(){
        return resourceWebClient.get().uri("/resource/all")
                .retrieve().bodyToMono(new ParameterizedTypeReference<List<ResourceDto>>() {});
    }
}
