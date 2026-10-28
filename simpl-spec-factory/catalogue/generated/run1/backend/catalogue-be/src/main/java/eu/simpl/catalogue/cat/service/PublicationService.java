package eu.simpl.catalogue.cat.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.simpl.catalogue.cat.crypto.Jcs;
import eu.simpl.catalogue.cat.crypto.KeyRegistry;
import eu.simpl.catalogue.cat.crypto.SignatureVerifier;
import eu.simpl.catalogue.cat.store.RdRecord;
import eu.simpl.catalogue.cat.store.RdRepository;
import eu.simpl.catalogue.cat.store.ShapeRepository;
import eu.simpl.catalogue.common.ApiException;
import eu.simpl.catalogue.common.Caller;
import eu.simpl.catalogue.common.Json;
import eu.simpl.catalogue.common.ProblemTypes;
import eu.simpl.catalogue.common.RdProjection;
import eu.simpl.catalogue.common.Roles;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The publication pipeline of D14, with versioning (D13) and the quality gate. */
@Service
public class PublicationService {
    public record Published(RdRecord record, ObjectNode quality) {}

    private final VcChecks checks;
    private final KeyRegistry keys;
    private final RdRepository repo;
    private final QualityGate gate;

    public PublicationService(VcChecks checks, KeyRegistry keys, RdRepository repo, QualityGate gate) {
        this.checks = checks;
        this.keys = keys;
        this.repo = repo;
        this.gate = gate;
    }

    @Transactional
    public synchronized Published publish(Caller caller, String body) {
        // 1. role
        caller.requireAny(Roles.PUBLISHER);
        // 2. syntax
        JsonNode vc = checks.structure(body);
        checks.semantics(vc);
        JsonNode cs = vc.get("credentialSubject");
        int version = version(cs);
        // 3. signature
        String x = checks.signature(vc);
        // 4. ownership
        String providedBy = RdProjection.providedBy(vc);
        if (providedBy == null || caller.participantId() == null || !providedBy.equals(caller.participantId())) {
            throw new ApiException(ProblemTypes.PROVIDER_MISMATCH,
                    "simpl:providedBy does not match the participant of the access token.");
        }
        if (!keys.isRegistered(caller.participantId(), x)) {
            throw new ApiException(ProblemTypes.PROVIDER_MISMATCH,
                    "The signing key is not registered for participant " + caller.participantId() + ".");
        }
        // 5. schema, 6. SHACL
        ShapeRepository.Shape shape = checks.schema(vc);
        checks.shacl(vc, shape);
        // 7. duplicate and versioning
        String id = cs.get("@id").asText();
        if (repo.byId(id).isPresent()) {
            throw new ApiException(ProblemTypes.ALREADY_EXISTS, "A resource description with id " + id + " already exists.");
        }
        String sdHash = Jcs.sdHash(vc);
        if (repo.byHash(sdHash).isPresent()) {
            throw new ApiException(ProblemTypes.ALREADY_EXISTS, "This resource description is already stored.");
        }
        String previous = previousId(cs);
        RdRecord prev = null;
        String resourceKey = id;
        if (version == 1) {
            if (previous != null) throw conflict("Version 1 must not name a previous version.");
        } else {
            if (previous == null) throw conflict("Version " + version + " must name simpl:previousVersion.");
            prev = repo.byId(previous).orElseThrow(() -> conflict("The previous version " + previous + " is unknown."));
            if (!prev.active()) throw conflict("The previous version " + previous + " is not active.");
            if (!caller.participantId().equals(prev.providedBy())) {
                throw conflict("The previous version " + previous + " belongs to another participant.");
            }
            if (prev.version() + 1 != version) {
                throw conflict("Version must be " + (prev.version() + 1) + " (previous version is " + prev.version() + ").");
            }
            resourceKey = prev.resourceKey();
        }
        // 8. quality gate
        QualityGate.Outcome q = gate.evaluate(vc, id, shape.schemaId());
        // 9. store
        long now = System.currentTimeMillis();
        RdRecord rec = new RdRecord(id, sdHash, SignatureVerifier.issuerOf(vc), providedBy, "active", now, now,
                resourceKey, version, shape.schemaId(), Json.write(vc), Json.write(q.qar()), Json.write(q.summary()),
                q.overall(), q.classification());
        if (prev != null) repo.setStatus(prev.id(), "revoked", now);
        repo.insert(rec);
        return new Published(rec, q.summary());
    }

    private static ApiException conflict(String detail) {
        return new ApiException(ProblemTypes.VERSION_CONFLICT, detail);
    }

    static int version(JsonNode cs) {
        JsonNode v = cs.get("simpl:version");
        if (v == null || v.isNull()) return 1;
        String s = Json.literal(v);
        try {
            int i = Integer.parseInt(s == null ? "" : s.trim());
            if (i < 1) throw new NumberFormatException();
            return i;
        } catch (NumberFormatException e) {
            throw ApiException.badRequest("simpl:version must be an integer of at least 1.");
        }
    }

    static String previousId(JsonNode cs) {
        JsonNode p = cs.get("simpl:previousVersion");
        if (p == null || p.isNull()) return null;
        if (p.isTextual()) return p.asText();
        return p.path("@id").asText(null);
    }
}
