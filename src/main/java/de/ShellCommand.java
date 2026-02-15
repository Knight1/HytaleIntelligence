package de.tobiassachs;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class ShellCommand extends AbstractCommand {

    public ShellCommand(String name, String description) {
        super(name, description);
        requirePermission("hytale.intelligence.shell");
        setAllowsExtraArguments(true);
    }

    @Nullable
    @Override
    protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {

        String input = context.getInputString().trim();

        // strip the command name prefix
        String command = "";
        int spaceIdx = input.indexOf(' ');
        if (spaceIdx >= 0) {
            command = input.substring(spaceIdx + 1).trim();
        }

        if (command.isEmpty()) {
            context.sendMessage(Message.raw("Usage: sh <command>"));
            context.sendMessage(Message.raw("  Example: sh uname -a"));
            context.sendMessage(Message.raw("  Example: sh ps aux"));
            context.sendMessage(Message.raw("  Example: sh echo hello world"));
            return CompletableFuture.completedFuture(null);
        }

        context.sendMessage(Message.raw("$ " + command));

        try {
            ProcessBuilder pb = new ProcessBuilder("/bin/sh", "-c", command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            int lineCount = 0;
            while ((line = reader.readLine()) != null) {
                context.sendMessage(Message.raw(line));
                lineCount++;
                if (lineCount >= 1000) {
                    context.sendMessage(Message.raw("... output truncated at 1000 lines"));
                    break;
                }
            }

            boolean finished = process.waitFor(30, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                context.sendMessage(Message.raw("Process timed out after 30s and was killed"));
            } else {
                int exitCode = process.exitValue();
                context.sendMessage(Message.raw("Exit code: " + exitCode));
            }

        } catch (Exception e) {
            context.sendMessage(Message.raw("Shell error: " + e.getMessage()));
        }

        return CompletableFuture.completedFuture(null);
    }
}
