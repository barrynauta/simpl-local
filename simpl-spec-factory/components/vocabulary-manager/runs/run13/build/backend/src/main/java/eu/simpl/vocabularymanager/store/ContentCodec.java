package eu.simpl.vocabularymanager.store;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;

/**
 * Keeps the uploaded bytes exactly as uploaded inside one store literal (D29, D106). Valid UTF-8 text
 * without control characters is stored as a string; anything else as xsd:base64Binary.
 */
public final class ContentCodec {

    private ContentCodec() {}

    public static Node encode(byte[] bytes) {
        String text = strictUtf8(bytes);
        if (text != null && plainText(text)) {
            return NodeFactory.createLiteralString(text);
        }
        return NodeFactory.createLiteralDT(Base64.getEncoder().encodeToString(bytes), XSDDatatype.XSDbase64Binary);
    }

    public static byte[] decode(Node literal) {
        String lexical = literal.getLiteralLexicalForm();
        if (XSDDatatype.XSDbase64Binary.getURI().equals(literal.getLiteralDatatypeURI())) {
            return Base64.getDecoder().decode(lexical);
        }
        return lexical.getBytes(StandardCharsets.UTF_8);
    }

    /** Decodes strictly; returns null when the bytes are not valid UTF-8. */
    public static String strictUtf8(byte[] bytes) {
        try {
            return StandardCharsets.UTF_8
                    .newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
        } catch (CharacterCodingException e) {
            return null;
        }
    }

    private static boolean plainText(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if ((c < 0x20 && c != '\n' && c != '\r' && c != '\t') || c == 0x7f || Character.isSurrogate(c)) {
                return false;
            }
        }
        return true;
    }
}
