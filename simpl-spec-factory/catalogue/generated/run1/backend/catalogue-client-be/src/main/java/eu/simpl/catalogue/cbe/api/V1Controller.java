package eu.simpl.catalogue.cbe.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.simpl.catalogue.cbe.CbeProps;
import eu.simpl.catalogue.cbe.Downstream;
import eu.simpl.catalogue.cbe.QualityLabels;
import eu.simpl.catalogue.common.AdvancedQuery;
import eu.simpl.catalogue.common.ApiException;
import eu.simpl.catalogue.common.Caller;
import eu.simpl.catalogue.common.FederationConfig;
import eu.simpl.catalogue.common.Http;
import eu.simpl.catalogue.common.Json;
import eu.simpl.catalogue.common.Paging;
import eu.simpl.catalogue.common.ProblemTypes;
import eu.simpl.catalogue.common.QualityParams;
import eu.simpl.catalogue.common.Roles;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/** cbe v1: grouped search, advanced search, QAR, catalogues, own detail (D8, D38, D46). */
@RestController
@RequestMapping("/v1")
public class V1Controller {
    private final GroupedSearch grouped;
    private final Downstream down;
    private final FederationConfig cfg;
    private final CbeProps props;
    private final QualityLabels labels;

    public V1Controller(GroupedSearch grouped, Downstream down, FederationConfig cfg, CbeProps props, QualityLabels labels) {
        this.grouped = grouped;
        this.down = down;
        this.cfg = cfg;
        this.props = props;
        this.labels = labels;
    }

    static List<String> splitIds(String[] values) {
        if (values == null) return null;
        List<String> out = new ArrayList<>();
        for (String v : values) {
            if (v.isEmpty()) { out.add(""); continue; }
            for (String p : v.split(",")) out.add(p.trim());
        }
        return out;
    }

    static String plain(Double d) {
        return BigDecimal.valueOf(d).stripTrailingZeros().toPlainString();
    }

    @GetMapping(value = "/selfDescriptions", produces = "application/json")
    public ResponseEntity<String> search(HttpServletRequest req,
                                         @RequestParam(required = false) String q,
                                         @RequestParam(required = false) String minimumScore,
                                         @RequestParam(required = false) String minimumClassification,
                                         @RequestParam(required = false) String page,
                                         @RequestParam(required = false) String pageSize) {
        Caller caller = Caller.current().requireAny(Roles.SEARCHER);
        if (q == null || q.isBlank()) throw ApiException.invalidInput("Field 'q' must not be empty");
        QualityParams quality = QualityParams.parse(minimumScore, minimumClassification, 100.0);
        labels.validate(quality);
        Paging p = Paging.parse(page, pageSize);
        List<String> ids = grouped.resolve(splitIds(req.getParameterValues("catalogueIds")));
        ObjectNode out = grouped.run(ids, p, caller.bearer(), id -> {
            if (cfg.isOwn(id)) {
                Http.Query query = Http.query().add("searchString", q);
                if (quality.minimumScore() != null) query.add("minimumScore", plain(quality.minimumScore()));
                query.add("minimumClassification", quality.minimumClassification());
                query.add("page", p.page()).add("pageSize", p.pageSize());
                return new GroupedSearch.Call(HttpMethod.GET, query.uri(grouped.qma() + "/selfDescriptions"), null);
            }
            URI uri = Http.query().add("catalogueIds", id).add("searchString", q).add("page", p.page())
                    .add("pageSize", p.pageSize()).uri(grouped.qma() + "/governanceAuthority/selfDescriptions");
            return new GroupedSearch.Call(HttpMethod.GET, uri, null);
        });
        return json(out);
    }

    @PostMapping(value = "/selfDescriptions/advanced", consumes = "application/json", produces = "application/json")
    public ResponseEntity<String> advanced(@RequestBody(required = false) String text,
                                           @RequestParam(required = false) String page,
                                           @RequestParam(required = false) String pageSize) {
        Caller caller = Caller.current().requireAny(Roles.SEARCHER);
        if (text == null || text.isBlank()) throw ApiException.invalidInput("Request body must not be empty");
        JsonNode body = Json.parseOrNull(text);
        if (body != null && body.isTextual()) body = Json.parseOrNull(body.asText());
        if (body == null || !body.isObject()) throw ApiException.badRequest("The request body must be a JSON object.");
        Paging p = Paging.parse(page, pageSize);
        QualityParams quality = QualityParams.fromBody(body, 100.0);
        labels.validate(quality);
        AdvancedQuery.parse(body);
        List<String> raw = null;
        JsonNode idsNode = body.get("catalogueIds");
        if (idsNode != null && !idsNode.isNull()) {
            if (!idsNode.isArray()) throw ApiException.invalidInput("catalogueIds must be an array of catalogue identifiers.");
            raw = new ArrayList<>();
            for (JsonNode n : idsNode) raw.add(n.isTextual() ? n.asText() : "");
        }
        List<String> ids = grouped.resolve(raw);
        final JsonNode b = body;
        ObjectNode out = grouped.run(ids, p, caller.bearer(), id -> {
            String paging = Http.query().add("page", p.page()).add("pageSize", p.pageSize()).relative("");
            boolean own = cfg.isOwn(id);
            ObjectNode fb = AdvancedQuery.withoutReserved(b, own);
            if (own) {
                if (quality.minimumScore() == null) fb.remove("minimumScore");
                if (quality.minimumClassification() == null) fb.remove("minimumClassification");
            }
            fb.putArray("catalogueIds").add(id);
            String path = own ? "/selfDescriptions/advancedSearch" : "/governanceAuthority/selfDescriptions/advancedSearch";
            return new GroupedSearch.Call(HttpMethod.POST, URI.create(grouped.qma() + path + paging), Json.write(fb));
        });
        return json(out);
    }

    @GetMapping(value = "/selfDescriptions/{selfDescriptionId}/qualityAssessmentRecord", produces = "application/json")
    public ResponseEntity<String> qar(@PathVariable String selfDescriptionId) {
        Caller caller = Caller.current().requireAny(Roles.SEARCHER);
        Downstream.Res r = down.call(HttpMethod.GET,
                URI.create(props.getCatUrl() + "/self-descriptions/" + Http.seg(selfDescriptionId) + "/qualityAssessmentRecord"),
                caller.bearer(), null, "application/json");
        if (r.status() == 204) return ResponseEntity.noContent().build();
        if (r.status() == 404) {
            throw new ApiException(ProblemTypes.NOT_FOUND, "SelfDescription with identifier '" + selfDescriptionId + "' was not found");
        }
        if (!r.ok()) throw new ApiException(ProblemTypes.INTERNAL, "Unexpected internal error");
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, "application/json").body(r.body());
    }

    @GetMapping(value = "/catalogues", produces = "application/json")
    public ResponseEntity<String> catalogues(@RequestParam(required = false) String page,
                                             @RequestParam(required = false) String pageSize) {
        Caller.current().requireAny(Roles.SEARCHER);
        Paging p = Paging.parse(page, pageSize);
        ObjectNode out = Json.object();
        ArrayNode items = out.putArray("items");
        cfg.federatedCatalogues.stream().skip(p.offset()).limit(p.pageSize()).forEach(c -> {
            ObjectNode i = items.addObject();
            i.put("id", c.id);
            i.put("name", c.name);
            i.put("authorityURL", c.authorityURL);
            i.put("active", c.active);
        });
        out.put("page", p.page());
        out.put("pageSize", p.pageSize());
        out.put("total", cfg.federatedCatalogues.size());
        return json(out);
    }

    @GetMapping(value = "/selfDescriptions/{selfDescriptionId}", produces = "application/json")
    public ResponseEntity<String> detail(@PathVariable String selfDescriptionId) {
        Caller caller = Caller.current().requireAny(Roles.SEARCHER);
        return Detail.fetch(down, grouped.qma(), caller, selfDescriptionId, cfg.ownCatalogue.id);
    }

    @GetMapping("/contractClauses/documents/{documentId}")
    public ResponseEntity<String> document(@PathVariable String documentId) {
        Caller.current().requireAny(Roles.SEARCHER);
        throw new ApiException(ProblemTypes.NOT_IMPLEMENTED, "Contract clause documents are not part of this build.");
    }

    static ResponseEntity<String> json(Object body) {
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, "application/json").body(Json.write(body));
    }
}
