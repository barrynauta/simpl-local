package eu.simpl.cm.stubs;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

/** One negotiation driven by the stub (C.2). */
public class Negotiation {

    public record Step(String state, String at) {
    }

    final String contractNegotiationId;
    final String contractAgreementId;
    final String contractDefinitionId;
    final String contractOfferId;
    final String providerId;
    final String consumerId;
    final String assetId;
    final String stopAfter;

    volatile String state;
    volatile String terminationReason;
    final List<Step> steps = new CopyOnWriteArrayList<>();
    private final LinkedBlockingQueue<StubState.Callback> inbox = new LinkedBlockingQueue<>();
    private final Semaphore advance = new Semaphore(0);

    Negotiation(String nid, String aid, String did, String offerId, String providerId, String consumerId,
                String assetId, String stopAfter) {
        this.contractNegotiationId = nid;
        this.contractAgreementId = aid;
        this.contractDefinitionId = did;
        this.contractOfferId = offerId;
        this.providerId = providerId;
        this.consumerId = consumerId;
        this.assetId = assetId;
        this.stopAfter = stopAfter;
    }

    synchronized void enter(String newState) {
        state = newState;
        steps.add(new Step(newState, Instant.now().toString()));
    }

    void deliver(StubState.Callback cb) {
        inbox.add(cb);
    }

    /** Waits for the first callback that matches; null on timeout. A timeout of 0 means no limit. */
    StubState.Callback await(Predicate<StubState.Callback> match, long timeoutMillis) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        while (true) {
            StubState.Callback cb;
            if (timeoutMillis <= 0) {
                cb = inbox.take();
            } else {
                long left = deadline - System.currentTimeMillis();
                if (left <= 0) {
                    return null;
                }
                cb = inbox.poll(left, TimeUnit.MILLISECONDS);
                if (cb == null) {
                    return null;
                }
            }
            if (match.test(cb)) {
                return cb;
            }
        }
    }

    void pauseIfRequested(String reached) throws InterruptedException {
        if (stopAfter != null && stopAfter.equals(reached)) {
            advance.acquire();
        }
    }

    void advance() {
        advance.release();
    }
}
