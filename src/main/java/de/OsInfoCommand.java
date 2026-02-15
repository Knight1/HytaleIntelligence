package de.tobiassachs;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;

import static de.tobiassachs.CommandUtils.*;

public class OsInfoCommand extends AbstractCommand {

    public OsInfoCommand(String name, String description) {
        super(name, description);
    }

    @Nullable
    @Override
    protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {

        // ---- OS Release Info ----
        context.sendMessage(Message.raw("---- OS Release ----"));
        context.sendMessage(Message.raw("OS: " + System.getProperty("os.name") + " (" + System.getProperty("os.arch") + ")"));
        context.sendMessage(Message.raw("/etc/os-release:"));
        sendMultiline(context, readFileSafe("/etc/os-release"));
        context.sendMessage(Message.raw("/etc/lsb-release:"));
        sendMultiline(context, readFileSafe("/etc/lsb-release"));
        context.sendMessage(Message.raw("/etc/redhat-release: " + readFileSafe("/etc/redhat-release")));
        context.sendMessage(Message.raw("/etc/debian_version: " + readFileSafe("/etc/debian_version")));
        context.sendMessage(Message.raw("/etc/alpine-release: " + readFileSafe("/etc/alpine-release")));

        // ---- Kernel Info ----
        context.sendMessage(Message.raw("---- Kernel ----"));
        context.sendMessage(Message.raw("Kernel version: " + readFileSafe("/proc/version")));
        context.sendMessage(Message.raw("uname -a: " + runCommand("uname -a")));
        context.sendMessage(Message.raw("Kernel release: " + readFileSafe("/proc/sys/kernel/osrelease")));
        context.sendMessage(Message.raw("Kernel hostname: " + readFileSafe("/proc/sys/kernel/hostname")));
        context.sendMessage(Message.raw("Kernel domainname: " + readFileSafe("/proc/sys/kernel/domainname")));

        // ---- Machine / Host Identity ----
        context.sendMessage(Message.raw("---- Machine / Host Identity ----"));
        context.sendMessage(Message.raw("Machine ID: " + readFileSafe("/etc/machine-id")));
        context.sendMessage(Message.raw("Boot ID: " + readFileSafe("/proc/sys/kernel/random/boot_id")));

        // ---- User Info ----
        context.sendMessage(Message.raw("---- User Info ----"));
        context.sendMessage(Message.raw("user.name: " + System.getProperty("user.name")));
        context.sendMessage(Message.raw("user.home: " + System.getProperty("user.home")));
        context.sendMessage(Message.raw("user.dir: " + System.getProperty("user.dir")));
        context.sendMessage(Message.raw("whoami: " + runCommand("whoami")));
        context.sendMessage(Message.raw("id: " + runCommand("id")));
        context.sendMessage(Message.raw("groups: " + runCommand("groups")));

        // ---- Hardware Identity ----
        context.sendMessage(Message.raw("---- Hardware Identity ----"));
        context.sendMessage(Message.raw("Product Name: " + readFileSafe("/sys/class/dmi/id/product_name")));
        context.sendMessage(Message.raw("Product UUID: " + readFileSafe("/sys/class/dmi/id/product_uuid")));
        context.sendMessage(Message.raw("Board Name: " + readFileSafe("/sys/class/dmi/id/board_name")));
        context.sendMessage(Message.raw("Board Vendor: " + readFileSafe("/sys/class/dmi/id/board_vendor")));
        context.sendMessage(Message.raw("BIOS Vendor: " + readFileSafe("/sys/class/dmi/id/bios_vendor")));
        context.sendMessage(Message.raw("BIOS Version: " + readFileSafe("/sys/class/dmi/id/bios_version")));
        context.sendMessage(Message.raw("Sys Vendor: " + readFileSafe("/sys/class/dmi/id/sys_vendor")));
        context.sendMessage(Message.raw("Chassis Type: " + readFileSafe("/sys/class/dmi/id/chassis_type")));

        return CompletableFuture.completedFuture(null);
    }
}
