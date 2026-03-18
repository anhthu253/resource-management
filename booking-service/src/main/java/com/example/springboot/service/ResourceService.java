package com.example.springboot.service;

import com.example.springboot.dto.ResourceDto;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

@Service
public class ResourceService {
    @Value("${services.resource.url}")
    private String resourceURL;
    public List<ResourceDto> getAllResources() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .uri(new URI(resourceURL + "/resource/all"))
                .timeout(Duration.ofSeconds(20))
                .GET()
                .build();

        HttpClient httpClient = HttpClient.newHttpClient();
        String json = httpClient.send(request, HttpResponse.BodyHandlers.ofString()).body();
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, new TypeReference<List<ResourceDto>>() {});
    }
}
