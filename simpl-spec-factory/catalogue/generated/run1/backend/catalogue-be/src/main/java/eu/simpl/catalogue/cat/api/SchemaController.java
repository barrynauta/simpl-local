package eu.simpl.catalogue.cat.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.simpl.catalogue.cat.quality.QualityClient;
import eu.simpl.catalogue.cat.service.ShapeService;
import eu.simpl.catalogue.cat.store.RdRepository;
import eu.simpl.catalogue.common.ApiException;
import eu.simpl.catalogue.common.Caller;
import eu.simpl.catalogue.common.Json;
import eu.simpl.catalogue.common.ProblemTypes;
import eu.simpl.catalogue.common.Roles;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.regex.Pattern;

/** cat /schemas: the SHACL shape store (D5, D56, D58). */
@RestController
public class SchemaController {
    private static final Pattern SCHEMA_NAME = Pattern.compile("^[A-Z][A-Za-z0-9]{2,63}$");

    private final ShapeService shapes;
    private final RdRepository rds;
    private final QualityClient quality;

    public SchemaController(ShapeService shapes, RdRepository rds, QualityClient quality) {
        this.shapes = shapes;
        this.rds = rds;
        this.quality = quality;
    }

    @GetMapping(value = "/schemas", produces = "application/json")
    public ResponseEntity<String> list() {
        Caller.current().requireAny(Roles.SCHEMA_READ);
        ObjectNode out = Json.object();
        out.putArray("ontologies");
        var arr = out.putArray("shapes");
        shapes.ids().forEach(arr::add);
        out.putArray("vocabularies");
        return SelfDescriptionController.json(200, out);
    }

    @PostMapping(value = "/schemas", consumes = {"text/turtle", "application/ld+json", "application/json", "application/rdf+xml"})
    public ResponseEntity<String> add(@RequestBody(required = false) String body,
                                      @RequestHeader(HttpHeaders.CONTENT_TYPE) String contentType) {
        Caller.current();
        if (body == null || body.isBlank()) throw ApiException.badRequest("The schema body is empty.");
        String id = shapes.create(body, contentType);
        return ResponseEntity.status(201).header(HttpHeaders.LOCATION, "/schemas/" + id)
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .body(Json.write(Json.object().put("schemaId", id)));
    }

    @GetMapping(value = "/schemas/{schemaId}", produces = "text/turtle")
    public ResponseEntity<String> get(@PathVariable String schemaId) {
        Caller.current().requireAny(Roles.SCHEMA_READ);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, "text/turtle").body(shapes.get(schemaId).content());
    }

    @DeleteMapping("/schemas/{schemaId}")
    public ResponseEntity<Void> delete(@PathVariable String schemaId) {
        Caller.current();
        shapes.delete(schemaId);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/schemas/{schemaName}/recalcQualityScore", produces = "application/json")
    public ResponseEntity<String> recalc(@PathVariable String schemaName, @RequestBody(required = false) String body,
                                         @RequestHeader(value = HttpHeaders.CONTENT_TYPE, required = false) String ct) {
        Caller.current();
        if (!SCHEMA_NAME.matcher(schemaName).matches()) {
            throw ApiException.invalidInput("schemaName '" + schemaName
                    + "' is invalid. Must be PascalCase alphanumeric, 3-64 characters.");
        }
        if (ct != null && !ct.toLowerCase().startsWith("application/json")) {
            throw new ApiException(ProblemTypes.UNSUPPORTED_MEDIA_TYPE,
                    "Server refused to accept the request because the message content format is not supported.");
        }
        JsonNode b = Json.parseOrNull(body);
        if (b == null || !b.isObject() || !b.path("profileVersion").isTextual() || b.get("profileVersion").asText().isBlank()) {
            throw ApiException.invalidInput("The request body must contain profileVersion.");
        }
        if (!shapes.exists(schemaName)) {
            throw new ApiException(ProblemTypes.SCHEMA_NOT_FOUND, "Schema '" + schemaName + "' could not be resolved.");
        }
        boolean hasProfile;
        try {
            hasProfile = quality.activeProfile(schemaName).isPresent();
        } catch (RuntimeException e) {
            throw new ApiException(ProblemTypes.INTERNAL, "The quality profile could not be read.");
        }
        if (!hasProfile) {
            throw new ApiException(ProblemTypes.ACTIVE_QUALITY_PROFILE_NOT_FOUND,
                    "No active Metadata Quality Profile is linked to schema '" + schemaName + "'.");
        }
        ObjectNode out = Json.object();
        out.put("status", "ACCEPTED");
        out.put("schemaName", schemaName);
        out.put("profileVersion", b.get("profileVersion").asText());
        out.put("estimatedResourceCount", rds.activeCountForSchema(schemaName));
        return SelfDescriptionController.json(202, out);
    }
}
