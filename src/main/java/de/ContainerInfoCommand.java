package de.tobiassachs;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;

import static de.tobiassachs.CommandUtils.*;

public class ContainerInfoCommand extends AbstractCommand {

    public ContainerInfoCommand(String name, String description) {
        super(name, description);
    }

    @Nullable
    @Override
    protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {

        context.sendMessage(Message.raw("---- Docker / Container ----"));
        context.sendMessage(Message.raw("Docker: " + isDocker()));
        context.sendMessage(Message.raw("Container runtime: " + detectContainerRuntime()));
        context.sendMessage(Message.raw("Container ID: " + readFileSafe("/etc/hostname")));
        context.sendMessage(Message.raw("Container cgroup:"));
        sendMultiline(context, readFileSafe("/proc/self/cgroup"));

        context.sendMessage(Message.raw("---- Container Limits ----"));
        context.sendMessage(Message.raw("Memory Limit: " + readFileSafe("/sys/fs/cgroup/memory.max")));
        context.sendMessage(Message.raw("CPU Quota: " + readFileSafe("/sys/fs/cgroup/cpu.max")));

        context.sendMessage(Message.raw("---- DNS / resolv.conf ----"));
        sendMultiline(context, readFileSafe("/etc/resolv.conf"));

        return CompletableFuture.completedFuture(null);
    }

    private boolean isDocker() {
        try {
            if (new File("/.dockerenv").exists()) return true;

            String cgroup = Files.readString(Paths.get("/proc/1/cgroup"));
            return cgroup.contains("docker") ||
                    cgroup.contains("containerd") ||
                    cgroup.contains("kubepods");
        } catch (Exception e) {
            return false;
        }
    }

    private String detectContainerRuntime() {
        try {
            String cgroup = Files.readString(Paths.get("/proc/1/cgroup"));
            if (cgroup.contains("docker")) return "Docker";
            if (cgroup.contains("containerd")) return "containerd";
            if (cgroup.contains("kubepods")) return "Kubernetes";
            if (cgroup.contains("lxc")) return "LXC";
        } catch (Exception ignored) {}

        if (new File("/.dockerenv").exists()) return "Docker";
        if (new File("/run/.containerenv").exists()) return "Podman";

        return "Unknown / None";
    }
}
