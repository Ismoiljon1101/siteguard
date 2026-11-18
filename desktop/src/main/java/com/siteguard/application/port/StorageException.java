package com.siteguard.application.port;

/** Unchecked wrapper so application code never sees JDBC types. */
public class StorageException extends RuntimeException {
    public StorageException(String message, Throwable cause) { super(message, cause); }
    public StorageException(String message) { super(message); }
}
