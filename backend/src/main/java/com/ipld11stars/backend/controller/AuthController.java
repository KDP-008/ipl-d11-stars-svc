package com.ipld11stars.backend.controller;

import com.ipld11stars.backend.model.User;
import com.ipld11stars.backend.model.UserRole;
import com.ipld11stars.backend.service.UserService;
import com.ipld11stars.backend.security.JwtTokenProvider;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "http://localhost:3000")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired(required = false)
    private JwtTokenProvider jwtTokenProvider;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
            // Validate input
            if (request.getFirstName() == null || request.getFirstName().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "First name is required"));
            }
            if (request.getUserId() == null || request.getUserId().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "User ID is required"));
            }
            if (request.getUserId().length() < 3) {
                return ResponseEntity.badRequest().body(Map.of("error", "User ID must be at least 3 characters"));
            }
            if (request.getPassword() == null || request.getPassword().length() < 6) {
                return ResponseEntity.badRequest().body(Map.of("error", "Password must be at least 6 characters"));
            }

            // Check if user already exists
            if (userService.existsByUserId(request.getUserId())) {
                return ResponseEntity.badRequest().body(Map.of("error", "User ID already taken"));
            }

            // Create user with default PLAYER role
            User user = userService.createUserWithId(
                request.getFirstName(),
                request.getLastName() != null ? request.getLastName() : "",
                request.getUserId(),
                request.getPassword(),
                UserRole.PLAYER
            );

            Map<String, Object> response = new HashMap<>();
            response.put("id", user.getId());
            response.put("userId", user.getUserId());
            response.put("firstName", user.getFirstName());
            response.put("role", user.getRole().getValue());
            response.put("message", "User registered successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            if (request.getUserId() == null || request.getPassword() == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "User ID and password are required"));
            }

            // Authenticate user with password
            boolean authenticated = userService.authenticateUserWithId(request.getUserId(), request.getPassword());
            if (!authenticated) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid user ID or password"));
            }

            Optional<User> userOpt = userService.findByUserId(request.getUserId());
            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
            }

            User user = userOpt.get();
            
            // Generate JWT token if JwtTokenProvider is available
            String token = null;
            if (jwtTokenProvider != null) {
                token = jwtTokenProvider.generateToken(user);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("user", Map.of(
                "id", user.getId(),
                "userId", user.getUserId(),
                "firstName", user.getFirstName(),
                "lastName", user.getLastName(),
                "role", user.getRole().getValue(),
                "status", user.getStatus()
            ));
            if (token != null) {
                response.put("token", token);
            }
            response.put("message", "Login successful");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.badRequest().body(Map.of("error", "Authorization header missing or invalid"));
            }

            String token = authHeader.substring(7);
            
            // Verify token if JwtTokenProvider is available
            if (jwtTokenProvider != null && !jwtTokenProvider.validateToken(token)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid or expired token"));
            }

            // Extract user ID from token
            String userId = null;
            if (jwtTokenProvider != null) {
                userId = jwtTokenProvider.getUserIdFromToken(token);
            }

            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "User ID not found in token"));
            }

            Optional<User> userOpt = userService.findById(userId);
            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
            }

            User user = userOpt.get();
            Map<String, Object> userData = new HashMap<>();
            userData.put("id", user.getId());
            userData.put("firstName", user.getFirstName());
            userData.put("lastName", user.getLastName());
            userData.put("userId", user.getUserId());
            userData.put("role", user.getRole().getValue());
            userData.put("status", user.getStatus());

            return ResponseEntity.ok(userData);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @Data
    public static class RegisterRequest {
        private String firstName;
        private String lastName;
        private String userId;
        private String password;
    }

    @Getter
    @Setter
    public static class LoginRequest {
        private String userId;
        private String password;
    }
}