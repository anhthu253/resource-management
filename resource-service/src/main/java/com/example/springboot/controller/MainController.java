package com.example.springboot.controller;

import com.example.springboot.service.ResourceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/resource")
@CrossOrigin(origins = "*")
public class MainController {
    private final ResourceService resourceService;

    public MainController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @GetMapping("/all")
    public ResponseEntity<List<String>> getResources(){
        return new ResponseEntity<>(resourceService.getAllResources(), HttpStatus.OK);
    }
}
