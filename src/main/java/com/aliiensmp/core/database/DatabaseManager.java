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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseManager {

    private static final Logger LOGGER = Logger.getLogger(DatabaseManager.class.getName());
    private static final AtomicInteger THREAD_COUNTER = new AtomicInteger(1);
    private static final int MAX_ASYNC_THREADS = 32;

    private volatile HikariDataSource dataSource;
    private volatile ExecutorService asyncExecutor;

    /**
     * Method to be passed from the database type used later on
     * @param config
     */
    public synchronized void connect(HikariConfig config) {
        disconnect();

        if (config.getPoolName() == null) {
            config.setPoolName("AliienCore-Pool-" + THREAD_COUNTER.getAndIncrement());
        }

        this.dataSource = new HikariDataSource(config);
        this.asyncExecutor = createExecutor(config.getMaximumPoolSize());
    }

    /**
     * Closes the connection with the database
     */
    public synchronized void disconnect() {
        ExecutorService executor = asyncExecutor;
        asyncExecutor = null;
        if (executor != null) {
            executor.shutdown();
        }

        HikariDataSource currentDataSource = dataSource;
        dataSource = null;
        if (currentDataSource != null && !currentDataSource.isClosed()) {
            currentDataSource.close();
        }
    }

    /**
     * @return a connection grabbed from the pool
     * @throws SQLException
     */
    public Connection getConnection() throws SQLException {
        HikariDataSource currentDataSource = dataSource;
        if (currentDataSource == null || currentDataSource.isClosed()) {
            throw new SQLException("Database connection pool has not been initialized.");
        }
        return currentDataSource.getConnection();
    }

    /**
     * Executes an async database update (INSERT, UPDATE, DELETE, CREATE)
     *
     * @param query The SQL query with '?' placeholders.
     * @param params The variables to inject into the placeholders.
     * @return A CompletableFuture returning true if successful, false if it failed.
     */
    public CompletableFuture<Boolean> executeAsync(String query, Object... params) {
        ExecutorService executor = getAsyncExecutor();
        if (executor == null) {
            return CompletableFuture.completedFuture(false);
        }

        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(query)) {

                bindParameters(ps, params);

                ps.executeUpdate();
                return true;

            } catch (SQLException e) {
                logFailure("update", query, e);
                return false;
            }
        }, executor);
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
        ExecutorService executor = getAsyncExecutor();
        if (executor == null) {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(query)) {

                bindParameters(ps, params);

                try (ResultSet rs = ps.executeQuery()) {
                    return parser.apply(rs);
                }

            } catch (SQLException | RuntimeException e) {
                logFailure("query", query, e);
                return null;
            }
        }, executor);
    }

    /**
     * Initializes a local SQLite database connection.
     */
    public void connectSQLite(Plugin plugin, String fileName) {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs() && !plugin.getDataFolder().isDirectory()) {
            throw new IllegalStateException("Unable to create plugin data folder: " + plugin.getDataFolder().getAbsolutePath());
        }

        String finalFileName = fileName.endsWith(".db") ? fileName : fileName + ".db";
        File dbFile = new File(plugin.getDataFolder(), finalFileName);

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
        config.setMaximumPoolSize(1);
        config.setMinimumIdle(1);
        config.setConnectionTestQuery("SELECT 1");
        config.setConnectionTimeout(10_000L);
        config.setValidationTimeout(5_000L);
        config.setPoolName("AliienCore-SQLite");

        connect(config);
    }

    /**
     * Standard MySQL connection with my default network optimizations
     */
    public void connectMySQL(String host, int port, String database, String username, String password) {
        connectMySQL(host, port, database, username, password, 10, 10, 10000, 1800000);
    }

    /**
     * MySQL connection that allows full control over HikariCP optimizations
     */
    public void connectMySQL(String host, int port, String database, String username, String password, int maxPoolSize, int minIdle, long timeout, long maxLifetime) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(maxPoolSize);
        config.setMinimumIdle(Math.min(Math.max(1, minIdle), maxPoolSize));
        config.setConnectionTimeout(timeout);
        config.setValidationTimeout(Math.min(timeout, 5_000L));
        config.setMaxLifetime(maxLifetime);
        if (maxLifetime > 0L) {
            long keepaliveTime = Math.min(300_000L, Math.max(30_000L, maxLifetime / 2));
            if (keepaliveTime < maxLifetime) {
                config.setKeepaliveTime(keepaliveTime);
            }
        }
        config.setPoolName("AliienCore-MySQL");

        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("rewriteBatchedStatements", "true");
        config.addDataSourceProperty("cacheResultSetMetadata", "true");
        config.addDataSourceProperty("maintainTimeStats", "false");
        config.addDataSourceProperty("tcpKeepAlive", "true");
        config.addDataSourceProperty("useSSL", "false");
        config.addDataSourceProperty("allowPublicKeyRetrieval", "true");

        connect(config);
    }

    private ExecutorService createExecutor(int maximumPoolSize) {
        int threadCount = Math.max(1, Math.min(maximumPoolSize, MAX_ASYNC_THREADS));
        return Executors.newFixedThreadPool(threadCount, runnable -> {
            Thread thread = new Thread(runnable, "AliienCore-DB-" + THREAD_COUNTER.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        });
    }

    private ExecutorService getAsyncExecutor() {
        ExecutorService executor = asyncExecutor;
        if (executor == null || executor.isShutdown()) {
            LOGGER.warning("DatabaseManager was used before a connection pool was initialized.");
            return null;
        }
        return executor;
    }

    private void bindParameters(PreparedStatement statement, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            statement.setObject(i + 1, params[i]);
        }
    }

    private void logFailure(String operation, String query, Exception exception) {
        LOGGER.log(Level.WARNING, "Failed to execute database " + operation + ": " + query, exception);
    }
}
