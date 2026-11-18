package com.siteguard.adapter.sqlite;

import com.siteguard.application.port.StorageException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;
import org.sqlite.SQLiteConfig;

/** Connection factory for one SQLite file. Writers take the write lock up front (IMMEDIATE) and wait up to 5 s. */
public final class SqliteDatabase {
    private final String url;

    public SqliteDatabase(Path file) {
        Path absolute = Objects.requireNonNull(file).toAbsolutePath();
        try {
            if (absolute.getParent() != null) Files.createDirectories(absolute.getParent());
        } catch (IOException e) {
            throw new StorageException("Cannot create database directory", e);
        }
        this.url = "jdbc:sqlite:" + absolute;
    }

    public Connection open(boolean foreignKeys) throws SQLException {
        SQLiteConfig config = new SQLiteConfig();
        config.enforceForeignKeys(foreignKeys);
        config.setBusyTimeout(5000);
        config.setTransactionMode(SQLiteConfig.TransactionMode.IMMEDIATE);
        return DriverManager.getConnection(url, config.toProperties());
    }
}
