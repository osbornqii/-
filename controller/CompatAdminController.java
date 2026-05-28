package com.campusdelivery.controller;

import com.campusdelivery.dto.AdminLoginRequest;
import com.campusdelivery.dto.AdminResponse;
import com.campusdelivery.dto.AdminUpdateProfileRequest;
import com.campusdelivery.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class CompatAdminController {
    private final AdminService service;

    public CompatAdminController(AdminService service) {
        this.service = service;
    }

    @PostMapping("/login")
    public AdminResponse login(@Valid @RequestBody AdminLoginRequest request) {
        return service.login(request);
    }

    @PutMapping("/profile")
    public AdminResponse updateProfile(@Valid @RequestBody AdminUpdateProfileRequest request) {
        return service.updateProfile(request);
    }
}
