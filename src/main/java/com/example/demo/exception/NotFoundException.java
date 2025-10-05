package com.example.demo.exception;

/**
 * Generic runtime exception to signal that a requested domain object was not found.
 */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) { super(message); }
}

