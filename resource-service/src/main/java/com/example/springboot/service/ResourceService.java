package com.example.springboot.service;

import com.example.springboot.model.Resource;
import com.example.springboot.repository.ResourceRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ResourceService {
    private final ResourceRepository resourceRepository;

    public ResourceService(ResourceRepository resourceRepository) {
        this.resourceRepository = resourceRepository;
    }

    public List<String> getAllResources(){
        return resourceRepository.findAll().stream().map(resource -> resource.getName()).toList();
    }
}
