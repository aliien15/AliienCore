package com.aliiensmp.core.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class DatabaseManager {

    private HikariDataSource dataSource;

    /**
     * Method to be passed from the database type used later on
     * @param config
     */
    public void connect(HikariConfig config) {
        // This initializes the pool based on the provided config
        this.dataSource = new HikariDataSource(config);
    }

    /**
     * Closes the connection with the database
     */
    public void disconnect() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    /**
     * @return a connection grabbed from the pool
     * @throws SQLException
     */
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    /**
     * Executes an async database update (INSERT, UPDATE, DELETE, CREATE)
     *
     * @param query The SQL query with '?' placeholders.
     * @param params The variables to inject into the placeholders.
     * @return A CompletableFuture returning true if successful, false if it failed.
     */
    public CompletableFuture<Boolean> executeAsync(String query, Object... params) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(query)) {

                for (int i = 0; i < params.length; i++) {
                    ps.setObject(i + 1, params[i]);
                }

                ps.executeUpdate();
                return true;

            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        });
    }

    /**
     * Executes an async database query (SELECT).
     *
     * @param query The SQL query with '?' placeholders.
     * @param parser A function dictating how to read the ResultSet.
     * @param params The variables to inject into the placeholders.
     * @return A CompletableFuture containing the parsed data.
     */
    public <T> CompletableFuture<T> queryAsync(String query, Function<ResultSet, T> parser, Object... params) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(query)) {

                for (int i = 0; i < params.length; i++) {
                    ps.setObject(i + 1, params[i]);
                }

                try (ResultSet rs = ps.executeQuery()) {
                    return parser.apply(rs);
                }

            } catch (SQLException e) {
                e.printStackTrace();
                return null;
            }
        });
    }

    /**
     * Initializes a local SQLite database connection.
     * Uses Paper's natively bundled SQLite driver.
     */
    public void connectSQLite(Plugin plugin, String fileName) {
        // Ensure the plugin folder actually exists
        // Probably not needed, but just to make sure
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();

        File dbFile = new File(plugin.getDataFolder(), fileName + ".db");

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());

        config.setMaximumPoolSize(1);
        config.setConnectionTestQuery("SELECT 1");

        connect(config);
    }

    /**
     * Initializes a network MySQL connection.
     * Requires 'mysql-connector-j' shaded in pom.xml.
     */
    public void connectMySQL(String host, int port, String database, String username, String password) {
        HikariConfig config = new HikariConfig();

        config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database);
        config.setUsername(username);
        config.setPassword(password);

        // Network optimizations
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(10);
        config.setConnectionTimeout(10000);
        config.setMaxLifetime(1800000);

        connect(config);
    }
}