package eu.simpl.vocabularymanager.web;

import eu.simpl.vocabularymanager.error.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Reads a JSON request body with Jackson (REUSE-02): at most 1 MB (D110), no duplicate keys, no
 * trailing content, a JSON object at the top (BLG-evo-object).
 */
@Component
public class JsonBody {

    private final JsonMapper strict;
    private final long maxBytes;

    public JsonBody(eu.simpl.vocabularymanager.config.AppProperties properties) {
        this.strict = JsonMapper.builder()
                .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
                .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                .build();
        this.maxBytes = properties.limits().maxJsonBytes();
    }

    /**
     * Reads the body as a JSON object.
     *
     * @throws ApiException 413 above 1 MB, 400 when the body is not one JSON object
     */
    public JsonNode readObject(HttpServletRequest request) {
        byte[] raw;
        try (InputStream in = request.getInputStream()) {
            raw = in.readNBytes((int) maxBytes + 1);
        } catch (IOException ex) {
            throw ApiException.badRequest("body", "body", "The request body could not be read.");
        }
        if (raw.length > maxBytes) {
            throw ApiException.tooLarge("body", "The request body is larger than 1 MB.");
        }
        JsonNode node;
        try {
            node = strict.readTree(raw);
        } catch (JacksonException ex) {
            throw ApiException.badRequest("body", "body", "The request body is not valid JSON.");
        }
        if (node == null || !node.isObject()) {
            throw ApiException.badRequest("body", "body", "The request body must be a JSON object.");
        }
        return node;
    }

    /** One issue name per property the schema does not define (D52). */
    public static List<String> unknownProperties(JsonNode object, Set<String> allowed) {
        List<String> unknown = new ArrayList<>();
        for (String name : object.propertyNames()) {
            if (!allowed.contains(name)) {
                unknown.add(name);
            }
        }
        return unknown;
    }
}
