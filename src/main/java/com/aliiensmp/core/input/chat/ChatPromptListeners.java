package com.aliiensmp.core.input.chat;

import com.aliiensmp.core.menu.MenuHolder;
import com.aliiensmp.core.utils.MessageUtils;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class ChatPromptListeners implements Listener {

    /**
     * Handles cancelling a chat input if the player leaves the server in the middle of it
     *
     * @param event the player quit event
     */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        ChatPrompt.cancelPrompt(event.getPlayer().getUniqueId());
    }

    /**
     * Handles cancelling a chat input if the player executes a command instead of a valid input
     *
     * @param event the player command execution event
     */
    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        ChatPrompt.cancelPrompt(event.getPlayer().getUniqueId());
    }

    /**
     * Handles a valid or cancellation of a chat input
     *
     * @param event the player chat event
     */
    @EventHandler
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();

        ChatPrompt.ActivePrompt action = ChatPrompt.ACTIVE_PROMPTS.remove(player.getUniqueId());
        if (action == null) return;

        event.setCancelled(true);
        action.timeoutTask().cancel();

        String input = PlainTextComponentSerializer.plainText().serialize(event.message());

        if (input.equalsIgnoreCase("cancel")) {
            action.onCancel().run();
            return;
        }

        action.onInput().accept(input);
    }
}
