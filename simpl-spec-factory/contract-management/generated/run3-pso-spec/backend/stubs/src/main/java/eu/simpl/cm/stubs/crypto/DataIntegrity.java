package eu.simpl.cm.stubs.crypto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** DataIntegrityProof with cryptosuite eddsa-jcs-2022, exactly as Appendix E.2. */
public final class DataIntegrity {

    public static final String PROOF_TYPE = "DataIntegrityProof";
    public static final String CRYPTOSUITE = "eddsa-jcs-2022";

    private DataIntegrity() {
    }

    /** Returns a copy of the document with an embedded proof. */
    public static ObjectNode sign(ObjectNode credential, Ed25519Key key, String created) {
        ObjectNode document = credential.deepCopy();
        document.remove("proof");
        ObjectNode proofConfig = document.objectNode();
        proofConfig.put("type", PROOF_TYPE);
        proofConfig.put("cryptosuite", CRYPTOSUITE);
        proofConfig.put("created", created);
        proofConfig.put("verificationMethod", key.verificationMethod());
        proofConfig.put("proofPurpose", "assertionMethod");
        proofConfig.set("@context", document.get("@context"));
        byte[] signature = key.sign(hashData(proofConfig, document));
        ObjectNode proof = proofConfig.deepCopy();
        proof.remove("@context");
        proof.put("proofValue", "z" + Base58.encode(signature));
        ObjectNode signed = document.deepCopy();
        signed.set("proof", proof);
        return signed;
    }

    /** Checks the signature only. Returns false for any decoding problem. */
    public static boolean verifySignature(JsonNode credential) {
        try {
            JsonNode proof = credential.get("proof");
            if (proof == null || !proof.isObject()) {
                return false;
            }
            ObjectNode document = ((ObjectNode) credential).deepCopy();
            document.remove("proof");
            ObjectNode proofConfig = ((ObjectNode) proof).deepCopy();
            JsonNode proofValue = proofConfig.remove("proofValue");
            if (proofValue == null || !proofValue.isTextual() || !proofValue.textValue().startsWith("z")) {
                return false;
            }
            proofConfig.set("@context", document.get("@context"));
            String vm = proof.path("verificationMethod").asText("");
            int hash = vm.indexOf('#');
            if (hash < 0) {
                return false;
            }
            byte[] publicKey = Ed25519Key.decodeMultibase(vm.substring(hash + 1));
            byte[] signature = Base58.decode(proofValue.textValue().substring(1));
            return Ed25519Key.verify(publicKey, hashData(proofConfig, document), signature);
        } catch (RuntimeException e) {
            return false;
        }
    }

    static byte[] hashData(JsonNode proofConfig, JsonNode document) {
        byte[] a = sha256(Jcs.canonicalize(proofConfig));
        byte[] b = sha256(Jcs.canonicalize(document));
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
}
