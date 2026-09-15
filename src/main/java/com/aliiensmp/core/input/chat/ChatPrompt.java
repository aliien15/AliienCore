package com.aliiensmp.core.input.chat;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class ChatPrompt {
    protected static final Map<UUID, ActivePrompt> ACTIVE_PROMPTS = new ConcurrentHashMap<>();
    protected static JavaPlugin plugin;
    protected volatile static String cancellationToggle = "cancel";
    protected record ActivePrompt(Consumer<String> onInput, Runnable onCancel, ScheduledTask timeoutTask) {}

    /**
     * Initialized the Chat Prompt feature so that it can be used
     *
     * @param corePlugin the plugin using this feature
     * @requires {@code corePlugin != null}
     */
    public static void init(JavaPlugin corePlugin) {
        plugin = corePlugin;
    }

    /**
     * Starts a chat input
     *
     * @param player player to register the input from
     * @param waitingTime the time (in ticks) before the plugin gives up on waiting for an input
     * @param input what to do with the input
     * @param onCancel what to do if the input gets canceled (the time runs out, the player leaves, etc)
     * @requires params should not be null
     */
    public static void startInput(@NotNull Player player, long waitingTime, @NotNull Consumer<String> input, @NotNull Runnable onCancel) {
        UUID playerUuid = player.getUniqueId();
        cancelPrompt(playerUuid);

        ScheduledTask timeoutTask = player.getScheduler().runDelayed(plugin, task -> {
            ActivePrompt expiredPrompt = ACTIVE_PROMPTS.remove(playerUuid);
            if (expiredPrompt != null) {
                expiredPrompt.onCancel().run();
            }
        }, null, waitingTime);

        ACTIVE_PROMPTS.put(playerUuid, new ActivePrompt(input, onCancel, timeoutTask));
    }

    /**
     * Cancels a chat prompt/input
     *
     * @param playerUuid UUID of the player who was doing the action
     * @ensures the method properly handles situations where the plugin isn't waiting for a player's input/prompt,
     * which means that this can be used in "just-to-make-sure" situations
     */
    protected static void cancelPrompt(@NotNull UUID playerUuid) {
        Optional.ofNullable(ACTIVE_PROMPTS.remove(playerUuid)).ifPresent(prompt -> {
            prompt.timeoutTask().cancel();
            prompt.onCancel().run();
        });
    }

    /**
     * @param uuid the player's UUID to check
     * @return true if the plugin is currently waiting for a chat input from this player
     */
    public static boolean hasPrompt(@NotNull UUID uuid) {
        return ACTIVE_PROMPTS.containsKey(uuid);
    }

    /**
     * Set whatever players have to type in the chat to cancel a chat prompt to whatever you want. This
     * value is, by default, set to {@code "cancel"}, so if you don't use this method it'll just be that.
     * I recommend making this something intuitive and not something that the players may want to type
     * into the prompt.
     *
     * @param newCancellationToggle the new cancellation {@link String}
     * @requires {@code newCancellationToggle.isBlank() == false}
     * @ensures the async chat listenener will ignore the capitalization of the string. For example, if it is
     *      set to "cancel" players can also use "Cancel", "CANCEL", "CaNcEl", etc.
     * @throws IllegalArgumentException if the String passed into the function is empty or blank
     */
    public static void setCancellationToggle(@NotNull String newCancellationToggle) throws IllegalArgumentException {
        if (newCancellationToggle.isBlank()) {
            throw new IllegalArgumentException("String for chat prompt cancellation must not be blank.");
        }

        cancellationToggle = newCancellationToggle.trim();
    }

    /**
     * Shuts down chat prompts
     * <p>
     * <b>INTERNAL USE ONLY:</b> Called by {@link AliienCore#shutdown()}.
     * Plugin code should not call this directly.
     */
    @ApiStatus.Internal
    public static void shutdown() {
        ACTIVE_PROMPTS.forEach((uuid, prompt) -> {
            try {
                if (prompt.timeoutTask != null) {
                    prompt.timeoutTask.cancel();
                }
            } catch (RuntimeException e) {
                // Plugin is already shut down
            }
        });

        ACTIVE_PROMPTS.clear();
        plugin = null;
    }
}
