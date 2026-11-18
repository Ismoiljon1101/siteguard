package com.siteguard.application.service;

/** Login failed. The message is always the same so callers cannot tell why. */
public class AuthenticationException extends ApplicationException {
    public static final String MESSAGE = "Invalid operator ID or password, or the account is temporarily locked.";
    public AuthenticationException() { super(MESSAGE); }
}
