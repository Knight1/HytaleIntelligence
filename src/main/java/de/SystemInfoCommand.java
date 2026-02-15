package de.tobiassachs;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class SystemInfoCommand extends AbstractCommand {

    public SystemInfoCommand(String name, String description) {
        super(name, description);
    }

    @Nullable
    @Override
    protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {


        boolean docker = new File("/.dockerenv").exists();
        int cores = Runtime.getRuntime().availableProcessors();
        String os = System.getProperty("os.name");
        String arch = System.getProperty("os.arch");

        context.sendMessage(Message.raw("Docker: " + docker));
        context.sendMessage(Message.raw("CPU cores: " + cores));
        context.sendMessage(Message.raw("OS: " + os + " (" + arch + ")"));
        Map<String, String> env = System.getenv();
        context.sendMessage(Message.raw("ENV count: " + env.size()));

        context.sendMessage(Message.raw("---- ENV VARIABLES (" + env.size() + ") ----"));

        for (Map.Entry<String, String> entry : env.entrySet()) {
            context.sendMessage(
                    Message.raw(entry.getKey() + "=" + entry.getValue())
            );
        }
        return CompletableFuture.completedFuture(null);
    }

}