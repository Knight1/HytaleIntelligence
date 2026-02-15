package de.tobiassachs;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;

import static de.tobiassachs.CommandUtils.*;

public class HardwareIdCommand extends AbstractCommand {

    public HardwareIdCommand(String name, String description) {
        super(name, description);
    }

    @Nullable
    @Override
    protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {

        // ---- BIOS ----
        context.sendMessage(Message.raw("---- BIOS ----"));
        context.sendMessage(Message.raw("BIOS Vendor:  " + readFileSafe("/sys/class/dmi/id/bios_vendor")));
        context.sendMessage(Message.raw("BIOS Version: " + readFileSafe("/sys/class/dmi/id/bios_version")));
        context.sendMessage(Message.raw("BIOS Date:    " + readFileSafe("/sys/class/dmi/id/bios_date")));
        context.sendMessage(Message.raw("BIOS Release: " + readFileSafe("/sys/class/dmi/id/bios_release")));

        // ---- Mainboard ----
        context.sendMessage(Message.raw("---- Mainboard ----"));
        context.sendMessage(Message.raw("Board Name:    " + readFileSafe("/sys/class/dmi/id/board_name")));
        context.sendMessage(Message.raw("Board Vendor:  " + readFileSafe("/sys/class/dmi/id/board_vendor")));
        context.sendMessage(Message.raw("Board Version: " + readFileSafe("/sys/class/dmi/id/board_version")));
        context.sendMessage(Message.raw("Board Serial:  " + readFileSafe("/sys/class/dmi/id/board_serial")));
        context.sendMessage(Message.raw("Board Asset:   " + readFileSafe("/sys/class/dmi/id/board_asset_tag")));

        // ---- System / Product ----
        context.sendMessage(Message.raw("---- System / Product ----"));
        context.sendMessage(Message.raw("Product Name:    " + readFileSafe("/sys/class/dmi/id/product_name")));
        context.sendMessage(Message.raw("Product UUID:    " + readFileSafe("/sys/class/dmi/id/product_uuid")));
        context.sendMessage(Message.raw("Product Serial:  " + readFileSafe("/sys/class/dmi/id/product_serial")));
        context.sendMessage(Message.raw("Product Version: " + readFileSafe("/sys/class/dmi/id/product_version")));
        context.sendMessage(Message.raw("Product Family:  " + readFileSafe("/sys/class/dmi/id/product_family")));
        context.sendMessage(Message.raw("Product SKU:     " + readFileSafe("/sys/class/dmi/id/product_sku")));
        context.sendMessage(Message.raw("Sys Vendor:      " + readFileSafe("/sys/class/dmi/id/sys_vendor")));

        // ---- Chassis ----
        context.sendMessage(Message.raw("---- Chassis ----"));
        context.sendMessage(Message.raw("Chassis Type:    " + readFileSafe("/sys/class/dmi/id/chassis_type")));
        context.sendMessage(Message.raw("Chassis Vendor:  " + readFileSafe("/sys/class/dmi/id/chassis_vendor")));
        context.sendMessage(Message.raw("Chassis Version: " + readFileSafe("/sys/class/dmi/id/chassis_version")));
        context.sendMessage(Message.raw("Chassis Serial:  " + readFileSafe("/sys/class/dmi/id/chassis_serial")));
        context.sendMessage(Message.raw("Chassis Asset:   " + readFileSafe("/sys/class/dmi/id/chassis_asset_tag")));

        // ---- Memory DIMMs (dmidecode) ----
        context.sendMessage(Message.raw("---- Memory DIMMs ----"));
        String memInfo = runCommandMultiline("dmidecode -t memory");
        if (!memInfo.equals("Unavailable")) {
            boolean inDevice = false;
            for (String line : memInfo.split("\n")) {
                String trimmed = line.trim();
                if (trimmed.equals("Memory Device")) {
                    inDevice = true;
                    context.sendMessage(Message.raw(""));
                    context.sendMessage(Message.raw("  [Memory Device]"));
                    continue;
                }
                if (inDevice && trimmed.isEmpty()) {
                    inDevice = false;
                    continue;
                }
                if (inDevice) {
                    if (trimmed.startsWith("Size:") ||
                            trimmed.startsWith("Type:") ||
                            trimmed.startsWith("Speed:") ||
                            trimmed.startsWith("Configured Memory Speed:") ||
                            trimmed.startsWith("Manufacturer:") ||
                            trimmed.startsWith("Serial Number:") ||
                            trimmed.startsWith("Part Number:") ||
                            trimmed.startsWith("Locator:") ||
                            trimmed.startsWith("Bank Locator:") ||
                            trimmed.startsWith("Form Factor:") ||
                            trimmed.startsWith("Data Width:") ||
                            trimmed.startsWith("Total Width:") ||
                            trimmed.startsWith("Asset Tag:") ||
                            trimmed.startsWith("Rank:") ||
                            trimmed.startsWith("Configured Voltage:")) {
                        context.sendMessage(Message.raw("    " + trimmed));
                    }
                }
            }
        } else {
            context.sendMessage(Message.raw("  dmidecode not available (requires root)"));
            context.sendMessage(Message.raw("  Trying /sys/firmware/dmi/..."));
            sendMultiline(context, runCommandMultiline("ls -la /sys/firmware/dmi/entries/"));
        }

        // ---- CPU IDs ----
        context.sendMessage(Message.raw("---- CPU ----"));
        String cpuinfo = readFileSafe("/proc/cpuinfo");
        if (!cpuinfo.equals("Unavailable")) {
            for (String line : cpuinfo.split("\n")) {
                String trimmed = line.trim();
                if (trimmed.startsWith("model name") ||
                        trimmed.startsWith("cpu family") ||
                        trimmed.startsWith("model\t") ||
                        trimmed.startsWith("stepping") ||
                        trimmed.startsWith("microcode") ||
                        trimmed.startsWith("physical id") ||
                        trimmed.startsWith("core id") ||
                        trimmed.startsWith("apicid") ||
                        trimmed.startsWith("cpuid level")) {
                    context.sendMessage(Message.raw("  " + trimmed));
                }
            }
        } else {
            context.sendMessage(Message.raw("  /proc/cpuinfo: Unavailable"));
        }

        // ---- Block Devices / Disk Serials ----
        context.sendMessage(Message.raw("---- Block Devices ----"));
        String lsblk = runCommandMultiline("lsblk -o NAME,SIZE,TYPE,MODEL,SERIAL,VENDOR,REV,HCTL");
        if (!lsblk.equals("Unavailable")) {
            sendMultiline(context, lsblk);
        } else {
            context.sendMessage(Message.raw("  lsblk: Unavailable"));
        }

        // ---- Disk IDs (/dev/disk/by-id) ----
        context.sendMessage(Message.raw("---- /dev/disk/by-id ----"));
        sendMultiline(context, runCommandMultiline("ls -la /dev/disk/by-id/"));

        // ---- Network MACs ----
        context.sendMessage(Message.raw("---- Network Interface MACs ----"));
        String ipLink = runCommandMultiline("ip link show");
        if (!ipLink.equals("Unavailable")) {
            String currentIface = "";
            for (String line : ipLink.split("\n")) {
                String trimmed = line.trim();
                if (trimmed.matches("^\\d+:.*")) {
                    String[] p = trimmed.split(":");
                    if (p.length >= 2) {
                        currentIface = p[1].trim();
                    }
                } else if (trimmed.startsWith("link/ether")) {
                    String mac = trimmed.split("\\s+")[1];
                    context.sendMessage(Message.raw("  " + currentIface + " → " + mac));
                }
            }
        } else {
            context.sendMessage(Message.raw("  ip link: Unavailable, trying /sys/class/net/"));
            sendMultiline(context, runCommandMultiline("cat /sys/class/net/*/address"));
        }

        // ---- PCI Devices ----
        context.sendMessage(Message.raw("---- PCI Devices ----"));
        String lspci = runCommandMultiline("lspci");
        if (!lspci.equals("Unavailable")) {
            sendMultiline(context, lspci);
        } else {
            context.sendMessage(Message.raw("  lspci: Unavailable"));
        }

        // ---- USB Devices ----
        context.sendMessage(Message.raw("---- USB Devices ----"));
        String lsusb = runCommandMultiline("lsusb");
        if (!lsusb.equals("Unavailable")) {
            sendMultiline(context, lsusb);
        } else {
            context.sendMessage(Message.raw("  lsusb: Unavailable"));
        }

        // ---- GPU ----
        context.sendMessage(Message.raw("---- GPU ----"));
        String lspciGpu = runCommandMultiline("lspci -v -s $(lspci | grep -i vga | cut -d' ' -f1)");
        if (lspciGpu.equals("Unavailable")) {
            sendMultiline(context, readFileSafe("/proc/driver/nvidia/version"));
        } else {
            sendMultiline(context, lspciGpu);
        }

        // ---- DMI full dump (if available) ----
        context.sendMessage(Message.raw("---- dmidecode BIOS/Board Serials ----"));
        context.sendMessage(Message.raw("dmidecode -s bios-version: " + runCommand("dmidecode -s bios-version")));
        context.sendMessage(Message.raw("dmidecode -s bios-release-date: " + runCommand("dmidecode -s bios-release-date")));
        context.sendMessage(Message.raw("dmidecode -s system-manufacturer: " + runCommand("dmidecode -s system-manufacturer")));
        context.sendMessage(Message.raw("dmidecode -s system-product-name: " + runCommand("dmidecode -s system-product-name")));
        context.sendMessage(Message.raw("dmidecode -s system-serial-number: " + runCommand("dmidecode -s system-serial-number")));
        context.sendMessage(Message.raw("dmidecode -s system-uuid: " + runCommand("dmidecode -s system-uuid")));
        context.sendMessage(Message.raw("dmidecode -s baseboard-manufacturer: " + runCommand("dmidecode -s baseboard-manufacturer")));
        context.sendMessage(Message.raw("dmidecode -s baseboard-product-name: " + runCommand("dmidecode -s baseboard-product-name")));
        context.sendMessage(Message.raw("dmidecode -s baseboard-serial-number: " + runCommand("dmidecode -s baseboard-serial-number")));
        context.sendMessage(Message.raw("dmidecode -s chassis-manufacturer: " + runCommand("dmidecode -s chassis-manufacturer")));
        context.sendMessage(Message.raw("dmidecode -s chassis-serial-number: " + runCommand("dmidecode -s chassis-serial-number")));
        context.sendMessage(Message.raw("dmidecode -s chassis-type: " + runCommand("dmidecode -s chassis-type")));
        context.sendMessage(Message.raw("dmidecode -s processor-version: " + runCommand("dmidecode -s processor-version")));
        context.sendMessage(Message.raw("dmidecode -s processor-frequency: " + runCommand("dmidecode -s processor-frequency")));

        return CompletableFuture.completedFuture(null);
    }
}
