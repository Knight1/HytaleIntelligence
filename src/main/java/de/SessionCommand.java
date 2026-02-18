package de.tobiassachs;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.auth.ServerAuthManager;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;

public class SessionCommand extends AbstractCommand {

    public SessionCommand(String name, String description) {
        super(name, description);
        requirePermission("hytale.intelligence.info");
    }

    @Nullable
    @Override
    protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {

        ServerAuthManager authManager = ServerAuthManager.getInstance();

        // ---- Auth State ----
        context.sendMessage(Message.raw("---- Auth State ----"));
        context.sendMessage(Message.raw("  Auth Mode: " + authManager.getAuthMode()));
        context.sendMessage(Message.raw("  Auth Status: " + authManager.getAuthStatus()));
        context.sendMessage(Message.raw("  Singleplayer: " + authManager.isSingleplayer()));
        context.sendMessage(Message.raw("  Server Session ID: " + authManager.getServerSessionId()));

        if (authManager.getTokenExpiry() != null) {
            context.sendMessage(Message.raw("  Token Expiry: " + authManager.getTokenExpiry()));
        }

        // ---- Session Token ----
        context.sendMessage(Message.raw(""));
        context.sendMessage(Message.raw("---- Session Token ----"));
        if (authManager.hasSessionToken()) {
            String sessionToken = authManager.getSessionToken();
            context.sendMessage(Message.raw("  " + sessionToken));
            decodeJwt(context, sessionToken);
        } else {
            context.sendMessage(Message.raw("  Not set"));
        }

        // ---- Identity Token ----
        context.sendMessage(Message.raw(""));
        context.sendMessage(Message.raw("---- Identity Token ----"));
        if (authManager.hasIdentityToken()) {
            String identityToken = authManager.getIdentityToken();
            context.sendMessage(Message.raw("  " + identityToken));
            decodeJwt(context, identityToken);
        } else {
            context.sendMessage(Message.raw("  Not set"));
        }

        // ---- OAuth Access Token ----
        context.sendMessage(Message.raw(""));
        context.sendMessage(Message.raw("---- OAuth Access Token ----"));
        try {
            String oauthToken = authManager.getOAuthAccessToken();
            if (oauthToken != null && !oauthToken.isEmpty()) {
                context.sendMessage(Message.raw("  " + oauthToken));
                decodeJwt(context, oauthToken);
            } else {
                context.sendMessage(Message.raw("  Not set"));
            }
        } catch (Exception e) {
            context.sendMessage(Message.raw("  " + e.getMessage()));
        }

        // ---- Selected Profile ----
        context.sendMessage(Message.raw(""));
        context.sendMessage(Message.raw("---- Selected Profile ----"));
        try {
            var profile = authManager.getSelectedProfile();
            if (profile != null) {
                context.sendMessage(Message.raw("  " + profile));
            } else {
                context.sendMessage(Message.raw("  None"));
            }
        } catch (Exception e) {
            context.sendMessage(Message.raw("  " + e.getMessage()));
        }

        // ---- Game Session ----
        context.sendMessage(Message.raw(""));
        context.sendMessage(Message.raw("---- Game Session ----"));
        try {
            var gameSession = authManager.getGameSession();
            if (gameSession != null) {
                context.sendMessage(Message.raw("  " + gameSession));
            } else {
                context.sendMessage(Message.raw("  None"));
            }
        } catch (Exception e) {
            context.sendMessage(Message.raw("  " + e.getMessage()));
        }

        // ---- Auth-related env vars (keep as supplementary) ----
        context.sendMessage(Message.raw(""));
        context.sendMessage(Message.raw("---- Auth-Related ENV ----"));
        boolean anyFound = false;
        for (var entry : System.getenv().entrySet()) {
            String key = entry.getKey().toUpperCase();
            if (key.contains("TOKEN") || key.contains("SESSION") || key.contains("AUTH")
                    || key.contains("SECRET") || key.contains("CREDENTIAL") || key.contains("HYTALE")) {
                context.sendMessage(Message.raw("  " + entry.getKey() + "=" + entry.getValue()));
                anyFound = true;
            }
        }
        if (!anyFound) {
            context.sendMessage(Message.raw("  (none found)"));
        }

        return CompletableFuture.completedFuture(null);
    }

    private void decodeJwt(CommandContext context, String jwt) {
        try {
            String[] parts = jwt.split("\\.");
            if (parts.length < 2) {
                context.sendMessage(Message.raw("  (not a valid JWT format)"));
                return;
            }

            String header = decodeBase64(parts[0]);
            context.sendMessage(Message.raw("  Header: " + header));

            String payload = decodeBase64(parts[1]);
            context.sendMessage(Message.raw("  Payload: " + payload));

            extractField(context, payload, "iss", "Issuer");
            extractField(context, payload, "sub", "Subject");
            extractField(context, payload, "aud", "Audience");
            extractField(context, payload, "exp", "Expires");
            extractField(context, payload, "iat", "Issued At");
            extractField(context, payload, "nbf", "Not Before");
            extractField(context, payload, "jti", "JWT ID");

        } catch (Exception e) {
            context.sendMessage(Message.raw("  (failed to decode JWT: " + e.getMessage() + ")"));
        }
    }

    private String decodeBase64(String encoded) {
        String padded = encoded;
        int mod = padded.length() % 4;
        if (mod > 0) {
            padded += "====".substring(mod);
        }
        return new String(Base64.getUrlDecoder().decode(padded));
    }

    private void extractField(CommandContext context, String json, String field, String label) {
        String value = SessionValidateCommand.extractJsonString(json, field);
        if (value == null) {
            // Try numeric value
            String search = "\"" + field + "\"";
            int idx = json.indexOf(search);
            if (idx < 0) return;
            int colonIdx = json.indexOf(':', idx + search.length());
            if (colonIdx < 0) return;
            int valueStart = colonIdx + 1;
            while (valueStart < json.length() && json.charAt(valueStart) == ' ') valueStart++;
            if (valueStart >= json.length()) return;
            int valueEnd = valueStart;
            while (valueEnd < json.length() && json.charAt(valueEnd) != ',' && json.charAt(valueEnd) != '}') {
                valueEnd++;
            }
            value = json.substring(valueStart, valueEnd).trim();
        }

        if ((field.equals("exp") || field.equals("iat") || field.equals("nbf")) && value.matches("\\d+")) {
            long epoch = Long.parseLong(value);
            java.util.Date date = new java.util.Date(epoch * 1000);
            context.sendMessage(Message.raw("    " + label + ": " + value + " (" + date + ")"));
        } else {
            context.sendMessage(Message.raw("    " + label + ": " + value));
        }
    }
}
