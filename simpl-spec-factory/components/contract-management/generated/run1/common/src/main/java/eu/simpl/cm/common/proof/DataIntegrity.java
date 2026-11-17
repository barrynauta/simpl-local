package eu.simpl.cm.common.proof;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;
import org.bouncycastle.crypto.signers.Ed25519Signer;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * W3C Data Integrity proof with cryptosuite eddsa-jcs-2022, exactly as Appendix E.2 of the digest describes.
 */
public final class DataIntegrity {
    public static final String PROOF_TYPE = "DataIntegrityProof";
    public static final String CRYPTOSUITE = "eddsa-jcs-2022";

    public enum Outcome { VALID, MALFORMED, INVALID_SIGNATURE }

    private DataIntegrity() {
    }

    public static byte[] publicKeyFromSeed(byte[] seed) {
        return new Ed25519PrivateKeyParameters(seed, 0).generatePublicKey().getEncoded();
    }

    /** Returns a copy of {@code document} (without proof) with an embedded proof. */
    public static ObjectNode sign(ObjectNode document, byte[] seed, String created) {
        ObjectNode doc = document.deepCopy();
        doc.remove("proof");
        byte[] publicKey = publicKeyFromSeed(seed);
        ObjectNode proofConfig = JsonNodeFactory.instance.objectNode();
        proofConfig.put("type", PROOF_TYPE);
        proofConfig.put("cryptosuite", CRYPTOSUITE);
        proofConfig.put("created", created);
        proofConfig.put("verificationMethod", DidKey.verificationMethod(publicKey));
        proofConfig.put("proofPurpose", "assertionMethod");
        proofConfig.set("@context", doc.get("@context"));
        byte[] hashData = hashData(proofConfig, doc);
        Ed25519Signer signer = new Ed25519Signer();
        signer.init(true, new Ed25519PrivateKeyParameters(seed, 0));
        signer.update(hashData, 0, hashData.length);
        byte[] signature = signer.generateSignature();
        ObjectNode proof = proofConfig.deepCopy();
        proof.remove("@context");
        proof.put("proofValue", "z" + Base58.encode(signature));
        doc.set("proof", proof);
        return doc;
    }

    public static Outcome verify(JsonNode credential) {
        if (credential == null || !credential.isObject()) {
            return Outcome.MALFORMED;
        }
        JsonNode proofNode = credential.get("proof");
        if (proofNode == null || !proofNode.isObject()) {
            return Outcome.MALFORMED;
        }
        ObjectNode proof = (ObjectNode) proofNode;
        if (!PROOF_TYPE.equals(text(proof, "type")) || !CRYPTOSUITE.equals(text(proof, "cryptosuite"))) {
            return Outcome.MALFORMED;
        }
        String vm = text(proof, "verificationMethod");
        String proofValue = text(proof, "proofValue");
        if (vm == null || proofValue == null || !proofValue.startsWith("z")) {
            return Outcome.MALFORMED;
        }
        byte[] publicKey;
        byte[] signature;
        try {
            publicKey = DidKey.publicKey(vm);
            signature = Base58.decode(proofValue.substring(1));
        } catch (IllegalArgumentException e) {
            return Outcome.MALFORMED;
        }
        ObjectNode doc = ((ObjectNode) credential).deepCopy();
        doc.remove("proof");
        ObjectNode proofConfig = proof.deepCopy();
        proofConfig.remove("proofValue");
        proofConfig.set("@context", doc.get("@context"));
        byte[] hashData = hashData(proofConfig, doc);
        if (signature.length != 64) {
            return Outcome.INVALID_SIGNATURE;
        }
        Ed25519Signer verifier = new Ed25519Signer();
        verifier.init(false, new Ed25519PublicKeyParameters(publicKey, 0));
        verifier.update(hashData, 0, hashData.length);
        return verifier.verifySignature(signature) ? Outcome.VALID : Outcome.INVALID_SIGNATURE;
    }

    static byte[] hashData(JsonNode proofConfig, JsonNode document) {
        byte[] a = sha256(Jcs.canonicalBytes(proofConfig));
        byte[] b = sha256(Jcs.canonicalBytes(document));
        byte[] out = new byte[64];
        System.arraycopy(a, 0, out, 0, 32);
        System.arraycopy(b, 0, out, 32, 32);
        return out;
    }

    public static byte[] sha256(byte[] data) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(data);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public static String sha256Hex(byte[] data) {
        return java.util.HexFormat.of().formatHex(sha256(data));
    }

    private static String text(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return v != null && v.isTextual() ? v.textValue() : null;
    }
}
