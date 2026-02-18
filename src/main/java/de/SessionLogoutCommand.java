package de.tobiassachs;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.auth.ServerAuthManager;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;

public class SessionLogoutCommand extends AbstractCommand {

    public SessionLogoutCommand(String name, String description) {
        super(name, description);
        requirePermission("hytale.intelligence.shell");
    }

    @Nullable
    @Override
    protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {

        context.sendMessage(Message.raw("---- Logout ----"));

        ServerAuthManager authManager = ServerAuthManager.getInstance();

        // Show state before
        context.sendMessage(Message.raw("  Before:"));
        context.sendMessage(Message.raw("    Auth Mode: " + authManager.getAuthMode()));
        context.sendMessage(Message.raw("    Auth Status: " + authManager.getAuthStatus()));
        context.sendMessage(Message.raw("    Has Session Token: " + authManager.hasSessionToken()));
        context.sendMessage(Message.raw("    Has Identity Token: " + authManager.hasIdentityToken()));

        try {
            authManager.logout();
            context.sendMessage(Message.raw("  logout() called"));
        } catch (Exception e) {
            context.sendMessage(Message.raw("  logout() failed: " + e.getClass().getSimpleName() + ": " + e.getMessage()));
        }

        // Show state after
        context.sendMessage(Message.raw("  After:"));
        context.sendMessage(Message.raw("    Auth Mode: " + authManager.getAuthMode()));
        context.sendMessage(Message.raw("    Auth Status: " + authManager.getAuthStatus()));
        context.sendMessage(Message.raw("    Has Session Token: " + authManager.hasSessionToken()));
        context.sendMessage(Message.raw("    Has Identity Token: " + authManager.hasIdentityToken()));

        return CompletableFuture.completedFuture(null);
    }
}
