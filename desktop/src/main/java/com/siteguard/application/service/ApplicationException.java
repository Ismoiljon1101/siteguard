package com.siteguard.application.service;

/** Base for expected, user-facing failures. The transaction in progress is rolled back. */
public class ApplicationException extends RuntimeException {
    public ApplicationException(String message) { super(message); }
}
