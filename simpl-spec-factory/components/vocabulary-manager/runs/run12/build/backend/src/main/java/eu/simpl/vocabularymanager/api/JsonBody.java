package eu.simpl.vocabularymanager.api;

import eu.simpl.vocabularymanager.error.ApiException;
import eu.simpl.vocabularymanager.error.Issue;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Reads a JSON request body with Jackson (limit 1 MB, D110, D150), refusing duplicate keys. The
 * operation checks the tree against its schema so every unknown or invalid property gets its own
 * issue (D52, D68).
 */
@Component
public class JsonBody {

    private final JsonMapper strict;

    public JsonBody(JsonMapper mapper) {
        this.strict = mapper.rebuild()
                .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
                .build();
    }

    public JsonNode read(HttpServletRequest request) {
        byte[] bytes;
        try (InputStream in = request.getInputStream()) {
            bytes = in.readNBytes((int) NegotiationFilter.JSON_LIMIT + 1);
            if (bytes.length > NegotiationFilter.JSON_LIMIT) {
                throw ApiException.jsonTooLarge();
            }
        } catch (NegotiationFilter.BodyTooLargeException e) {
            throw ApiException.jsonTooLarge();
        } catch (IOException e) {
            throw ApiException.badRequest(
                    Issue.schemaViolation("body", "body", "The request body could not be read.", null));
        }
        if (bytes.length == 0) {
            throw ApiException.badRequest(
                    Issue.schemaViolation("body", "body", "A JSON request body is required.", null));
        }
        try {
            JsonNode node = strict.readTree(bytes);
            if (node == null || node.isMissingNode()) {
                throw ApiException.badRequest(
                        Issue.schemaViolation("body", "body", "A JSON request body is required.", null));
            }
            return node;
        } catch (JacksonException e) {
            throw ApiException.badRequest(
                    Issue.schemaViolation("body", "body", "The request body is not valid JSON.", null));
        }
    }
}
