package eu.simpl.cm.stubs.crypto;

import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;
import org.bouncycastle.crypto.signers.Ed25519Signer;

import java.util.HexFormat;

/** An Ed25519 key pair with its did:key identifiers (Appendix E.1). */
public final class Ed25519Key {

    private final Ed25519PrivateKeyParameters privateKey;
    private final byte[] publicKey;
    private final String multibase;

    private Ed25519Key(byte[] seed) {
        this.privateKey = new Ed25519PrivateKeyParameters(seed, 0);
        this.publicKey = privateKey.generatePublicKey().getEncoded();
        this.multibase = publicKeyMultibase(publicKey);
    }

    public static Ed25519Key fromSeed(byte[] seed) {
        if (seed.length != 32) {
            throw new IllegalArgumentException("Seed must be 32 bytes");
        }
        return new Ed25519Key(seed);
    }

    public static Ed25519Key fromRepeatedByte(int b) {
        byte[] seed = new byte[32];
        java.util.Arrays.fill(seed, (byte) b);
        return fromSeed(seed);
    }

    public static String publicKeyMultibase(byte[] publicKey) {
        byte[] prefixed = new byte[publicKey.length + 2];
        prefixed[0] = (byte) 0xed;
        prefixed[1] = (byte) 0x01;
        System.arraycopy(publicKey, 0, prefixed, 2, publicKey.length);
        return "z" + Base58.encode(prefixed);
    }

    /** Decodes a did:key multibase value into the 32-byte public key, or throws. */
    public static byte[] decodeMultibase(String multibase) {
        if (multibase == null || !multibase.startsWith("z")) {
            throw new IllegalArgumentException("Not a base58btc multibase value");
        }
        byte[] raw = Base58.decode(multibase.substring(1));
        if (raw.length != 34 || raw[0] != (byte) 0xed || raw[1] != (byte) 0x01) {
            throw new IllegalArgumentException("Not an Ed25519 multikey");
        }
        byte[] pk = new byte[32];
        System.arraycopy(raw, 2, pk, 0, 32);
        return pk;
    }

    public byte[] sign(byte[] data) {
        Ed25519Signer signer = new Ed25519Signer();
        signer.init(true, privateKey);
        signer.update(data, 0, data.length);
        return signer.generateSignature();
    }

    public static boolean verify(byte[] publicKey, byte[] data, byte[] signature) {
        try {
            Ed25519Signer verifier = new Ed25519Signer();
            verifier.init(false, new Ed25519PublicKeyParameters(publicKey, 0));
            verifier.update(data, 0, data.length);
            return verifier.verifySignature(signature);
        } catch (RuntimeException e) {
            return false;
        }
    }

    public String publicKeyHex() {
        return HexFormat.of().formatHex(publicKey);
    }

    public String multibase() {
        return multibase;
    }

    public String did() {
        return "did:key:" + multibase;
    }

    public String verificationMethod() {
        return did() + "#" + multibase;
    }
}
