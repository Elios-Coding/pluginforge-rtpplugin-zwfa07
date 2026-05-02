package com.pluginforge.rtpplugin;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Random;

public class RTPManager {
    private final RTPPlugin plugin;
    private final ActivationManager activationManager;
    private final Random random = new Random();

    public RTPManager(RTPPlugin plugin, ActivationManager activationManager) {
        this.plugin = plugin;
        this.activationManager = activationManager;
    }

    public void teleportPlayer(Player player, int min, int max) {
        if (!activationManager.isActivated()) {
            player.sendMessage("Plugin not activated. Use /rtp activate");
            return;
        }
        World world = Bukkit.getWorlds().get(0);
        new BukkitRunnable() {
            @Override
            public void run() {
                Location safe = findSafeLocation(world, min, max, 25);
                if (safe == null) {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            player.sendMessage("Could not find a safe RTP location. Try again.");
                        }
                    }.runTask(plugin);
                    return;
                }
                world.getChunkAtAsync(safe).thenAccept(chunk -> new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (player.isOnline()) {
                            player.teleport(safe);
                            player.sendMessage("Teleported to a random safe location.");
                        }
                    }
                }.runTask(plugin));
            }
        }.runTaskAsynchronously(plugin);
    }

    private Location findSafeLocation(World world, int min, int max, int attempts) {
        int range = Math.max(1, max - min + 1);
        for (int i = 0; i < attempts; i++) {
            int distance = min + random.nextInt(range);
            double angle = random.nextDouble() * Math.PI * 2.0D;
            int x = (int) Math.round(Math.cos(angle) * distance);
            int z = (int) Math.round(Math.sin(angle) * distance);
            int y = world.getHighestBlockYAt(x, z);
            Location candidate = new Location(world, x + 0.5D, y + 1.0D, z + 0.5D);
            if (isSafe(candidate)) return candidate;
        }
        return null;
    }

    private boolean isSafe(Location location) {
        Block feet = location.getBlock();
        Block head = feet.getRelative(0, 1, 0);
        Block ground = feet.getRelative(0, -1, 0);
        if (!feet.getType().isAir() || !head.getType().isAir()) return false;
        Material groundType = ground.getType();
        if (!groundType.isSolid()) return false;
        if (groundType == Material.LAVA || groundType == Material.MAGMA_BLOCK
                || groundType == Material.WATER || groundType == Material.CAMPFIRE
                || groundType == Material.CACTUS) return false;
        if (ground.isLiquid()) return false;
        return true;
    }
}
