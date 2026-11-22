package com.example.library.dtos;

import com.example.library.entities.User;
import org.springframework.security.core.GrantedAuthority;

import java.util.List;

public record UserResponseDTO(
        Long id,
        String name,
        String email,
        String phone,
        List<String> roles
) {

    public UserResponseDTO(User user) {
        this(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getRoles().stream()
                        .map(GrantedAuthority::getAuthority)
                        .toList()
        );
    }
}
