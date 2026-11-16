package eu.simpl.catalogue.cat.graph;

import com.fasterxml.jackson.databind.JsonNode;
import eu.simpl.catalogue.cat.CatProps;
import eu.simpl.catalogue.cat.rdf.JsonLdSupport;
import eu.simpl.catalogue.cat.store.RdRecord;
import eu.simpl.catalogue.cat.store.RdRepository;
import eu.simpl.catalogue.common.ApiException;
import eu.simpl.catalogue.common.ProblemTypes;
import jakarta.annotation.PreDestroy;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.neo4j.driver.TransactionConfig;
import org.neo4j.driver.Value;
import org.neo4j.driver.exceptions.ClientException;
import org.neo4j.driver.exceptions.Neo4jException;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Path;
import org.neo4j.driver.types.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The openCypher store of D84 and D85: a projection of the claims of active RDs into a graph database.
 * PostgreSQL stays the record of truth; the projection is rebuilt from it at startup. Without GRAPH_URL
 * the projection is off and /query answers 503 (used by the build's own tests).
 */
@Service
public class ClaimsGraph {
    private static final Logger log = LoggerFactory.getLogger(ClaimsGraph.class);

    private final RdRepository repo;
    private final Driver driver;

    public ClaimsGraph(CatProps props, RdRepository repo) {
        this.repo = repo;
        String url = props.getGraphUrl();
        this.driver = url == null || url.isBlank() ? null : GraphDatabase.driver(url, AuthTokens.none());
    }

    public boolean enabled() { return driver != null; }

    @PreDestroy
    void close() {
        if (driver != null) driver.close();
    }

    /** Rebuilds the projection from the stored active RDs. Waits for the graph database to answer. */
    @EventListener(ApplicationReadyEvent.class)
    public void resync() {
        if (driver == null) return;
        for (int attempt = 1; ; attempt++) {
            try {
                driver.verifyConnectivity();
                break;
            } catch (RuntimeException e) {
                if (attempt >= 60) throw e;
                sleep();
            }
        }
        try (Session s = driver.session()) {
            s.executeWriteWithoutResult(tx -> tx.run("MATCH (n:" + ClaimsMapping.BASE_LABEL + ") DETACH DELETE n"));
        }
        int n = 0;
        for (RdRecord r : repo.all()) {
            if (r.active()) {
                load(r.id(), r.vc());
                n++;
            }
        }
        log.info("Claims graph rebuilt with {} active resource descriptions", n);
    }

    /** Loads the claims of one RD (on publish, D85). */
    public void load(String rdId, JsonNode vc) {
        if (driver == null) return;
        ClaimsMapping.Mapped m = ClaimsMapping.map(JsonLdSupport.subjectGraph(vc), rdId);
        try (Session s = driver.session()) {
            s.executeWriteWithoutResult(tx -> {
                tx.run("MATCH (n:" + ClaimsMapping.BASE_LABEL + ") WHERE $g IN n.claimsGraphUri DETACH DELETE n",
                        Map.of("g", rdId));
                Map<String, String> ids = new HashMap<>();
                for (ClaimsMapping.GraphNode node : m.nodes()) {
                    String labels = node.labels().stream().map(ClaimsGraph::quote).collect(Collectors.joining(":"));
                    String id = tx.run("CREATE (n:" + labels + ") SET n = $props RETURN elementId(n) AS id",
                            Map.of("props", node.props())).single().get("id").asString();
                    ids.put(node.key(), id);
                }
                for (ClaimsMapping.GraphRel rel : m.rels()) {
                    tx.run("MATCH (a), (b) WHERE elementId(a) = $a AND elementId(b) = $b "
                                    + "CREATE (a)-[r:" + quote(rel.type()) + "]->(b) SET r.claimsGraphUri = $g",
                            Map.of("a", ids.get(rel.from()), "b", ids.get(rel.to()), "g", List.of(rdId)));
                }
            });
        }
    }

    /** Removes the claims of one RD (on revoke, on delete, and for the previous version on a new version). */
    public void remove(String rdId) {
        if (driver == null) return;
        try (Session s = driver.session()) {
            s.executeWriteWithoutResult(tx -> tx.run(
                    "MATCH (n:" + ClaimsMapping.BASE_LABEL + ") WHERE $g IN n.claimsGraphUri DETACH DELETE n",
                    Map.of("g", rdId)));
        }
    }

    /** Runs one read-only openCypher statement (D84). */
    public List<Map<String, Object>> query(String statement, Map<String, Object> params, int timeoutSeconds) {
        if (driver == null) {
            throw new ApiException(ProblemTypes.SERVICE_UNAVAILABLE, "The query store is not configured.");
        }
        TransactionConfig cfg = TransactionConfig.builder().withTimeout(Duration.ofSeconds(timeoutSeconds)).build();
        try (Session s = driver.session()) {
            return s.executeRead(tx -> {
                List<Map<String, Object>> rows = new ArrayList<>();
                for (Record rec : tx.run(statement, params).list()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (String k : rec.keys()) row.put(k, plain(rec.get(k)));
                    rows.add(row);
                }
                return rows;
            }, cfg);
        } catch (ClientException e) {
            String code = e.code() == null ? "" : e.code();
            if (code.contains("TransactionTimedOut") || code.contains("Timeout")) {
                throw new ApiException(ProblemTypes.QUERY_TIMEOUT, "The query did not finish within " + timeoutSeconds + " seconds.");
            }
            if (code.contains("AccessMode") || code.contains("ForbiddenDueToTransactionType")) {
                throw ApiException.badRequest("The query must not change the catalogue.");
            }
            throw ApiException.badRequest("The query is not valid openCypher: " + firstLine(e.getMessage()));
        } catch (Neo4jException e) {
            String code = e.code() == null ? "" : e.code();
            if (code.contains("TransactionTimedOut") || code.contains("Timeout")) {
                throw new ApiException(ProblemTypes.QUERY_TIMEOUT, "The query did not finish within " + timeoutSeconds + " seconds.");
            }
            log.warn("Query failed: {} {}", code, e.getMessage());
            throw new ApiException(ProblemTypes.INTERNAL, "The query could not be run.");
        }
    }

    private static Object plain(Value v) {
        return plainObject(v.asObject());
    }

    private static Object plainObject(Object o) {
        if (o instanceof Node n) return plainMap(n.asMap());
        if (o instanceof Relationship r) {
            Map<String, Object> m = plainMap(r.asMap());
            m.put("type", r.type());
            return m;
        }
        if (o instanceof Path p) {
            List<Object> out = new ArrayList<>();
            p.nodes().forEach(n -> out.add(plainMap(n.asMap())));
            return out;
        }
        if (o instanceof Map<?, ?> m) {
            Map<String, Object> out = new LinkedHashMap<>();
            m.forEach((k, v) -> out.put(String.valueOf(k), plainObject(v)));
            return out;
        }
        if (o instanceof List<?> l) return l.stream().map(ClaimsGraph::plainObject).toList();
        if (o == null || o instanceof String || o instanceof Number || o instanceof Boolean) return o;
        return String.valueOf(o);
    }

    private static Map<String, Object> plainMap(Map<String, Object> m) {
        Map<String, Object> out = new LinkedHashMap<>();
        m.forEach((k, v) -> out.put(k, plainObject(v)));
        return out;
    }

    static String quote(String name) {
        return "`" + name.replace("`", "``") + "`";
    }

    private static String firstLine(String s) {
        if (s == null) return "";
        int i = s.indexOf('\n');
        return (i < 0 ? s : s.substring(0, i)).trim();
    }

    private static void sleep() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
