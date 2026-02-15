package de.tobiassachs;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static de.tobiassachs.CommandUtils.*;

public class SystemInfoCommand extends AbstractCommand {

    public SystemInfoCommand(String name, String description) {
        super(name, description);
    }

    @Nullable
    @Override
    protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {

        int cores = Runtime.getRuntime().availableProcessors();
        String os = System.getProperty("os.name");
        String arch = System.getProperty("os.arch");

        context.sendMessage(Message.raw("CPU cores: " + cores));
        context.sendMessage(Message.raw("OS: " + os + " (" + arch + ")"));
        context.sendMessage(Message.raw("Cloud: " + detectCloudProvider()));
        context.sendMessage(Message.raw("Virtualization: " + detectVirtualization()));

        // ---- CPU MODEL ----
        String cpuModel = "Unknown";
        try {
            for (String line : Files.readAllLines(Paths.get("/proc/cpuinfo"))) {
                if (line.startsWith("model name")) {
                    cpuModel = line.split(":", 2)[1].trim();
                    break;
                }
            }
        } catch (Exception ignored) {}

        context.sendMessage(Message.raw("CPU Model: " + cpuModel));

        // ---- ENV VARIABLES ----
        Map<String, String> env = System.getenv();
        context.sendMessage(Message.raw("---- ENV VARIABLES (" + env.size() + ") ----"));
        for (Map.Entry<String, String> entry : env.entrySet()) {
            context.sendMessage(Message.raw(entry.getKey() + "=" + entry.getValue()));
        }

        // ---- LOCAL / HOST IPs ----
        context.sendMessage(Message.raw("---- Local Network Interfaces ----"));
        try {
            Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
            while (nets.hasMoreElements()) {
                NetworkInterface net = nets.nextElement();
                Enumeration<InetAddress> addrs = net.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    context.sendMessage(
                            Message.raw(net.getName() + " → " + addr.getHostAddress())
                    );
                }
            }
        } catch (Exception e) {
            context.sendMessage(Message.raw("Failed to enumerate interfaces"));
        }

        // ---- External IPv4 ----
        try {
            String ipv4 = readFromURL("https://api.ipify.org");
            context.sendMessage(Message.raw("External IPv4: " + ipv4));
        } catch (Exception e) {
            context.sendMessage(Message.raw("External IPv4: Failed"));
        }

        // ---- External IPv6 ----
        try {
            String ipv6 = readFromURL("https://api64.ipify.org");
            context.sendMessage(Message.raw("External IPv6: " + ipv6));
        } catch (Exception e) {
            context.sendMessage(Message.raw("External IPv6: Failed"));
        }

        return CompletableFuture.completedFuture(null);
    }

    private String readFromURL(String urlString) throws Exception {
        URL url = new URL(urlString);
        BufferedReader br = new BufferedReader(
                new InputStreamReader(url.openStream())
        );
        return br.readLine();
    }

    private String detectCloudProvider() {
        try {
            if (checkUrl("http://169.254.169.254/latest/meta-data/"))
                return "AWS";

            if (checkUrlWithHeader(
                    "http://169.254.169.254/computeMetadata/v1/",
                    "Metadata-Flavor", "Google"))
                return "GCP";

            if (checkUrl("http://169.254.169.254/metadata/instance"))
                return "Azure";

        } catch (Exception ignored) {}

        return "Unknown";
    }

    private String detectVirtualization() {
        try {
            String product = Files.readString(Paths.get("/sys/class/dmi/id/product_name"));
            String vendor  = Files.readString(Paths.get("/sys/class/dmi/id/sys_vendor"));

            String combined = (product + vendor).toLowerCase();

            if (combined.contains("kvm")) return "KVM";
            if (combined.contains("vmware")) return "VMware";
            if (combined.contains("virtualbox")) return "VirtualBox";
            if (combined.contains("hyper-v")) return "Hyper-V";

            return "Physical / Unknown";

        } catch (Exception e) {
            return "Unknown";
        }
    }

    private boolean checkUrl(String url) {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(500))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .timeout(Duration.ofMillis(500))
                    .build();

            client.send(request, HttpResponse.BodyHandlers.discarding());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean checkUrlWithHeader(String url, String header, String value) {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(500))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header(header, value)
                    .GET()
                    .timeout(Duration.ofMillis(500))
                    .build();

            client.send(request, HttpResponse.BodyHandlers.discarding());
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
