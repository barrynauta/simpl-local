package eu.simpl.catalogue.cat.api;

import com.fasterxml.jackson.databind.JsonNode;
import eu.simpl.catalogue.cat.service.ShapeService;
import eu.simpl.catalogue.common.ApiException;
import eu.simpl.catalogue.common.Json;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

/** POST /webhooks/schema-events, no token (D60). */
@RestController
public class WebhookController {
    private static final Set<String> TYPES = Set.of("SchemaPublished", "SchemaRevoked");
    private final ShapeService shapes;

    public WebhookController(ShapeService shapes) {
        this.shapes = shapes;
    }

    @PostMapping(value = "/webhooks/schema-events", consumes = "application/json")
    public ResponseEntity<Void> event(@RequestBody(required = false) String body) {
        JsonNode e = Json.parseOrNull(body);
        String detail = "The request body is invalid or does not conform to the SchemaChangeEvent schema.";
        if (e == null || !e.isObject()) throw ApiException.badRequest(detail);
        if (!e.path("eventId").isTextual() || !e.path("timestamp").isTextual()) throw ApiException.badRequest(detail);
        String type = e.path("eventType").asText(null);
        if (type == null || !TYPES.contains(type)) throw ApiException.badRequest(detail);
        JsonNode schema = e.path("data").path("schema");
        if (!schema.isObject()) throw ApiException.badRequest(detail);
        String name = schema.path("name").asText(null);
        if (name == null || name.isBlank()) throw ApiException.badRequest("data.schema.name is required.");
        JsonNode v = e.path("data").path("version").path("version");
        shapes.event(type, name, v.isValueNode() && !v.isNull() ? v.asText() : null);
        return ResponseEntity.ok().build();
    }
}
