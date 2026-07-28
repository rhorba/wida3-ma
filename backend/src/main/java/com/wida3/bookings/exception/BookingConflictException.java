package com.wida3.bookings.exception;

public class BookingConflictException extends RuntimeException {

    public BookingConflictException() {
        super("The selected weeks are no longer available for this listing");
    }
}
