package com.aliiensmp.core.database;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URLClassLoader;
import java.nio.file.Path;
import java.sql.Driver;
import java.sql.DriverManager;
import java.util.Properties;

class H2MigrationIT {
    @TempDir Path directory;

    @Test
    void databaseCreatedByPreviousDriverCanBeOpenedAndModifiedBySelectedDriver() throws Exception {
        String url =
                "jdbc:h2:file:" + directory.resolve("previous").toAbsolutePath() + ";MODE=MySQL";
        // Isolate the previous driver so the current dependency cannot create the fixture for it.
        try (var loader =
                new URLClassLoader(
                        new java.net.URL[] {
                            Path.of("target/migration/h2-2.2.224.jar").toUri().toURL()
                        },
                        ClassLoader.getPlatformClassLoader())) {
            Driver previous =
                    (Driver) loader.loadClass("org.h2.Driver").getConstructor().newInstance();
            try (var connection = previous.connect(url, new Properties());
                    var statement = connection.createStatement()) {
                statement.execute(
                        "CREATE TABLE saved_data(id INT PRIMARY KEY, message VARCHAR(100))");
                statement.execute("INSERT INTO saved_data VALUES(1, 'saved before upgrade')");
            }
        }
        try (var connection = DriverManager.getConnection(url);
                var statement = connection.createStatement()) {
            try (var result = statement.executeQuery("SELECT message FROM saved_data WHERE id=1")) {
                assertTrue(result.next());
                assertEquals("saved before upgrade", result.getString(1));
            }
            assertEquals(
                    1,
                    statement.executeUpdate(
                            "INSERT INTO saved_data VALUES(2, 'saved after upgrade')"));
        }
        try (var connection = DriverManager.getConnection(url);
                var statement = connection.createStatement();
                var result = statement.executeQuery("SELECT COUNT(*) FROM saved_data")) {
            assertTrue(result.next());
            assertEquals(2, result.getInt(1));
        }
    }
}
