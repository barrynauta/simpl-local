package eu.simpl.vocabulary.api.request;

import eu.simpl.vocabulary.api.problem.Issue;
import eu.simpl.vocabulary.api.problem.Problems;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import tools.jackson.core.JacksonException;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Reads a JSON request body into a tree with strict duplicate detection (OWT-A05); never into objects. */
public final class JsonBody {

    private static final JsonMapper STRICT = JsonMapper.builder()
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .build();

    private JsonBody() {}

    /**
     * Reads and parses the body.
     *
     * @param request the request
     * @param maxBytes limit of the body (413 above it)
     * @return the JSON tree
     * @throws eu.simpl.vocabulary.api.problem.ApiException 400 when the body is missing or not JSON, 413 when
     *     too large
     */
    public static JsonNode read(HttpServletRequest request, long maxBytes) {
        byte[] bytes;
        try (InputStream in = request.getInputStream()) {
            bytes = in.readNBytes((int) Math.min(Integer.MAX_VALUE - 8, maxBytes + 1));
        } catch (IOException e) {
            throw Problems.badRequest(
                    Issue.schemaViolation(Issue.IN_BODY, "body", "The request body could not be read."));
        }
        if (bytes.length > maxBytes) {
            throw Problems.payloadTooLarge();
        }
        return parse(bytes);
    }

    /**
     * Parses bytes as one JSON value.
     *
     * @param bytes the body
     * @return the tree
     */
    public static JsonNode parse(byte[] bytes) {
        if (bytes.length == 0) {
            throw Problems.badRequest(Issue.schemaViolation(Issue.IN_BODY, "body", "A request body is required."));
        }
        try {
            JsonNode node = STRICT.readTree(bytes);
            if (node == null || node.isMissingNode()) {
                throw Problems.badRequest(Issue.schemaViolation(Issue.IN_BODY, "body", "A request body is required."));
            }
            return node;
        } catch (JacksonException e) {
            throw Problems.badRequest(
                    Issue.schemaViolation(Issue.IN_BODY, "body", "The request body is not valid JSON."));
        }
    }
}
