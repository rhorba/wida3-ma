package com.wida3.bookings.exception;

public class ListingNotBookableException extends RuntimeException {

    public ListingNotBookableException() {
        super("This listing is not currently available for booking");
    }
}
