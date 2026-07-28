package com.wida3.bookings.exception;

import java.util.UUID;

public class BookingNotFoundException extends RuntimeException {

    public BookingNotFoundException(UUID id) {
        super("No booking found with id " + id);
    }
}
