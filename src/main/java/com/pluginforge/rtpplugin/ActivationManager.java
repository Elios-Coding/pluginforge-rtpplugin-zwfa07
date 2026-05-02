package com.pluginforge.rtpplugin;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ActivationManager {
    private final RTPPlugin plugin;
    private final Set<UUID> activating = new HashSet<>();

    public ActivationManager(RTPPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isActivated() {
        return plugin.getConfig().getBoolean("activated", false);
    }

    public void beginActivation(Player player) {
        activating.add(player.getUniqueId());
    }

    public boolean isActivating(Player player) {
        return activating.contains(player.getUniqueId());
    }

    public void cancelActivating(Player player) {
        activating.remove(player.getUniqueId());
    }

    public void submitServerIP(Player player, String serverIp) {
        activating.remove(player.getUniqueId());
        plugin.getConfig().set("server-ip", serverIp);
        plugin.saveConfig();
        player.sendMessage("Verifying server IP with Firebase...");

        new BukkitRunnable() {
            @Override
            public void run() {
                boolean ok = postToFirebase(serverIp);
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (!player.isOnline()) return;
                        if (ok) {
                            plugin.getConfig().set("activated", true);
                            plugin.saveConfig();
                            player.sendMessage("Plugin successfully activated");
                        } else {
                            player.sendMessage("Activation failed. Firebase error");
                        }
                    }
                }.runTask(plugin);
            }
        }.runTaskAsynchronously(plugin);
    }

    private boolean postToFirebase(String serverIp) {
        String base = plugin.getConfig().getString("firebase-url", "https://rtp-backend-6c475-default-rtdb.firebaseio.com");
        if (base == null || base.isEmpty()) return false;
        if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        String endpoint = base + "/servers.json";

        String json = "{\"serverIp\":\"" + escape(serverIp)
                + "\",\"timestamp\":" + System.currentTimeMillis()
                + ",\"plugin\":\"RTPPlugin\"}";

        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(endpoint).openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            try (OutputStream os = conn.getOutputStream()) {
                os.write(json.getBytes(StandardCharsets.UTF_8));
            }
            int code = conn.getResponseCode();
            plugin.getLogger().info("Firebase activation responded with HTTP " + code);
            return code == 200;
        } catch (Exception e) {
            plugin.getLogger().warning("Firebase activation failed: " + e.getMessage());
            return false;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
