package com.ericthilen.travelbookingplatform.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class StaffMenuControllerAdvice {

    @ModelAttribute("staffMenuName")
    public String staffMenuName(Authentication authentication) {
        if (authentication == null || !isStaff(authentication)) {
            return "";
        }

        String username = authentication.getName();
        String firstPart = username.contains("@")
                ? username.substring(0, username.indexOf('@'))
                : username;
        String cleanName = firstPart
                .replaceAll("[^A-Za-zÅÄÖåäö ]", "")
                .trim();

        if (cleanName.isBlank()) {
            cleanName = "personal";
        }

        String firstName = cleanName.contains(" ")
                ? cleanName.substring(0, cleanName.indexOf(' '))
                : cleanName;

        return "ERIGOS " + firstName.toUpperCase();
    }

    private boolean isStaff(Authentication authentication) {
        return authentication
                .getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority ->
                        "ROLE_ADMIN".equals(authority)
                                || "ROLE_AGENT".equals(authority)
                );
    }
}
