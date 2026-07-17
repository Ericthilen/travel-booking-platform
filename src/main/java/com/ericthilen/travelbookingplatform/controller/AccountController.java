package com.ericthilen.travelbookingplatform.controller;

import com.ericthilen.travelbookingplatform.dto.AccountProfileRequest;
import com.ericthilen.travelbookingplatform.dto.PasswordChangeRequest;
import com.ericthilen.travelbookingplatform.model.Customer;
import com.ericthilen.travelbookingplatform.model.User;
import com.ericthilen.travelbookingplatform.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/mitt-konto")
    public String showAccount(
            Authentication authentication,
            @RequestParam(defaultValue = "profile") String page,
            Model model
    ) {
        loadAccountModel(authentication.getName(), model);
        model.addAttribute(
                "activeAccountPage",
                cleanPage(page)
        );
        return "account";
    }

    @PostMapping("/mitt-konto/profil")
    public String updateProfile(
            Authentication authentication,
            @Valid @ModelAttribute("profileRequest") AccountProfileRequest profileRequest,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            loadAccountModel(authentication.getName(), model);
            model.addAttribute("profileRequest", profileRequest);
            model.addAttribute("activeAccountPage", "settings");
            return "account";
        }

        try {
            User updatedUser = accountService.updateProfile(
                    authentication.getName(),
                    profileRequest
            );

            Authentication updatedAuthentication =
                    new UsernamePasswordAuthenticationToken(
                            updatedUser.getEmail(),
                            authentication.getCredentials(),
                            authentication.getAuthorities()
                    );

            SecurityContextHolder.getContext()
                    .setAuthentication(updatedAuthentication);

            redirectAttributes.addFlashAttribute(
                    "accountMessage",
                    "Kontouppgifterna har sparats."
            );
        } catch (IllegalArgumentException exception) {
            bindingResult.reject("profileError", exception.getMessage());
            loadAccountModel(authentication.getName(), model);
            model.addAttribute("profileRequest", profileRequest);
            model.addAttribute("activeAccountPage", "settings");
            return "account";
        }

        return "redirect:/mitt-konto?page=settings";
    }

    @PostMapping("/mitt-konto/losenord")
    public String changePassword(
            Authentication authentication,
            @Valid @ModelAttribute("passwordRequest") PasswordChangeRequest passwordRequest,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (!passwordRequest.getNewPassword()
                .equals(passwordRequest.getConfirmPassword())) {
            bindingResult.rejectValue(
                    "confirmPassword",
                    "passwordsDoNotMatch",
                    "De nya lösenorden matchar inte."
            );
        }

        if (bindingResult.hasErrors()) {
            loadAccountModel(authentication.getName(), model);
            model.addAttribute("passwordRequest", passwordRequest);
            model.addAttribute("activeAccountPage", "password");
            return "account";
        }

        try {
            accountService.changePassword(
                    authentication.getName(),
                    passwordRequest
            );

            redirectAttributes.addFlashAttribute(
                    "passwordMessage",
                    "Lösenordet har ändrats."
            );
        } catch (IllegalArgumentException exception) {
            bindingResult.reject("passwordError", exception.getMessage());
            loadAccountModel(authentication.getName(), model);
            model.addAttribute("passwordRequest", passwordRequest);
            model.addAttribute("activeAccountPage", "password");
            return "account";
        }

        return "redirect:/mitt-konto?page=password";
    }

    @PostMapping("/mitt-konto/koppla-gastbokningar")
    public String connectGuestBookings(
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        User user = accountService.getUser(authentication.getName());
        int connectedBookings = accountService.connectGuestBookings(user);

        redirectAttributes.addFlashAttribute(
                "guestBookingMessage",
                connectedBookings + " gästbokningar kopplades till kontot."
        );

        return "redirect:/mitt-konto?page=bookings";
    }

    private void loadAccountModel(
            String email,
            Model model
    ) {
        User user = accountService.getUser(email);
        Optional<Customer> customer = accountService.getCustomer(user);
        int connectedBookings = accountService.connectGuestBookings(user);

        model.addAttribute("user", user);
        model.addAttribute(
                "accountFirstName",
                firstName(user.getFullName())
        );
        model.addAttribute("customer", customer.orElse(null));
        model.addAttribute(
                "connectedBookings",
                connectedBookings
        );
        model.addAttribute(
                "activeBookings",
                accountService.getActiveBookings(user.getEmail())
        );
        model.addAttribute(
                "cancelledBookings",
                accountService.getCancelledBookings(user.getEmail())
        );
        model.addAttribute(
                "invoices",
                accountService.getInvoices(user.getEmail())
        );
        model.addAttribute(
                "payments",
                accountService.getPayments(user.getEmail())
        );

        if (!model.containsAttribute("profileRequest")) {
            model.addAttribute(
                    "profileRequest",
                    accountService.createProfileRequest(user, customer)
            );
        }

        if (!model.containsAttribute("passwordRequest")) {
            model.addAttribute(
                    "passwordRequest",
                    new PasswordChangeRequest()
            );
        }
    }

    private String firstName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "Kund";
        }

        return fullName.trim().split("\\s+")[0];
    }

    private String cleanPage(String page) {
        return switch (page) {
            case "bookings",
                    "invoices",
                    "payments",
                    "cancelled",
                    "settings",
                    "password" -> page;
            default -> "profile";
        };
    }
}
