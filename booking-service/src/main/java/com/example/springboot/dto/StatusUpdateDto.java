package com.example.springboot.dto;

import com.example.springboot.model.Status;

public record StatusUpdateDto(long id, Status status, String message){}

