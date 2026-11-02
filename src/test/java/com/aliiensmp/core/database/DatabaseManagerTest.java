package com.aliiensmp.core.database;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.zaxxer.hikari.HikariConfig;

import net.jqwik.api.*;
import net.jqwik.api.constraints.StringLength;

import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Path;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

class DatabaseManagerTest {
    @TempDir Path directory;

    @ParameterizedTest
    @ValueSource(strings = {"h2", "sqlite"})
    void embeddedDatabasesPersistBoundValuesAndReconnect(String backend) throws Exception {
        Plugin plugin = mock(Plugin.class);
        when(plugin.getDataFolder()).thenReturn(directory.toFile());
        DatabaseManager db = new DatabaseManager();

        try {
            connect(db, plugin, backend);

            assertTrue(
                    db.executeAsync(
                                    "CREATE TABLE entries (id INTEGER PRIMARY KEY, value_text"
                                            + " VARCHAR(200))")
                            .get(10, TimeUnit.SECONDS));

            String hostile = "O'Brien'); DROP TABLE entries; -- \u2603";

            assertTrue(
                    db.executeAsync("INSERT INTO entries VALUES (?, ?)", 1, hostile)
                            .get(10, TimeUnit.SECONDS));

            assertEquals(
                    hostile,
                    db.queryAsync(
                                    "SELECT value_text FROM entries WHERE id = ?",
                                    rs -> {
                                        try {
                                            assertTrue(rs.next());
                                            return rs.getString(1);
                                        } catch (SQLException e) {
                                            throw new RuntimeException(e);
                                        }
                                    },
                                    1)
                            .get(10, TimeUnit.SECONDS));

            assertFalse(
                    db.executeAsync("INSERT INTO missing VALUES (?)", 1).get(10, TimeUnit.SECONDS));

            assertNull(
                    db.queryAsync("SELECT * FROM missing", rs -> "unexpected")
                            .get(10, TimeUnit.SECONDS));

            db.disconnect();

            assertThrows(SQLException.class, db::getConnection);

            connect(db, plugin, backend);

            assertEquals(
                    1,
                    db.queryAsync(
                                    "SELECT COUNT(*) FROM entries",
                                    rs -> {
                                        try {
                                            rs.next();
                                            return rs.getInt(1);
                                        } catch (SQLException e) {
                                            throw new RuntimeException(e);
                                        }
                                    })
                            .get(10, TimeUnit.SECONDS));
        } finally {
            db.disconnect();
        }
    }

    private void connect(DatabaseManager db, Plugin plugin, String backend) {
        if (backend.equals("h2")) db.connectH2(plugin, "data");
        else db.connectSQLite(plugin, "data");
    }

    @Test
    void uninitializedAndRepeatedShutdownAreSafe() throws Exception {
        DatabaseManager db = new DatabaseManager();

        assertThrows(SQLException.class, db::getConnection);
        assertFalse(db.executeAsync("SELECT 1").get());
        assertNull(db.queryAsync("SELECT 1", rs -> 1).get());

        db.disconnect();
        db.disconnect();
    }

    @Property(tries = 60)
    void arbitraryStringsRoundTripThroughPreparedStatements(
            @ForAll @StringLength(max = 100) String value) throws Exception {
        HikariConfig config = new HikariConfig();

        config.setJdbcUrl("jdbc:h2:mem:" + UUID.randomUUID());
        config.setMaximumPoolSize(1);

        DatabaseManager db = new DatabaseManager();

        try {
            db.connect(config);

            assertTrue(
                    db.executeAsync("CREATE TABLE entries (value_text VARCHAR(200))")
                            .get(5, TimeUnit.SECONDS));

            assertTrue(
                    db.executeAsync("INSERT INTO entries VALUES (?)", value)
                            .get(5, TimeUnit.SECONDS));

            assertEquals(
                    value,
                    db.queryAsync(
                                    "SELECT value_text FROM entries",
                                    rs -> {
                                        try {
                                            rs.next();
                                            return rs.getString(1);
                                        } catch (SQLException e) {
                                            throw new RuntimeException(e);
                                        }
                                    })
                            .get(5, TimeUnit.SECONDS));
        } finally {
            db.disconnect();
        }
    }
}
