package eu.simpl.cm.manager.domain;

import java.util.EnumSet;
import java.util.Set;

/** D37 status enum and the sets used elsewhere. */
public enum ContractStatus {
    REQUESTED, ACCEPTED, REJECTED, SIGNED, ACTIVE, FAILED, SUSPENDED, REACTIVATION_REQUESTED, REVOCATION_REQUESTED,
    REVOKED, TERMINATED;

    public static final Set<ContractStatus> ESTABLISHED = EnumSet.of(ACTIVE, SUSPENDED, REACTIVATION_REQUESTED, REVOCATION_REQUESTED);
    public static final Set<ContractStatus> OPEN = EnumSet.of(REQUESTED, ACCEPTED, SIGNED, ACTIVE, SUSPENDED,
            REACTIVATION_REQUESTED, REVOCATION_REQUESTED);
    public static final Set<ContractStatus> TERMINAL = EnumSet.of(REJECTED, FAILED, REVOKED, TERMINATED);

    public boolean isOpen() {
        return OPEN.contains(this);
    }

    public boolean isEstablished() {
        return ESTABLISHED.contains(this);
    }

    public boolean isTerminal() {
        return TERMINAL.contains(this);
    }

    /** D38 mapping to the OpenAPI technical status. */
    public String technical() {
        if (this == REQUESTED) {
            return "CREATED";
        }
        if (this == ACCEPTED) {
            return "INITIATED";
        }
        if (this == SIGNED) {
            return "FINALIZING";
        }
        if (isEstablished()) {
            return "FINALIZED";
        }
        return "TERMINATED";
    }
}
