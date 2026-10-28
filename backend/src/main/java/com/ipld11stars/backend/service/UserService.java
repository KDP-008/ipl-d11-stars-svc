package com.ipld11stars.backend.service;

import com.ipld11stars.backend.model.User;
import com.ipld11stars.backend.model.UserRole;
import com.ipld11stars.backend.repository.jdbc.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public Optional<User> findById(String id) {
        return userRepository.findById(id);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> findByUserId(String userId) {
        return userRepository.findByUserId(userId);
    }

    public User createUser(String firstName, String lastName, String email, String password, UserRole role) {
        User user = new User(firstName, lastName, email, passwordEncoder.encode(password), role);
        return userRepository.save(user);
    }

    public User createUser(String firstName, String lastName, String email, String password) {
        return createUser(firstName, lastName, email, password, UserRole.PLAYER);
    }

    public User createUserWithId(String firstName, String lastName, String userId, String password, UserRole role) {
        User user = new User(firstName, lastName, userId, passwordEncoder.encode(password), role);
        return userRepository.save(user);
    }

    public User createUserWithId(String firstName, String lastName, String userId, String password) {
        return createUserWithId(firstName, lastName, userId, password, UserRole.PLAYER);
    }

    public User updateUser(User user) {
        return userRepository.save(user);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public boolean existsByUserId(String userId) {
        return userRepository.existsByUserId(userId);
    }

    public void deleteUser(String id) {
        userRepository.delete(id);
    }

    public boolean authenticateUser(String email, String password) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return false;
        }
        User user = userOpt.get();
        return passwordEncoder.matches(password, user.getPasswordHash());
    }

    public boolean authenticateUserWithId(String userId, String password) {
        Optional<User> userOpt = userRepository.findByUserId(userId);
        if (userOpt.isEmpty()) {
            return false;
        }
        User user = userOpt.get();
        return passwordEncoder.matches(password, user.getPasswordHash());
    }
}