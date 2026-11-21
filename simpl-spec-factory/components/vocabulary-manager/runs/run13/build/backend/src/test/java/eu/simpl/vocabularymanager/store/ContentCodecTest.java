package eu.simpl.vocabularymanager.store;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class ContentCodecTest {

    @Test
    void textRoundTripsAsString() {
        byte[] bytes = "line1\r\nline \"2\"\té\n".getBytes(StandardCharsets.UTF_8);
        var node = ContentCodec.encode(bytes);
        assertThat(node.getLiteralDatatypeURI()).endsWith("#string");
        assertThat(ContentCodec.decode(node)).isEqualTo(bytes);
    }

    @Test
    void otherBytesRoundTripAsBase64() {
        byte[] bytes = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF, 'a', 0, (byte) 0xFF};
        var node = ContentCodec.encode(bytes);
        assertThat(node.getLiteralDatatypeURI()).endsWith("#base64Binary");
        assertThat(ContentCodec.decode(node)).isEqualTo(bytes);
        assertThat(ContentCodec.strictUtf8(new byte[] {(byte) 0xC3})).isNull();
    }
}
