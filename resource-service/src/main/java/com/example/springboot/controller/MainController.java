package com.example.springboot.controller;

import com.example.springboot.dto.PriceRequest;
import com.example.springboot.dto.ResourcePrice;
import com.example.springboot.dto.ResourceResponse;
import com.example.springboot.service.ResourceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/resource")
@CrossOrigin(origins = "*")
public class MainController {
    private final ResourceService resourceService;

    public MainController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @GetMapping("/all")
    public ResponseEntity<List<ResourceResponse>> getResources(){
        return new ResponseEntity<>(resourceService.getAllResources(), HttpStatus.OK);
    }
    @PostMapping("/price")
    public ResponseEntity<Double> postResourcePrices(@RequestBody PriceRequest priceRequest){
        Optional<Double> result = resourceService.getTotalPrice(priceRequest);
        if(result.isEmpty()) return new ResponseEntity<>(0.0, HttpStatus.NOT_FOUND);
        return new ResponseEntity<>(result.get(), HttpStatus.OK);
    }
}
