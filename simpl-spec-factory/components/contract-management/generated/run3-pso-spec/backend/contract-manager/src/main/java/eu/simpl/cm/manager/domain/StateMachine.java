package eu.simpl.cm.manager.domain;

import java.util.ArrayList;
import java.util.List;

/** Derived views of the D37 state machine. */
public final class StateMachine {

    private StateMachine() {
    }

    /** allowedActions of A.3: ACCEPT/REJECT, the L actions, and UPDATE_CONSENT. */
    public static List<String> allowedActions(ContractRecord r) {
        List<String> out = new ArrayList<>();
        if (r.partyRole == PartyRole.CONSUMER && r.status == ContractStatus.REQUESTED) {
            out.add("ACCEPT");
            out.add("REJECT");
        }
        for (LifecycleAction a : LifecycleAction.values()) {
            if (a.allowed(r.status, r.partyRole, r.revocationRequestedBy)) {
                out.add(a.name());
            }
        }
        if (r.partyRole == PartyRole.PROVIDER && r.consentStatus != ConsentStatus.NOT_DEFINED && r.status.isOpen()) {
            out.add("UPDATE_CONSENT");
        }
        return out;
    }
}
