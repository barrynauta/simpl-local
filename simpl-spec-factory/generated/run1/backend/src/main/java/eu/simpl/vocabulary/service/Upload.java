package eu.simpl.vocabulary.service;

import java.util.Locale;
import java.util.Set;

/** An uploaded file part. */
public record Upload(String filename, String contentType, byte[] bytes) {

    private static final Set<String> TURTLE_TYPES = Set.of("text/turtle", "application/x-turtle");
    private static final Set<String> GENERIC_TYPES = Set.of("", "application/octet-stream", "text/plain");

    /**
     * D15: Turtle only. A part counts as Turtle when its content type is text/turtle (or application/x-turtle),
     * when the file name ends with .ttl, or when it has a generic content type and no file extension at all.
     * Everything else is answered with 415.
     */
    public boolean looksLikeTurtle() {
        String ct = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
        int semicolon = ct.indexOf(';');
        if (semicolon >= 0) {
            ct = ct.substring(0, semicolon);
        }
        ct = ct.trim();
        String ext = extension();
        if (TURTLE_TYPES.contains(ct) || ext.equals("ttl")) {
            return true;
        }
        return GENERIC_TYPES.contains(ct) && ext.isEmpty();
    }

    private String extension() {
        if (filename == null) {
            return "";
        }
        String base = filename.substring(Math.max(filename.lastIndexOf('/'), filename.lastIndexOf('\\')) + 1);
        int dot = base.lastIndexOf('.');
        return dot <= 0 ? "" : base.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
