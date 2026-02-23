package com.example.springboot.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

@Entity
@Getter
@Setter
@Table(name="users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;
    private String email;
    private String hash;
    private String firstName;
    private String lastName;
    private String telephone;
    @OneToMany(mappedBy = "user")
    private Set<Address> addresses = new HashSet<>();
    @OneToMany(mappedBy = "user")
    private Set<Booking> bookings = new HashSet<>();
}
