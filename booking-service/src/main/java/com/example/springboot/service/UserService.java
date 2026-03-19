package com.example.springboot.service;

import com.example.springboot.model.User;
import com.example.springboot.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void createUser(User user){
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }

        try {
            this.userRepository.save(user);
        } catch (DataIntegrityViolationException ex) {
            // handle duplicate / constraint errors
            throw ex;
        }
    }
}
