package com.campusdelivery.controller;

import com.campusdelivery.dto.UserLoginRequest;
import com.campusdelivery.dto.UserRegisterRequest;
import com.campusdelivery.dto.UserResponse;
import com.campusdelivery.dto.UserUpdateProfileRequest;
import com.campusdelivery.service.UserAccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class UserAuthController {
    private final UserAccountService service;

    public UserAuthController(UserAccountService service) {
        this.service = service;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody UserRegisterRequest request) {
        return service.register(request);
    }

    @PostMapping("/login")
    public UserResponse login(@Valid @RequestBody UserLoginRequest request) {
        return service.login(request);
    }

    @PutMapping("/profile")
    public UserResponse updateProfile(@Valid @RequestBody UserUpdateProfileRequest request) {
        return service.updateProfile(request);
    }
}
