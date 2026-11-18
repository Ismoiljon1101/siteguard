package com.siteguard.application.service;

import com.siteguard.domain.Role;

/** A first-run demo credential. The plaintext exists only in this object; the database keeps the hash. */
public record GeneratedCredential(String loginId, String displayName, Role role, String password) {
    @Override public String toString() { return "GeneratedCredential[" + loginId + ", password hidden]"; }
}
