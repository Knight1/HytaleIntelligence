package de.tobiassachs;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class CaCertsCommand extends AbstractCommand {

    private final OptionalArg<List<String>> argsArg;

    public CaCertsCommand(String name, String description) {
        super(name, description);
        this.argsArg = withListOptionalArg("args", "Flags: -a to show all", ArgTypes.STRING);
    }

    @Nullable
    @Override
    protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {

        boolean showAll = false;
        List<String> args = this.argsArg.get(context);
        if (args != null) {
            for (String arg : args) {
                if (arg.equals("-a") || arg.equals("--all")) {
                    showAll = true;
                }
            }
        }

        context.sendMessage(Message.raw("---- CA Certificates (JVM TrustStore) ----"));
        context.sendMessage(Message.raw("TrustStore path: " + System.getProperty("javax.net.ssl.trustStore", "<default cacerts>")));

        try {
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init((KeyStore) null);

            X509TrustManager tm = Arrays.stream(tmf.getTrustManagers())
                    .filter(m -> m instanceof X509TrustManager)
                    .map(m -> (X509TrustManager) m)
                    .findFirst()
                    .orElse(null);

            if (tm == null) {
                context.sendMessage(Message.raw("No X509TrustManager found"));
                return CompletableFuture.completedFuture(null);
            }

            X509Certificate[] certs = tm.getAcceptedIssuers();
            context.sendMessage(Message.raw("Total CA certificates: " + certs.length));

            if (showAll) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                context.sendMessage(Message.raw(""));

                for (int i = 0; i < certs.length; i++) {
                    X509Certificate cert = certs[i];
                    String cn = extractCN(cert.getSubjectX500Principal().getName());
                    String issuerCn = extractCN(cert.getIssuerX500Principal().getName());
                    String notBefore = sdf.format(cert.getNotBefore());
                    String notAfter = sdf.format(cert.getNotAfter());

                    context.sendMessage(Message.raw(
                            (i + 1) + ". " + cn
                    ));
                    context.sendMessage(Message.raw(
                            "   Issuer: " + issuerCn
                                    + "  Valid: " + notBefore + " → " + notAfter
                                    + "  Algo: " + cert.getSigAlgName()
                    ));
                }
            } else {
                context.sendMessage(Message.raw("Use 'cacerts -a' to list all certificates"));
            }

        } catch (Exception e) {
            context.sendMessage(Message.raw("Failed to read CA certs: " + e.getMessage()));
        }

        return CompletableFuture.completedFuture(null);
    }

    private String extractCN(String dn) {
        for (String part : dn.split(",")) {
            String trimmed = part.trim();
            if (trimmed.startsWith("CN=")) {
                return trimmed.substring(3);
            }
        }
        return dn;
    }
}
