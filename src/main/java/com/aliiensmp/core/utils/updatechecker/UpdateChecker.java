package com.aliiensmp.core.utils.updatechecker;

import org.bukkit.plugin.Plugin;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class UpdateChecker {

    private static final int CONNECT_TIMEOUT_MS = 5_000;
    private static final int READ_TIMEOUT_MS = 5_000;
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(CONNECT_TIMEOUT_MS))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

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
        HttpRequest request;
        try {
            request = HttpRequest.newBuilder(new URI(this.versionUrl))
                    .timeout(Duration.ofMillis(READ_TIMEOUT_MS))
                    .header("User-Agent", plugin.getName() + "/" + plugin.getDescription().getVersion())
                    .GET()
                    .build();
        } catch (URISyntaxException | IllegalArgumentException exception) {
            logUpdateFailure(exception);
            return CompletableFuture.completedFuture(Optional.<String>empty());
        } catch (NullPointerException exception) {
            return CompletableFuture.failedFuture(exception);
        }

        return HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .thenApply(response -> {
                    if (response.statusCode() < 200 || response.statusCode() >= 300) {
                        plugin.getLogger().warning("Unable to check for updates: HTTP " + response.statusCode());
                        return Optional.<String>empty();
                    }

                    String responseBody = response.body();
                    if (responseBody == null) {
                        return Optional.<String>empty();
                    }

                    int lineEnd = responseBody.indexOf('\n');
                    String version = lineEnd >= 0 ? responseBody.substring(0, lineEnd) : responseBody;
                    String trimmedVersion = version.trim();
                    return trimmedVersion.isEmpty() ? Optional.<String>empty() : Optional.of(trimmedVersion);
                })
                .exceptionally(exception -> {
                    logUpdateFailure(exception);
                    return Optional.empty();
                });
    }

    private void logUpdateFailure(Throwable exception) {
        Throwable cause = exception.getCause() != null ? exception.getCause() : exception;
        plugin.getLogger().warning("Unable to check for updates: " + cause.getMessage());
    }
}