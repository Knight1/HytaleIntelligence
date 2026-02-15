package de.tobiassachs;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.nio.file.FileStore;
import java.nio.file.FileSystems;
import java.util.concurrent.CompletableFuture;

import static de.tobiassachs.CommandUtils.*;

public class DiskSpaceCommand extends AbstractCommand {

    public DiskSpaceCommand(String name, String description) {
        super(name, description);
        requirePermission("hytale.intelligence.info");
    }

    @Nullable
    @Override
    protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {

        // ---- df -h style output via Java API ----
        context.sendMessage(Message.raw("---- Disk Space (Java FileStore) ----"));
        try {
            for (FileStore store : FileSystems.getDefault().getFileStores()) {
                long total = store.getTotalSpace();
                long usable = store.getUsableSpace();
                long used = total - usable;
                String usePercent = total > 0 ? String.format("%.1f%%", (used * 100.0 / total)) : "N/A";

                context.sendMessage(Message.raw(
                        store.name() + " (" + store.type() + ") → "
                                + "Total: " + humanReadable(total)
                                + "  Used: " + humanReadable(used)
                                + "  Avail: " + humanReadable(usable)
                                + "  Use%: " + usePercent
                ));
            }
        } catch (Exception e) {
            context.sendMessage(Message.raw("Failed to read FileStores: " + e.getMessage()));
        }

        // ---- df -h from shell ----
        context.sendMessage(Message.raw("---- df -h ----"));
        sendMultiline(context, runCommandMultiline("df -h"));

        // ---- Inode usage ----
        context.sendMessage(Message.raw("---- df -i (inodes) ----"));
        sendMultiline(context, runCommandMultiline("df -i"));

        // ---- Mount info ----
        context.sendMessage(Message.raw("---- Mounts ----"));
        sendMultiline(context, readFileSafe("/proc/mounts"));

        return CompletableFuture.completedFuture(null);
    }

    private String humanReadable(long bytes) {
        if (bytes < 1024) return bytes + " B";
        String[] units = {"KB", "MB", "GB", "TB", "PB"};
        int idx = -1;
        double value = bytes;
        while (value >= 1024 && idx < units.length - 1) {
            value /= 1024;
            idx++;
        }
        return String.format("%.1f %s", value, units[idx]);
    }
}
