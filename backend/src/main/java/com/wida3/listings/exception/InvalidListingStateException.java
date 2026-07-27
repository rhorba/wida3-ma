package com.wida3.listings.exception;

import com.wida3.listings.entity.ListingStatus;

public class InvalidListingStateException extends RuntimeException {

    public InvalidListingStateException(ListingStatus current, String action) {
        super("Cannot " + action + " a listing with status " + current);
    }
}
