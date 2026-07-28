package com.wida3.bookings.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record BookingResponse(
        UUID id,
        UUID listingId,
        String listingTitle,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal totalPrice,
        String status,
        String accessCode,
        String paymentFailureReason) {
}
