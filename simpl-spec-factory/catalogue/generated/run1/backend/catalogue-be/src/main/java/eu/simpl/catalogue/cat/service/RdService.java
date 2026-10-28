package eu.simpl.catalogue.cat.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.simpl.catalogue.cat.quality.Qar;
import eu.simpl.catalogue.cat.store.RdRecord;
import eu.simpl.catalogue.cat.store.RdRepository;
import eu.simpl.catalogue.common.ApiException;
import eu.simpl.catalogue.common.Caller;
import eu.simpl.catalogue.common.Json;
import eu.simpl.catalogue.common.Paging;
import eu.simpl.catalogue.common.ProblemTypes;
import eu.simpl.catalogue.common.RdProjection;
import eu.simpl.catalogue.common.Roles;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/** Read, list, revoke, delete, versions and QAR operations on stored RDs. */
@Service
public class RdService {
    private final RdRepository repo;
    private final QualityGate gate;

    public RdService(RdRepository repo, QualityGate gate) {
        this.repo = repo;
        this.gate = gate;
    }

    public RdRecord get(String id) {
        return repo.byId(id).orElseThrow(() -> notFound(id));
    }

    public static ApiException notFound(String id) {
        return new ApiException(ProblemTypes.RESOURCE_NOT_FOUND, "Resource description " + id + " was not found.");
    }

    @Transactional
    public synchronized RdRecord revoke(Caller caller, String id) {
        caller.requireAny(Roles.PUBLISHER);
        RdRecord r = get(id);
        owner(caller, r);
        if (r.active()) {
            repo.setStatus(id, "revoked", System.currentTimeMillis());
            r = get(id);
        }
        return r;
    }

    @Transactional
    public synchronized void delete(Caller caller, String hash) {
        caller.requireAny(Roles.PUBLISHER);
        RdRecord r = repo.byHash(hash).orElseThrow(() -> new ApiException(ProblemTypes.RESOURCE_NOT_FOUND,
                "No resource description with hash " + hash + " was found."));
        owner(caller, r);
        repo.delete(r.id());
    }

    private static void owner(Caller caller, RdRecord r) {
        if (caller.participantId() == null || !caller.participantId().equals(r.providedBy())) {
            throw ApiException.forbidden("Only the owner of the resource description may do this.");
        }
    }

    /** D19: all other versions of the same resource, version descending. */
    public ObjectNode versions(String id, Paging paging) {
        RdRecord r = get(id);
        List<RdRecord> others = new ArrayList<>(repo.byResourceKey(r.resourceKey()));
        others.removeIf(o -> o.id().equals(id));
        ObjectNode out = Json.object();
        out.put("totalCount", others.size());
        ArrayNode items = out.putArray("items");
        others.stream().skip(paging.offset()).limit(paging.pageSize()).forEach(o -> {
            ObjectNode n = RdProjection.mainProperties(o.vc());
            n.put("version", o.version());
            n.put("status", o.status());
            items.addObject().set("n", n);
        });
        return out;
    }

    /** D18 list with filters. */
    public ObjectNode list(ListFilter f, boolean withMeta, boolean withContent, int offset, int limit) {
        List<RdRecord> hits = new ArrayList<>();
        for (RdRecord r : repo.all()) {
            if (f.matches(r)) hits.add(r);
        }
        ObjectNode out = Json.object();
        out.put("totalCount", hits.size());
        ArrayNode items = out.putArray("items");
        hits.stream().skip(offset).limit(limit).forEach(r -> {
            ObjectNode item = items.addObject();
            if (withMeta) item.set("meta", r.meta());
            if (withContent) item.put("content", r.content());
        });
        return out;
    }

    @Transactional
    public synchronized void putQar(String id, String body) {
        RdRecord r = get(id);
        JsonNode doc = Json.parseOrNull(body);
        QualityGate.Outcome o;
        try {
            o = gate.summarise(doc, id, r.schemaName());
        } catch (Qar.InvalidQarException e) {
            throw ApiException.invalidInput(e.getMessage());
        }
        repo.setQuality(id, Json.write(doc), Json.write(o.summary()), o.overall(), o.classification());
    }

    /** Filters of D18. Null lists mean "not set". */
    public record ListFilter(Range upload, Range status, List<String> issuers, List<String> validators,
                             List<String> statuses, List<String> ids, List<String> hashes, List<String> providedBy) {
        boolean matches(RdRecord r) {
            if (upload != null && !upload.contains(r.uploadMs())) return false;
            if (status != null && !status.contains(r.statusMs())) return false;
            if (issuers != null && !issuers.contains(r.issuer())) return false;
            if (validators != null) return false;
            if (statuses != null && !statuses.contains(r.status())) return false;
            if (ids != null && !ids.contains(r.id())) return false;
            if (hashes != null && !hashes.contains(r.sdHash())) return false;
            if (providedBy != null && !providedBy.contains(r.providedBy())) return false;
            return true;
        }
    }

    public record Range(long startMs, long endMs) {
        boolean contains(long ms) { return ms >= startMs && ms < endMs; }

        public static Range parse(String name, String v) {
            if (v == null || v.isBlank()) return null;
            String[] p = v.split("/", -1);
            if (p.length != 2) throw ApiException.invalidInput(name + " must be start/end as ISO 8601 timestamps.");
            try {
                return new Range(instant(p[0]).toEpochMilli(), instant(p[1]).toEpochMilli());
            } catch (DateTimeParseException e) {
                throw ApiException.invalidInput(name + " must be start/end as ISO 8601 timestamps.");
            }
        }

        private static Instant instant(String s) {
            try {
                return Instant.parse(s.trim());
            } catch (DateTimeParseException e) {
                return OffsetDateTime.parse(s.trim()).toInstant();
            }
        }
    }
}
