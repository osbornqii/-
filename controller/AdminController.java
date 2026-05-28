package com.campusdelivery.controller;

import com.campusdelivery.dto.AdminLoginRequest;
import com.campusdelivery.dto.AdminResponse;
import com.campusdelivery.dto.CreateAdminRequest;
import com.campusdelivery.service.AdminService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admins")
public class AdminController {
    private final AdminService service;

    public AdminController(AdminService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdminResponse create(@Valid @RequestBody CreateAdminRequest request) {
        return service.create(request);
    }

    @PostMapping("/login")
    public AdminResponse login(@Valid @RequestBody AdminLoginRequest request) {
        return service.login(request);
    }

    @GetMapping
    public List<AdminResponse> list() {
        return service.list();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}

