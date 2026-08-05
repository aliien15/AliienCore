package com.aliiensmp.core.discord;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

/**
 * A chainable, asynchronous builder for sending Discord Webhooks.
 * Utilizes Java's native HttpClient to ensure completely non-blocking execution.
 */
public class DiscordWebhook {

    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    private final String webhookUrl;
    private final JsonObject embed;
    private final JsonArray fieldsArray;
    private String content = null;

    /**
     * Initializes a new Discord Webhook payload.
     *
     * @param webhookUrl The destination URL for the webhook.
     */
    public DiscordWebhook(String webhookUrl) {
        this.webhookUrl = webhookUrl;
        this.embed = new JsonObject();
        this.fieldsArray = new JsonArray();
    }

    /**
     * Sets the main title of the embed.
     *
     * @param title The text to display as the title.
     * @return This builder instance for chaining.
     */
    public DiscordWebhook setTitle(String title) {
        if (title != null) this.embed.addProperty("title", title);
        return this;
    }

    /**
     * Sets the description of the embed
     *
     * @param description The text to display as the description
     * @return This builder instance for chaining.
     */
    public DiscordWebhook setDescription(String description) {
        if (description != null) this.embed.addProperty("description", description);
        return this;
    }

    /**
     * Sets the embed color using a raw decimal integer.
     *
     * @param hexColor The integer representation of a hex color (e.g., 0xFF0000).
     * @return This builder instance for chaining.
     */
    public DiscordWebhook setColor(int hexColor) {
        this.embed.addProperty("color", hexColor);
        return this;
    }

    /**
     * Sets the embed color using a standard hex string (e.g., "#FF0000" or "FF0000").
     * This is perfect for parsing colors directly from config files.
     * * @param hexString The string representation of a hex color.
     * @return This builder instance for chaining.
     */
    public DiscordWebhook setColor(String hexString) {
        if (hexString == null || hexString.isEmpty()) return this;

        try {
            String cleanHex = hexString.replace("#", "");
            int parsedColor = Integer.parseInt(cleanHex, 16);

            this.embed.addProperty("color", parsedColor);
        } catch (NumberFormatException e) {
            System.err.println("[AliienCore] Invalid hex color string provided to Discord Webhook: " + hexString);
        }

        return this;
    }

    /**
     * Sets the top-level message content of the webhook payload.
     * <p>
     * Unlike embed fields, text set here is displayed outside the embed card
     * and is capable of triggering standard Discord push notifications or mentions.
     *
     * @param content The raw text message to send.
     * @return This builder instance for chaining.
     */
    public DiscordWebhook setContent(String content) {
        return setContent(content, false);
    }

    /**
     * Sets the top-level message content of the webhook payload, with optional
     * automatic Discord role mention formatting.
     * <p>
     * When {@code isRoleToPing} is set to {@code true}, the input string is
     * automatically wrapped in Discord's role mention syntax ({@code <@&ID>}).
     * This allows server owners to supply raw role IDs directly from configuration
     * files without manually adding the mention tag.
     *
     * @param content The raw text message or Discord Role ID to send.
     * @param isRoleToPing If {@code true}, wraps the input string in Discord's role mention format ({@code <@&content>}).
     * @return This builder instance for chaining.
     */
    public DiscordWebhook setContent(String content, boolean isRoleToPing) {
        if (content != null && !content.isEmpty())
            this.content = isRoleToPing ? "<@&" + content + ">" : content;

        return this;
    }

    /**
     * Adds a custom data field to the Discord embed.
     * Fields are perfect for displaying structured key-value information (e.g., "Staff Member" -> "AdminName").
     *
     * @param name   The title or header of the field.
     * @param value  The main text content of the field.
     * @param inline If true, the field will attempt to display side-by-side with other inline fields.
     * @return This builder instance for chaining.
     */
    public DiscordWebhook addField(String name, String value, boolean inline) {
        if (name == null || value == null) return this;

        JsonObject object = new JsonObject();
        object.addProperty("name", name);
        object.addProperty("value", value);
        object.addProperty("inline", inline);

        this.fieldsArray.add(object);
        return this;
    }

    /**
     * Compiles the configured webhook data into a JSON payload and dispatches it
     * asynchronously to the Discord API using Java's native HttpClient.
     * * Because this utilizes CompletableFuture.runAsync(), it is completely non-blocking
     * and is strictly safe to execute directly on the main server thread without causing lag.
     */
    public void sendAsync() {
        if (this.webhookUrl == null || this.webhookUrl.isEmpty()) return;

        CompletableFuture.runAsync(() -> {
            try {
                // Attach the fields array to the embed if it has anything in it
                if (!this.fieldsArray.isEmpty()) {
                    this.embed.add("fields", this.fieldsArray);
                }

                JsonArray embedsArray = new JsonArray();
                embedsArray.add(this.embed);

                JsonObject payload = new JsonObject();
                payload.add("embeds", embedsArray);
                if (this.content != null) payload.addProperty("content", this.content);

                // Build the HTTP POST request
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(this.webhookUrl))
                        .header("Content-Type", "application/json")
                        .header("User-Agent", "AliienCore")
                        .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                        .build();

                // Send the request async
                HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                        .thenAccept(response -> {
                            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                                System.err.println("[AliienCore] Discord Webhook failed: HTTP " + response.statusCode());
                            }
                        });

            } catch (Exception e) {
                System.err.println("[AliienCore] Failed to build Discord Webhook payload.");
                e.printStackTrace();
            }
        });
    }
}