package eu.simpl.schemamanager.web;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;

/** Accept and Content-Type checks of D8. */
public final class Negotiation {

    public static final MediaType TEXT_TURTLE = MediaType.parseMediaType("text/turtle");

    public enum ContentChoice { CONTENT, METADATA }

    private Negotiation() {
    }

    private static List<MediaType> parse(String accept) {
        try {
            List<MediaType> types = new ArrayList<>(MediaType.parseMediaTypes(accept));
            types.removeIf(t -> t.getQualityValue() <= 0.0);
            types.sort(Comparator.comparingDouble(MediaType::getQualityValue).reversed());
            return types;
        } catch (IllegalArgumentException e) {
            return List.of();
        }
    }

    /** JSON operations: 406 unless Accept is absent or admits JSON, problem JSON or anything. */
    public static void requireJson(String accept) {
        if (accept == null || accept.isBlank()) {
            return;
        }
        for (MediaType t : parse(accept)) {
            if (t.includes(MediaType.APPLICATION_JSON) || t.includes(MediaType.APPLICATION_PROBLEM_JSON)) {
                return;
            }
        }
        throw ApiException.notAcceptable();
    }

    /**
     * Content operations: text/turtle (or a wildcard) gives content; application/json gives
     * metadata only where {@code allowMetadata} is set (D10). Anything else gives 406.
     */
    public static ContentChoice content(String accept, boolean allowMetadata) {
        if (accept == null || accept.isBlank()) {
            return ContentChoice.CONTENT;
        }
        for (MediaType t : parse(accept)) {
            if (t.includes(TEXT_TURTLE)) {
                return ContentChoice.CONTENT;
            }
            if (allowMetadata && t.includes(MediaType.APPLICATION_JSON)) {
                return ContentChoice.METADATA;
            }
        }
        throw ApiException.notAcceptable();
    }

    public static void requireMultipart(String contentType) {
        if (!isType(contentType, MediaType.MULTIPART_FORM_DATA)) {
            throw ApiException.unsupportedMediaType();
        }
    }

    public static void requireJsonBody(String contentType) {
        if (!isType(contentType, MediaType.APPLICATION_JSON)) {
            throw ApiException.unsupportedMediaType();
        }
    }

    private static boolean isType(String contentType, MediaType expected) {
        if (contentType == null || contentType.isBlank()) {
            return false;
        }
        try {
            MediaType mt = MediaType.parseMediaType(contentType);
            return mt.getType().equalsIgnoreCase(expected.getType())
                    && mt.getSubtype().equalsIgnoreCase(expected.getSubtype());
        } catch (InvalidMediaTypeException e) {
            return false;
        }
    }
}
