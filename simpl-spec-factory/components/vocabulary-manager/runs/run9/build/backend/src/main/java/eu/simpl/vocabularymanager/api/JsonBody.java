package eu.simpl.vocabularymanager.api;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import tools.jackson.core.JacksonException;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Reads a JSON request body as a tree, with a size limit and duplicate-key detection. */
public final class JsonBody {

    private static final JsonMapper MAPPER = JsonMapper.builder()
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .build();

    private JsonBody() {}

    /**
     * @param request the request
     * @param maxBytes body limit
     * @return the parsed object
     */
    public static JsonNode readObject(HttpServletRequest request, int maxBytes) {
        byte[] body;
        try (InputStream in = request.getInputStream()) {
            body = in.readNBytes(maxBytes + 1);
        } catch (IOException ex) {
            throw invalid("The request body could not be read.");
        }
        if (body.length > maxBytes) {
            throw new ProblemException(Problems.jsonTooLarge());
        }
        JsonNode node;
        try {
            node = MAPPER.readTree(body);
        } catch (JacksonException ex) {
            throw invalid("The request body is not valid JSON.");
        }
        if (node == null || node.isMissingNode() || !node.isObject()) {
            throw invalid("The request body must be a JSON object.");
        }
        return node;
    }

    private static ProblemException invalid(String detail) {
        return Problems.badRequest(Issue.schemaViolation("body", "body", detail, null));
    }

    /** A JSON value as a positive 32-bit integer, or null (D91). */
    public static Integer positiveInt(JsonNode node) {
        if (node == null || !node.isNumber()) {
            return null;
        }
        if (node.isIntegralNumber()) {
            if (!node.canConvertToLong()) {
                return null;
            }
            long value = node.longValue();
            return value >= 1 && value <= Integer.MAX_VALUE ? (int) value : null;
        }
        double value = node.doubleValue();
        if (value != Math.rint(value) || value < 1 || value > Integer.MAX_VALUE) {
            return null;
        }
        return (int) value;
    }
}
