package com.aliiensmp.core.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

/**
 * Centralized database manager handling connection pooling via HikariCP
 * and non-blocking asynchronous queries.
 */
public class DatabaseManager {

    private static final Logger LOGGER = Logger.getLogger(DatabaseManager.class.getName());
    private static final AtomicInteger THREAD_COUNTER = new AtomicInteger(1);
    private static final int MAX_ASYNC_THREADS = 32;

    private volatile HikariDataSource dataSource;
    private volatile ExecutorService asyncExecutor;

    /**
     * Initializes the connection pool and backing executor service using a pre-configured {@link HikariConfig}.
     * <p>
     * If an existing pool or executor is active, it will be safely shut down before the new one is created.
     *
     * @param config the configured HikariCP settings
     */
    public synchronized void connect(@NotNull HikariConfig config) {
        disconnect();

        if (config.getPoolName() == null) {
            config.setPoolName("AliienCore-Pool-" + THREAD_COUNTER.getAndIncrement());
        }

        this.dataSource = new HikariDataSource(config);
        this.asyncExecutor = createExecutor(config.getMaximumPoolSize());
    }

    /**
     * Closes the active {@link HikariDataSource} connection pool and terminates
     * the asynchronous thread pool executor.
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
     * Obtains an active connection from the pool.
     *
     * @return an active {@link Connection} from the pool
     * @throws SQLException if the pool is uninitialized, closed, or unable to allocate a connection
     */
    @NotNull
    public Connection getConnection() throws SQLException {
        HikariDataSource currentDataSource = dataSource;
        if (currentDataSource == null || currentDataSource.isClosed()) {
            throw new SQLException("Database connection pool has not been initialized.");
        }
        return currentDataSource.getConnection();
    }

    /**
     * Executes an asynchronous write operation ({@code INSERT}, {@code UPDATE}, {@code DELETE}, {@code CREATE}).
     *
     * @param query  the SQL statement using '?' parameter placeholders
     * @param params positional arguments matching the query placeholders
     * @return a {@link CompletableFuture} yielding {@code true} if successful, or {@code false} if an error occurred
     */
    @NotNull
    public CompletableFuture<Boolean> executeAsync(@NotNull String query, @NotNull Object... params) {
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
     * Executes an asynchronous read operation ({@code SELECT}) and parses the resulting {@link ResultSet}.
     *
     * @param <T>    the return type parsed from the query result
     * @param query  the SQL statement using '?' parameter placeholders
     * @param parser a transformation function mapping the {@link ResultSet} to the target type
     * @param params positional arguments matching the query placeholders
     * @return a {@link CompletableFuture} containing the parsed result, or {@code null} if execution failed
     */
    @NotNull
    public <T> CompletableFuture<@Nullable T> queryAsync(@NotNull String query, @NotNull Function<ResultSet, T> parser, @NotNull Object... params) {
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
     * Initializes a local SQLite database connection pool.
     * <p>
     * Pool size is strictly locked to 1 to prevent database file locks.
     * The file name does not require the {@code .db} extension; it will be appended automatically if missing.
     *
     * @param plugin   the owning plugin instance used to resolve the data folder
     * @param fileName the target database file name
     * @throws IllegalStateException if the plugin data directory cannot be created
     */
    public void connectSQLite(@NotNull Plugin plugin, @NotNull String fileName) throws IllegalStateException {
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
     * Initializes an embedded H2 database connection pool using default connection limits and timeouts.
     *
     * @param plugin   the owning plugin instance used to resolve the data folder
     * @param fileName the target database file name (without {@code .db} or {@code .mv.db})
     * @throws IllegalStateException if the plugin data directory cannot be created
     */
    public void connectH2(@NotNull Plugin plugin, @NotNull String fileName) throws IllegalStateException {
        connectH2(plugin, fileName, 10, 2, 10_000L, 1_800_000L);
    }

    /**
     * Initializes an embedded H2 database connection pool with fine-grained pool sizing and timeouts.
     *
     * @param plugin      the owning plugin instance used to resolve the data folder
     * @param fileName    the target database file name
     * @param maxPoolSize the maximum number of connections allowed in the pool
     * @param minIdle     the minimum number of idle connections maintained
     * @param timeout     maximum time in milliseconds to wait for a connection from the pool
     * @param maxLifetime maximum lifetime in milliseconds for an existing connection in the pool
     * @throws IllegalStateException if the plugin data directory cannot be created
     */
    public void connectH2(@NotNull Plugin plugin, @NotNull String fileName, int maxPoolSize, int minIdle, long timeout, long maxLifetime) throws IllegalStateException {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs() && !plugin.getDataFolder().isDirectory()) {
            throw new IllegalStateException("Unable to create plugin data folder: " + plugin.getDataFolder().getAbsolutePath());
        }

        String cleanName = fileName.endsWith(".db") ? fileName.substring(0, fileName.lastIndexOf('.')) : fileName;
        File dbFile = new File(plugin.getDataFolder(), cleanName);

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:h2:file:" + dbFile.getAbsolutePath() + ";AUTO_SERVER=TRUE;MODE=MySQL");
        config.setDriverClassName("org.h2.Driver");
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
        config.setPoolName("AliienCore-H2");

        connect(config);
    }

    /**
     * Initializes a MySQL connection pool using default limits (max pool 10, min idle 10, 10s timeout, 30m lifetime).
     *
     * @param host     the database host address
     * @param port     the database port (usually 3306)
     * @param database the schema or database name
     * @param username the authentication user
     * @param password the authentication password
     */
    public void connectMySQL(@NotNull String host, int port, @NotNull String database, @NotNull String username, @NotNull String password) {
        connectMySQL(host, port, database, username, password, 10, 10, 10_000L, 1_800_000L);
    }

    /**
     * Initializes a MySQL connection pool with fine-grained performance properties and pool limits.
     *
     * @param host        the database host address
     * @param port        the database port (usually 3306)
     * @param database    the schema or database name
     * @param username    the authentication user
     * @param password    the authentication password
     * @param maxPoolSize the maximum number of connections in the pool
     * @param minIdle     the minimum number of idle connections maintained
     * @param timeout     connection timeout in milliseconds
     * @param maxLifetime maximum lifetime of a connection in milliseconds
     */
    public void connectMySQL(@NotNull String host, int port, @NotNull String database, @NotNull String username, @NotNull String password, int maxPoolSize, int minIdle, long timeout, long maxLifetime) {
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

    /**
     * Initializes a MariaDB connection pool using default limits (max pool 10, min idle 10, 10s timeout, 30m lifetime).
     *
     * @param host     the database host address
     * @param port     the database port (usually 3306)
     * @param database the schema or database name
     * @param username the authentication user
     * @param password the authentication password
     */
    public void connectMariaDB(@NotNull String host, int port, @NotNull String database, @NotNull String username, @NotNull String password) {
        connectMariaDB(host, port, database, username, password, 10, 10, 10_000L, 1_800_000L);
    }

    /**
     * Initializes a MariaDB connection pool with fine-grained performance properties and pool limits.
     *
     * @param host        the database host address
     * @param port        the database port (usually 3306)
     * @param database    the schema or database name
     * @param username    the authentication user
     * @param password    the authentication password
     * @param maxPoolSize the maximum number of connections in the pool
     * @param minIdle     the minimum number of idle connections maintained
     * @param timeout     connection timeout in milliseconds
     * @param maxLifetime maximum lifetime of a connection in milliseconds
     */
    public void connectMariaDB(@NotNull String host, int port, @NotNull String database, @NotNull String username, @NotNull String password, int maxPoolSize, int minIdle, long timeout, long maxLifetime) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mariadb://" + host + ":" + port + "/" + database);
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
        config.setPoolName("AliienCore-MariaDB");

        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("cacheResultSetMetadata", "true");
        config.addDataSourceProperty("maintainTimeStats", "false");
        config.addDataSourceProperty("tcpKeepAlive", "true");
        config.addDataSourceProperty("useSSL", "false");
        config.addDataSourceProperty("allowPublicKeyRetrieval", "true");

        connect(config);
    }

    @NotNull
    private ExecutorService createExecutor(int maximumPoolSize) {
        int threadCount = Math.max(1, Math.min(maximumPoolSize, MAX_ASYNC_THREADS));
        return Executors.newFixedThreadPool(threadCount, runnable -> {
            Thread thread = new Thread(runnable, "AliienCore-DB-" + THREAD_COUNTER.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        });
    }

    @Nullable
    private ExecutorService getAsyncExecutor() {
        ExecutorService executor = asyncExecutor;
        if (executor == null || executor.isShutdown()) {
            LOGGER.warning("DatabaseManager was used before a connection pool was initialized.");
            return null;
        }
        return executor;
    }

    private void bindParameters(@NotNull PreparedStatement statement, @NotNull Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            statement.setObject(i + 1, params[i]);
        }
    }

    private void logFailure(@NotNull String operation, @NotNull String query, @NotNull Exception exception) {
        LOGGER.log(Level.WARNING, "Failed to execute database " + operation + ": " + query, exception);
    }
}