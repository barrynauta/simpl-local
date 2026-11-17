package eu.simpl.catalogue.cat.crypto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.simpl.catalogue.common.Json;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;
import org.bouncycastle.crypto.signers.Ed25519Signer;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Verifies the JsonWebSignature2020 proof profile of D22: detached JWS with b64=false, EdDSA over
 * ASCII(base64url(header)) + "." + JCS(VC without proof), key from the did:jwk verification method.
 */
public final class SignatureVerifier {
    public record Result(boolean valid, String reason, String publicX) {
        static Result fail(String reason) { return new Result(false, reason, null); }
    }

    private static final JsonNode EXPECTED_HEADER = Json.parse("{\"alg\":\"EdDSA\",\"b64\":false,\"crit\":[\"b64\"]}");

    private SignatureVerifier() {}

    public static Result verify(JsonNode vc) {
        JsonNode proof = vc.path("proof");
        if (!proof.isObject()) return Result.fail("proof is missing");
        if (!"JsonWebSignature2020".equals(proof.path("type").asText())) return Result.fail("proof type must be JsonWebSignature2020");
        String issuer = issuerOf(vc);
        String vm = proof.path("verificationMethod").asText(null);
        if (vm == null) return Result.fail("verificationMethod is missing");
        int hash = vm.indexOf('#');
        String vmDid = hash >= 0 ? vm.substring(0, hash) : vm;
        if (issuer == null || !vmDid.equals(issuer)) return Result.fail("verificationMethod does not belong to the issuer");
        String x;
        try {
            x = DidJwk.publicX(vmDid);
        } catch (IllegalArgumentException e) {
            return Result.fail(e.getMessage());
        }
        String jws = proof.path("jws").asText(null);
        if (jws == null) return Result.fail("jws is missing");
        String[] parts = jws.split("\\.", -1);
        if (parts.length != 3 || !parts[1].isEmpty()) return Result.fail("jws must be a detached JWS");
        byte[] headerBytes;
        byte[] sig;
        byte[] key;
        try {
            headerBytes = Base64.getUrlDecoder().decode(DidJwk.stripPadding(parts[0]));
            sig = Base64.getUrlDecoder().decode(DidJwk.stripPadding(parts[2]));
            key = Base64.getUrlDecoder().decode(x);
        } catch (IllegalArgumentException e) {
            return Result.fail("jws or key is not base64url");
        }
        JsonNode header = Json.parseOrNull(new String(headerBytes, StandardCharsets.UTF_8));
        if (header == null || !header.equals(EXPECTED_HEADER)) return Result.fail("unexpected JWS header");
        if (key.length != 32 || sig.length != 64) return Result.fail("bad key or signature length");
        ObjectNode unsigned = ((ObjectNode) vc).deepCopy();
        unsigned.remove("proof");
        ByteArrayOutputStream input = new ByteArrayOutputStream();
        input.writeBytes(parts[0].getBytes(StandardCharsets.US_ASCII));
        input.write('.');
        input.writeBytes(Jcs.canonical(unsigned));
        Ed25519Signer signer = new Ed25519Signer();
        signer.init(false, new Ed25519PublicKeyParameters(key, 0));
        byte[] data = input.toByteArray();
        signer.update(data, 0, data.length);
        if (!signer.verifySignature(sig)) return Result.fail("signature does not verify");
        return new Result(true, null, x);
    }

    public static String issuerOf(JsonNode vc) {
        JsonNode i = vc.path("issuer");
        if (i.isTextual()) return i.asText();
        if (i.isObject() && i.path("id").isTextual()) return i.get("id").asText();
        return null;
    }
}
