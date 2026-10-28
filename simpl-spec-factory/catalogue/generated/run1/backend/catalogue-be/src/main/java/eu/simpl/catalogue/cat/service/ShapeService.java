package eu.simpl.catalogue.cat.service;

import eu.simpl.catalogue.cat.CatProps;
import eu.simpl.catalogue.cat.rdf.ShapeSupport;
import eu.simpl.catalogue.cat.store.ShapeRepository;
import eu.simpl.catalogue.common.ApiException;
import eu.simpl.catalogue.common.ProblemTypes;
import org.apache.jena.graph.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/** SHACL shape store of D58, loaded from the seed folders at startup, plus the webhook statuses of D60. */
@Service
public class ShapeService implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(ShapeService.class);

    private final ShapeRepository repo;
    private final CatProps props;

    public ShapeService(ShapeRepository repo, CatProps props) {
        this.repo = repo;
        this.props = props;
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        for (String dir : props.getSeedDirs()) {
            Path d = Path.of(dir);
            if (!Files.isDirectory(d)) {
                log.warn("Seed folder {} not found", dir);
                continue;
            }
            try (Stream<Path> files = Files.list(d)) {
                for (Path f : files.filter(p -> p.toString().endsWith(".ttl")).sorted().toList()) {
                    String ttl = Files.readString(f);
                    Graph g = ShapeSupport.parseTurtle(ttl);
                    String id = ShapeSupport.offeringShapeId(g);
                    String fileId = f.getFileName().toString().replaceAll("\\.ttl$", "");
                    if (id == null) id = fileId;
                    repo.upsertSeed(new ShapeRepository.Shape(id, "1", "PUBLISHED", ttl));
                    log.info("Loaded seed shape {} from {}", id, f);
                }
            }
        }
    }

    public List<String> ids() { return repo.ids(); }

    public ShapeRepository.Shape get(String id) {
        return repo.byId(id).orElseThrow(() -> new ApiException(ProblemTypes.RESOURCE_NOT_FOUND,
                "Schema '" + id + "' was not found."));
    }

    @Transactional
    public synchronized String create(String body, String contentType) {
        String ct = contentType == null ? "" : contentType.toLowerCase();
        if (ct.startsWith("application/rdf+xml")) {
            throw ApiException.badRequest("Only SHACL shapes (Turtle or JSON-LD) are accepted.");
        }
        Graph g;
        String ttl;
        try {
            if (ct.startsWith("text/turtle")) {
                g = ShapeSupport.parseTurtle(body);
                ttl = body;
            } else {
                g = ShapeSupport.parseJsonLd(body);
                ttl = ShapeSupport.toTurtle(g);
            }
        } catch (RuntimeException e) {
            throw ApiException.badRequest("The schema cannot be parsed: " + e.getMessage());
        }
        String id = ShapeSupport.offeringShapeId(g);
        if (id == null) {
            throw ApiException.badRequest("No sh:NodeShape with sh:targetClass simpl:DataOffering, "
                    + "simpl:ApplicationOffering or simpl:InfrastructureOffering was found.");
        }
        if (repo.byId(id).isPresent()) {
            throw new ApiException(ProblemTypes.ALREADY_EXISTS, "Schema '" + id + "' already exists.");
        }
        String status = "PUBLISHED";
        String version = "1";
        var memo = repo.remembered(id);
        if (memo.isPresent()) {
            status = memo.get().status();
            if (memo.get().version() != null) version = memo.get().version();
        }
        repo.insert(new ShapeRepository.Shape(id, version, status, ttl));
        return id;
    }

    @Transactional
    public void delete(String id) {
        if (!repo.delete(id)) {
            throw new ApiException(ProblemTypes.RESOURCE_NOT_FOUND, "Schema '" + id + "' was not found.");
        }
    }

    public boolean exists(String id) { return repo.byId(id).isPresent(); }

    @Transactional
    public synchronized void event(String type, String name, String version) {
        String status = "SchemaRevoked".equals(type) ? "REVOKED" : "PUBLISHED";
        String v = "SchemaPublished".equals(type) ? version : null;
        if (repo.byId(name).isPresent()) {
            repo.setStatus(name, status, v);
        } else {
            repo.remember(name, status, v);
        }
    }
}
