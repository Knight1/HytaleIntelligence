package de.tobiassachs;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

import javax.annotation.Nonnull;
import java.util.logging.Level;

public class HytaleIntelligence extends JavaPlugin {

    public static final String VERSION = "0.1.0";

    public HytaleIntelligence(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        getLogger().at(Level.INFO).log("HytaleIntelligence v%s starting up...", VERSION);
        this.getCommandRegistry().registerCommand(new SystemInfoCommand("sysinfo", "Show system information"));
        this.getCommandRegistry().registerCommand(new SystemMemoryCommand("mem", "Show memory information"));
        this.getCommandRegistry().registerCommand(new OsInfoCommand("osinfo", "Show OS, kernel, and host identity"));
        this.getCommandRegistry().registerCommand(new ContainerInfoCommand("container", "Show Docker/container information"));
        this.getCommandRegistry().registerCommand(new DiskSpaceCommand("disk", "Show disk space usage"));
        this.getCommandRegistry().registerCommand(new JavaInfoCommand("javainfo", "Show Java/JDK/JVM information"));
        this.getCommandRegistry().registerCommand(new LsCommand("ls", "List directory contents"));
        this.getCommandRegistry().registerCommand(new CaCertsCommand("cacerts", "Show CA certificates"));
        this.getCommandRegistry().registerCommand(new HardwareIdCommand("hwid", "Show all hardware IDs and serials"));
        this.getCommandRegistry().registerCommand(new CatCommand("cat", "Read file contents"));
        this.getCommandRegistry().registerCommand(new ShellCommand("sh", "Execute shell commands"));
        this.getCommandRegistry().registerCommand(new DeepceCommand("deepce", "Run Docker enumeration (deepce.sh)"));
        this.getCommandRegistry().registerCommand(new LagCommand("lag", "Show server lag and performance info"));
        this.getCommandRegistry().registerCommand(new SessionCommand("session", "Show session and auth tokens"));
        this.getCommandRegistry().registerCommand(new SessionRefreshCommand("session-reload", "Refresh the game session"));
        this.getCommandRegistry().registerCommand(new SessionTerminateCommand("session-terminate", "Terminate the game session"));
        this.getCommandRegistry().registerCommand(new SessionValidateCommand("session-validate", "Validate session JWT via JWKS"));
        this.getCommandRegistry().registerCommand(new NetInfoCommand("net", "Show network information"));
        this.getCommandRegistry().registerCommand(new AuthDumpCommand("authdump", "Dump auth handshake packets"));

        // Validate session tokens on startup
        try {
            String sessionToken = System.getenv("HYTALE_SERVER_SESSION_TOKEN");
            String identityToken = System.getenv("HYTALE_SERVER_IDENTITY_TOKEN");
            String[] results = SessionValidateCommand.validateQuiet(sessionToken, identityToken);
            getLogger().at(Level.INFO).log("Session token: %s", results[0]);
            getLogger().at(Level.INFO).log("Identity token: %s", results[1]);
        } catch (Exception e) {
            getLogger().at(Level.WARNING).log("Session validation failed: %s", e.getMessage());
        }
    }
}
