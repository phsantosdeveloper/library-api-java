package com.example.library.controllers;

import com.example.library.dtos.UserResponseDTO;
import com.example.library.services.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping(value = "/me")
    public ResponseEntity<UserResponseDTO> getMe() {
        UserResponseDTO dto = userService.getMe();
        return ResponseEntity.ok().body(dto);
    }
}
