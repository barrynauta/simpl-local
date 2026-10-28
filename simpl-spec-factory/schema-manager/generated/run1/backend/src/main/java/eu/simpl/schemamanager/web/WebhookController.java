package eu.simpl.schemamanager.web;

import com.fasterxml.jackson.databind.JsonNode;
import eu.simpl.schemamanager.domain.Webhook;
import eu.simpl.schemamanager.persistence.WebhookRepository;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/** Webhook subscriptions (SIMPL-19992, D41). Any authenticated caller (D7). */
@RestController
public class WebhookController {

    public static final List<String> EVENT_TYPES = List.of("SchemaPublished", "SchemaRevoked");

    private final WebhookRepository repo;
    private final Json json;
    private final Clock clock;

    public WebhookController(WebhookRepository repo, Json json, Clock clock) {
        this.repo = repo;
        this.json = json;
        this.clock = clock;
    }

    @GetMapping("/webhooks")
    public ResponseEntity<byte[]> list(@RequestHeader(value = HttpHeaders.ACCEPT, required = false) String accept) {
        Negotiation.requireJson(accept);
        return json.response(HttpStatus.OK, repo.findAll(), null);
    }

    @PostMapping("/webhooks")
    public ResponseEntity<byte[]> create(@RequestHeader(value = HttpHeaders.ACCEPT, required = false) String accept,
                                         @RequestHeader(value = HttpHeaders.CONTENT_TYPE, required = false) String contentType,
                                         @RequestBody(required = false) byte[] body) {
        Negotiation.requireJson(accept);
        Negotiation.requireJsonBody(contentType);
        JsonNode node;
        try {
            node = body == null || body.length == 0 ? null : json.mapper().readTree(body);
        } catch (IOException e) {
            throw ApiException.badRequest("Request body is not valid JSON.");
        }
        if (node == null || !node.isObject()) {
            throw ApiException.badRequest("Request body must be a JSON object with 'targetUrl' and 'events'.");
        }
        JsonNode urlNode = node.get("targetUrl");
        if (urlNode == null || !urlNode.isTextual() || !isHttpUrl(urlNode.asText())) {
            throw ApiException.badRequest("The targetUrl is invalid or malformed. Only absolute http and https URLs are accepted.");
        }
        String targetUrl = urlNode.asText().trim();
        JsonNode eventsNode = node.get("events");
        if (eventsNode == null || eventsNode.isNull() || !eventsNode.isArray() || eventsNode.isEmpty()) {
            throw ApiException.badRequest("At least one event type is required in 'events'.");
        }
        List<String> events = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (JsonNode e : eventsNode) {
            if (!e.isTextual()) {
                throw ApiException.badRequest("Every entry of 'events' must be a string.");
            }
            String value = e.asText();
            if (!EVENT_TYPES.contains(value)) {
                throw new ApiException(HttpStatus.NOT_ACCEPTABLE, "'" + value
                        + "' is not a valid event type. Valid event types are: SchemaPublished, SchemaRevoked");
            }
            if (!seen.add(value)) {
                throw ApiException.badRequest("Event type '" + value + "' is listed more than once.");
            }
            events.add(value);
        }
        if (repo.exists(targetUrl, events)) {
            throw ApiException.conflict("Webhook with the same targetUrl and events already exists.");
        }
        Webhook w = new Webhook(UUID.randomUUID(), targetUrl, events);
        try {
            repo.insert(w, Instant.now(clock));
        } catch (DuplicateKeyException e) {
            throw ApiException.conflict("Webhook with the same targetUrl and events already exists.");
        }
        return json.response(HttpStatus.CREATED, w, null);
    }

    @DeleteMapping("/webhooks/{webhookId}")
    public ResponseEntity<byte[]> delete(@PathVariable String webhookId,
                                         @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String accept) {
        Negotiation.requireJson(accept);
        UUID id;
        try {
            id = UUID.fromString(webhookId);
            if (!id.toString().equalsIgnoreCase(webhookId)) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException e) {
            throw notFound(webhookId);
        }
        if (!repo.delete(id)) {
            throw notFound(webhookId);
        }
        return ResponseEntity.noContent().build();
    }

    private static ApiException notFound(String id) {
        return ApiException.notFound("Webhook with id '" + id + "' was not found");
    }

    static boolean isHttpUrl(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        try {
            URI uri = new URI(value.trim());
            String scheme = uri.getScheme();
            return uri.isAbsolute()
                    && ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                    && uri.getHost() != null && !uri.getHost().isBlank();
        } catch (URISyntaxException e) {
            return false;
        }
    }
}
