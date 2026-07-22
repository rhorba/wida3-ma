package com.wida3.files.exception;

public class FileTooLargeException extends RuntimeException {

    public FileTooLargeException(long maxSizeMb) {
        super("File exceeds the maximum allowed size of " + maxSizeMb + "MB");
    }
}
