package eu.simpl.schemamanager.service;

import eu.simpl.schemamanager.config.Settings;
import eu.simpl.schemamanager.domain.Concept;
import eu.simpl.schemamanager.domain.SchemaStatus;
import eu.simpl.schemamanager.domain.SchemaType;
import eu.simpl.schemamanager.domain.SchemaVersion;
import eu.simpl.schemamanager.notify.SchemaEvent;
import eu.simpl.schemamanager.persistence.SchemaRepository;
import eu.simpl.schemamanager.validation.SchemaValidator;
import eu.simpl.schemamanager.web.ApiException;
import eu.simpl.schemamanager.web.Issue;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/** Concept and version lifecycle with the atomic slot rule (D11, D14 to D18). */
@Service
public class SchemaService {

    public static final String NAME_CLASH = "A schema with that name already exists.";
    public static final String SEARCH_SLOT_TAKEN = "A published federated search schema already exists.";
    public static final String RESOURCE_SLOT_TAKEN = "Another schema is in PUBLISHED state for the resource type.";
    public static final String INITIAL_CHANGELOG = "Initial version.";

    private final SchemaRepository repo;
    private final SchemaValidator validator;
    private final TransactionTemplate tx;
    private final ApplicationEventPublisher events;
    private final Settings settings;
    private final Clock clock;

    public SchemaService(SchemaRepository repo, SchemaValidator validator, TransactionTemplate tx,
                         ApplicationEventPublisher events, Settings settings, Clock clock) {
        this.repo = repo;
        this.validator = validator;
        this.tx = tx;
        this.events = events;
        this.settings = settings;
        this.clock = clock;
    }

    public record CreateCommand(String name, String title, String description, SchemaType type,
                                String resourceType, byte[] content, String user) {
    }

    public record NewVersionCommand(String name, SchemaType type, String resourceType, String changelog,
                                    byte[] content, String user) {
    }

    public record Result(Concept concept, SchemaVersion version, List<Issue> warnings) {
    }

    private Instant now() {
        return Instant.now(clock).truncatedTo(ChronoUnit.MILLIS);
    }

    // ---- URIs (D2)

    public String conceptUri(Concept c) {
        return settings.publicBase() + "/schemas/" + enc(c.name()) + typeQuery(c.schemaType());
    }

    public String versionUri(Concept c, int version) {
        return settings.publicBase() + "/schemas/" + enc(c.name()) + "/" + version + typeQuery(c.schemaType());
    }

    private static String typeQuery(SchemaType type) {
        return type == SchemaType.SEARCH ? "?schemaType=SEARCH" : "";
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    // ---- Lookup

    /** Finds a concept; a RESOURCE resourceType filter that does not match gives 404 (D11). */
    public Concept find(SchemaType type, String name, String resourceType) {
        Concept c = repo.findConcept(type, name).orElseThrow(() -> notFound(name, type));
        if (resourceType != null && !resourceType.equals(c.resourceType())) {
            throw notFound(name, type);
        }
        return c;
    }

    private static ApiException notFound(String name, SchemaType type) {
        return ApiException.notFound("Schema with name '" + name + "' and schemaType " + type + " was not found");
    }

    // ---- Create (D14, D16, D17, D19, D23)

    public Result create(CreateCommand cmd) {
        // Conflicts (409) come before content validation (D23). The name clash wins (D17).
        if (repo.existsByTypeAndName(cmd.type(), cmd.name())) {
            throw ApiException.conflict(NAME_CLASH);
        }
        String slot = Concept.slotKey(cmd.type(), cmd.resourceType());
        if (cmd.type() == SchemaType.SEARCH && repo.publishedInSlot(slot, null)) {
            throw ApiException.conflict(SEARCH_SLOT_TAKEN);
        }
        List<Issue> warnings = validator.validate(cmd.content(), cmd.type());
        try {
            return tx.execute(status -> {
                repo.lockSlot(slot);
                if (repo.existsByTypeAndName(cmd.type(), cmd.name())) {
                    throw ApiException.conflict(NAME_CLASH);
                }
                boolean slotTaken = repo.publishedInSlot(slot, null);
                if (slotTaken && cmd.type() == SchemaType.SEARCH) {
                    throw ApiException.conflict(SEARCH_SLOT_TAKEN);
                }
                Instant now = now();
                Concept c = new Concept(UUID.randomUUID(), cmd.type(), cmd.name(), cmd.title(), cmd.description(),
                        cmd.type() == SchemaType.SEARCH ? null : cmd.resourceType(),
                        slotTaken ? SchemaStatus.REVOKED : SchemaStatus.PUBLISHED, now, now, cmd.user(), 1,
                        INITIAL_CHANGELOG);
                repo.insertConcept(c);
                SchemaVersion v = new SchemaVersion(c.id(), 1, INITIAL_CHANGELOG, now, cmd.user());
                repo.insertVersion(v, cmd.content());
                if (c.status() == SchemaStatus.PUBLISHED) {
                    events.publishEvent(new SchemaEvent(SchemaEvent.Kind.PUBLISHED, c, 1, INITIAL_CHANGELOG,
                            versionUri(c, 1), now));
                }
                return new Result(c, v, warnings);
            });
        } catch (DuplicateKeyException e) {
            // A concurrent create won the race for the name or the slot.
            throw ApiException.conflict(e.getMessage() != null && e.getMessage().contains("uq_one_published_per_slot")
                    ? SEARCH_SLOT_TAKEN : NAME_CLASH);
        }
    }

    // ---- New version (D14, D15, D16, D20)

    public Result newVersion(NewVersionCommand cmd) {
        Concept found = find(cmd.type(), cmd.name(), null);
        if (cmd.resourceType() != null && !cmd.resourceType().equals(found.resourceType())) {
            throw ApiException.badRequest("Field 'resourceType' must equal the resource type of the schema ('"
                    + found.resourceType() + "'); the type of a schema never changes.");
        }
        List<Issue> warnings = validator.validate(cmd.content(), cmd.type());
        return tx.execute(status -> {
            repo.lockSlot(found.slotKey());
            Concept c = repo.findConceptForUpdate(found.id()).orElseThrow(() -> notFound(cmd.name(), cmd.type()));
            int next = repo.maxVersion(c.id()) + 1;
            SchemaStatus newStatus = c.status() == SchemaStatus.PUBLISHED || !repo.publishedInSlot(c.slotKey(), c.id())
                    ? SchemaStatus.PUBLISHED : SchemaStatus.REVOKED;
            Instant now = now();
            SchemaVersion v = new SchemaVersion(c.id(), next, cmd.changelog(), now, cmd.user());
            repo.insertVersion(v, cmd.content());
            repo.updateStatus(c.id(), newStatus, now);
            Concept updated = c.withStatus(newStatus, now).withLatest(next, cmd.changelog());
            if (newStatus == SchemaStatus.PUBLISHED) {
                events.publishEvent(new SchemaEvent(SchemaEvent.Kind.NEW_VERSION_PUBLISHED, updated, next,
                        cmd.changelog(), versionUri(updated, next), now));
            }
            return new Result(updated, v, warnings);
        });
    }

    // ---- Status change (D18)

    public Concept changeStatus(SchemaType type, String name, String resourceType, SchemaStatus target) {
        Concept found = find(type, name, resourceType);
        try {
            return tx.execute(status -> {
                repo.lockSlot(found.slotKey());
                Concept c = repo.findConceptForUpdate(found.id()).orElseThrow(() -> notFound(name, type));
                if (c.status() == target) {
                    throw ApiException.conflict("Schema '" + name + "' is already in status " + target + ".");
                }
                if (target == SchemaStatus.PUBLISHED && repo.publishedInSlot(c.slotKey(), c.id())) {
                    throw ApiException.conflict(type == SchemaType.SEARCH ? SEARCH_SLOT_TAKEN : RESOURCE_SLOT_TAKEN);
                }
                Instant now = now();
                repo.updateStatus(c.id(), target, now);
                int latest = repo.maxVersion(c.id());
                String changelog = repo.findVersion(c.id(), latest).map(SchemaVersion::changelog).orElse(null);
                Concept updated = c.withStatus(target, now).withLatest(latest, changelog);
                events.publishEvent(new SchemaEvent(
                        target == SchemaStatus.PUBLISHED ? SchemaEvent.Kind.PUBLISHED : SchemaEvent.Kind.REVOKED,
                        updated, latest, changelog, versionUri(updated, latest), now));
                return updated;
            });
        } catch (DataIntegrityViolationException e) {
            throw ApiException.conflict(type == SchemaType.SEARCH ? SEARCH_SLOT_TAKEN : RESOURCE_SLOT_TAKEN);
        }
    }

    // ---- Reads

    public record Page<T>(List<T> items, long total, int page, int pageSize) {
        public int totalPages() {
            return (int) ((total + pageSize - 1) / pageSize);
        }
    }

    public Page<Concept> list(SchemaType type, SchemaStatus status, String resourceType, int page, int pageSize) {
        long total = repo.countConcepts(type, status, resourceType);
        long offset = (long) (page - 1) * pageSize;
        List<Concept> items = offset >= total ? List.of()
                : repo.listConcepts(type, status, resourceType, (int) offset, pageSize);
        return new Page<>(items, total, page, pageSize);
    }

    public Page<SchemaVersion> versions(Concept c, int page, int pageSize) {
        long total = repo.countVersions(c.id());
        long offset = (long) (page - 1) * pageSize;
        if (offset >= total) {
            throw ApiException.notFound("Page " + page + " of the versions of schema '" + c.name() + "' is empty");
        }
        return new Page<>(repo.versions(c.id(), (int) offset, pageSize), total, page, pageSize);
    }

    public SchemaVersion version(Concept c, Integer version, String rawVersion) {
        if (version == null) {
            throw versionNotFound(c, rawVersion);
        }
        return repo.findVersion(c.id(), version).orElseThrow(() -> versionNotFound(c, rawVersion));
    }

    private static ApiException versionNotFound(Concept c, String raw) {
        return ApiException.notFound("Schema version '" + raw + "' not found for schema '" + c.name() + "'");
    }

    public byte[] content(Concept c, int version) {
        byte[] b = repo.content(c.id(), version);
        if (b == null) {
            throw versionNotFound(c, String.valueOf(version));
        }
        return b;
    }

    /** Derived version status: the highest version has the concept status, all lower ones are REVOKED (D16). */
    public static SchemaStatus versionStatus(Concept c, int version) {
        return c.latestVersion() != null && version == c.latestVersion() ? c.status() : SchemaStatus.REVOKED;
    }
}
