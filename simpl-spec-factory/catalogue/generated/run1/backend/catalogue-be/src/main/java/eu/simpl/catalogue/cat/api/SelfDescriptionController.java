package eu.simpl.catalogue.cat.api;

import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.simpl.catalogue.cat.service.PublicationService;
import eu.simpl.catalogue.cat.service.RdService;
import eu.simpl.catalogue.cat.store.RdRecord;
import eu.simpl.catalogue.common.ApiException;
import eu.simpl.catalogue.common.Caller;
import eu.simpl.catalogue.common.Json;
import eu.simpl.catalogue.common.Paging;
import eu.simpl.catalogue.common.Roles;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** /self-descriptions operations of the composed catalogue-be contract. */
@RestController
public class SelfDescriptionController {
    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;
    private static final String LD_JSON = "application/ld+json";

    private final PublicationService publication;
    private final RdService rds;

    public SelfDescriptionController(PublicationService publication, RdService rds) {
        this.publication = publication;
        this.rds = rds;
    }

    @GetMapping(value = "/self-descriptions", produces = JSON)
    public ResponseEntity<String> list(HttpServletRequest req,
                                       @RequestParam(required = false) String uploadTimerange,
                                       @RequestParam(required = false) String statusTimerange,
                                       @RequestParam(required = false) String withMeta,
                                       @RequestParam(required = false) String withContent,
                                       @RequestParam(required = false) String offset,
                                       @RequestParam(required = false) String limit) {
        Caller.current().requireAny(Roles.SEARCHER);
        List<String> statuses = Params.list(req, "statuses");
        if (statuses == null) statuses = List.of("active");
        for (String s : statuses) {
            if (!s.equals("active") && !s.equals("revoked")) {
                throw ApiException.invalidInput("statuses must contain only active or revoked.");
            }
        }
        RdService.ListFilter f = new RdService.ListFilter(
                RdService.Range.parse("uploadTimerange", uploadTimerange),
                RdService.Range.parse("statusTimerange", statusTimerange),
                Params.list(req, "issuers"), Params.list(req, "validators"), statuses,
                Params.list(req, "ids"), Params.list(req, "hashes"), Params.list(req, "providedBy"));
        ObjectNode out = rds.list(f, Params.bool(withMeta, "withMeta", true), Params.bool(withContent, "withContent", false),
                Params.integer(offset, "offset", 0, 0, Integer.MAX_VALUE), Params.integer(limit, "limit", 100, 1, 1000));
        return json(200, out);
    }

    @PostMapping(value = "/self-descriptions", consumes = JSON, produces = JSON)
    public ResponseEntity<String> add(@RequestBody(required = false) String body) {
        return created(publication.publish(Caller.current(), body == null ? "" : body));
    }

    @PostMapping(value = "/self-descriptions/resource", consumes = JSON, produces = JSON)
    public ResponseEntity<String> addResource(@RequestBody(required = false) String body) {
        return created(publication.publish(Caller.current(), body == null ? "" : body));
    }

    private ResponseEntity<String> created(PublicationService.Published p) {
        ObjectNode meta = p.record().meta();
        meta.set("qualityAssessment", p.quality());
        return ResponseEntity.status(201)
                .header(HttpHeaders.LOCATION, location(p.record().id()))
                .header(HttpHeaders.CONTENT_TYPE, JSON)
                .body(Json.write(meta));
    }

    static String location(String id) {
        return "/self-descriptions/" + URLEncoder.encode(id, StandardCharsets.UTF_8);
    }

    @GetMapping(value = "/self-descriptions/{selfDescriptionId}", produces = JSON)
    public ResponseEntity<String> get(@PathVariable String selfDescriptionId) {
        Caller.current().requireAny(Roles.SEARCHER);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, JSON).body(rds.get(selfDescriptionId).content());
    }

    @GetMapping(value = "/self-descriptions/{selfDescriptionId}/qualityAssessmentRecord", produces = {JSON, LD_JSON})
    public ResponseEntity<String> getQar(@PathVariable String selfDescriptionId) {
        Caller.current().requireAny(Roles.SEARCHER);
        RdRecord r = rds.get(selfDescriptionId);
        if (r.qar() == null) return ResponseEntity.noContent().build();
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, JSON).body(r.qar());
    }

    @PostMapping(value = "/self-descriptions/{selfDescriptionId}/qualityAssessmentRecord",
            consumes = {JSON, LD_JSON}, produces = JSON)
    public ResponseEntity<String> putQar(@PathVariable String selfDescriptionId, @RequestBody(required = false) String body) {
        Caller.current();
        rds.putQar(selfDescriptionId, body);
        return json(200, Json.object().put("status", "ACCEPTED"));
    }

    @PostMapping(value = "/self-descriptions/{selfDescriptionId}/revoke", produces = JSON)
    public ResponseEntity<String> revoke(@PathVariable String selfDescriptionId,
                                         @RequestHeader(value = "User-Attributes", required = false) String attrs) {
        RdRecord r = rds.revoke(Caller.current(), selfDescriptionId);
        return ResponseEntity.ok().header(HttpHeaders.LOCATION, location(r.id()))
                .header(HttpHeaders.CONTENT_TYPE, JSON).body(Json.write(r.meta()));
    }

    @GetMapping(value = "/self-descriptions/{selfDescriptionId}/versions", produces = JSON)
    public ResponseEntity<String> versions(@PathVariable String selfDescriptionId,
                                           @RequestParam(required = false) String page,
                                           @RequestParam(required = false) String pageSize) {
        Caller.current().requireAny(Roles.SEARCHER);
        Paging paging = Paging.parse(page, pageSize);
        return json(200, rds.versions(selfDescriptionId, paging));
    }

    @DeleteMapping("/self-descriptions/{self_description_hash}")
    public ResponseEntity<Void> delete(@PathVariable("self_description_hash") String hash) {
        rds.delete(Caller.current(), hash);
        return ResponseEntity.ok().build();
    }

    static ResponseEntity<String> json(int status, Object body) {
        return ResponseEntity.status(status).header(HttpHeaders.CONTENT_TYPE, JSON).body(Json.write(body));
    }
}
