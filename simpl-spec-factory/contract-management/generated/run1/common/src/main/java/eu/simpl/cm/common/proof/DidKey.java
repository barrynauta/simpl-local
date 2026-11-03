package eu.simpl.cm.common.proof;

import java.util.Arrays;

/** did:key for Ed25519 public keys (multicodec 0xed 0x01). */
public final class DidKey {
    private static final byte[] PREFIX = {(byte) 0xed, 0x01};

    private DidKey() {
    }

    public static String multibase(byte[] publicKey) {
        byte[] data = new byte[PREFIX.length + publicKey.length];
        System.arraycopy(PREFIX, 0, data, 0, PREFIX.length);
        System.arraycopy(publicKey, 0, data, PREFIX.length, publicKey.length);
        return "z" + Base58.encode(data);
    }

    public static String did(byte[] publicKey) {
        return "did:key:" + multibase(publicKey);
    }

    public static String verificationMethod(byte[] publicKey) {
        return did(publicKey) + "#" + multibase(publicKey);
    }

    /** DID part of a verification method (before '#'). */
    public static String didOf(String verificationMethod) {
        int i = verificationMethod.indexOf('#');
        return i < 0 ? verificationMethod : verificationMethod.substring(0, i);
    }

    /** Decodes the Ed25519 public key from a did:key DID or verification method. */
    public static byte[] publicKey(String didOrVerificationMethod) {
        String did = didOf(didOrVerificationMethod);
        if (!did.startsWith("did:key:z")) {
            throw new IllegalArgumentException("Not a did:key with base58btc multibase");
        }
        byte[] data = Base58.decode(did.substring("did:key:z".length()));
        if (data.length != 34 || data[0] != PREFIX[0] || data[1] != PREFIX[1]) {
            throw new IllegalArgumentException("Not an Ed25519 did:key");
        }
        byte[] key = Arrays.copyOfRange(data, 2, 34);
        if (didOrVerificationMethod.contains("#")) {
            String fragment = didOrVerificationMethod.substring(didOrVerificationMethod.indexOf('#') + 1);
            if (!fragment.equals(multibase(key))) {
                throw new IllegalArgumentException("Verification method fragment does not match the DID");
            }
        }
        return key;
    }
}
