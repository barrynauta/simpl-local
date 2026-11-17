package eu.simpl.cm.stubs.crypto;

import java.math.BigInteger;
import java.util.Arrays;

/** Base58btc (Bitcoin alphabet). */
public final class Base58 {

    private static final String ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";
    private static final BigInteger BASE = BigInteger.valueOf(58);

    private Base58() {
    }

    public static String encode(byte[] input) {
        BigInteger n = new BigInteger(1, input);
        StringBuilder sb = new StringBuilder();
        while (n.signum() > 0) {
            BigInteger[] qr = n.divideAndRemainder(BASE);
            sb.append(ALPHABET.charAt(qr[1].intValue()));
            n = qr[0];
        }
        for (byte b : input) {
            if (b != 0) {
                break;
            }
            sb.append('1');
        }
        return sb.reverse().toString();
    }

    public static byte[] decode(String input) {
        BigInteger n = BigInteger.ZERO;
        for (char c : input.toCharArray()) {
            int d = ALPHABET.indexOf(c);
            if (d < 0) {
                throw new IllegalArgumentException("Invalid base58 character");
            }
            n = n.multiply(BASE).add(BigInteger.valueOf(d));
        }
        byte[] raw = n.toByteArray();
        if (raw.length > 1 && raw[0] == 0) {
            raw = Arrays.copyOfRange(raw, 1, raw.length);
        }
        if (n.signum() == 0) {
            raw = new byte[0];
        }
        int zeros = 0;
        while (zeros < input.length() && input.charAt(zeros) == '1') {
            zeros++;
        }
        byte[] out = new byte[zeros + raw.length];
        System.arraycopy(raw, 0, out, zeros, raw.length);
        return out;
    }
}
