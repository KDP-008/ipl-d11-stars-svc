package com.ipld11stars.backend.support;

import com.ipld11stars.backend.model.User;
import com.ipld11stars.backend.model.UserRole;
import com.ipld11stars.backend.repository.jdbc.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Creates throwaway users with unique handles and email addresses.
 */
@Component
public class UserFixtures {

    public static final String PASSWORD = "password123";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserFixtures(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User create(String firstName) {
        return create(firstName, true);
    }

    public User create(String firstName, boolean withEmail) {
        String handle = firstName.toLowerCase() + UUID.randomUUID().toString().substring(0, 8);
        User user = new User();
        user.setUserId(handle);
        user.setFirstName(firstName);
        user.setLastName("Tester");
        user.setEmail(withEmail ? handle + "@example.com" : null);
        user.setPasswordHash(passwordEncoder.encode(PASSWORD));
        user.setRole(UserRole.PLAYER);
        user.setStatus("ACTIVE");
        return userRepository.save(user);
    }
}
