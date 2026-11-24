package com.example.library.services;

import com.example.library.entities.User;
import com.example.library.exceptions.ForbiddenException;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserService userService;

    public AuthService(UserService userService) {
        this.userService = userService;
    }

    public void validateSelfOrAdmin(Long userId) {
        User me = userService.authenticated();

        if(!me.hasRole("ROLE_ADMIN") && !me.getId().equals(userId)) {
            throw new ForbiddenException("Access denied");
        }
    }
}
