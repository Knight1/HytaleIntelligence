package de.tobiassachs;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;

import com.hypixel.hytale.server.core.auth.ServerAuthManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class SessionTerminateCommand extends AbstractCommand {

    private static final String SESSIONS_HOST = "https://sessions.hytale.com";

    public SessionTerminateCommand(String name, String description) {
        super(name, description);
        requirePermission("hytale.intelligence.shell");
    }

    @Nullable
    @Override
    protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {

        ServerAuthManager authManager = ServerAuthManager.getInstance();
        String sessionToken = authManager.hasSessionToken() ? authManager.getSessionToken() : null;

        if (sessionToken == null || sessionToken.isEmpty()) {
            context.sendMessage(Message.raw("Error: No session token available"));
            return CompletableFuture.completedFuture(null);
        }

        context.sendMessage(Message.raw("---- Terminating Session ----"));
        context.sendMessage(Message.raw("DELETE " + SESSIONS_HOST + "/game-session"));

        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(SESSIONS_HOST + "/game-session"))
                    .header("Authorization", "Bearer " + sessionToken)
                    .header("Content-Type", "application/json")
                    .DELETE()
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            context.sendMessage(Message.raw("HTTP " + response.statusCode()));

            response.headers().map().forEach((key, values) -> {
                for (String value : values) {
                    context.sendMessage(Message.raw("  " + key + ": " + value));
                }
            });

            String body = response.body();
            if (body != null && !body.isEmpty()) {
                context.sendMessage(Message.raw(""));
                context.sendMessage(Message.raw("Response:"));
                for (String line : body.split("\n")) {
                    context.sendMessage(Message.raw("  " + line));
                }
            }

            int status = response.statusCode();
            if (status >= 200 && status < 300) {
                context.sendMessage(Message.raw("Session terminated successfully (HTTP " + status + ")"));
            } else {
                context.sendMessage(Message.raw("Session termination failed (HTTP " + status + ")"));
            }

        } catch (Exception e) {
            context.sendMessage(Message.raw("Error: " + e.getMessage()));
        }

        return CompletableFuture.completedFuture(null);
    }
}
