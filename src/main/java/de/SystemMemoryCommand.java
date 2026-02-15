package de.tobiassachs;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;

import static de.tobiassachs.CommandUtils.*;

public class SystemMemoryCommand extends AbstractCommand {

    public SystemMemoryCommand(String name, String description) {
        super(name, description);
        requirePermission("hytale.intelligence.info");
    }

    @Nullable
    @Override
    protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {

        Runtime rt = Runtime.getRuntime();
        long maxMem = rt.maxMemory();
        long totalMem = rt.totalMemory();
        long freeMem = rt.freeMemory();
        long usedMem = totalMem - freeMem;

        // ---- JVM Heap (Runtime) ----
        context.sendMessage(Message.raw("---- JVM Heap (Runtime) ----"));
        context.sendMessage(Message.raw("Max Memory:   " + humanReadable(maxMem)));
        context.sendMessage(Message.raw("Total Memory: " + humanReadable(totalMem)));
        context.sendMessage(Message.raw("Used Memory:  " + humanReadable(usedMem)));
        context.sendMessage(Message.raw("Free Memory:  " + humanReadable(freeMem)));
        context.sendMessage(Message.raw("Usage:        " + String.format("%.1f%%", (usedMem * 100.0 / maxMem))));

        // ---- JVM Heap/Non-Heap (MXBean) ----
        context.sendMessage(Message.raw("---- JVM Memory (MXBean) ----"));
        try {
            MemoryMXBean memMx = ManagementFactory.getMemoryMXBean();
            MemoryUsage heap = memMx.getHeapMemoryUsage();
            MemoryUsage nonHeap = memMx.getNonHeapMemoryUsage();

            context.sendMessage(Message.raw("Heap Init:      " + humanReadable(heap.getInit())));
            context.sendMessage(Message.raw("Heap Used:      " + humanReadable(heap.getUsed())));
            context.sendMessage(Message.raw("Heap Committed: " + humanReadable(heap.getCommitted())));
            context.sendMessage(Message.raw("Heap Max:       " + humanReadable(heap.getMax())));

            context.sendMessage(Message.raw("Non-Heap Init:      " + humanReadable(nonHeap.getInit())));
            context.sendMessage(Message.raw("Non-Heap Used:      " + humanReadable(nonHeap.getUsed())));
            context.sendMessage(Message.raw("Non-Heap Committed: " + humanReadable(nonHeap.getCommitted())));
            context.sendMessage(Message.raw("Non-Heap Max:       " + humanReadable(nonHeap.getMax())));
        } catch (Exception e) {
            context.sendMessage(Message.raw("MemoryMXBean: Unavailable"));
        }

        // ---- Memory Pools ----
        context.sendMessage(Message.raw("---- Memory Pools ----"));
        try {
            for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
                MemoryUsage usage = pool.getUsage();
                context.sendMessage(Message.raw(
                        pool.getName() + " (" + pool.getType() + ") → "
                                + "Used: " + humanReadable(usage.getUsed())
                                + "  Committed: " + humanReadable(usage.getCommitted())
                                + "  Max: " + humanReadable(usage.getMax())
                ));
            }
        } catch (Exception e) {
            context.sendMessage(Message.raw("Memory Pools: Unavailable"));
        }

        // ---- System Memory (/proc/meminfo) ----
        context.sendMessage(Message.raw("---- System Memory (/proc/meminfo) ----"));
        String meminfo = readFileSafe("/proc/meminfo");
        if (!meminfo.equals("Unavailable")) {
            for (String line : meminfo.split("\n")) {
                String trimmed = line.trim();
                if (trimmed.startsWith("MemTotal") ||
                        trimmed.startsWith("MemFree") ||
                        trimmed.startsWith("MemAvailable") ||
                        trimmed.startsWith("Buffers") ||
                        trimmed.startsWith("Cached") ||
                        trimmed.startsWith("SwapTotal") ||
                        trimmed.startsWith("SwapFree") ||
                        trimmed.startsWith("Shmem") ||
                        trimmed.startsWith("SReclaimable")) {
                    context.sendMessage(Message.raw("  " + trimmed));
                }
            }
        } else {
            context.sendMessage(Message.raw("  Unavailable"));
        }

        // ---- Container Memory Limits ----
        context.sendMessage(Message.raw("---- Container Memory Limits ----"));
        context.sendMessage(Message.raw("Docker Memory Limit: " + getDockerMemoryLimit()));
        context.sendMessage(Message.raw("memory.current: " + readFileSafe("/sys/fs/cgroup/memory.current")));
        context.sendMessage(Message.raw("memory.swap.max:     " + readFileSafe("/sys/fs/cgroup/memory.swap.max")));
        context.sendMessage(Message.raw("memory.swap.current: " + readFileSafe("/sys/fs/cgroup/memory.swap.current")));

        // ---- free -h ----
        context.sendMessage(Message.raw("---- free -h ----"));
        sendMultiline(context, runCommandMultiline("free -h"));

        return CompletableFuture.completedFuture(null);
    }

    private String getDockerMemoryLimit() {
        try {
            return Files.readString(Paths.get("/sys/fs/cgroup/memory.max")).trim();
        } catch (Exception e) {
            return "Unlimited or not containerized";
        }
    }

    private String humanReadable(long bytes) {
        if (bytes < 0) return "N/A";
        if (bytes < 1024) return bytes + " B";
        String[] units = {"KB", "MB", "GB", "TB"};
        int idx = -1;
        double value = bytes;
        while (value >= 1024 && idx < units.length - 1) {
            value /= 1024;
            idx++;
        }
        return String.format("%.1f %s", value, units[idx]);
    }
}
