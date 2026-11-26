package com.minibanco.auth.user;

import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toResponse(User user){
        return new UserResponse(user.getId(), user.getUsername());
    }

}
