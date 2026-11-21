package eu.simpl.vocabularymanager.api;

import eu.simpl.vocabularymanager.error.ProblemIssue;
import eu.simpl.vocabularymanager.error.Problems;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;
import tools.jackson.core.JacksonException;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Reads a JSON request body as a tree with Jackson, at most 1 MB (D110, D150), with duplicate keys
 * refused. The handlers then check the tree against the decided schema.
 */
public final class JsonBody {

    public static final int LIMIT = 1024 * 1024;
    private static final JsonMapper MAPPER = JsonMapper.builder()
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .build();

    private JsonBody() {}

    /** Reads the body; 413 above 1 MB, 400 when it is not one JSON object. */
    public static JsonNode readObject(HttpServletRequest request) {
        long declared = request.getContentLengthLong();
        if (declared > LIMIT) {
            throw Problems.jsonTooLarge();
        }
        byte[] bytes;
        try (InputStream in = request.getInputStream()) {
            bytes = in.readNBytes(LIMIT + 1);
        } catch (IOException e) {
            throw Problems.badRequest(invalid("The request body could not be read."));
        }
        if (bytes.length > LIMIT) {
            throw Problems.jsonTooLarge();
        }
        JsonNode node;
        try {
            node = MAPPER.readTree(bytes);
        } catch (JacksonException e) {
            throw Problems.badRequest(invalid("The request body is not valid JSON."));
        }
        if (node == null || !node.isObject()) {
            throw Problems.badRequest(invalid("The request body must be a JSON object."));
        }
        return node;
    }

    private static ProblemIssue invalid(String detail) {
        return ProblemIssue.schemaViolation("body", "body", detail, null);
    }

    /** One issue per property the schema does not define (D52). */
    public static void unknownProperties(
            JsonNode object, Set<String> allowed, String prefix, List<ProblemIssue> issues) {
        for (String name : object.propertyNames()) {
            if (!allowed.contains(name)) {
                issues.add(ProblemIssue.schemaViolation(
                        "body", prefix + name, "Property '" + prefix + name + "' is not supported.", null));
            }
        }
    }

    /** A JSON integer without fraction or exponent (D112) in the int32 range, at least 1. */
    public static Integer positiveInt(JsonNode node, String field, List<ProblemIssue> issues) {
        if (node != null && node.isIntegralNumber() && node.canConvertToInt() && node.intValue() >= 1) {
            return node.intValue();
        }
        issues.add(ProblemIssue.schemaViolation(
                "body", field, "'" + field + "' must be a whole number from 1 to 2147483647.", null));
        return null;
    }
}
