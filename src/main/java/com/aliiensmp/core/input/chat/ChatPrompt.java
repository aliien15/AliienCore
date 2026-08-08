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

    public static void init(JavaPlugin corePlugin) {
        plugin = corePlugin;
    }

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

    protected static void cancelPrompt(UUID playerUuid) {
        Optional.ofNullable(ACTIVE_PROMPTS.remove(playerUuid)).ifPresent(prompt -> {
            prompt.timeoutTask().cancel();
            prompt.onCancel().run();
    });
    }
}
