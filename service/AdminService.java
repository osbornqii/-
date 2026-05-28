package com.campusdelivery.service;

import com.campusdelivery.dto.AdminLoginRequest;
import com.campusdelivery.dto.AdminResponse;
import com.campusdelivery.dto.AdminUpdateProfileRequest;
import com.campusdelivery.dto.CreateAdminRequest;
import com.campusdelivery.entity.Admin;
import com.campusdelivery.repository.AdminRepository;
import com.campusdelivery.security.PasswordHasher;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminService {
    private final AdminRepository repository;

    public AdminService(AdminRepository repository) {
        this.repository = repository;
    }

    public AdminResponse create(CreateAdminRequest request) {
        if (repository.findByUsername(request.username()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "username already exists");
        }

        Admin admin = new Admin();
        admin.setUsername(request.username());
        admin.setPassword(PasswordHasher.sha256(request.password()));
        admin.setUserType(request.userType());

        try {
            Admin saved = repository.save(admin);
            return toResponse(saved);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "username already exists");
        }
    }

    public AdminResponse login(AdminLoginRequest request) {
        Admin admin = repository.findByUsername(request.username())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid username or password"));

        String hashed = PasswordHasher.sha256(request.password());
        if (!hashed.equals(admin.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid username or password");
        }

        return toResponse(admin);
    }

    public List<AdminResponse> list() {
        return repository.findAll().stream().map(AdminService::toResponse).collect(Collectors.toList());
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "admin not found");
        }
        repository.deleteById(id);
    }

    public AdminResponse updateProfile(AdminUpdateProfileRequest request) {
        Admin admin = repository.findById(request.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "admin not found"));

        boolean hasUpdate = false;

        if (request.username() != null && !request.username().isBlank() && !request.username().equals(admin.getUsername())) {
            if (repository.findByUsername(request.username()).isPresent()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "username already exists");
            }
            admin.setUsername(request.username());
            hasUpdate = true;
        }

        if (request.newPassword() != null && !request.newPassword().isBlank()) {
            if (request.oldPassword() == null || request.oldPassword().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "oldPassword is required");
            }
            String oldHashed = PasswordHasher.sha256(request.oldPassword());
            if (!oldHashed.equals(admin.getPassword())) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid username or password");
            }
            admin.setPassword(PasswordHasher.sha256(request.newPassword()));
            hasUpdate = true;
        }

        if (!hasUpdate) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "no changes");
        }

        Admin saved = repository.save(admin);
        return toResponse(saved);
    }

    private static AdminResponse toResponse(Admin admin) {
        return new AdminResponse(admin.getId(), admin.getUsername(), admin.getUserType());
    }
}
