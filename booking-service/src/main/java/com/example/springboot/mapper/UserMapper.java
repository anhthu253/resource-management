package com.example.springboot.mapper;

import com.example.springboot.dto.UserDto;
import com.example.springboot.model.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {
    public User mapUserDtoToUser(UserDto userDto){
        if (userDto == null) return null;
        User user = new User();
        user.setFirstName(userDto.getFirstName());
        user.setLastName(userDto.getLastName());
        user.setEmail(userDto.getEmail());
        user.setHash(userDto.getPassword());
        user.setTelephone(userDto.getTelephone());
        return user;
    }
}
