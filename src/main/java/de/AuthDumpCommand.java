package de.tobiassachs;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters;
import com.hypixel.hytale.server.core.io.adapter.PacketFilter;
import com.hypixel.hytale.server.core.io.adapter.PacketWatcher;
import com.hypixel.hytale.protocol.packets.auth.AuthGrant;
import com.hypixel.hytale.protocol.packets.auth.AuthToken;
import com.hypixel.hytale.protocol.packets.auth.ServerAuthToken;
import com.hypixel.hytale.protocol.packets.auth.ConnectAccept;
import com.hypixel.hytale.protocol.packets.connection.Connect;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class AuthDumpCommand extends AbstractCommand {

    private PacketFilter inboundFilter;
    private PacketFilter outboundFilter;
    private boolean active = false;
    private final List<String> capturedLines = Collections.synchronizedList(new ArrayList<>());

    public AuthDumpCommand(String name, String description) {
        super(name, description);
        setAllowsExtraArguments(true);
        requirePermission("hytale.intelligence.shell");
    }

    @Nullable
    @Override
    protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {

        String input = context.getInputString().trim();
        String arg = "";
        int spaceIdx = input.indexOf(' ');
        if (spaceIdx >= 0) {
            arg = input.substring(spaceIdx + 1).trim().toLowerCase();
        }

        if (arg.equals("stop") || arg.equals("off")) {
            if (!active) {
                context.sendMessage(Message.raw("Auth dump is not active"));
                return CompletableFuture.completedFuture(null);
            }
            stopCapture();
            context.sendMessage(Message.raw("Auth dump stopped"));
            context.sendMessage(Message.raw("Captured " + capturedLines.size() + " entries"));
            return CompletableFuture.completedFuture(null);
        }

        if (arg.equals("show") || arg.equals("log")) {
            context.sendMessage(Message.raw("---- Auth Dump Log (" + capturedLines.size() + " entries) ----"));
            synchronized (capturedLines) {
                for (String line : capturedLines) {
                    context.sendMessage(Message.raw(line));
                }
            }
            if (capturedLines.isEmpty()) {
                context.sendMessage(Message.raw("  (no packets captured yet)"));
            }
            return CompletableFuture.completedFuture(null);
        }

        if (arg.equals("clear")) {
            int count = capturedLines.size();
            capturedLines.clear();
            context.sendMessage(Message.raw("Cleared " + count + " entries"));
            return CompletableFuture.completedFuture(null);
        }

        if (arg.equals("start") || arg.equals("on") || arg.isEmpty()) {
            if (active) {
                context.sendMessage(Message.raw("Auth dump is already active (" + capturedLines.size() + " entries captured)"));
                return CompletableFuture.completedFuture(null);
            }
            startCapture();
            context.sendMessage(Message.raw("Auth dump started - watching for auth handshake packets"));
            context.sendMessage(Message.raw("Use 'authdump show' to view captured packets"));
            context.sendMessage(Message.raw("Use 'authdump stop' to stop capturing"));
            return CompletableFuture.completedFuture(null);
        }

        context.sendMessage(Message.raw("Usage: authdump [start|stop|show|clear]"));
        return CompletableFuture.completedFuture(null);
    }

    private void startCapture() {
        PacketWatcher inboundWatcher = (handler, packet) -> {
            processPacket("INBOUND", packet);
        };

        PacketWatcher outboundWatcher = (handler, packet) -> {
            processPacket("OUTBOUND", packet);
        };

        inboundFilter = PacketAdapters.registerInbound(inboundWatcher);
        outboundFilter = PacketAdapters.registerOutbound(outboundWatcher);
        active = true;
    }

    private void stopCapture() {
        if (inboundFilter != null) {
            PacketAdapters.deregisterInbound(inboundFilter);
            inboundFilter = null;
        }
        if (outboundFilter != null) {
            PacketAdapters.deregisterOutbound(outboundFilter);
            outboundFilter = null;
        }
        active = false;
    }

    private void processPacket(String direction, Object packet) {
        String timestamp = java.time.LocalTime.now().toString();

        if (packet instanceof Connect p) {
            capturedLines.add(timestamp + " [" + direction + "] Connect (ID 0)");
            capturedLines.add("  protocolCrc: " + p.protocolCrc);
            capturedLines.add("  protocolBuildNumber: " + p.protocolBuildNumber);
            capturedLines.add("  clientVersion: " + p.clientVersion);
            capturedLines.add("  clientType: " + p.clientType);
            capturedLines.add("  uuid: " + p.uuid);
            capturedLines.add("  username: " + p.username);
            capturedLines.add("  language: " + p.language);
            if (p.identityToken != null) {
                capturedLines.add("  identityToken: " + p.identityToken);
                decodeJwtSummary("  identityToken", p.identityToken);
            } else {
                capturedLines.add("  identityToken: null");
            }
            if (p.referralData != null) {
                capturedLines.add("  referralData: " + bytesToHex(p.referralData) + " (" + p.referralData.length + " bytes)");
            }
            if (p.referralSource != null) {
                capturedLines.add("  referralSource: " + p.referralSource);
            }
        } else if (packet instanceof AuthGrant p) {
            capturedLines.add(timestamp + " [" + direction + "] AuthGrant (ID 11)");
            if (p.authorizationGrant != null) {
                capturedLines.add("  authorizationGrant: " + p.authorizationGrant);
            } else {
                capturedLines.add("  authorizationGrant: null");
            }
            if (p.serverIdentityToken != null) {
                capturedLines.add("  serverIdentityToken: " + p.serverIdentityToken);
                decodeJwtSummary("  serverIdentityToken", p.serverIdentityToken);
            } else {
                capturedLines.add("  serverIdentityToken: null");
            }
        } else if (packet instanceof AuthToken p) {
            capturedLines.add(timestamp + " [" + direction + "] AuthToken (ID 12)");
            if (p.accessToken != null) {
                capturedLines.add("  accessToken: " + p.accessToken);
                decodeJwtSummary("  accessToken", p.accessToken);
            } else {
                capturedLines.add("  accessToken: null");
            }
            if (p.serverAuthorizationGrant != null) {
                capturedLines.add("  serverAuthorizationGrant: " + p.serverAuthorizationGrant);
            } else {
                capturedLines.add("  serverAuthorizationGrant: null");
            }
        } else if (packet instanceof ServerAuthToken p) {
            capturedLines.add(timestamp + " [" + direction + "] ServerAuthToken (ID 13)");
            if (p.serverAccessToken != null) {
                capturedLines.add("  serverAccessToken: " + p.serverAccessToken);
                decodeJwtSummary("  serverAccessToken", p.serverAccessToken);
            } else {
                capturedLines.add("  serverAccessToken: null");
            }
            if (p.passwordChallenge != null) {
                capturedLines.add("  passwordChallenge: " + bytesToHex(p.passwordChallenge) + " (" + p.passwordChallenge.length + " bytes)");
            } else {
                capturedLines.add("  passwordChallenge: null");
            }
        } else if (packet instanceof ConnectAccept p) {
            capturedLines.add(timestamp + " [" + direction + "] ConnectAccept (ID 14)");
            if (p.passwordChallenge != null) {
                capturedLines.add("  passwordChallenge: " + bytesToHex(p.passwordChallenge) + " (" + p.passwordChallenge.length + " bytes)");
            } else {
                capturedLines.add("  passwordChallenge: null");
            }
        }
    }

    private void decodeJwtSummary(String prefix, String jwt) {
        try {
            String[] parts = jwt.split("\\.");
            if (parts.length >= 2) {
                String header = decodeBase64(parts[0]);
                String payload = decodeBase64(parts[1]);
                capturedLines.add(prefix + ".header: " + header);
                capturedLines.add(prefix + ".payload: " + payload);
            }
        } catch (Exception e) {
            capturedLines.add(prefix + ".decode: failed (" + e.getMessage() + ")");
        }
    }

    private String decodeBase64(String encoded) {
        String padded = encoded;
        int mod = padded.length() % 4;
        if (mod > 0) {
            padded += "====".substring(mod);
        }
        byte[] decoded = Base64.getUrlDecoder().decode(padded);
        return new String(decoded);
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
