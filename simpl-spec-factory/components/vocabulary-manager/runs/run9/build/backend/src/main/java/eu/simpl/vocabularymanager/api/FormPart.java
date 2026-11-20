package eu.simpl.vocabularymanager.api;

import java.nio.charset.StandardCharsets;

/**
 * One multipart part.
 *
 * @param name part name
 * @param bytes content
 */
public record FormPart(String name, byte[] bytes) {

    /** The content as UTF-8 text. */
    public String text() {
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
