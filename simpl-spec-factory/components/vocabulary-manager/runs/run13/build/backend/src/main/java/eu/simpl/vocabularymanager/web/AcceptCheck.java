package eu.simpl.vocabularymanager.web;

import eu.simpl.vocabularymanager.web.OperationCatalog.Produces;
import java.util.List;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;

/**
 * The Accept rule of D63 and D127. A JSON operation accepts application/json, application/problem+json,
 * application/* and *&#47;*; a content download accepts text/turtle, text/*, application/* and *&#47;*. No
 * Accept header is accepted. Media ranges with q=0 do not count.
 */
public final class AcceptCheck {

    private static final List<MediaType> JSON_ACCEPTED = List.of(
            MediaType.ALL,
            MediaType.parseMediaType("application/*"),
            MediaType.APPLICATION_JSON,
            MediaType.APPLICATION_PROBLEM_JSON);
    private static final List<MediaType> TURTLE_ACCEPTED = List.of(
            MediaType.ALL,
            MediaType.parseMediaType("application/*"),
            MediaType.parseMediaType("text/*"),
            MediaType.parseMediaType("text/turtle"));

    private AcceptCheck() {}

    public static boolean acceptable(List<String> headerValues, Produces produces) {
        if (headerValues.isEmpty()) {
            return true;
        }
        String joined = String.join(",", headerValues);
        if (joined.isBlank()) {
            return true;
        }
        List<MediaType> requested;
        try {
            requested = MediaType.parseMediaTypes(joined);
        } catch (InvalidMediaTypeException e) {
            return false;
        }
        List<MediaType> accepted = produces == Produces.TURTLE ? TURTLE_ACCEPTED : JSON_ACCEPTED;
        for (MediaType media : requested) {
            if (media.getQualityValue() <= 0.0) {
                continue;
            }
            MediaType bare = new MediaType(media.getType(), media.getSubtype());
            if (accepted.contains(bare)) {
                return true;
            }
        }
        return false;
    }
}
