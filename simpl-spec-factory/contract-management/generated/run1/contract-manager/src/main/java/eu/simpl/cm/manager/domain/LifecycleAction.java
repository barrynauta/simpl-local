package eu.simpl.cm.manager.domain;

import java.util.EnumSet;
import java.util.Set;

import static eu.simpl.cm.manager.domain.ContractStatus.ACTIVE;
import static eu.simpl.cm.manager.domain.ContractStatus.REACTIVATION_REQUESTED;
import static eu.simpl.cm.manager.domain.ContractStatus.REVOCATION_REQUESTED;
import static eu.simpl.cm.manager.domain.ContractStatus.REVOKED;
import static eu.simpl.cm.manager.domain.ContractStatus.SUSPENDED;
import static eu.simpl.cm.manager.domain.ContractStatus.TERMINATED;

/** D37 lifecycle table L1 to L7. */
public enum LifecycleAction {
    SUSPEND(EnumSet.of(ACTIVE), SUSPENDED, EnumSet.of(PartyRole.PROVIDER), true, "SUSPENDED", false),
    REQUEST_REACTIVATION(EnumSet.of(SUSPENDED), REACTIVATION_REQUESTED, EnumSet.of(PartyRole.CONSUMER), true, "REACTIVATION_REQUESTED", false),
    REACTIVATE(EnumSet.of(SUSPENDED, REACTIVATION_REQUESTED), ACTIVE, EnumSet.of(PartyRole.PROVIDER), false, "REACTIVATED", false),
    REQUEST_REVOCATION(EnumSet.of(ACTIVE), REVOCATION_REQUESTED, EnumSet.of(PartyRole.PROVIDER, PartyRole.CONSUMER), true, "REVOCATION_REQUESTED", false),
    ACCEPT_REVOCATION(EnumSet.of(REVOCATION_REQUESTED), REVOKED, EnumSet.of(PartyRole.PROVIDER, PartyRole.CONSUMER), false, "REVOCATION_ACCEPTED", true),
    DENY_REVOCATION(EnumSet.of(REVOCATION_REQUESTED), ACTIVE, EnumSet.of(PartyRole.PROVIDER, PartyRole.CONSUMER), true, "REVOCATION_DENIED", true),
    TERMINATE(EnumSet.copyOf(ContractStatus.ESTABLISHED), TERMINATED, EnumSet.of(PartyRole.PROVIDER), true, "TERMINATED", false);

    public final Set<ContractStatus> from;
    public final ContractStatus to;
    public final Set<PartyRole> actors;
    public final boolean reasonRequired;
    public final String messageType;
    /** Only the party that did not request the revocation may act. */
    public final boolean respondsToRevocation;

    LifecycleAction(Set<ContractStatus> from, ContractStatus to, Set<PartyRole> actors, boolean reasonRequired,
                    String messageType, boolean respondsToRevocation) {
        this.from = from;
        this.to = to;
        this.actors = actors;
        this.reasonRequired = reasonRequired;
        this.messageType = messageType;
        this.respondsToRevocation = respondsToRevocation;
    }

    /** May a party in {@code role} perform this action on a record in {@code status}? */
    public boolean allowed(PartyRole role, ContractStatus status, PartyRole revocationRequestedBy) {
        if (!actors.contains(role) || !from.contains(status)) {
            return false;
        }
        if (respondsToRevocation) {
            return revocationRequestedBy != null && revocationRequestedBy != role;
        }
        return true;
    }

    public static LifecycleAction byMessageType(String messageType) {
        for (LifecycleAction a : values()) {
            if (a.messageType.equals(messageType)) {
                return a;
            }
        }
        return null;
    }

    public static LifecycleAction parse(String s) {
        if (s == null) {
            return null;
        }
        try {
            return valueOf(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
