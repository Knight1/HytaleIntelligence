package de.tobiassachs;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

import javax.annotation.Nonnull;

public class HytaleIntelligence extends JavaPlugin {

    public HytaleIntelligence(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        this.getCommandRegistry().registerCommand(new SystemInfoCommand("sysinfo", "Show system information"));
    }
}
