package com.ericthilen.travelbookingplatform.service;

import com.ericthilen.travelbookingplatform.dto.RegistrationRequest;
import com.ericthilen.travelbookingplatform.model.Role;
import com.ericthilen.travelbookingplatform.model.User;
import com.ericthilen.travelbookingplatform.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public boolean emailExists(String email) {
        if (email == null) {
            return false;
        }

        return userRepository.existsByEmailIgnoreCase(email.trim());
    }

    public User registerUser(RegistrationRequest registrationRequest) {
        String normalizedEmail = registrationRequest
                .getEmail()
                .trim()
                .toLowerCase();

        String encodedPassword = passwordEncoder.encode(
                registrationRequest.getPassword()
        );

        User user = new User(
                registrationRequest.getFullName().trim(),
                normalizedEmail,
                encodedPassword,
                Role.ROLE_USER
        );

        return userRepository.save(user);
    }
}