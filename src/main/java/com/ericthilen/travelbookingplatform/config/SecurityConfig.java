package com.ericthilen.travelbookingplatform.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/",
                                "/error",
                                "/resor",
                                "/resor/**",
                                "/bokning",
                                "/bokning/**",
                                "/hitta-bokning",
                                "/hitta-bokning/**",
                                "/resevillkor",
                                "/avbokningsvillkor",
                                "/integritetspolicy",
                                "/cookies",
                                "/cookieinformation",
                                "/betalningsvillkor",
                                "/kontakt",
                                "/kontaktuppgifter",
                                "/paketresor",
                                "/information-om-paketresor",
                                "/resegaranti",
                                "/information-om-resegaranti",
                                "/kundnojdhet/**",
                                "/chat/**",
                                "/login",
                                "/kundtjanst/login",
                                "/register",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/videos/**"
                        ).permitAll()
                        .requestMatchers(
                                "/admin",
                                "/admin/**"
                        )
                        .hasRole("ADMIN")
                        .requestMatchers(
                                "/kundtjanst",
                                "/kundtjanst/**"
                        )
                        .hasRole("AGENT")
                        .requestMatchers(
                                "/mina-bokningar",
                                "/mina-bokningar/**",
                                "/mitt-konto",
                                "/mitt-konto/**"
                        )
                        .hasAnyRole("USER", "ADMIN", "AGENT")
                        .anyRequest()
                        .authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .usernameParameter("email")
                        .successHandler((request, response, authentication) -> {
                            boolean agentLogin =
                                    request.getParameter("agentLogin") != null;
                            boolean isSupportUser =
                                    authentication
                                            .getAuthorities()
                                            .stream()
                                            .map(GrantedAuthority::getAuthority)
                                            .anyMatch(authority ->
                                                    "ROLE_ADMIN".equals(authority)
                                                            || "ROLE_AGENT".equals(authority)
                                            );

                            if (isSupportUser) {
                                response.sendRedirect("/");
                                return;
                            }

                            if (agentLogin) {
                                response.sendRedirect("/kundtjanst/login?denied");
                                return;
                            }

                            response.sendRedirect("/");
                        })
                        .failureHandler((request, response, exception) -> {
                            if (request.getParameter("agentLogin") != null) {
                                response.sendRedirect("/kundtjanst/login?error");
                                return;
                            }

                            response.sendRedirect("/login?error");
                        })
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/?logout")
                        .permitAll()
                );

        return http.build();
    }
}
