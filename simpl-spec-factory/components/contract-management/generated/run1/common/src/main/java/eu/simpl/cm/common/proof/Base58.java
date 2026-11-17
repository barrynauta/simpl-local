package eu.simpl.cm.common.proof;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;

/** Base58 with the Bitcoin alphabet (base58btc). */
public final class Base58 {
    private static final String ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";
    private static final BigInteger FIFTY_EIGHT = BigInteger.valueOf(58);

    private Base58() {
    }

    public static String encode(byte[] input) {
        BigInteger value = new BigInteger(1, input);
        StringBuilder sb = new StringBuilder();
        while (value.signum() > 0) {
            BigInteger[] qr = value.divideAndRemainder(FIFTY_EIGHT);
            sb.append(ALPHABET.charAt(qr[1].intValue()));
            value = qr[0];
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
        BigInteger value = BigInteger.ZERO;
        int leadingZeros = 0;
        boolean counting = true;
        for (char c : input.toCharArray()) {
            int digit = ALPHABET.indexOf(c);
            if (digit < 0) {
                throw new IllegalArgumentException("Invalid base58 character");
            }
            if (counting && c == '1') {
                leadingZeros++;
            } else {
                counting = false;
            }
            value = value.multiply(FIFTY_EIGHT).add(BigInteger.valueOf(digit));
        }
        byte[] raw = value.signum() == 0 ? new byte[0] : value.toByteArray();
        int start = raw.length > 1 && raw[0] == 0 ? 1 : (raw.length == 1 && raw[0] == 0 ? 1 : 0);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (int i = 0; i < leadingZeros; i++) {
            out.write(0);
        }
        out.write(raw, start, raw.length - start);
        return out.toByteArray();
    }
}
