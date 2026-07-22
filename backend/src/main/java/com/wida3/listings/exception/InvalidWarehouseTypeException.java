package com.wida3.listings.exception;

public class InvalidWarehouseTypeException extends RuntimeException {

    public InvalidWarehouseTypeException(String value) {
        super("Invalid warehouse type: " + value);
    }
}
