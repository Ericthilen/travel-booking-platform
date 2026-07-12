package com.ericthilen.travelbookingplatform.controller;

import com.ericthilen.travelbookingplatform.dto.RegistrationRequest;
import com.ericthilen.travelbookingplatform.service.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String showLoginPage() {
        return "login";
    }

    @GetMapping("/register")
    public String showRegistrationPage(Model model) {
        model.addAttribute(
                "registrationRequest",
                new RegistrationRequest()
        );

        return "register";
    }

    @PostMapping("/register")
    public String registerUser(
            @Valid RegistrationRequest registrationRequest,
            BindingResult bindingResult
    ) {
        if (!registrationRequest.getPassword().equals(
                registrationRequest.getConfirmPassword()
        )) {
            bindingResult.rejectValue(
                    "confirmPassword",
                    "passwordsDoNotMatch",
                    "Lösenorden matchar inte."
            );
        }

        if (userService.emailExists(registrationRequest.getEmail())) {
            bindingResult.rejectValue(
                    "email",
                    "emailAlreadyExists",
                    "Det finns redan ett konto med den e-postadressen."
            );
        }

        if (bindingResult.hasErrors()) {
            return "register";
        }

        userService.registerUser(registrationRequest);

        return "redirect:/login?registered";
    }
}