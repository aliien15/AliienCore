package com.aliiensmp.core.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Universal dispatcher for sending formatted messages, action bars, and titles.
 * Automatically handles prefix injection, local placeholders, ColorUtils parsing,
 * and PlaceholderAPI integration (if installed).
 */
public final class MessageUtils {

    private static final Method PLACEHOLDER_METHOD = resolvePlaceholderMethod();
    private static final Title.Times DEFAULT_TITLE_TIMES = Title.Times.times(
            Duration.ofMillis(500),
            Duration.ofMillis(3500),
            Duration.ofMillis(1000)
    );

    private MessageUtils() {
    }

    /**
     * Sends a fully formatted message to a {@link CommandSender}.
     *
     * @param sender       The recipient (Player or Console).
     * @param prefix       The plugin's prefix (can be empty or null).
     * @param message      The raw message string.
     * @param placeholders Local placeholder pairs (e.g., "%player%", "Aliien").
     */
    public static void send(CommandSender sender, String prefix, String message, String... placeholders) {
        if (message == null || message.isEmpty()) return;

        String finalMessage = applyPlaceholders(message, placeholders);
        sendResolved(sender, prefix, finalMessage);
    }


    /**
     * Sends a fully formatted message to a {@link Player} if they are online through their {@link UUID}
     * This is recommended over the regular send method if you are executing this async/outside the main thread
     *
     * @param playerUuid   The Player UUID
     * @param prefix       The plugin's prefix (can be empty or null).
     * @param message      The raw message string.
     * @param placeholders Local placeholder pairs (e.g., "%player%", "Aliien").
     */
    public static void sendIfOnline(UUID playerUuid, String prefix, String message, String... placeholders) {
        Optional.ofNullable(Bukkit.getPlayer(playerUuid)).ifPresent(p -> send(p, prefix, message, placeholders));
    }

    /**
     * Broadcasts a fully formatted message to all online players and the console.
     * Utilizes the existing send method to ensure PAPI placeholders are parsed per-player.
     *
     * @param prefix The plugin's prefix (can be empty or null).
     * @param message The raw message string.
     * @param placeholders Local placeholder pairs (e.g., "%event%", "boss_fight").
     */
    public static void broadcast(String prefix, String message, String... placeholders) {
        broadcast(prefix, message, player -> true, placeholders);
    }

    /**
     * Broadcasts a fully formatted message certain online players and the console.
     * Utilizes the existing send method to ensure PAPI placeholders are parsed per-player.
     *
     * @param prefix The plugin's prefix (can be empty or null).
     * @param message The raw message string.
     * @param filter a lambda to filter out players who shouldn't get this message.
     * @param placeholders Local placeholder pairs (e.g., "%event%", "boss_fight").
     * @ensures if {@code filter == null} it is handled as a true value, meaning it will target all players and won't throw a {@link NullPointerException}
     */
    public static void broadcast(String prefix, String message, Predicate<Player> filter, String... placeholders) {
        if (message == null || message.isEmpty()) return;

        Predicate<Player> playerFilter = filter != null ? filter : player -> true;
        String finalMessage = applyPlaceholders(message, placeholders);

        Bukkit.getOnlinePlayers().stream()
                .filter(playerFilter)
                .forEach(player -> sendResolved(player, prefix, finalMessage));

        sendResolved(Bukkit.getConsoleSender(), prefix, finalMessage);
    }

    /**
     * Sends a fully formatted Action Bar message to a {@link Player}
     *
     * @param player       The recipient.
     * @param message      The raw action bar string.
     * @param placeholders Local placeholder pairs.
     */
    public static void sendActionBar(Player player, String message, String... placeholders) {
        if (message == null || message.isEmpty()) return;

        String finalMessage = applyPlaceholders(message, placeholders);
        finalMessage = applyPAPI(player, finalMessage);

        player.sendActionBar(ColorUtils.color(finalMessage));
    }

    /**
     * Sends a fully formatted Action Bar message to a {@link Player} if they are online through their {@link UUID}
     * This is recommended over the regular sendActionBar method if you are executing this async/outside the main thread
     *
     * @param playerUuid   The recipient.
     * @param message      The raw action bar string.
     * @param placeholders Local placeholder pairs.
     */
    public static void sendActionBarIfOnline(UUID playerUuid, String message, String... placeholders) {
        Optional.ofNullable(Bukkit.getPlayer(playerUuid)).ifPresent(p -> sendActionBar(p, message, placeholders));
    }

    /**
     * Sends a fully formatted Title and Subtitle to a Player.
     *
     * @param player       The recipient.
     * @param title        The raw main title string.
     * @param subtitle     The raw subtitle string.
     * @param placeholders Local placeholder pairs.
     */
    public static void sendTitle(Player player, String title, String subtitle, String... placeholders) {
        String finalTitle = title != null ? applyPAPI(player, applyPlaceholders(title, placeholders)) : "";
        String finalSubtitle = subtitle != null ? applyPAPI(player, applyPlaceholders(subtitle, placeholders)) : "";

        player.showTitle(Title.title(
                ColorUtils.color(finalTitle),
                ColorUtils.color(finalSubtitle),
                DEFAULT_TITLE_TIMES
        ));
    }

    /**
     * Sends a fully formatted Title and Subtitle to a {@link Player} if they are online through their {@link UUID}
     * This is recommended over the regular sendTitle method if you are executing this async/outside the main thread
     *
     * @param playerUuid   The recipient.
     * @param title        The raw main title string.
     * @param subtitle     The raw subtitle string.
     * @param placeholders Local placeholder pairs.
     */
    public static void sendTitleIfOnline(UUID playerUuid, String title, String subtitle, String... placeholders) {
        Optional.ofNullable(Bukkit.getPlayer(playerUuid)).ifPresent(p -> sendTitle(p, title, subtitle, placeholders));
    }

    /**
     * Internal helper to process local key-value vararg placeholders.
     */
    private static String applyPlaceholders(String text, String... placeholders) {
        if (placeholders == null || placeholders.length == 0) return text;

        String result = text;
        for (int i = 0; i < placeholders.length; i += 2) {
            if (i + 1 < placeholders.length) {
                result = result.replace(placeholders[i], placeholders[i + 1]);
            }
        }
        return result;
    }

    private static void sendResolved(CommandSender sender, String prefix, String message) {
        String fullMessage = (prefix != null ? prefix : "") + message;

        // Apply PlaceholderAPI if the sender is a Player and PAPI is installed
        if (sender instanceof Player player) {
            fullMessage = applyPAPI(player, fullMessage);
        }

        Component component = ColorUtils.color(fullMessage);
        sender.sendMessage(component);
    }

    /**
     * Internal helper to safely parse PlaceholderAPI placeholders.
     * Fails silently and returns the original text if PAPI is not installed if it's not in the main thread.
     */
    private static String applyPAPI(Player player, String text) {
        if (PLACEHOLDER_METHOD == null || !Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            return text;
        }

        if (!Bukkit.isPrimaryThread()) {
            return text;
        }

        try {
            return (String) PLACEHOLDER_METHOD.invoke(null, player, text);
        } catch (IllegalAccessException | InvocationTargetException | ClassCastException exception) {
            return text;
        }
    }

    private static Method resolvePlaceholderMethod() {
        try {
            return Class.forName("me.clip.placeholderapi.PlaceholderAPI")
                    .getMethod("setPlaceholders", Player.class, String.class);
        } catch (ClassNotFoundException | NoSuchMethodException exception) {
            return null;
        }
    }
}
