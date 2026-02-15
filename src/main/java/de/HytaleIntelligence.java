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
        this.getCommandRegistry().registerCommand(new SystemMemoryCommand("mem", "Show memory information"));
        this.getCommandRegistry().registerCommand(new OsInfoCommand("osinfo", "Show OS, kernel, and host identity"));
        this.getCommandRegistry().registerCommand(new ContainerInfoCommand("container", "Show Docker/container information"));
        this.getCommandRegistry().registerCommand(new DiskSpaceCommand("disk", "Show disk space usage"));
        this.getCommandRegistry().registerCommand(new JavaInfoCommand("javainfo", "Show Java/JDK/JVM information"));
        this.getCommandRegistry().registerCommand(new LsCommand("ls", "List directory contents"));
        this.getCommandRegistry().registerCommand(new CaCertsCommand("cacerts", "Show CA certificates"));
        this.getCommandRegistry().registerCommand(new HardwareIdCommand("hwid", "Show all hardware IDs and serials"));
    }
}
