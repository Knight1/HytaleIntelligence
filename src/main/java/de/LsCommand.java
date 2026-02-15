package de.tobiassachs;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;

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

    public LsCommand(String name, String description) {
        super(name, description);
        setAllowsExtraArguments(true);
    }

    @Nullable
    @Override
    protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {

        String input = context.getInputString().trim();

        // parse flags and path from raw input, e.g. "ls -lsah /dev"
        boolean longFormat = false;
        boolean showAll = false;
        boolean humanReadable = false;
        boolean showSize = false;
        String targetPath = ".";

        String[] parts = input.split("\\s+");
        for (int i = 1; i < parts.length; i++) {
            String part = parts[i];
            if (part.startsWith("-")) {
                String flags = part.substring(1);
                for (char c : flags.toCharArray()) {
                    switch (c) {
                        case 'l': longFormat = true; break;
                        case 'a': showAll = true; break;
                        case 'h': humanReadable = true; break;
                        case 's': showSize = true; break;
                        default:
                            context.sendMessage(Message.raw("ls: unknown flag '-" + c + "'"));
                            return CompletableFuture.completedFuture(null);
                    }
                }
            } else {
                targetPath = part;
            }
        }

        final boolean fLong = longFormat;
        final boolean fAll = showAll;
        final boolean fHuman = humanReadable;
        final boolean fSize = showSize;

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
                    .filter(p -> fAll || !p.getFileName().toString().startsWith("."))
                    .sorted()
                    .collect(Collectors.toList());

            if (fAll) {
                context.sendMessage(Message.raw(formatEntry(dir, ".", fLong, fHuman, fSize)));
                if (dir.getParent() != null) {
                    context.sendMessage(Message.raw(formatEntry(dir.getParent(), "..", fLong, fHuman, fSize)));
                }
            }

            long totalBlocks = 0;
            if (fSize) {
                for (Path entry : entries) {
                    try {
                        totalBlocks += Files.size(entry) / 1024;
                    } catch (IOException ignored) {}
                }
                context.sendMessage(Message.raw("total " + totalBlocks));
            }

            for (Path entry : entries) {
                context.sendMessage(Message.raw(formatEntry(entry, fLong, fHuman, fSize)));
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
                String perms;
                try {
                    PosixFileAttributes attrs = Files.readAttributes(path, PosixFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
                    perms = PosixFilePermissions.toString(attrs.permissions());
                } catch (Exception e) {
                    perms = "---------";
                }

                String type = Files.isDirectory(path) ? "d" : (Files.isSymbolicLink(path) ? "l" : "-");
                sb.append(type).append(perms).append(" ");

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

                if (humanReadable) {
                    sb.append(String.format("%8s ", humanReadableSize(size)));
                } else {
                    sb.append(String.format("%8d ", size));
                }

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
