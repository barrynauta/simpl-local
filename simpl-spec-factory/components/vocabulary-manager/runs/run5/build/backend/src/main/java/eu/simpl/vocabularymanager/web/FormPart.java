package eu.simpl.vocabularymanager.web;

import java.nio.charset.StandardCharsets;

/**
 * One part of a multipart/form-data request.
 *
 * @param name form field name
 * @param fileName submitted file name, null for plain fields
 * @param bytes part content
 */
public record FormPart(String name, String fileName, byte[] bytes) {

    /** The part as UTF-8 text. */
    public String text() {
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
