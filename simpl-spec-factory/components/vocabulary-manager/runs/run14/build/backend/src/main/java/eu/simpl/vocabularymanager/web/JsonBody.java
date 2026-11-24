package eu.simpl.vocabularymanager.web;

import eu.simpl.vocabularymanager.error.ApiException;
import eu.simpl.vocabularymanager.error.Issue;
import eu.simpl.vocabularymanager.error.ProblemType;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import tools.jackson.core.JacksonException;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Reads a JSON request body strictly (D52, D110, D112, D150): at most 1 MB, a JSON object, no duplicate keys, no
 * unknown properties.
 */
public final class JsonBody {

    private static final JsonMapper STRICT = JsonMapper.builder()
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .build();

    private JsonBody() {}

    /**
     * Reads the body as a JSON object.
     *
     * @param request the request
     * @return the object node
     * @throws ApiException 413 above 1 MB, 400 when the body is not one JSON object
     */
    public static JsonNode readObject(HttpServletRequest request) {
        byte[] body;
        try (InputStream input = request.getInputStream()) {
            body = input.readNBytes((int) GatekeeperFilter.JSON_LIMIT + 1);
        } catch (IOException e) {
            throw new ApiException(ProblemType.BAD_REQUEST, "The request body could not be read.", List.of());
        }
        if (body.length > GatekeeperFilter.JSON_LIMIT) {
            throw new ApiException(ProblemType.PAYLOAD_TOO_LARGE, GatekeeperFilter.JSON_TOO_LARGE, List.of());
        }
        JsonNode node;
        try {
            node = body.length == 0 ? null : STRICT.readTree(body);
        } catch (JacksonException e) {
            throw ApiException.badRequest("body", "body", "The request body is not valid JSON.");
        }
        if (node == null || !node.isObject()) {
            throw ApiException.badRequest("body", "body", "The request body must be a JSON object.");
        }
        return node;
    }

    /**
     * Rejects properties the schema does not define, one issue per name (D52).
     *
     * @param node the object
     * @param prefix issue name prefix, empty at the root, for example "items[0]."
     * @param allowed the defined properties
     */
    public static void requireOnly(JsonNode node, String prefix, Set<String> allowed) {
        List<Issue> issues = new ArrayList<>();
        Iterator<String> names = node.propertyNames().iterator();
        while (names.hasNext()) {
            String name = names.next();
            if (!allowed.contains(name)) {
                issues.add(Issue.schemaViolation(
                        "body", prefix + name, "Property '" + prefix + name + "' is not supported."));
            }
        }
        if (!issues.isEmpty()) {
            throw ApiException.badRequest(issues);
        }
    }

    /** True for a JSON integer without fraction or exponent in the int32 range from min (D91, D112). */
    public static boolean isInteger(JsonNode node, long min) {
        return node != null && node.isIntegralNumber() && node.canConvertToInt() && node.asLong() >= min;
    }
}
