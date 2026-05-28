package com.campusdelivery.service;

import com.campusdelivery.dto.UserLoginRequest;
import com.campusdelivery.dto.UserRegisterRequest;
import com.campusdelivery.dto.UserResponse;
import com.campusdelivery.dto.UserUpdateProfileRequest;
import com.campusdelivery.entity.UserAccount;
import com.campusdelivery.repository.UserAccountRepository;
import com.campusdelivery.security.PasswordHasher;
import java.time.LocalDateTime;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserAccountService {
    private final UserAccountRepository repository;

    public UserAccountService(UserAccountRepository repository) {
        this.repository = repository;
    }

    public UserResponse register(UserRegisterRequest request) {
        if (repository.findByPhone(request.phone()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "phone already exists");
        }

        UserAccount user = new UserAccount();
        user.setUsername(request.username());
        user.setPhone(request.phone());
        user.setPassword(PasswordHasher.sha256(request.password()));
        user.setCreatedAt(LocalDateTime.now());

        try {
            UserAccount saved = repository.save(user);
            return toResponse(saved);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "phone already exists");
        }
    }

    public UserResponse login(UserLoginRequest request) {
        UserAccount user = repository.findByPhone(request.phone())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid phone or password"));

        String hashed = PasswordHasher.sha256(request.password());
        if (!hashed.equals(user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid phone or password");
        }

        return toResponse(user);
    }

    public UserResponse updateProfile(UserUpdateProfileRequest request) {
        UserAccount user = repository.findByPhone(request.phone())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found"));

        boolean hasUpdate = false;

        if (request.username() != null && !request.username().isBlank()) {
            user.setUsername(request.username());
            hasUpdate = true;
        }

        if (request.newPassword() != null && !request.newPassword().isBlank()) {
            if (request.oldPassword() == null || request.oldPassword().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "oldPassword is required");
            }
            String oldHashed = PasswordHasher.sha256(request.oldPassword());
            if (!oldHashed.equals(user.getPassword())) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid phone or password");
            }
            user.setPassword(PasswordHasher.sha256(request.newPassword()));
            hasUpdate = true;
        }

        if (!hasUpdate) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "no changes");
        }

        UserAccount saved = repository.save(user);
        return toResponse(saved);
    }

    private static UserResponse toResponse(UserAccount user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getPhone());
    }
}
