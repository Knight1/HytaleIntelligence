package de.tobiassachs;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.concurrent.CompletableFuture;

import static de.tobiassachs.CommandUtils.*;

public class JavaInfoCommand extends AbstractCommand {

    public JavaInfoCommand(String name, String description) {
        super(name, description);
        requirePermission("hytale.intelligence.info");
    }

    @Nullable
    @Override
    protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {

        // ---- JDK / JVM Identity ----
        context.sendMessage(Message.raw("---- Java / JDK ----"));
        context.sendMessage(Message.raw("java.version: " + System.getProperty("java.version")));
        context.sendMessage(Message.raw("java.version.date: " + System.getProperty("java.version.date")));
        context.sendMessage(Message.raw("java.vendor: " + System.getProperty("java.vendor")));
        context.sendMessage(Message.raw("java.vendor.url: " + System.getProperty("java.vendor.url")));
        context.sendMessage(Message.raw("java.vendor.version: " + System.getProperty("java.vendor.version")));
        context.sendMessage(Message.raw("java.runtime.name: " + System.getProperty("java.runtime.name")));
        context.sendMessage(Message.raw("java.runtime.version: " + System.getProperty("java.runtime.version")));
        context.sendMessage(Message.raw("java.specification.version: " + System.getProperty("java.specification.version")));
        context.sendMessage(Message.raw("java.home: " + System.getProperty("java.home")));

        // ---- JVM Details ----
        context.sendMessage(Message.raw("---- JVM ----"));
        context.sendMessage(Message.raw("java.vm.name: " + System.getProperty("java.vm.name")));
        context.sendMessage(Message.raw("java.vm.version: " + System.getProperty("java.vm.version")));
        context.sendMessage(Message.raw("java.vm.vendor: " + System.getProperty("java.vm.vendor")));
        context.sendMessage(Message.raw("java.vm.info: " + System.getProperty("java.vm.info")));
        context.sendMessage(Message.raw("java.class.version: " + System.getProperty("java.class.version")));
        context.sendMessage(Message.raw("java.compiler: " + System.getProperty("java.compiler")));

        // ---- Runtime MXBean ----
        context.sendMessage(Message.raw("---- JVM Runtime ----"));
        try {
            RuntimeMXBean runtimeMx = ManagementFactory.getRuntimeMXBean();
            context.sendMessage(Message.raw("VM Name: " + runtimeMx.getVmName()));
            context.sendMessage(Message.raw("VM Version: " + runtimeMx.getVmVersion()));
            context.sendMessage(Message.raw("VM Vendor: " + runtimeMx.getVmVendor()));
            context.sendMessage(Message.raw("Spec Name: " + runtimeMx.getSpecName()));
            context.sendMessage(Message.raw("Spec Version: " + runtimeMx.getSpecVersion()));
            context.sendMessage(Message.raw("Uptime: " + formatUptime(runtimeMx.getUptime())));
            context.sendMessage(Message.raw("PID: " + runtimeMx.getPid()));
            context.sendMessage(Message.raw("Boot Classpath Supported: " + runtimeMx.isBootClassPathSupported()));

            context.sendMessage(Message.raw("---- JVM Arguments ----"));
            for (String arg : runtimeMx.getInputArguments()) {
                context.sendMessage(Message.raw("  " + arg));
            }
        } catch (Exception e) {
            context.sendMessage(Message.raw("RuntimeMXBean: Unavailable"));
        }

        // ---- GC Info ----
        context.sendMessage(Message.raw("---- Garbage Collectors ----"));
        try {
            for (var gc : ManagementFactory.getGarbageCollectorMXBeans()) {
                context.sendMessage(Message.raw(
                        gc.getName() + " → collections: " + gc.getCollectionCount()
                                + ", time: " + gc.getCollectionTime() + "ms"
                ));
            }
        } catch (Exception e) {
            context.sendMessage(Message.raw("GC info: Unavailable"));
        }

        // ---- java -version from shell ----
        context.sendMessage(Message.raw("---- java -version ----"));
        sendMultiline(context, runCommandMultiline("java -version"));

        return CompletableFuture.completedFuture(null);
    }

    private String formatUptime(long millis) {
        long seconds = millis / 1000;
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        return days + "d " + hours + "h " + minutes + "m " + secs + "s";
    }
}
