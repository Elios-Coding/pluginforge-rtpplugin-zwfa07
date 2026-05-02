package com.pluginforge.rtpplugin;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatListener implements Listener {
    private final RTPPlugin plugin;
    private final ActivationManager activationManager;

    public ChatListener(RTPPlugin plugin, ActivationManager activationManager) {
        this.plugin = plugin;
        this.activationManager = activationManager;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!activationManager.isActivating(player)) return;
        String message = event.getMessage();
        // Ignore command-like input so players can still run /rtp etc. while
        // an activation prompt is open.
        if (message != null && message.startsWith("/")) return;
        event.setCancelled(true);
        String serverIp = message == null ? "" : message.trim();
        if (serverIp.isEmpty()) {
            player.sendMessage("Server IP cannot be empty. Activation cancelled.");
            activationManager.cancelActivating(player);
            return;
        }
        activationManager.submitServerIP(player, serverIp);
    }
}
