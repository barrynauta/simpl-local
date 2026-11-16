package eu.simpl.cm.manager.service;

import eu.simpl.cm.common.CorrelationContext;
import eu.simpl.cm.manager.CmProperties;
import eu.simpl.cm.manager.domain.ContractRecord;
import eu.simpl.cm.manager.domain.ContractRepository;
import eu.simpl.cm.manager.domain.ContractStatus;
import eu.simpl.cm.manager.domain.EntryType;
import eu.simpl.cm.manager.domain.HistoryEntry;
import eu.simpl.cm.manager.domain.HistoryRepository;
import eu.simpl.cm.manager.domain.Trigger;
import eu.simpl.cm.manager.events.EventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Transactional primitives on contract records. Every transition writes one history entry and
 * one status event (D37, D49, D61); the event goes out after commit.
 */
@Service
public class ContractStore {

    private static final Logger LOG = LoggerFactory.getLogger(ContractStore.class);

    /** Thrown when a record is not in one of the expected states. */
    public static class TransitionRefused extends RuntimeException {
        public final ContractStatus actual;

        public TransitionRefused(ContractStatus actual) {
            super("Transition not allowed from " + actual);
            this.actual = actual;
        }
    }

    private final ContractRepository contracts;
    private final HistoryRepository history;
    private final EventPublisher events;
    private final CmProperties props;

    public ContractStore(ContractRepository contracts, HistoryRepository history, EventPublisher events,
                         CmProperties props) {
        this.contracts = contracts;
        this.history = history;
        this.events = events;
        this.props = props;
    }

    @Transactional(readOnly = true)
    public Optional<ContractRecord> find(String nid) {
        return contracts.findByContractNegotiationId(nid);
    }

    @Transactional(readOnly = true)
    public Optional<ContractRecord> findByAgreement(String aid) {
        return contracts.findByContractAgreementIdOrderByCreatedDateDesc(aid).stream().findFirst();
    }

    /** Saves a new record and writes the creation transition (previousStatus null). */
    @Transactional
    public ContractRecord create(ContractRecord r, Trigger trigger, String actor) {
        Instant now = Instant.now();
        r.createdDate = now;
        r.lastUpdatedDate = now;
        ContractRecord saved = contracts.saveAndFlush(r);
        writeTransition(saved, null, trigger, actor, null, null);
        return saved;
    }

    /**
     * Moves a record from one of {@code from} to {@code to}. The mutator runs before saving.
     * Throws TransitionRefused when the current status is not in {@code from}.
     */
    @Transactional
    public ContractRecord transition(String nid, Set<ContractStatus> from, ContractStatus to, Trigger trigger,
                                     String actor, String reasonCode, String reason,
                                     Consumer<ContractRecord> mutator) {
        ContractRecord r = contracts.findByContractNegotiationId(nid).orElseThrow();
        if (!from.contains(r.status)) {
            throw new TransitionRefused(r.status);
        }
        ContractStatus previous = r.status;
        r.status = to;
        if (reasonCode != null) {
            r.reasonCode = reasonCode;
        }
        if (mutator != null) {
            mutator.accept(r);
        }
        r.lastUpdatedDate = Instant.now();
        ContractRecord saved = contracts.saveAndFlush(r);
        writeTransition(saved, previous, trigger, actor, reasonCode, reason);
        return saved;
    }

    /** Changes fields without a transition and without history. */
    @Transactional
    public ContractRecord update(String nid, Consumer<ContractRecord> mutator) {
        ContractRecord r = contracts.findByContractNegotiationId(nid).orElseThrow();
        mutator.accept(r);
        r.lastUpdatedDate = Instant.now();
        return contracts.saveAndFlush(r);
    }

    /** Appends a non-transition history entry (fromStatus = toStatus = current status). */
    @Transactional
    public void note(String nid, EntryType type, Trigger trigger, String actor, String reasonCode, String reason) {
        contracts.findByContractNegotiationId(nid).ifPresent(r -> append(r.contractNegotiationId, r.status, r.status,
                type, trigger, actor, reasonCode, reason));
    }

    /** Appends a non-transition entry and publishes a status event with unchanged status (consent, A.5). */
    @Transactional
    public ContractRecord noteWithEvent(String nid, Consumer<ContractRecord> mutator, Trigger trigger, String actor,
                                        String reasonCode, String reason) {
        ContractRecord r = contracts.findByContractNegotiationId(nid).orElseThrow();
        mutator.accept(r);
        r.lastUpdatedDate = Instant.now();
        ContractRecord saved = contracts.saveAndFlush(r);
        append(saved.contractNegotiationId, saved.status, saved.status, EntryType.TRANSITION, trigger, actor,
                reasonCode, reason);
        publishStatus(saved, saved.status, trigger, actor, reasonCode, reason);
        return saved;
    }

    private void writeTransition(ContractRecord r, ContractStatus previous, Trigger trigger, String actor,
                                 String reasonCode, String reason) {
        append(r.contractNegotiationId, previous, r.status, EntryType.TRANSITION, trigger, actor, reasonCode, reason);
        publishStatus(r, previous, trigger, actor, reasonCode, reason);
        LOG.info("Contract {} {} -> {} (trigger {}, correlationId {})", r.contractNegotiationId, previous, r.status,
                trigger, CorrelationContext.get());
    }

    private void publishStatus(ContractRecord r, ContractStatus previous, Trigger trigger, String actor,
                               String reasonCode, String reason) {
        Map<String, Object> e = new LinkedHashMap<>();
        e.put("eventId", UUID.randomUUID().toString());
        e.put("participantId", props.participantId());
        e.put("partyRole", r.partyRole.name());
        e.put("contractNegotiationId", r.contractNegotiationId);
        e.put("contractAgreementId", r.contractAgreementId);
        e.put("previousStatus", previous == null ? null : previous.name());
        e.put("newStatus", r.status.name());
        e.put("reasonCode", reasonCode);
        e.put("reason", reason);
        e.put("trigger", trigger.name());
        e.put("actor", actor);
        e.put("occurredAt", Instant.now().toString());
        events.statusEvent(e);
    }

    private void append(String nid, ContractStatus from, ContractStatus to, EntryType type, Trigger trigger,
                        String actor, String reasonCode, String reason) {
        HistoryEntry h = new HistoryEntry();
        h.contractNegotiationId = nid;
        h.at = Instant.now();
        h.fromStatus = from;
        h.toStatus = to;
        h.entryType = type;
        h.trigger = trigger;
        h.actor = actor;
        h.reasonCode = reasonCode;
        h.reason = reason;
        h.correlationId = CorrelationContext.get();
        history.save(h);
    }
}
