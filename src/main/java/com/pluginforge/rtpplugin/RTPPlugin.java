package com.pluginforge.rtpplugin;

import org.bukkit.plugin.java.JavaPlugin;

public class RTPPlugin extends JavaPlugin {
    private RTPManager rtpManager;
    private ActivationManager activationManager;
    private ChatListener chatListener;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.activationManager = new ActivationManager(this);
        this.rtpManager = new RTPManager(this, activationManager);
        this.chatListener = new ChatListener(this, activationManager);

        RTPCommand command = new RTPCommand(this, rtpManager, activationManager);
        if (getCommand("rtp") != null) {
            getCommand("rtp").setExecutor(command);
            getCommand("rtp").setTabCompleter(command);
        }

        getServer().getPluginManager().registerEvents(new RTPAdminGUI(this, rtpManager, activationManager), this);
        getServer().getPluginManager().registerEvents(chatListener, this);

        getLogger().info("RTPPlugin enabled. Activated=" + activationManager.isActivated());
    }

    @Override
    public void onDisable() {
        getLogger().info("RTPPlugin disabled.");
    }

    public RTPManager getRTPManager() { return rtpManager; }
    public ActivationManager getActivationManager() { return activationManager; }
}
