package com.example.springboot.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserDto {
    private Long userId;
    private String email;
    private String firstName;
    private String lastName;
    private String telephone;
}
