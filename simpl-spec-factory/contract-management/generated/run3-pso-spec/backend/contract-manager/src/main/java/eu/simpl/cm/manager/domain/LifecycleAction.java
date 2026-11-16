package eu.simpl.cm.manager.domain;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

import static eu.simpl.cm.manager.domain.ContractStatus.ACTIVE;
import static eu.simpl.cm.manager.domain.ContractStatus.REACTIVATION_REQUESTED;
import static eu.simpl.cm.manager.domain.ContractStatus.REVOCATION_REQUESTED;
import static eu.simpl.cm.manager.domain.ContractStatus.REVOKED;
import static eu.simpl.cm.manager.domain.ContractStatus.SUSPENDED;
import static eu.simpl.cm.manager.domain.ContractStatus.TERMINATED;

/** The lifecycle transitions L1 to L7 of D37. */
public enum LifecycleAction {
    SUSPEND(EnumSet.of(ACTIVE), SUSPENDED, Who.PROVIDER, true, "SUSPENDED"),
    REQUEST_REACTIVATION(EnumSet.of(SUSPENDED), REACTIVATION_REQUESTED, Who.CONSUMER, true, "REACTIVATION_REQUESTED"),
    REACTIVATE(EnumSet.of(SUSPENDED, REACTIVATION_REQUESTED), ACTIVE, Who.PROVIDER, false, "REACTIVATED"),
    REQUEST_REVOCATION(EnumSet.of(ACTIVE), REVOCATION_REQUESTED, Who.EITHER, true, "REVOCATION_REQUESTED"),
    ACCEPT_REVOCATION(EnumSet.of(REVOCATION_REQUESTED), REVOKED, Who.NOT_REQUESTER, false, "REVOCATION_ACCEPTED"),
    DENY_REVOCATION(EnumSet.of(REVOCATION_REQUESTED), ACTIVE, Who.NOT_REQUESTER, true, "REVOCATION_DENIED"),
    TERMINATE(EnumSet.of(ACTIVE, SUSPENDED, REACTIVATION_REQUESTED, REVOCATION_REQUESTED), TERMINATED, Who.PROVIDER,
            true, "TERMINATED");

    enum Who { PROVIDER, CONSUMER, EITHER, NOT_REQUESTER }

    private final Set<ContractStatus> from;
    private final ContractStatus to;
    private final Who who;
    private final boolean reasonRequired;
    private final String messageType;

    LifecycleAction(Set<ContractStatus> from, ContractStatus to, Who who, boolean reasonRequired, String messageType) {
        this.from = from;
        this.to = to;
        this.who = who;
        this.reasonRequired = reasonRequired;
        this.messageType = messageType;
    }

    public ContractStatus to() {
        return to;
    }

    public boolean reasonRequired() {
        return reasonRequired;
    }

    public String messageType() {
        return messageType;
    }

    /**
     * Whether a party with role {@code actorRole} may perform this action on a record in {@code status}
     * whose revocation was requested by {@code revocationRequestedBy} (may be null).
     */
    public boolean allowed(ContractStatus status, PartyRole actorRole, PartyRole revocationRequestedBy) {
        if (!from.contains(status)) {
            return false;
        }
        return switch (who) {
            case PROVIDER -> actorRole == PartyRole.PROVIDER;
            case CONSUMER -> actorRole == PartyRole.CONSUMER;
            case EITHER -> true;
            case NOT_REQUESTER -> revocationRequestedBy != null && actorRole != revocationRequestedBy;
        };
    }

    public static Optional<LifecycleAction> parse(String s) {
        if (s == null) {
            return Optional.empty();
        }
        for (LifecycleAction a : values()) {
            if (a.name().equals(s)) {
                return Optional.of(a);
            }
        }
        return Optional.empty();
    }

    public static Optional<LifecycleAction> fromMessageType(String type) {
        for (LifecycleAction a : values()) {
            if (a.messageType.equals(type)) {
                return Optional.of(a);
            }
        }
        return Optional.empty();
    }
}
