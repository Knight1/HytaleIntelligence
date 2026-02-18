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
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class SessionValidateCommand extends AbstractCommand {

    private static final String JWKS_URL = "https://sessions.hytale.com/.well-known/jwks.json";

    // X.509 DER prefix for Ed25519 public keys (12 bytes) followed by the 32-byte raw key
    private static final byte[] ED25519_X509_PREFIX = {
            0x30, 0x2a, 0x30, 0x05, 0x06, 0x03, 0x2b, 0x65, 0x70, 0x03, 0x21, 0x00
    };

    public SessionValidateCommand(String name, String description) {
        super(name, description);
        requirePermission("hytale.intelligence.shell");
    }

    @Nullable
    @Override
    protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {

        ServerAuthManager authManager = ServerAuthManager.getInstance();
        String sessionToken = authManager.hasSessionToken() ? authManager.getSessionToken() : null;
        String identityToken = authManager.hasIdentityToken() ? authManager.getIdentityToken() : null;

        if ((sessionToken == null || sessionToken.isEmpty())
                && (identityToken == null || identityToken.isEmpty())) {
            context.sendMessage(Message.raw("Error: No session or identity token available"));
            return CompletableFuture.completedFuture(null);
        }

        List<String> lines = validateAll(sessionToken, identityToken);
        for (String line : lines) {
            context.sendMessage(Message.raw(line));
        }

        return CompletableFuture.completedFuture(null);
    }

    /**
     * Validates both session and identity tokens against the JWKS endpoint.
     * Returns a list of log lines describing the results.
     * Can be called from anywhere (e.g. plugin startup) without a CommandContext.
     */
    public static List<String> validateAll(String sessionToken, String identityToken) {
        List<String> lines = new ArrayList<>();

        if ((sessionToken == null || sessionToken.isEmpty())
                && (identityToken == null || identityToken.isEmpty())) {
            lines.add("No session or identity token set, skipping validation");
            return lines;
        }

        try {
            // Fetch JWKS once for both tokens
            lines.add("---- Fetching JWKS ----");
            lines.add("GET " + JWKS_URL);

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            HttpRequest jwksRequest = HttpRequest.newBuilder()
                    .uri(URI.create(JWKS_URL))
                    .GET()
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> jwksResponse = client.send(jwksRequest, HttpResponse.BodyHandlers.ofString());
            lines.add("HTTP " + jwksResponse.statusCode());

            if (jwksResponse.statusCode() != 200) {
                lines.add("Error: Failed to fetch JWKS");
                String body = jwksResponse.body();
                if (body != null && !body.isEmpty()) {
                    lines.add("Response: " + body);
                }
                return lines;
            }

            List<JwkKey> keys = parseJwks(jwksResponse.body());
            lines.add("Found " + keys.size() + " key(s) in JWKS");
            for (JwkKey key : keys) {
                lines.add("  kid=" + key.kid + " alg=" + key.alg + " crv=" + key.crv + " kty=" + key.kty);
            }

            // Validate session token
            lines.add("");
            if (sessionToken != null && !sessionToken.isEmpty()) {
                validateToken(lines, "HYTALE_SERVER_SESSION_TOKEN", sessionToken, keys);
            } else {
                lines.add("---- Session Token ----");
                lines.add("Not set, skipping");
            }

            // Validate identity token
            lines.add("");
            if (identityToken != null && !identityToken.isEmpty()) {
                validateToken(lines, "HYTALE_SERVER_IDENTITY_TOKEN", identityToken, keys);
            } else {
                lines.add("---- Identity Token ----");
                lines.add("Not set, skipping");
            }

        } catch (Exception e) {
            lines.add("Error: " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }

        return lines;
    }

    /**
     * Quick validation returning only pass/fail per token. No verbose output.
     * Index 0 = session token result, index 1 = identity token result.
     * Values: "valid", "invalid", "not set", "error: ..."
     */
    public static String[] validateQuiet(String sessionToken, String identityToken) {
        String[] result = new String[2];
        result[0] = validateTokenQuiet(sessionToken);
        result[1] = validateTokenQuiet(identityToken);
        return result;
    }

    private static String validateTokenQuiet(String jwt) {
        if (jwt == null || jwt.isEmpty()) return "not set";

        String[] jwtParts = jwt.split("\\.");
        if (jwtParts.length != 3) return "invalid (not a JWT)";

        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            HttpRequest jwksRequest = HttpRequest.newBuilder()
                    .uri(URI.create(JWKS_URL))
                    .GET()
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> jwksResponse = client.send(jwksRequest, HttpResponse.BodyHandlers.ofString());
            if (jwksResponse.statusCode() != 200) return "error: JWKS fetch failed (HTTP " + jwksResponse.statusCode() + ")";

            List<JwkKey> keys = parseJwks(jwksResponse.body());

            String headerJson = decodeBase64(jwtParts[0]);
            String kid = extractJsonString(headerJson, "kid");

            JwkKey matchingKey = null;
            if (kid != null) {
                for (JwkKey key : keys) {
                    if (kid.equals(key.kid)) { matchingKey = key; break; }
                }
            }
            if (matchingKey == null && keys.size() == 1) matchingKey = keys.get(0);
            if (matchingKey == null) return "error: no matching key for kid=" + kid;

            byte[] rawPublicKey = Base64.getUrlDecoder().decode(padBase64(matchingKey.x));
            if (rawPublicKey.length != 32) return "error: bad key length";

            byte[] x509Encoded = new byte[ED25519_X509_PREFIX.length + rawPublicKey.length];
            System.arraycopy(ED25519_X509_PREFIX, 0, x509Encoded, 0, ED25519_X509_PREFIX.length);
            System.arraycopy(rawPublicKey, 0, x509Encoded, ED25519_X509_PREFIX.length, rawPublicKey.length);

            java.security.KeyFactory keyFactory = java.security.KeyFactory.getInstance("Ed25519");
            java.security.PublicKey publicKey = keyFactory.generatePublic(new X509EncodedKeySpec(x509Encoded));

            byte[] signedContent = (jwtParts[0] + "." + jwtParts[1]).getBytes(java.nio.charset.StandardCharsets.UTF_8);
            byte[] signature = Base64.getUrlDecoder().decode(padBase64(jwtParts[2]));

            Signature verifier = Signature.getInstance("Ed25519");
            verifier.initVerify(publicKey);
            verifier.update(signedContent);

            return verifier.verify(signature) ? "valid" : "invalid";
        } catch (Exception e) {
            return "error: " + e.getMessage();
        }
    }

    private static void validateToken(List<String> lines, String name, String jwt, List<JwkKey> keys) {
        lines.add("---- Validating " + name + " ----");

        String[] jwtParts = jwt.split("\\.");
        if (jwtParts.length != 3) {
            lines.add("Error: Not a valid JWT (expected 3 parts, got " + jwtParts.length + ")");
            return;
        }

        try {
            // Decode header
            String headerJson = decodeBase64(jwtParts[0]);
            lines.add("JWT Header: " + headerJson);

            String kid = extractJsonString(headerJson, "kid");
            String alg = extractJsonString(headerJson, "alg");

            lines.add("Algorithm: " + (alg != null ? alg : "not specified"));
            lines.add("Key ID:    " + (kid != null ? kid : "not specified"));

            if (alg != null && !alg.equals("EdDSA")) {
                lines.add("Warning: Expected algorithm EdDSA, got " + alg);
            }

            // Decode payload
            String payloadJson = decodeBase64(jwtParts[1]);
            lines.add("JWT Payload: " + payloadJson);

            // Check expiration
            String expStr = extractJsonValue(payloadJson, "exp");
            if (expStr != null && expStr.matches("\\d+")) {
                long exp = Long.parseLong(expStr);
                long now = System.currentTimeMillis() / 1000;
                if (now > exp) {
                    java.util.Date expDate = new java.util.Date(exp * 1000);
                    lines.add("WARNING: Token expired at " + expDate);
                } else {
                    long remaining = exp - now;
                    lines.add("Token expires in: " + formatSeconds(remaining));
                }
            }

            // Find matching key
            JwkKey matchingKey = null;
            if (kid != null) {
                for (JwkKey key : keys) {
                    if (kid.equals(key.kid)) {
                        matchingKey = key;
                        break;
                    }
                }
            }

            if (matchingKey == null && keys.size() == 1) {
                matchingKey = keys.get(0);
                lines.add("No kid match, using only available key");
            }

            if (matchingKey == null) {
                lines.add("Error: No matching key found for kid=" + kid);
                return;
            }

            lines.add("Using key: " + matchingKey.kid);

            // Build Ed25519 public key
            byte[] rawPublicKey = Base64.getUrlDecoder().decode(padBase64(matchingKey.x));
            lines.add("Public key length: " + rawPublicKey.length + " bytes");

            if (rawPublicKey.length != 32) {
                lines.add("Error: Expected 32-byte Ed25519 public key, got " + rawPublicKey.length);
                return;
            }

            byte[] x509Encoded = new byte[ED25519_X509_PREFIX.length + rawPublicKey.length];
            System.arraycopy(ED25519_X509_PREFIX, 0, x509Encoded, 0, ED25519_X509_PREFIX.length);
            System.arraycopy(rawPublicKey, 0, x509Encoded, ED25519_X509_PREFIX.length, rawPublicKey.length);

            KeyFactory keyFactory = KeyFactory.getInstance("Ed25519");
            PublicKey publicKey = keyFactory.generatePublic(new X509EncodedKeySpec(x509Encoded));

            // Verify signature
            byte[] signedContent = (jwtParts[0] + "." + jwtParts[1]).getBytes(java.nio.charset.StandardCharsets.UTF_8);
            byte[] signature = Base64.getUrlDecoder().decode(padBase64(jwtParts[2]));

            Signature verifier = Signature.getInstance("Ed25519");
            verifier.initVerify(publicKey);
            verifier.update(signedContent);
            boolean valid = verifier.verify(signature);

            if (valid) {
                lines.add("VALID - Signature verification PASSED");
            } else {
                lines.add("INVALID - Signature verification FAILED");
            }

        } catch (Exception e) {
            lines.add("Error: " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private static String decodeBase64(String encoded) {
        String padded = padBase64(encoded);
        byte[] decoded = Base64.getUrlDecoder().decode(padded);
        return new String(decoded);
    }

    private static String padBase64(String encoded) {
        int mod = encoded.length() % 4;
        if (mod > 0) {
            return encoded + "====".substring(mod);
        }
        return encoded;
    }

    static String extractJsonString(String json, String field) {
        String search = "\"" + field + "\"";
        int idx = json.indexOf(search);
        if (idx < 0) return null;

        int colonIdx = json.indexOf(':', idx + search.length());
        if (colonIdx < 0) return null;

        int valueStart = colonIdx + 1;
        while (valueStart < json.length() && json.charAt(valueStart) == ' ') valueStart++;
        if (valueStart >= json.length() || json.charAt(valueStart) != '"') return null;

        int valueEnd = json.indexOf('"', valueStart + 1);
        if (valueEnd < 0) return null;

        return json.substring(valueStart + 1, valueEnd);
    }

    private static String extractJsonValue(String json, String field) {
        String search = "\"" + field + "\"";
        int idx = json.indexOf(search);
        if (idx < 0) return null;

        int colonIdx = json.indexOf(':', idx + search.length());
        if (colonIdx < 0) return null;

        int valueStart = colonIdx + 1;
        while (valueStart < json.length() && json.charAt(valueStart) == ' ') valueStart++;
        if (valueStart >= json.length()) return null;

        if (json.charAt(valueStart) == '"') {
            int valueEnd = json.indexOf('"', valueStart + 1);
            if (valueEnd < 0) return null;
            return json.substring(valueStart + 1, valueEnd);
        } else {
            int valueEnd = valueStart;
            while (valueEnd < json.length() && json.charAt(valueEnd) != ',' && json.charAt(valueEnd) != '}') {
                valueEnd++;
            }
            return json.substring(valueStart, valueEnd).trim();
        }
    }

    static List<JwkKey> parseJwks(String json) {
        List<JwkKey> keys = new ArrayList<>();

        int keysIdx = json.indexOf("\"keys\"");
        if (keysIdx < 0) return keys;

        int arrayStart = json.indexOf('[', keysIdx);
        if (arrayStart < 0) return keys;

        int pos = arrayStart + 1;
        while (pos < json.length()) {
            int objStart = json.indexOf('{', pos);
            if (objStart < 0) break;

            int objEnd = json.indexOf('}', objStart);
            if (objEnd < 0) break;

            String keyObj = json.substring(objStart, objEnd + 1);

            JwkKey key = new JwkKey();
            key.kty = extractJsonString(keyObj, "kty");
            key.alg = extractJsonString(keyObj, "alg");
            key.use = extractJsonString(keyObj, "use");
            key.kid = extractJsonString(keyObj, "kid");
            key.crv = extractJsonString(keyObj, "crv");
            key.x = extractJsonString(keyObj, "x");
            keys.add(key);

            pos = objEnd + 1;
        }

        return keys;
    }

    private static String formatSeconds(long totalSeconds) {
        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long secs = totalSeconds % 60;
        return days + "d " + hours + "h " + minutes + "m " + secs + "s";
    }

    static class JwkKey {
        String kty;
        String alg;
        String use;
        String kid;
        String crv;
        String x;
    }
}
