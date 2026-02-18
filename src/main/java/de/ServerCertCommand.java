package de.tobiassachs;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.auth.CertificateUtil;
import com.hypixel.hytale.server.core.auth.ServerAuthManager;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;

public class ServerCertCommand extends AbstractCommand {

    public ServerCertCommand(String name, String description) {
        super(name, description);
        requirePermission("hytale.intelligence.info");
    }

    @Nullable
    @Override
    protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {

        context.sendMessage(Message.raw("---- Server Certificate ----"));
        try {
            ServerAuthManager authManager = ServerAuthManager.getInstance();
            X509Certificate cert = authManager.getServerCertificate();

            if (cert == null) {
                context.sendMessage(Message.raw("  No server certificate set (ServerAuthManager returned null)"));
                return CompletableFuture.completedFuture(null);
            }

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
            context.sendMessage(Message.raw("  Subject: " + cert.getSubjectX500Principal().getName()));
            context.sendMessage(Message.raw("  Issuer: " + cert.getIssuerX500Principal().getName()));
            context.sendMessage(Message.raw("  Serial: " + cert.getSerialNumber().toString(16)));
            context.sendMessage(Message.raw("  Not Before: " + sdf.format(cert.getNotBefore())));
            context.sendMessage(Message.raw("  Not After:  " + sdf.format(cert.getNotAfter())));
            context.sendMessage(Message.raw("  Sig Algorithm: " + cert.getSigAlgName()));
            context.sendMessage(Message.raw("  Version: " + cert.getVersion()));

            if (cert.getPublicKey() instanceof RSAPublicKey rsaPub) {
                context.sendMessage(Message.raw("  Public Key: RSA " + rsaPub.getModulus().bitLength() + " bit"));
            } else {
                context.sendMessage(Message.raw("  Public Key: " + cert.getPublicKey().getAlgorithm()));
            }

            // Validity check
            try {
                cert.checkValidity();
                context.sendMessage(Message.raw("  Status: VALID"));
            } catch (Exception e) {
                context.sendMessage(Message.raw("  Status: " + e.getMessage()));
            }

            // Self-signed check
            try {
                cert.verify(cert.getPublicKey());
                context.sendMessage(Message.raw("  Self-signed: YES"));
            } catch (Exception e) {
                context.sendMessage(Message.raw("  Self-signed: NO"));
            }

            // SHA-256 fingerprint
            try {
                java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
                byte[] digest = md.digest(cert.getEncoded());
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < digest.length; i++) {
                    if (i > 0) sb.append(":");
                    sb.append(String.format("%02X", digest[i]));
                }
                context.sendMessage(Message.raw("  SHA-256 Fingerprint: " + sb));
            } catch (Exception ignored) {}

            // Auth fingerprints
            String authFingerprint = authManager.getServerCertificateFingerprint();
            if (authFingerprint != null) {
                context.sendMessage(Message.raw("  Auth Fingerprint: " + authFingerprint));
            }
            try {
                String computedFingerprint = CertificateUtil.computeCertificateFingerprint(cert);
                context.sendMessage(Message.raw("  CertificateUtil Fingerprint: " + computedFingerprint));
            } catch (Exception ignored) {}

            // PEM encoding
            context.sendMessage(Message.raw(""));
            context.sendMessage(Message.raw("  ---- PEM ----"));
            byte[] encoded = cert.getEncoded();
            String base64 = Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(encoded);
            String pem = "-----BEGIN CERTIFICATE-----\n" + base64 + "\n-----END CERTIFICATE-----";
            for (String line : pem.split("\n")) {
                context.sendMessage(Message.raw("  " + line));
            }

        } catch (Exception e) {
            context.sendMessage(Message.raw("  Failed: " + e.getClass().getSimpleName() + ": " + e.getMessage()));
        }

        return CompletableFuture.completedFuture(null);
    }
}
