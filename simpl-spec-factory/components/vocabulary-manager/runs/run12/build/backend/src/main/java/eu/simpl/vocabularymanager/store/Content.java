package eu.simpl.vocabularymanager.store;

import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

/** Splits uploaded bytes into base64 chunks for the store and joins them again (D106). */
public final class Content {

    private Content() {}

    public static List<String> toChunks(byte[] bytes) {
        List<String> chunks = new ArrayList<>();
        if (bytes.length == 0) {
            chunks.add("");
            return chunks;
        }
        Base64.Encoder enc = Base64.getEncoder();
        for (int off = 0; off < bytes.length; off += Rdf.CHUNK_BYTES) {
            int len = Math.min(Rdf.CHUNK_BYTES, bytes.length - off);
            byte[] part = new byte[len];
            System.arraycopy(bytes, off, part, 0, len);
            chunks.add(enc.encodeToString(part));
        }
        return chunks;
    }

    public static byte[] fromChunks(List<String> chunks) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Base64.Decoder dec = Base64.getDecoder();
        for (String c : chunks) {
            out.writeBytes(dec.decode(c));
        }
        return out.toByteArray();
    }

    public static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
