package com.wida3.listings.exception;

public class InvalidPhotoUrlException extends RuntimeException {

    public InvalidPhotoUrlException(String url) {
        super("Photo URL must reference a file uploaded via /api/v1/files/upload: " + url);
    }
}
