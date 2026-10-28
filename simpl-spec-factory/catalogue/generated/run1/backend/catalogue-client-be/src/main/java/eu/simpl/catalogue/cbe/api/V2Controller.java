package eu.simpl.catalogue.cbe.api;

import eu.simpl.catalogue.cbe.CbeProps;
import eu.simpl.catalogue.cbe.Downstream;
import eu.simpl.catalogue.common.ApiException;
import eu.simpl.catalogue.common.Caller;
import eu.simpl.catalogue.common.FederationConfig;
import eu.simpl.catalogue.common.Http;
import eu.simpl.catalogue.common.ProblemTypes;
import eu.simpl.catalogue.common.Roles;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.regex.Pattern;

/** cbe v2: schemas from the Schema Manager stand-in, active quality profile, detail by catalogue (D8, D61). */
@RestController
@RequestMapping("/v2")
public class V2Controller {
    private static final Pattern SCHEMA_NAME = Pattern.compile("^[A-Z][a-zA-Z0-9]*$");

    private final Downstream down;
    private final CbeProps props;
    private final FederationConfig cfg;
    private final GroupedSearch grouped;

    public V2Controller(Downstream down, CbeProps props, FederationConfig cfg, GroupedSearch grouped) {
        this.down = down;
        this.props = props;
        this.cfg = cfg;
        this.grouped = grouped;
    }

    @GetMapping(value = "/schemas", produces = "application/json")
    public ResponseEntity<String> schemas(@RequestParam(required = false) String resourceType,
                                          @RequestParam(required = false) String schemaType) {
        Caller.current().requireAny(Roles.SEARCHER);
        String type = schemaType == null || schemaType.isEmpty() ? "RESOURCE" : schemaType;
        if (!type.equals("RESOURCE") && !type.equals("SEARCH")) {
            throw ApiException.invalidInput("schemaType must be RESOURCE or SEARCH.");
        }
        URI uri = Http.query().add("resourceType", resourceType == null || resourceType.isEmpty() ? null : resourceType)
                .add("schemaType", type).uri(props.getSchemaStubUrl() + "/schemas");
        Downstream.Res r = down.call(HttpMethod.GET, uri, null, null, "application/json");
        if (!r.ok()) throw new ApiException(ProblemTypes.INTERNAL, "Unexpected internal error");
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, "application/json").body(r.body());
    }

    @GetMapping(value = "/schemas/{schemaId}/content", produces = "text/turtle")
    public ResponseEntity<String> content(@PathVariable String schemaId) {
        Caller.current().requireAny(Roles.SEARCHER);
        return turtle(URI.create(props.getSchemaStubUrl() + "/schemas/" + Http.seg(schemaId) + "/content"), schemaId);
    }

    @GetMapping(value = "/schemas/{schemaId}/{version}", produces = "text/turtle")
    public ResponseEntity<String> version(@PathVariable String schemaId, @PathVariable String version) {
        Caller.current().requireAny(Roles.SEARCHER);
        return turtle(URI.create(props.getSchemaStubUrl() + "/schemas/" + Http.seg(schemaId) + "/" + Http.seg(version)), schemaId);
    }

    private ResponseEntity<String> turtle(URI uri, String schemaId) {
        Downstream.Res r = down.call(HttpMethod.GET, uri, null, null, "text/turtle");
        if (r.status() == 404) {
            throw new ApiException(ProblemTypes.NOT_FOUND, "Schema with identifier '" + schemaId + "' was not found");
        }
        if (!r.ok()) throw new ApiException(ProblemTypes.INTERNAL, "Unexpected internal error");
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, "text/turtle").body(r.body());
    }

    @GetMapping(value = "/schemas/{schemaName}/qualityProfiles/active", produces = "application/json")
    public ResponseEntity<String> profile(@PathVariable String schemaName) {
        Caller.current().requireAny(Roles.SEARCHER);
        if (schemaName.length() < 3 || schemaName.length() > 64 || !SCHEMA_NAME.matcher(schemaName).matches()) {
            throw ApiException.badRequest("Path parameter schemaName must contain only alphanumeric characters and follow PascalCase.");
        }
        URI uri = Http.query().add("schemaName", schemaName).uri(props.getQualityStubUrl() + "/v1/profiles/active");
        Downstream.Res r = down.call(HttpMethod.GET, uri, null, null, "application/json");
        if (r.status() == 404) {
            throw new ApiException(ProblemTypes.NOT_FOUND, "No active Metadata Quality Profile is linked to schema '" + schemaName + "'.");
        }
        if (!r.ok()) throw new ApiException(ProblemTypes.INTERNAL, "Unexpected internal error");
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, "application/json").body(r.body());
    }

    @GetMapping(value = "/selfDescriptions/{selfDescriptionId}", produces = "application/json")
    public ResponseEntity<String> detail(@PathVariable String selfDescriptionId,
                                         @RequestParam(required = false) String catalogueId) {
        Caller caller = Caller.current().requireAny(Roles.SEARCHER);
        if (catalogueId == null || !FederationConfig.CATALOGUE_ID.matcher(catalogueId).matches()) {
            throw ApiException.invalidInput("The dataspace/catalogue identifier catalogueId is missing or malformed.");
        }
        if (!cfg.isOwn(catalogueId) && cfg.federated(catalogueId).isEmpty()) {
            throw ApiException.invalidInput("The dataspace/catalogue identifier '" + catalogueId + "' is invalid.");
        }
        return Detail.fetch(down, grouped.qma(), caller, selfDescriptionId, catalogueId);
    }
}
