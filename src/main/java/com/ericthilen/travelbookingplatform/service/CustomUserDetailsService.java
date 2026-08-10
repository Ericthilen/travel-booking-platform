package com.ericthilen.travelbookingplatform.service;

import com.ericthilen.travelbookingplatform.model.User;
import com.ericthilen.travelbookingplatform.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.Set;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private static final String SUPPORT_AGENT_EMAIL = "eric@customerservice.com";
    private static final String CEO_EMAIL = "thileneric@erigotravel.com";

    private final UserRepository userRepository;

    public CustomUserDetailsService(
            UserRepository userRepository
    ) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        User user = userRepository
                .findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Ingen användare hittades med den e-postadressen."
                ));

        Set<String> authorities = new LinkedHashSet<>();
        authorities.add(user.getRole().name());

        if (SUPPORT_AGENT_EMAIL.equalsIgnoreCase(user.getEmail())) {
            authorities.add("ROLE_AGENT");
        }

        if (CEO_EMAIL.equalsIgnoreCase(user.getEmail())) {
            authorities.add("ROLE_CEO");
            authorities.add("ROLE_USER");
            authorities.add("ROLE_ADMIN");
            authorities.add("ROLE_AGENT");
        }

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPassword())
                .authorities(authorities.toArray(String[]::new))
                .build();
    }
}
