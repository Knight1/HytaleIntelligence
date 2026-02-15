package de.tobiassachs;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.arguments.system.DefaultArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.FlagArg;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermissions;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LsCommand extends AbstractCommand {

    private final DefaultArg<String> pathArg;
    private final FlagArg longFlag;
    private final FlagArg allFlag;
    private final FlagArg humanFlag;
    private final FlagArg sizeFlag;

    public LsCommand(String name, String description) {
        super(name, description);
        this.pathArg = withDefaultArg("path", "Directory to list", ArgTypes.STRING, ".", "current directory");
        this.longFlag = withFlagArg("l", "Long listing format");
        this.allFlag = withFlagArg("a", "Show hidden files");
        this.humanFlag = withFlagArg("h", "Human readable sizes");
        this.sizeFlag = withFlagArg("s", "Show file sizes");
    }

    @Nullable
    @Override
    protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {

        String targetPath = this.pathArg.get(context);
        boolean longFormat = this.longFlag.get(context);
        boolean showAll = this.allFlag.get(context);
        boolean humanReadable = this.humanFlag.get(context);
        boolean showSize = this.sizeFlag.get(context);

        Path dir = Paths.get(targetPath);

        if (!Files.exists(dir)) {
            context.sendMessage(Message.raw("ls: cannot access '" + targetPath + "': No such file or directory"));
            return CompletableFuture.completedFuture(null);
        }

        if (!Files.isDirectory(dir)) {
            context.sendMessage(Message.raw(formatEntry(dir, longFormat, humanReadable, showSize)));
            return CompletableFuture.completedFuture(null);
        }

        context.sendMessage(Message.raw("---- ls " + dir.toAbsolutePath() + " ----"));

        try (Stream<Path> stream = Files.list(dir)) {
            List<Path> entries = stream
                    .filter(p -> showAll || !p.getFileName().toString().startsWith("."))
                    .sorted()
                    .collect(Collectors.toList());

            if (showAll) {
                context.sendMessage(Message.raw(formatEntry(dir, ".", longFormat, humanReadable, showSize)));
                if (dir.getParent() != null) {
                    context.sendMessage(Message.raw(formatEntry(dir.getParent(), "..", longFormat, humanReadable, showSize)));
                }
            }

            long totalBlocks = 0;
            if (showSize) {
                for (Path entry : entries) {
                    try {
                        totalBlocks += Files.size(entry) / 1024;
                    } catch (IOException ignored) {}
                }
                context.sendMessage(Message.raw("total " + totalBlocks));
            }

            for (Path entry : entries) {
                context.sendMessage(Message.raw(formatEntry(entry, longFormat, humanReadable, showSize)));
            }

            context.sendMessage(Message.raw(entries.size() + " entries"));

        } catch (IOException e) {
            context.sendMessage(Message.raw("ls: cannot open directory '" + targetPath + "': " + e.getMessage()));
        }

        return CompletableFuture.completedFuture(null);
    }

    private String formatEntry(Path path, boolean longFormat, boolean humanReadable, boolean showSize) {
        return formatEntry(path, path.getFileName().toString(), longFormat, humanReadable, showSize);
    }

    private String formatEntry(Path path, String displayName, boolean longFormat, boolean humanReadable, boolean showSize) {
        StringBuilder sb = new StringBuilder();

        if (longFormat || showSize) {
            long size = 0;
            try {
                size = Files.size(path);
            } catch (IOException ignored) {}

            if (showSize) {
                sb.append(String.format("%6d ", size / 1024));
            }

            if (longFormat) {
                // permissions
                String perms;
                try {
                    PosixFileAttributes attrs = Files.readAttributes(path, PosixFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
                    perms = PosixFilePermissions.toString(attrs.permissions());
                } catch (Exception e) {
                    perms = "---------";
                }

                String type = Files.isDirectory(path) ? "d" : (Files.isSymbolicLink(path) ? "l" : "-");
                sb.append(type).append(perms).append(" ");

                // owner and group
                try {
                    PosixFileAttributes attrs = Files.readAttributes(path, PosixFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
                    sb.append(String.format("%-8s %-8s ", attrs.owner().getName(), attrs.group().getName()));
                } catch (Exception e) {
                    try {
                        sb.append(String.format("%-8s %-8s ", Files.getOwner(path).getName(), "?"));
                    } catch (Exception e2) {
                        sb.append(String.format("%-8s %-8s ", "?", "?"));
                    }
                }

                // size
                if (humanReadable) {
                    sb.append(String.format("%8s ", humanReadableSize(size)));
                } else {
                    sb.append(String.format("%8d ", size));
                }

                // modification time
                try {
                    long lastMod = Files.getLastModifiedTime(path).toMillis();
                    sb.append(new SimpleDateFormat("MMM dd HH:mm").format(new Date(lastMod))).append(" ");
                } catch (IOException e) {
                    sb.append("?            ");
                }
            }
        }

        sb.append(displayName);

        if (Files.isDirectory(path)) {
            sb.append("/");
        } else if (Files.isSymbolicLink(path)) {
            try {
                sb.append(" -> ").append(Files.readSymbolicLink(path));
            } catch (IOException ignored) {}
        }

        return sb.toString();
    }

    private String humanReadableSize(long bytes) {
        if (bytes < 1024) return bytes + "B";
        String[] units = {"K", "M", "G", "T"};
        int idx = -1;
        double value = bytes;
        while (value >= 1024 && idx < units.length - 1) {
            value /= 1024;
            idx++;
        }
        return String.format("%.1f%s", value, units[idx]);
    }
}
