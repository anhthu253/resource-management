package com.example.springboot.model;

import jakarta.persistence.*;
@Entity
@Table(name="user_address")
public class Address {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long addressId;
    @ManyToOne
    @JoinColumn(name="user_id")
    private User user;
    private String street;
    private String city;
    private String state;
    private String zipCode;
    private String country;
}
