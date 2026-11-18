package com.siteguard.adapter.sqlite;

import java.sql.Connection;
import java.sql.SQLException;

/** One numbered schema step. It runs in its own transaction with foreign keys off, then a foreign key check. */
public interface Migration {
    int version();
    String description();
    void apply(Connection connection) throws SQLException;
}
