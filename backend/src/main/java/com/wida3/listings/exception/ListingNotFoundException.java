package com.wida3.listings.exception;

import java.util.UUID;

public class ListingNotFoundException extends RuntimeException {

    public ListingNotFoundException(UUID id) {
        super("No listing found with id " + id);
    }
}
