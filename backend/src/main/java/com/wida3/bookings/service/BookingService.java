package com.wida3.bookings.service;

import com.wida3.auth.entity.User;
import com.wida3.auth.repository.UserRepository;
import com.wida3.bookings.dto.BookingResponse;
import com.wida3.bookings.dto.CreateBookingRequest;
import com.wida3.bookings.entity.AccessCode;
import com.wida3.bookings.entity.Booking;
import com.wida3.bookings.entity.BookingStatus;
import com.wida3.bookings.exception.BookingConflictException;
import com.wida3.bookings.exception.BookingNotFoundException;
import com.wida3.bookings.exception.InvalidBookingDatesException;
import com.wida3.bookings.exception.InvalidBookingStateException;
import com.wida3.bookings.exception.ListingNotBookableException;
import com.wida3.bookings.repository.BookingRepository;
import com.wida3.listings.entity.Listing;
import com.wida3.listings.entity.ListingStatus;
import com.wida3.listings.exception.ListingNotFoundException;
import com.wida3.listings.repository.ListingRepository;
import com.wida3.payments.entity.Payment;
import com.wida3.payments.entity.PaymentStatus;
import com.wida3.payments.service.PaymentResult;
import com.wida3.payments.service.PaymentService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final ListingRepository listingRepository;
    private final UserRepository userRepository;
    private final PaymentService paymentService;

    public BookingService(
            BookingRepository bookingRepository,
            ListingRepository listingRepository,
            UserRepository userRepository,
            PaymentService paymentService) {
        this.bookingRepository = bookingRepository;
        this.listingRepository = listingRepository;
        this.userRepository = userRepository;
        this.paymentService = paymentService;
    }

    @Transactional
    public BookingResponse create(String renterEmail, CreateBookingRequest request, String idempotencyKey) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Optional<Booking> existing = bookingRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                return toResponse(existing.get());
            }
        }

        validateDates(request.startDate(), request.endDate());

        User renter = userRepository.findByEmail(renterEmail)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + renterEmail));

        // Row-locks the listing so two concurrent booking attempts for it serialize here;
        // the DB-level EXCLUDE constraint (V6 migration) is the backstop if that's ever bypassed.
        Listing listing = listingRepository.findByIdForUpdate(request.listingId())
                .orElseThrow(() -> new ListingNotFoundException(request.listingId()));

        if (listing.getStatus() != ListingStatus.ACTIVE) {
            throw new ListingNotBookableException();
        }

        if (bookingRepository.existsOverlappingConfirmed(listing.getId(), request.startDate(), request.endDate())) {
            throw new BookingConflictException();
        }

        long weeks = ChronoUnit.DAYS.between(request.startDate(), request.endDate()) / 7;
        BigDecimal totalPrice = listing.getWeeklyPrice().multiply(BigDecimal.valueOf(weeks));

        Booking booking = new Booking(listing, renter, request.startDate(), request.endDate(), totalPrice, idempotencyKey);

        PaymentResult result = paymentService.charge(totalPrice, listing.getId().toString());
        if (result.success()) {
            Payment payment = new Payment(totalPrice, PaymentStatus.SUCCEEDED, result.providerRef());
            AccessCode accessCode = new AccessCode(generateAccessCode(), expiryFor(request.endDate()));
            booking.confirm(payment, accessCode);
        } else {
            Payment payment = new Payment(totalPrice, PaymentStatus.FAILED, null);
            booking.markFailedPayment(payment);
        }

        try {
            bookingRepository.save(booking);
        } catch (DataIntegrityViolationException e) {
            if (idempotencyKey != null) {
                Optional<Booking> raceWinner = bookingRepository.findByIdempotencyKey(idempotencyKey);
                if (raceWinner.isPresent()) {
                    return toResponse(raceWinner.get());
                }
            }
            throw new BookingConflictException();
        }

        return toResponse(booking);
    }

    @Transactional(readOnly = true)
    public BookingResponse get(UUID id, String requesterEmail, boolean isAdmin) {
        Booking booking = findOrThrow(id);
        requireViewAccess(booking, requesterEmail, isAdmin);
        return toResponse(booking);
    }

    @Transactional
    public BookingResponse cancel(UUID id, String requesterEmail, boolean isAdmin) {
        Booking booking = findOrThrow(id);
        requireViewAccess(booking, requesterEmail, isAdmin);

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new InvalidBookingStateException(
                    "Only a CONFIRMED booking can be cancelled (current status: " + booking.getStatus() + ")");
        }

        Payment payment = booking.getPayment();
        if (payment != null && payment.getStatus() == PaymentStatus.SUCCEEDED) {
            paymentService.refund(payment.getProviderRef(), payment.getAmount());
            payment.markRefunded();
        }

        booking.cancel();
        return toResponse(booking);
    }

    private void requireViewAccess(Booking booking, String requesterEmail, boolean isAdmin) {
        boolean isRenter = booking.getRenter().getEmail().equals(requesterEmail);
        boolean isOwner = booking.getListing().getOwner().getEmail().equals(requesterEmail);
        if (!isRenter && !isOwner && !isAdmin) {
            throw new AccessDeniedException("You do not have permission to view this booking");
        }
    }

    private Booking findOrThrow(UUID id) {
        return bookingRepository.findById(id).orElseThrow(() -> new BookingNotFoundException(id));
    }

    private void validateDates(LocalDate start, LocalDate end) {
        if (!end.isAfter(start)) {
            throw new InvalidBookingDatesException("End date must be after start date");
        }
        if (ChronoUnit.DAYS.between(start, end) % 7 != 0) {
            throw new InvalidBookingDatesException("Booking must span a whole number of weeks");
        }
    }

    private Instant expiryFor(LocalDate endDate) {
        return endDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    private String generateAccessCode() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }

    BookingResponse toResponse(Booking booking) {
        String code = booking.getAccessCode() != null ? booking.getAccessCode().getCode() : null;
        String failureReason = (booking.getPayment() != null && booking.getPayment().getStatus() == PaymentStatus.FAILED)
                ? "Payment declined by mock gateway"
                : null;
        return new BookingResponse(
                booking.getId(),
                booking.getListing().getId(),
                booking.getListing().getTitle(),
                booking.getStartDate(),
                booking.getEndDate(),
                booking.getTotalPrice(),
                booking.getStatus().name(),
                code,
                failureReason);
    }
}
