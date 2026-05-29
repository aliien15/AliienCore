package com.aliiensmp.core.discord;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class DiscordWebhookTest {

    @Test
    void builderMethodsShouldNeverCrashOnNulls() {
        assertDoesNotThrow(() -> {
            new DiscordWebhook("https://fake.url")
                    .setTitle(null)
                    .setColor((String) null)
                    .addField(null, "Value", true)
                    .addField("Name", null, false)
                    .addField(null, null, false);
        });
    }

    @Property
    void hexStringParserSafelyCatchesGarbage(@ForAll String randomGarbage) {
        assertDoesNotThrow(() -> {
            new DiscordWebhook("https://fake.url").setColor(randomGarbage);
        });
    }
}