package com.siteguard.bootstrap;

import com.siteguard.application.service.GeneratedCredential;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

/** Writes the one-time demo credentials. This is the only place plaintext is persisted; data/ is git-ignored. */
final class DemoCredentialsFile {
    private DemoCredentialsFile() { }

    static void write(Path file, List<GeneratedCredential> credentials, Instant now) {
        StringBuilder sb = new StringBuilder();
        sb.append("# SiteGuard FICTIONAL demo credentials, generated ").append(now).append('\n');
        sb.append("# Written once at first run. Only hashes are stored in the database. Never commit this file.\n");
        for (GeneratedCredential c : credentials) {
            sb.append(c.loginId()).append("  ").append(c.role()).append("  ").append(c.password()).append('\n');
        }
        try {
            Files.createDirectories(file.toAbsolutePath().getParent());
            Files.writeString(file, sb.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot write demo credentials file", e);
        }
    }
}
