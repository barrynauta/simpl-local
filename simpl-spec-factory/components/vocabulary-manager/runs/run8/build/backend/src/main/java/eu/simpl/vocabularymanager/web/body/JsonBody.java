package eu.simpl.vocabularymanager.web.body;

import eu.simpl.vocabularymanager.web.problem.ProblemIssue;
import eu.simpl.vocabularymanager.web.problem.Problems;
import eu.simpl.vocabularymanager.web.routing.RequestShapeInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import tools.jackson.core.JacksonException;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Reads a JSON request body as a tree, so every property can be checked against the schema by hand: unknown
 * properties, duplicate keys, wrong types (D52, OWT-A05). Never binds to classes (OWT-A08).
 */
public final class JsonBody {

    private static final JsonMapper STRICT =
            JsonMapper.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build();

    private JsonBody() {}

    /**
     * @param request the request
     * @return the body, which is a JSON object
     * @throws eu.simpl.vocabularymanager.web.problem.ApiException 413 or 400
     */
    public static JsonNode readObject(HttpServletRequest request) {
        byte[] body;
        try (InputStream input = request.getInputStream()) {
            body = input.readNBytes((int) RequestShapeInterceptor.MAX_JSON_BYTES + 1);
        } catch (IOException e) {
            throw Problems.schemaViolation("body", "body", "The request body cannot be read.");
        }
        if (body.length > RequestShapeInterceptor.MAX_JSON_BYTES) {
            throw Problems.payloadTooLarge("body");
        }
        JsonNode node;
        try {
            node = STRICT.readTree(body);
        } catch (JacksonException e) {
            throw Problems.schemaViolation("body", "body", "The request body is not valid JSON.");
        }
        if (node == null || !node.isObject()) {
            throw Problems.schemaViolation("body", "body", "The request body must be a JSON object.");
        }
        return node;
    }

    /**
     * @param node a JSON object
     * @param allowed the property names the schema defines
     * @param prefix name prefix for the issue (for example 'items[0].'), empty at the top level
     * @return one issue per unknown property (D52)
     */
    public static List<ProblemIssue> unknownProperties(JsonNode node, Set<String> allowed, String prefix) {
        List<ProblemIssue> issues = new ArrayList<>();
        for (String name : node.propertyNames()) {
            if (!allowed.contains(name)) {
                issues.add(ProblemIssue.schemaViolation(
                        "body", prefix + name, "Property '" + shorten(name) + "' is not defined by the schema."));
            }
        }
        return issues;
    }

    private static String shorten(String value) {
        return value.length() > 64 ? value.substring(0, 64) + "..." : value;
    }
}
