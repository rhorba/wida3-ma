package com.wida3.listings.exception;

public class TooManyPhotosException extends RuntimeException {

    public TooManyPhotosException(int max) {
        super("A listing may have at most " + max + " photos");
    }
}
