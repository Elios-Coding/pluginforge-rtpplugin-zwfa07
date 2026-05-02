package com.pluginforge.rtpplugin;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class RTPAdminGUI implements Listener {
    public static final String TITLE = "RTP Admin Panel";
    private final RTPPlugin plugin;
    private final RTPManager rtpManager;
    private final ActivationManager activationManager;

    public RTPAdminGUI(RTPPlugin plugin, RTPManager rtpManager, ActivationManager activationManager) {
        this.plugin = plugin;
        this.rtpManager = rtpManager;
        this.activationManager = activationManager;
    }

    public static void openMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 9, TITLE);
        inv.setItem(1, item(Material.GRASS_BLOCK, "500-1000"));
        inv.setItem(3, item(Material.DIRT, "1000-2000"));
        inv.setItem(5, item(Material.STONE, "2000-5000"));
        inv.setItem(7, item(Material.OBSIDIAN, "5000-10000"));
        player.openInventory(inv);
    }

    private static ItemStack item(Material material, String name) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!TITLE.equals(event.getView().getTitle())) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        if (!activationManager.isActivated()) {
            player.sendMessage("Plugin not activated. Use /rtp activate");
            player.closeInventory();
            return;
        }
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta() || clicked.getItemMeta() == null) return;
        String name = clicked.getItemMeta().getDisplayName();
        int min;
        int max;
        if ("500-1000".equals(name)) { min = 500; max = 1000; }
        else if ("1000-2000".equals(name)) { min = 1000; max = 2000; }
        else if ("2000-5000".equals(name)) { min = 2000; max = 5000; }
        else if ("5000-10000".equals(name)) { min = 5000; max = 10000; }
        else return;
        player.closeInventory();
        player.sendMessage("Teleporting within range " + min + "-" + max + "...");
        rtpManager.teleportPlayer(player, min, max);
    }
}
