package com.aliiensmp.core.utils.updatechecker;

import org.bukkit.plugin.Plugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class UpdateChecker {

    private static final int CONNECT_TIMEOUT_MS = 5_000;
    private static final int READ_TIMEOUT_MS = 5_000;

    private final Plugin plugin;
    private final String versionUrl;

    public UpdateChecker(Plugin plugin, String versionUrl) {
        this.plugin = plugin;
        this.versionUrl = versionUrl;
    }

    public void getVersion(final Consumer<String> consumer) {
        fetchVersion().thenAccept(version -> version.ifPresent(resolvedVersion ->
                plugin.getServer().getGlobalRegionScheduler().execute(plugin, () -> {
                    if (plugin.isEnabled()) {
                        consumer.accept(resolvedVersion);
                    }
                })
        ));
    }

    public CompletableFuture<Optional<String>> fetchVersion() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                URLConnection connection = new URI(this.versionUrl).toURL().openConnection();
                connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
                connection.setReadTimeout(READ_TIMEOUT_MS);
                connection.setRequestProperty("User-Agent", plugin.getName() + "/" + plugin.getDescription().getVersion());

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    String version = reader.readLine();
                    if (version == null) {
                        return Optional.empty();
                    }

                    String trimmedVersion = version.trim();
                    return trimmedVersion.isEmpty() ? Optional.empty() : Optional.of(trimmedVersion);
                }
            } catch (IOException | URISyntaxException exception) {
                plugin.getLogger().warning("Unable to check for updates: " + exception.getMessage());
                return Optional.empty();
            }
        });
    }
}
