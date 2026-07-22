package com.ericthilen.travelbookingplatform.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String bookingNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "departure_id", nullable = false)
    private Departure departure;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_type_id", nullable = false)
    private RoomType roomType;

    @Column(nullable = false)
    private int numberOfTravelers;

    @Column(nullable = false)
    private int numberOfRooms;

    @Column(length = 500)
    private String roomDistribution;

    @Column(nullable = false)
    private int totalPrice;

    @Column(nullable = false)
    private LocalDateTime bookedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DiscoverySource discoverySource;

    @Column(nullable = false)
    private boolean termsAccepted;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentPlan paymentPlan;

    @Column(nullable = false)
    private int depositAmount;

    private LocalDate depositDueDate;

    @Column(nullable = false)
    private int remainingAmount;

    @Column(nullable = false)
    private LocalDate finalPaymentDueDate;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            columnDefinition = "varchar(30) default 'CONFIRMED'"
    )
    private BookingStatus status = BookingStatus.CONFIRMED;

    @Column(nullable = false)
    private int paidAmount;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            columnDefinition = "varchar(30) default 'UNPAID'"
    )
    private PaymentStatus paymentStatus = PaymentStatus.UNPAID;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            columnDefinition = "varchar(30) default 'NOT_SENT'"
    )
    private EmailStatus bookingEmailStatus =
            EmailStatus.NOT_SENT;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            columnDefinition = "varchar(30) default 'NOT_SENT'"
    )
    private EmailStatus cancellationEmailStatus =
            EmailStatus.NOT_SENT;

    @Column(nullable = false)
    private int cancellationFee;

    @Column(nullable = false)
    private int refundAmount;

    private LocalDateTime cancelledAt;

    @Column(length = 120)
    private String cancellationReason;

    @OneToMany(
            mappedBy = "booking",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<Traveler> travelers = new ArrayList<>();

    @OneToMany(
            mappedBy = "booking",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<Payment> payments = new ArrayList<>();

    public Booking() {
    }

    public Booking(
            String bookingNumber,
            Customer customer,
            User user,
            Departure departure,
            RoomType roomType,
            int numberOfTravelers,
            int numberOfRooms,
            int totalPrice,
            LocalDateTime bookedAt,
            DiscoverySource discoverySource,
            PaymentPlan paymentPlan,
            int depositAmount,
            LocalDate depositDueDate,
            int remainingAmount,
            LocalDate finalPaymentDueDate
    ) {
        this.bookingNumber = bookingNumber;
        this.customer = customer;
        this.user = user;
        this.departure = departure;
        this.roomType = roomType;
        this.numberOfTravelers = numberOfTravelers;
        this.numberOfRooms = numberOfRooms;
        this.totalPrice = totalPrice;
        this.bookedAt = bookedAt;
        this.discoverySource = discoverySource;
        this.termsAccepted = true;
        this.paymentPlan = paymentPlan;
        this.depositAmount = depositAmount;
        this.depositDueDate = depositDueDate;
        this.remainingAmount = remainingAmount;
        this.finalPaymentDueDate = finalPaymentDueDate;
        this.status = BookingStatus.CONFIRMED;
        this.paidAmount = 0;
        this.paymentStatus = PaymentStatus.UNPAID;
        this.bookingEmailStatus = EmailStatus.NOT_SENT;
        this.cancellationEmailStatus = EmailStatus.NOT_SENT;
        this.cancellationFee = 0;
        this.refundAmount = 0;
    }

    public void addTraveler(Traveler traveler) {
        traveler.setBooking(this);
        travelers.add(traveler);
    }

    public void registerPayment(int amount) {
        if (status == BookingStatus.CANCELLED) {
            throw new IllegalStateException(
                    "Det går inte att registrera en betalning på en avbokad bokning."
            );
        }

        if (amount < 1) {
            throw new IllegalArgumentException(
                    "Betalningsbeloppet måste vara minst en krona."
            );
        }

        int remainingToPay = totalPrice - paidAmount;

        if (amount > remainingToPay) {
            throw new IllegalArgumentException(
                    "Betalningen är högre än det återstående beloppet."
            );
        }

        paidAmount += amount;
        remainingAmount = totalPrice - paidAmount;

        if (paidAmount == totalPrice) {
            paymentStatus = PaymentStatus.PAID;
        } else {
            paymentStatus = PaymentStatus.PARTIALLY_PAID;
        }
    }

    public void markBookingEmailStatus(
            EmailStatus emailStatus
    ) {
        if (emailStatus == null) {
            bookingEmailStatus = EmailStatus.NOT_SENT;
            return;
        }

        bookingEmailStatus = emailStatus;
    }

    public void markCancellationEmailStatus(
            EmailStatus emailStatus
    ) {
        if (emailStatus == null) {
            cancellationEmailStatus = EmailStatus.NOT_SENT;
            return;
        }

        cancellationEmailStatus = emailStatus;
    }

    public void cancel(
            int cancellationFee,
            int refundAmount,
            String cancellationReason
    ) {
        if (status == BookingStatus.CANCELLED) {
            throw new IllegalStateException(
                    "Bokningen är redan avbokad."
            );
        }

        this.status = BookingStatus.CANCELLED;
        this.cancellationFee = cancellationFee;
        this.refundAmount = refundAmount;
        this.remainingAmount = 0;
        this.cancelledAt = LocalDateTime.now();
        this.cancellationReason = cancellationReason;
        this.cancellationEmailStatus = EmailStatus.NOT_SENT;

        if (refundAmount > 0) {
            this.paymentStatus = PaymentStatus.REFUNDED;
        }
    }

    public Long getId() {
        return id;
    }

    public String getBookingNumber() {
        return bookingNumber;
    }

    public Customer getCustomer() {
        return customer;
    }

    public User getUser() {
        return user;
    }

    public void connectUser(User user) {
        if (this.user == null && user != null) {
            this.user = user;
        }
    }

    public Departure getDeparture() {
        return departure;
    }

    public RoomType getRoomType() {
        return roomType;
    }

    public int getNumberOfTravelers() {
        return numberOfTravelers;
    }

    public int getNumberOfRooms() {
        return numberOfRooms;
    }

    public String getRoomDistribution() {
        if (roomDistribution == null || roomDistribution.isBlank()) {
            return numberOfRooms + " rum";
        }

        return roomDistribution;
    }

    public void updateRoomDistribution(String roomDistribution) {
        this.roomDistribution = roomDistribution;
    }

    public int getTotalPrice() {
        return totalPrice;
    }

    public LocalDateTime getBookedAt() {
        return bookedAt;
    }

    public DiscoverySource getDiscoverySource() {
        return discoverySource;
    }

    public boolean isTermsAccepted() {
        return termsAccepted;
    }

    public PaymentPlan getPaymentPlan() {
        return paymentPlan;
    }

    public int getDepositAmount() {
        return depositAmount;
    }

    public LocalDate getDepositDueDate() {
        return depositDueDate;
    }

    public int getRemainingAmount() {
        return remainingAmount;
    }

    public LocalDate getFinalPaymentDueDate() {
        return finalPaymentDueDate;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public int getPaidAmount() {
        return paidAmount;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public EmailStatus getBookingEmailStatus() {
        return bookingEmailStatus;
    }

    public EmailStatus getCancellationEmailStatus() {
        return cancellationEmailStatus;
    }

    public int getCancellationFee() {
        return cancellationFee;
    }

    public int getRefundAmount() {
        return refundAmount;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public List<Traveler> getTravelers() {
        return travelers;
    }

    public List<Payment> getPayments() {
        return payments;
    }
}
