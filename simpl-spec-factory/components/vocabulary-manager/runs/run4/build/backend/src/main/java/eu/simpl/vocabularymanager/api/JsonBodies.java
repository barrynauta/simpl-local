package eu.simpl.vocabularymanager.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.simpl.vocabularymanager.error.Issue;
import eu.simpl.vocabularymanager.error.Problems;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

/** Parses JSON request bodies and rejects properties the operation does not accept (OWA-API3). */
@Component
public class JsonBodies {

    private final ObjectMapper objectMapper;

    public JsonBodies(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** Parses any JSON text; null when it is not valid JSON. */
    public JsonNode parse(String text) {
        try {
            return objectMapper.readTree(text);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    /**
     * Parses the body as a JSON object with only the allowed properties.
     *
     * @throws eu.simpl.vocabularymanager.error.VocabularyManagerException 400
     */
    public JsonNode object(String body, Set<String> allowed) {
        JsonNode node;
        try {
            node = body == null || body.isBlank() ? null : objectMapper.readTree(body);
        } catch (JsonProcessingException e) {
            throw Problems.badRequest(Issue.schemaViolation("body", null, null, "The request body is not valid JSON."));
        }
        if (node == null || !node.isObject()) {
            throw Problems.badRequest(
                    Issue.schemaViolation("body", null, null, "The request body must be a JSON object."));
        }
        List<Issue> issues = new ArrayList<>();
        Iterator<String> names = node.fieldNames();
        while (names.hasNext()) {
            String name = names.next();
            if (!allowed.contains(name)) {
                issues.add(Issue.schemaViolation("body", name, null, "Property '" + name + "' is not supported."));
            }
        }
        if (!issues.isEmpty()) {
            throw Problems.badRequest(issues);
        }
        return node;
    }
}
