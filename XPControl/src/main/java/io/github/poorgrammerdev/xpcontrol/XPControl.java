package io.github.poorgrammerdev.xpcontrol;

import org.bukkit.plugin.java.JavaPlugin;

public class XPControl extends JavaPlugin {

    @Override
    public void onEnable() {
        this.saveDefaultConfig();

        final XPVoucher xpVoucher = new XPVoucher(this);

        new DeathXPControl(this, xpVoucher).register();
        this.getServer().getPluginManager().registerEvents(xpVoucher, this);

        final XPStorage xpStorage = new XPStorage(this);
        xpStorage.register();

        final GiveCommand giveCommand = new GiveCommand(this, xpStorage);
        this.getCommand("givexpbottle").setExecutor(giveCommand);
        this.getCommand("givexpbottle").setTabCompleter(giveCommand);
    }

    @Override
    public void onDisable() {
    }
    
}