package eu.simpl.catalogue.cat.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.simpl.catalogue.cat.graph.ClaimsGraph;
import eu.simpl.catalogue.common.ApiException;
import eu.simpl.catalogue.common.Caller;
import eu.simpl.catalogue.common.Json;
import eu.simpl.catalogue.common.Roles;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** GET and POST /query (D84) over the claims graph (D85), and POST /query/search (D88). */
@RestController
public class QueryController {
    private static final String PAGE = """
            <!DOCTYPE html>
            <html lang="en"><head><meta charset="utf-8"><title>Catalogue query</title></head>
            <body>
            <h1>Send an openCypher query to the catalogue</h1>
            <p>POST the statement as JSON to <code>/query</code>: {"statement": "...", "parameters": {}}.</p>
            <form><label for="statement">Statement</label><br>
            <textarea id="statement" name="statement" rows="6" cols="80">MATCH (n:Resource) RETURN n.uri LIMIT 10</textarea>
            </form>
            </body></html>
            """;

    private final ClaimsGraph graph;

    public QueryController(ClaimsGraph graph) {
        this.graph = graph;
    }

    @GetMapping("/query")
    public ResponseEntity<String> page() {
        Caller.current().requireAny(Roles.SEARCHER);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, "text/html;charset=UTF-8").body(PAGE);
    }

    @PostMapping(value = "/query", consumes = "application/json", produces = "application/json")
    public ResponseEntity<String> query(@RequestBody(required = false) String body,
                                        @RequestParam(required = false) String queryLanguage,
                                        @RequestParam(required = false) String timeout,
                                        @RequestParam(required = false) String withTotalCount) {
        return run(body, queryLanguage, timeout, withTotalCount);
    }

    /** D88: in no specification; the PSO scenario TC_FC_09 shows 400 for an empty body. Same contract as POST /query. */
    @PostMapping(value = "/query/search", consumes = "application/json", produces = "application/json")
    public ResponseEntity<String> search(@RequestBody(required = false) String body,
                                         @RequestParam(required = false) String queryLanguage,
                                         @RequestParam(required = false) String timeout,
                                         @RequestParam(required = false) String withTotalCount) {
        return run(body, queryLanguage, timeout, withTotalCount);
    }

    private ResponseEntity<String> run(String body, String queryLanguage, String timeout, String withTotalCount) {
        Caller.current().requireAny(Roles.SEARCHER);
        String lang = queryLanguage == null || queryLanguage.isEmpty() ? "OPENCYPHER" : queryLanguage;
        if (!List.of("OPENCYPHER", "SPARQL", "GRAPHQL").contains(lang)) {
            throw ApiException.invalidInput("Parameter 'queryLanguage' must be OPENCYPHER, SPARQL or GRAPHQL.");
        }
        if (!lang.equals("OPENCYPHER")) {
            throw ApiException.invalidInput("Query language " + lang + " is not supported; use OPENCYPHER.");
        }
        int seconds = Params.integer(timeout, "timeout", 5, 1, 300);
        boolean total = Params.bool(withTotalCount, "withTotalCount", true);
        JsonNode doc = Json.parseOrNull(body);
        if (doc == null || !doc.isObject()) throw ApiException.badRequest("The request body must be a JSON object.");
        JsonNode st = doc.get("statement");
        if (st == null || !st.isTextual() || st.asText().isBlank()) {
            throw ApiException.badRequest("The request body must contain a non-empty 'statement'.");
        }
        JsonNode p = doc.get("parameters");
        Map<String, Object> params = Map.of();
        if (p != null && !p.isNull()) {
            if (!p.isObject()) throw ApiException.badRequest("'parameters' must be a JSON object.");
            params = Json.MAPPER.convertValue(p, new TypeReference<Map<String, Object>>() {});
        }
        List<Map<String, Object>> rows = graph.query(st.asText(), params, seconds);
        ObjectNode out = Json.object();
        if (total) out.put("totalCount", rows.size());
        out.set("items", Json.MAPPER.valueToTree(rows));
        return SelfDescriptionController.json(200, out);
    }
}
