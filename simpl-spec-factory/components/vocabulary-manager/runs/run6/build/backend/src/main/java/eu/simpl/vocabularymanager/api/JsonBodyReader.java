package eu.simpl.vocabularymanager.api;

import eu.simpl.vocabularymanager.config.AppProperties;
import eu.simpl.vocabularymanager.problem.Issue;
import eu.simpl.vocabularymanager.problem.Problems;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Reads a JSON request body with a size limit (413, NFR-04) and strict parsing (duplicate keys rejected), and
 * offers the shape checks of the JSON operations (400, decision D52).
 */
@Component
public class JsonBodyReader {

    private final JsonMapper mapper = JsonMapper.builder()
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .build();
    private final int maxBytes;

    /**
     * Creates the reader.
     *
     * @param properties the configuration (body limit)
     */
    public JsonBodyReader(AppProperties properties) {
        this.maxBytes = properties.limits().maxJsonBodyBytes();
    }

    /**
     * Reads the body.
     *
     * @param request the request, already checked to be application/json
     * @return the root node
     * @throws eu.simpl.vocabularymanager.problem.ProblemException 413 or 400
     */
    public JsonNode read(HttpServletRequest request) {
        byte[] body;
        try (InputStream in = request.getInputStream()) {
            body = in.readNBytes(maxBytes + 1);
        } catch (IOException e) {
            throw Problems.badRequest(Issue.schemaViolation(Issue.IN_BODY, "body", "The body cannot be read."));
        }
        if (body.length > maxBytes) {
            throw Problems.bodyTooLarge();
        }
        if (body.length == 0) {
            throw Problems.badRequest(Issue.schemaViolation(Issue.IN_BODY, "body", "A JSON body is required."));
        }
        try {
            return mapper.readTree(body);
        } catch (JacksonException e) {
            throw Problems.badRequest(Issue.schemaViolation(Issue.IN_BODY, "body", "The body is not valid JSON."));
        }
    }

    /**
     * Requires the node to be an object and adds issues for properties it does not define.
     *
     * @param node the node
     * @param path the property path prefix for issue names, empty for the root
     * @param allowed the defined properties
     * @param issues the issue list
     * @return true when the node is an object
     */
    public static boolean checkObject(JsonNode node, String path, Set<String> allowed, List<Issue> issues) {
        if (node == null || !node.isObject()) {
            issues.add(
                    Issue.schemaViolation(Issue.IN_BODY, path.isEmpty() ? "body" : path, "A JSON object is expected."));
            return false;
        }
        List<String> unknown = new ArrayList<>();
        for (String name : node.propertyNames()) {
            if (!allowed.contains(name)) {
                unknown.add(name);
            }
        }
        unknown.forEach(
                name -> issues.add(Issue.unknownInput(Issue.IN_BODY, path.isEmpty() ? name : path + "." + name)));
        return true;
    }
}
