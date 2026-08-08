package com.aliiensmp.core.input.chat;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class ChatPrompt {
    protected static final Map<UUID, ActivePrompt> ACTIVE_PROMPTS = new ConcurrentHashMap();
    protected static JavaPlugin plugin;
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
    public void startInput(Player player, long waitingTime, Consumer<String> input, Runnable onCancel) {
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
    protected static void cancelPrompt(UUID playerUuid) {
        Optional.ofNullable(ACTIVE_PROMPTS.remove(playerUuid)).ifPresent(prompt -> {
            prompt.timeoutTask().cancel();
            prompt.onCancel().run();
        });
    }
}
