package com.aliiensmp.core.files;

import com.aliiensmp.core.utils.DebugUtils;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class FileLogger {
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
    private static final ConcurrentHashMap<String, Object> FILE_LOCKS = new ConcurrentHashMap<>();
    private Path folderPath;
    private String fileName = "logs.log";
    private volatile boolean enabled = true;

    public FileLogger(@NotNull Plugin plugin) {
        this.folderPath = plugin.getDataFolder().toPath();
    }

    /**
     * Sets the path to where the FileLogger will attempt to write to. By default, this is set to
     * the path of the plugin used to initialize this instance, so you do not need to use this unless
     * you want to use some other specific path.
     *
     * @param folderPath the path where the FileLogger will write to.
     * @return this instance for chaining.
     */
    public FileLogger setPath(@NotNull Path folderPath) {
        this.folderPath = folderPath;
        return this;
    }

    /**
     * Set the name of the file to write to. This is set to "logs.log" by default.
     *
     * @param fileName the name of the file.
     * @return this instance for chaining.
     * @requires fileName must have the file extension at the end (e.g. "logs.log" or "logs.txt")
     * @throws IllegalArgumentException if {@code fileName.isBlank() == true}
     */
    public FileLogger setFileName(@NotNull String fileName) {
        if (fileName.isBlank()) {
            throw new IllegalArgumentException("File name cannot be blank!");
        }

        this.fileName = fileName;
        return this;
    }

    /**
     * Sets the enabled status, useful if you wanna have this option toggled but don't
     * want to have to manually write if statements everywhere.
     *
     * @param enabled the new enabled status.
     * @return this instance for chaining.
     */
    public FileLogger setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    /**
     * Toggles the enabled status, useful if you wanna have this option toggled but don't
     * want to have to manually write if statements everywhere.
     *
     * @return the new toggle status after it being toggled
     */
    public boolean toggleEnabled() {
        this.enabled = !this.enabled;
        return this.enabled;
    }

    /**
     * Sends a logging message to the desired file (set before calling this method), respecting the enabled
     * status. This method is run async through a {@link CompletableFuture#runAsync(Runnable)}
     *
     * @param message the message to log into the file.
     */
    public void logAsync(@NotNull String message) {
        if (!this.enabled) {
            return;
        }

        CompletableFuture.runAsync(() -> {
            Path targetFile = this.folderPath.resolve(this.fileName);
            String absolutePath = targetFile.toAbsolutePath().toString();
            Object fileLock = FILE_LOCKS.computeIfAbsent(absolutePath, k -> new Object());

            synchronized (fileLock) {
                try {
                    if (!Files.exists(this.folderPath)) {
                        Files.createDirectories(this.folderPath);
                    }

                    String timeStamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
                    String formattedMessage = "[" + timeStamp + "] " + message + System.lineSeparator();

                    Files.writeString(targetFile, formattedMessage, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e) {
                    DebugUtils.send(Level.SEVERE, "An error occurred while trying to write to %file%: %error%",
                            "%file%", this.fileName,
                            "%error%", e.getMessage()
                    );
                }
            }
        });
    }
}