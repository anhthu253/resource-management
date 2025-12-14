package com.example.springboot.service;

import com.example.springboot.dto.PriceRequest;
import com.example.springboot.dto.ResourcePrice;
import com.example.springboot.dto.ResourceResponse;
import com.example.springboot.model.PriceUnit;
import com.example.springboot.model.Resource;
import com.example.springboot.repository.ResourceRepository;
import org.springframework.stereotype.Service;

import javax.swing.text.html.Option;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ResourceService {
    private final ResourceRepository resourceRepository;

    public ResourceService(ResourceRepository resourceRepository) {
        this.resourceRepository = resourceRepository;
    }

    public List<ResourceResponse> getAllResources(){
        List<Resource> resources = this.resourceRepository.findAll();
        return resources.stream()
                .map(resource -> new ResourceResponse(
                        resource.getResourceId(),
                        resource.getName(),
                        resource.getBasePrice(),
                        resource.getPriceUnit())
                )
                .collect(Collectors.toList());
    }

    public Optional<Double> getTotalPrice(PriceRequest priceRequest){
        double totalPrice = 0;
        for (var resourceId:priceRequest.getResourceIds()) {
            Optional<Resource> resource = this.resourceRepository.findById(resourceId);
            if(resource.isEmpty()) return Optional.empty();
            long unitNr = 1;
            if(resource.get().getPriceUnit() == PriceUnit.hourly)
                unitNr = ChronoUnit.HOURS.between(priceRequest.getStartedAt(), priceRequest.getEndedAt());
            else if(resource.get().getPriceUnit() == PriceUnit.daily)
                unitNr = ChronoUnit.DAYS.between(priceRequest.getStartedAt(),priceRequest.getEndedAt());
            totalPrice += unitNr * resource.get().getBasePrice();
        }
        return Optional.of(totalPrice);
    }
}
