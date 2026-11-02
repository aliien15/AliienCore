package com.aliiensmp.core.discord;

import static org.junit.jupiter.api.Assertions.*;

import com.aliiensmp.core.support.HttpTestServer;
import com.google.gson.JsonParser;

import org.junit.jupiter.api.Test;

import java.util.concurrent.*;

class DiscordWebhookHttpTest {
    @Test
    void sendsEscapedJsonWithRoleMentionAndEmbedFields() throws Exception {
        BlockingQueue<String> requests = new LinkedBlockingQueue<>();

        try (var http =
                new HttpTestServer(
                        exchange -> {
                            try {
                                assertEquals("POST", exchange.getRequestMethod());
                                assertEquals(
                                        "application/json",
                                        exchange.getRequestHeaders().getFirst("Content-Type"));
                                requests.add(
                                        new String(
                                                exchange.getRequestBody().readAllBytes(),
                                                java.nio.charset.StandardCharsets.UTF_8));
                            } catch (java.io.IOException e) {
                                throw new java.io.UncheckedIOException(e);
                            }
                            HttpTestServer.respond(exchange, 200, "ok");
                        })) {
            new DiscordWebhook(http.url())
                    .setTitle("Title \"quoted\"")
                    .setDescription("line\n☃")
                    .setColor("#12AB34")
                    .setContent("1234", true)
                    .addField("name", "value", true)
                    .addField(null, "ignored", false)
                    .sendAsync();
            String body = requests.poll(10, TimeUnit.SECONDS);

            assertNotNull(body);

            var payload = JsonParser.parseString(body).getAsJsonObject();

            assertEquals("<@&1234>", payload.get("content").getAsString());

            var embed = payload.getAsJsonArray("embeds").get(0).getAsJsonObject();

            assertEquals("Title \"quoted\"", embed.get("title").getAsString());
            assertEquals("line\n☃", embed.get("description").getAsString());
            assertEquals(0x12AB34, embed.get("color").getAsInt());
            assertEquals(1, embed.getAsJsonArray("fields").size());
            assertTrue(
                    embed.getAsJsonArray("fields")
                            .get(0)
                            .getAsJsonObject()
                            .get("inline")
                            .getAsBoolean());
        }
    }

    @Test
    void absentDestinationIsANoop() {
        assertDoesNotThrow(() -> new DiscordWebhook(null).sendAsync());
        assertDoesNotThrow(() -> new DiscordWebhook("").sendAsync());
    }
}
