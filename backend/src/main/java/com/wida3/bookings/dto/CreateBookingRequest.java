package com.wida3.bookings.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public record CreateBookingRequest(
        @NotNull UUID listingId,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate) {
}
