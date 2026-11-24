package com.example.library.dtos;

import com.example.library.entities.User;

public record UserMinDTO(

        Long id,
        String name,
        String email,
        String phone
) {

    public UserMinDTO(User user) {
        this(user.getId(), user.getName(), user.getEmail(), user.getPhone());
    }
}
