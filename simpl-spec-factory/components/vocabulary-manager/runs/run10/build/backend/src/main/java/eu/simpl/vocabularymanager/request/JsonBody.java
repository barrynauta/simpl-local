package eu.simpl.vocabularymanager.request;

import eu.simpl.vocabularymanager.error.Issue;
import eu.simpl.vocabularymanager.error.Problems;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import tools.jackson.core.JacksonException;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Reads a JSON request body with Jackson into plain Java values (maps, lists, strings, numbers). The body is capped
 * at 1 MB (D110); it must be one JSON object without duplicate keys.
 */
public final class JsonBody {

    public static final int LIMIT = 1024 * 1024;
    private static final JsonMapper MAPPER = JsonMapper.builder()
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .build();

    private JsonBody() {}

    /**
     * Reads the body.
     *
     * @throws eu.simpl.vocabularymanager.error.ProblemException 413 above 1 MB, 400 when it is not a JSON object
     */
    public static Map<String, Object> read(HttpServletRequest request) {
        byte[] bytes;
        try (InputStream in = request.getInputStream()) {
            bytes = in.readNBytes(LIMIT + 1);
        } catch (IOException e) {
            throw invalid("The request body could not be read.");
        }
        if (bytes.length > LIMIT) {
            throw Problems.payloadTooLarge("The request body is larger than 1 MB.", "body");
        }
        return parse(bytes);
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> parse(byte[] bytes) {
        if (bytes.length == 0) {
            throw invalid("A JSON object is required.");
        }
        Object value;
        try {
            value = MAPPER.readValue(bytes, Object.class);
        } catch (JacksonException e) {
            throw invalid("The request body is not valid JSON.");
        }
        if (!(value instanceof Map<?, ?> map)) {
            throw invalid("The request body must be a JSON object.");
        }
        return (Map<String, Object>) map;
    }

    private static RuntimeException invalid(String detail) {
        return Problems.badRequest(Issue.schemaViolation("body", "body", detail, null));
    }

    /** True for a JSON number without fraction or exponent (D112). */
    public static boolean isInteger(Object value) {
        return value instanceof Integer || value instanceof Long || value instanceof java.math.BigInteger;
    }
}
