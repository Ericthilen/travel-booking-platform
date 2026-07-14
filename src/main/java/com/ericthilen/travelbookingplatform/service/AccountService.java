package com.ericthilen.travelbookingplatform.service;

import com.ericthilen.travelbookingplatform.dto.AccountProfileRequest;
import com.ericthilen.travelbookingplatform.dto.PasswordChangeRequest;
import com.ericthilen.travelbookingplatform.dto.PreviousTravelerOption;
import com.ericthilen.travelbookingplatform.model.Booking;
import com.ericthilen.travelbookingplatform.model.BookingStatus;
import com.ericthilen.travelbookingplatform.model.Customer;
import com.ericthilen.travelbookingplatform.model.Invoice;
import com.ericthilen.travelbookingplatform.model.Payment;
import com.ericthilen.travelbookingplatform.model.Traveler;
import com.ericthilen.travelbookingplatform.model.User;
import com.ericthilen.travelbookingplatform.repository.BookingRepository;
import com.ericthilen.travelbookingplatform.repository.CustomerRepository;
import com.ericthilen.travelbookingplatform.repository.InvoiceRepository;
import com.ericthilen.travelbookingplatform.repository.PaymentRepository;
import com.ericthilen.travelbookingplatform.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class AccountService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final BookingRepository bookingRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountService(
            UserRepository userRepository,
            CustomerRepository customerRepository,
            BookingRepository bookingRepository,
            InvoiceRepository invoiceRepository,
            PaymentRepository paymentRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.bookingRepository = bookingRepository;
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User getUser(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("Kontot kunde inte hittas."));
    }

    public Optional<Customer> getCustomer(User user) {
        return customerRepository.findByUser(user);
    }

    public AccountProfileRequest createProfileRequest(
            User user,
            Optional<Customer> customer
    ) {
        AccountProfileRequest request = new AccountProfileRequest();
        request.setFullName(user.getFullName());
        request.setEmail(user.getEmail());
        customer.ifPresent(value -> request.setPhone(value.getPhone()));

        return request;
    }

    public User updateProfile(
            String currentEmail,
            AccountProfileRequest request
    ) {
        User user = getUser(currentEmail);
        String newEmail = request.getEmail().trim().toLowerCase();

        Optional<User> existingUser =
                userRepository.findByEmailIgnoreCase(newEmail);

        if (existingUser.isPresent()
                && !existingUser.get().getId().equals(user.getId())) {
            throw new IllegalArgumentException(
                    "Det finns redan ett konto med den e-postadressen."
            );
        }

        user.setFullName(request.getFullName().trim());
        user.setEmail(newEmail);

        Optional<Customer> customer = customerRepository.findByUser(user);
        if (customer.isPresent()) {
            NameParts nameParts = splitName(request.getFullName());
            customer.get().updateContactInformation(
                    nameParts.firstName(),
                    nameParts.lastName(),
                    clean(request.getPhone()),
                    newEmail
            );
            customerRepository.save(customer.get());
        }

        return userRepository.save(user);
    }

    public void changePassword(
            String email,
            PasswordChangeRequest request
    ) {
        User user = getUser(email);

        if (!passwordEncoder.matches(
                request.getCurrentPassword(),
                user.getPassword()
        )) {
            throw new IllegalArgumentException(
                    "Nuvarande lösenord stämmer inte."
            );
        }

        if (!request.getNewPassword().equals(
                request.getConfirmPassword()
        )) {
            throw new IllegalArgumentException(
                    "De nya lösenorden matchar inte."
            );
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    public int connectGuestBookings(User user) {
        Optional<Customer> customer = customerRepository.findByUser(user);

        if (customer.isEmpty()) {
            return 0;
        }

        List<Booking> guestBookings =
                bookingRepository
                        .findAllByUserIsNullAndCustomerPersonalNumberAndCustomerEmailIgnoreCase(
                                customer.get().getPersonalNumber(),
                                customer.get().getEmail()
                        );

        for (Booking booking : guestBookings) {
            booking.connectUser(user);
            booking.getCustomer().connectUser(user);
        }

        bookingRepository.saveAll(guestBookings);

        return guestBookings.size();
    }

    public List<Booking> getActiveBookings(String email) {
        return bookingRepository
                .findAllByUserEmailIgnoreCaseAndStatusNotOrderByBookedAtDesc(
                        email,
                        BookingStatus.CANCELLED
                );
    }

    public List<Booking> getCancelledBookings(String email) {
        return bookingRepository
                .findAllByUserEmailIgnoreCaseAndStatusOrderByBookedAtDesc(
                        email,
                        BookingStatus.CANCELLED
                );
    }

    public List<Invoice> getInvoices(String email) {
        return invoiceRepository
                .findAllByBookingUserEmailIgnoreCaseOrderByCreatedAtDesc(
                        email
                );
    }

    public List<Payment> getPayments(String email) {
        return paymentRepository
                .findAllByBookingUserEmailIgnoreCaseOrderByPaymentDateDescRegisteredAtDesc(
                        email
                );
    }

    public List<PreviousTravelerOption> getPreviousTravelers(
            String email
    ) {
        List<Booking> bookings = bookingRepository
                .findAllByUserEmailIgnoreCaseOrderByBookedAtDesc(email);

        Map<String, PreviousTravelerOption> travelers =
                new LinkedHashMap<>();

        for (Booking booking : bookings) {
            for (Traveler traveler : booking.getTravelers()) {
                String key = traveler.getPersonalNumber();

                travelers.putIfAbsent(
                        key,
                        new PreviousTravelerOption(
                                traveler.getPersonalNumber(),
                                traveler.getFirstName(),
                                traveler.getLastName()
                        )
                );
            }
        }

        return travelers.values().stream().toList();
    }

    private NameParts splitName(String fullName) {
        String trimmed = fullName.trim();
        int lastSpace = trimmed.lastIndexOf(' ');

        if (lastSpace < 0) {
            return new NameParts(trimmed, trimmed);
        }

        return new NameParts(
                trimmed.substring(0, lastSpace).trim(),
                trimmed.substring(lastSpace + 1).trim()
        );
    }

    private String clean(String value) {
        if (value == null) {
            return "";
        }

        return value.trim();
    }

    private record NameParts(String firstName, String lastName) {
    }
}
