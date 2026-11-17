package eu.simpl.catalogue.cat.crypto;

import com.fasterxml.jackson.databind.JsonNode;
import eu.simpl.catalogue.common.Json;
import org.erdtman.jcs.JsonCanonicalizer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** RFC 8785 JSON Canonicalization Scheme, and the sdHash of D11. */
public final class Jcs {
    private Jcs() {}

    public static byte[] canonical(JsonNode node) {
        try {
            return new JsonCanonicalizer(Json.write(node)).getEncodedUTF8();
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot canonicalise JSON: " + e.getMessage(), e);
        }
    }

    public static String canonicalString(JsonNode node) {
        return new String(canonical(node), StandardCharsets.UTF_8);
    }

    /** Lowercase hex SHA-256 of the JCS form of the full VC including proof. */
    public static String sdHash(JsonNode vc) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(canonical(vc));
            return HexFormat.of().formatHex(d);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
