package com.aliiensmp.core.database;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

/** Requires real services; the integration profile fails rather than silently skipping them. */
class DatabaseManagerIT {
    @ParameterizedTest
    @ValueSource(strings = {"mariadb", "mysql"})
    void realServerSupportsPoolQueriesTransactionsAndShutdown(String backend) throws Exception {
        String host = required("DB_HOST");
        String user = required("DB_USER");
        String password = required("DB_PASSWORD");
        int port =
                Integer.parseInt(required(backend.equals("mysql") ? "MYSQL_PORT" : "MARIADB_PORT"));
        DatabaseManager db = new DatabaseManager();

        try {
            if (backend.equals("mysql")) db.connectMySQL(host, port, "aliien_test", user, password);
            else db.connectMariaDB(host, port, "aliien_test", user, password);

            assertTrue(
                    db.executeAsync(
                                    "CREATE TABLE IF NOT EXISTS aliien_entries (id INT PRIMARY KEY,"
                                            + " value_text VARCHAR(200))")
                            .get(15, TimeUnit.SECONDS));
            assertTrue(db.executeAsync("DELETE FROM aliien_entries").get(15, TimeUnit.SECONDS));

            assertTrue(
                    db.executeAsync("INSERT INTO aliien_entries VALUES (?, ?)", 1, "O'Brien ☃")
                            .get(15, TimeUnit.SECONDS));

            assertEquals(
                    "O'Brien ☃",
                    db.queryAsync(
                                    "SELECT value_text FROM aliien_entries WHERE id = ?",
                                    rs -> {
                                        try {
                                            assertTrue(rs.next());
                                            return rs.getString(1);
                                        } catch (SQLException e) {
                                            throw new RuntimeException(e);
                                        }
                                    },
                                    1)
                            .get(15, TimeUnit.SECONDS));

            try (var connection = db.getConnection()) {
                connection.setAutoCommit(false);

                try (var statement =
                        connection.prepareStatement(
                                "INSERT INTO aliien_entries VALUES (2, 'rollback')")) {
                    statement.executeUpdate();
                }
                connection.rollback();
            }

            assertEquals(
                    1,
                    db.queryAsync(
                                    "SELECT COUNT(*) FROM aliien_entries",
                                    rs -> {
                                        try {
                                            rs.next();
                                            return rs.getInt(1);
                                        } catch (SQLException e) {
                                            throw new RuntimeException(e);
                                        }
                                    })
                            .get(15, TimeUnit.SECONDS));
        } finally {
            db.disconnect();
        }
        assertThrows(SQLException.class, db::getConnection);
    }

    private static String required(String key) {
        return java.util.Objects.requireNonNull(
                System.getenv(key), "Missing integration setting " + key);
    }
}
