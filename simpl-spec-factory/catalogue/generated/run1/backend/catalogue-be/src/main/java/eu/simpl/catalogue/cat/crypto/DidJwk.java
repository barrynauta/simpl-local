package eu.simpl.catalogue.cat.crypto;

import com.fasterxml.jackson.databind.JsonNode;
import eu.simpl.catalogue.common.Json;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** did:jwk handling for Ed25519 keys (D22). */
public final class DidJwk {
    public static final String PREFIX = "did:jwk:";

    private DidJwk() {}

    /** Returns the base64url public key x of an Ed25519 did:jwk, or throws IllegalArgumentException. */
    public static String publicX(String did) {
        if (did == null || !did.startsWith(PREFIX)) throw new IllegalArgumentException("Not a did:jwk");
        String b64 = did.substring(PREFIX.length());
        int hash = b64.indexOf('#');
        if (hash >= 0) b64 = b64.substring(0, hash);
        byte[] raw;
        try {
            raw = Base64.getUrlDecoder().decode(stripPadding(b64));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("did:jwk is not base64url");
        }
        JsonNode jwk = Json.parseOrNull(new String(raw, StandardCharsets.UTF_8));
        if (jwk == null || !"OKP".equals(jwk.path("kty").asText()) || !"Ed25519".equals(jwk.path("crv").asText())
                || !jwk.path("x").isTextual()) {
            throw new IllegalArgumentException("did:jwk is not an Ed25519 OKP key");
        }
        return stripPadding(jwk.get("x").asText());
    }

    public static String fromX(String x) {
        String jwk = "{\"crv\":\"Ed25519\",\"kty\":\"OKP\",\"x\":\"" + x + "\"}";
        return PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(jwk.getBytes(StandardCharsets.UTF_8));
    }

    public static String stripPadding(String s) {
        int end = s.length();
        while (end > 0 && s.charAt(end - 1) == '=') end--;
        return s.substring(0, end);
    }
}
