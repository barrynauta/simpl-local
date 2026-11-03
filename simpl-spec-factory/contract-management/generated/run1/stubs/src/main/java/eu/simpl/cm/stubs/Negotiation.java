package eu.simpl.cm.stubs;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Semaphore;

/** One negotiation driven by the stub (C.2). */
public class Negotiation {
    public static final List<String> STATES = List.of("STARTED", "EXISTENCE_CHECKED", "OFFERED", "CONSUMER_SIGNED",
            "PROVIDER_SIGNED", "CONSUMER_VERIFIED", "FINALIZED", "TERMINATED");

    public record Step(String state, Instant at) {
    }

    public String contractNegotiationId;
    public String contractAgreementId;
    public String contractDefinitionId;
    public String contractOfferId;
    public String providerId;
    public String consumerId;
    public String assetId;
    public volatile String state;
    public volatile String terminationReason;
    public final List<Step> steps = new CopyOnWriteArrayList<>();
    @JsonIgnore
    public String stopAfter;
    @JsonIgnore
    public final Semaphore advance = new Semaphore(0);

    void enter(String newState) {
        state = newState;
        steps.add(new Step(newState, Instant.now()));
    }
}
