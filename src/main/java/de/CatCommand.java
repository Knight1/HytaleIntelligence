package de.tobiassachs;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class CatCommand extends AbstractCommand {

    public CatCommand(String name, String description) {
        super(name, description);
        requirePermission("hytale.intelligence.filesystem");
        setAllowsExtraArguments(true);
    }

    @Nullable
    @Override
    protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {

        String input = context.getInputString().trim();
        String[] parts = input.split("\\s+");

        boolean showLineNumbers = false;
        String filePath = null;

        for (int i = 1; i < parts.length; i++) {
            String part = parts[i];
            if (part.equals("-n")) {
                showLineNumbers = true;
            } else {
                filePath = part;
            }
        }

        if (filePath == null) {
            context.sendMessage(Message.raw("Usage: cat [-n] <file>"));
            context.sendMessage(Message.raw("  -n  Show line numbers"));
            return CompletableFuture.completedFuture(null);
        }

        Path path = Paths.get(filePath);

        if (!Files.exists(path)) {
            context.sendMessage(Message.raw("cat: " + filePath + ": No such file or directory"));
            return CompletableFuture.completedFuture(null);
        }

        if (Files.isDirectory(path)) {
            context.sendMessage(Message.raw("cat: " + filePath + ": Is a directory"));
            return CompletableFuture.completedFuture(null);
        }

        try {
            List<String> lines = Files.readAllLines(path);
            context.sendMessage(Message.raw("---- " + path.toAbsolutePath() + " (" + lines.size() + " lines) ----"));

            for (int i = 0; i < lines.size(); i++) {
                if (showLineNumbers) {
                    context.sendMessage(Message.raw(String.format("%4d  %s", i + 1, lines.get(i))));
                } else {
                    context.sendMessage(Message.raw(lines.get(i)));
                }
            }
        } catch (Exception e) {
            context.sendMessage(Message.raw("cat: " + filePath + ": " + e.getMessage()));
        }

        return CompletableFuture.completedFuture(null);
    }
}
