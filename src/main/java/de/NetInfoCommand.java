package de.tobiassachs;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.concurrent.CompletableFuture;

import static de.tobiassachs.CommandUtils.*;

public class NetInfoCommand extends AbstractCommand {

    public NetInfoCommand(String name, String description) {
        super(name, description);
        requirePermission("hytale.intelligence.info");
    }

    @Nullable
    @Override
    protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {

        // ---- Network Interfaces (Java) ----
        context.sendMessage(Message.raw("---- Network Interfaces ----"));
        try {
            Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
            while (nets.hasMoreElements()) {
                NetworkInterface net = nets.nextElement();
                context.sendMessage(Message.raw(net.getName() + " (index " + net.getIndex() + ")"));
                context.sendMessage(Message.raw("  Display Name: " + net.getDisplayName()));
                context.sendMessage(Message.raw("  Up: " + net.isUp() + "  Loopback: " + net.isLoopback()
                        + "  Virtual: " + net.isVirtual() + "  P2P: " + net.isPointToPoint()));
                context.sendMessage(Message.raw("  MTU: " + net.getMTU()));

                try {
                    byte[] mac = net.getHardwareAddress();
                    if (mac != null) {
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < mac.length; i++) {
                            sb.append(String.format("%02x", mac[i]));
                            if (i < mac.length - 1) sb.append(":");
                        }
                        context.sendMessage(Message.raw("  MAC: " + sb));
                    }
                } catch (Exception ignored) {}

                Enumeration<InetAddress> addrs = net.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    context.sendMessage(Message.raw("  Addr: " + addr.getHostAddress()));
                }
            }
        } catch (Exception e) {
            context.sendMessage(Message.raw("  Failed: " + e.getMessage()));
        }

        // ---- ip addr ----
        context.sendMessage(Message.raw("---- ip addr ----"));
        sendMultiline(context, runCommandMultiline("ip addr"));

        // ---- Routing Table ----
        context.sendMessage(Message.raw("---- Routing Table ----"));
        String routes = runCommandMultiline("ip route");
        if (routes.equals("Unavailable")) {
            routes = runCommandMultiline("route -n");
        }
        sendMultiline(context, routes);

        // ---- ARP / Neighbors ----
        context.sendMessage(Message.raw("---- ARP / Neighbors ----"));
        String neigh = runCommandMultiline("ip neigh");
        if (neigh.equals("Unavailable")) {
            neigh = runCommandMultiline("arp -n");
        }
        sendMultiline(context, neigh);

        // ---- DNS Configuration ----
        context.sendMessage(Message.raw("---- DNS (/etc/resolv.conf) ----"));
        sendMultiline(context, readFileSafe("/etc/resolv.conf"));

        // ---- /etc/hosts ----
        context.sendMessage(Message.raw("---- /etc/hosts ----"));
        sendMultiline(context, readFileSafe("/etc/hosts"));

        // ---- /etc/hostname ----
        context.sendMessage(Message.raw("---- Hostname ----"));
        context.sendMessage(Message.raw("  /etc/hostname: " + readFileSafe("/etc/hostname").trim()));
        context.sendMessage(Message.raw("  hostname cmd:  " + runCommand("hostname")));
        context.sendMessage(Message.raw("  hostname -f:   " + runCommand("hostname -f")));

        // ---- Listening Ports ----
        context.sendMessage(Message.raw("---- Listening Ports ----"));
        String listeners = runCommandMultiline("ss -tlnp");
        if (listeners.equals("Unavailable")) {
            listeners = runCommandMultiline("netstat -tlnp");
        }
        sendMultiline(context, listeners);

        // ---- Active Connections ----
        context.sendMessage(Message.raw("---- Active Connections ----"));
        String connections = runCommandMultiline("ss -tnp");
        if (connections.equals("Unavailable")) {
            connections = runCommandMultiline("netstat -tnp");
        }
        sendMultiline(context, connections);

        // ---- iptables ----
        context.sendMessage(Message.raw("---- Firewall Rules ----"));
        String iptables = runCommandMultiline("iptables -L -n --line-numbers");
        if (iptables.equals("Unavailable")) {
            context.sendMessage(Message.raw("  iptables: Unavailable"));
        } else {
            sendMultiline(context, iptables);
        }

        // ---- External IP ----
        context.sendMessage(Message.raw("---- External IP ----"));
        context.sendMessage(Message.raw("  IPv4: " + runCommand("curl -s --max-time 5 https://api.ipify.org")));
        context.sendMessage(Message.raw("  IPv6: " + runCommand("curl -s --max-time 5 https://api64.ipify.org")));

        return CompletableFuture.completedFuture(null);
    }
}
