package eu.simpl.cm.manager;

import eu.simpl.cm.manager.domain.ConsentStatus;
import eu.simpl.cm.manager.domain.ContractRecord;
import eu.simpl.cm.manager.domain.ContractStatus;
import eu.simpl.cm.manager.domain.LifecycleAction;
import eu.simpl.cm.manager.domain.PartyRole;
import eu.simpl.cm.manager.domain.StateMachine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static eu.simpl.cm.manager.domain.ContractStatus.*;
import static org.assertj.core.api.Assertions.assertThat;

/** D37 lifecycle table L1 to L7, allowedActions and the D38 mapping. */
class StateMachineTest {

    @ParameterizedTest(name = "{0} from {1} by {2} (revocation by {3}) -> {4}")
    @CsvSource({
            "SUSPEND, ACTIVE, PROVIDER, , true",
            "SUSPEND, ACTIVE, CONSUMER, , false",
            "SUSPEND, SUSPENDED, PROVIDER, , false",
            "REQUEST_REACTIVATION, SUSPENDED, CONSUMER, , true",
            "REQUEST_REACTIVATION, SUSPENDED, PROVIDER, , false",
            "REACTIVATE, SUSPENDED, PROVIDER, , true",
            "REACTIVATE, REACTIVATION_REQUESTED, PROVIDER, , true",
            "REACTIVATE, ACTIVE, PROVIDER, , false",
            "REQUEST_REVOCATION, ACTIVE, PROVIDER, , true",
            "REQUEST_REVOCATION, ACTIVE, CONSUMER, , true",
            "REQUEST_REVOCATION, SUSPENDED, CONSUMER, , false",
            "ACCEPT_REVOCATION, REVOCATION_REQUESTED, CONSUMER, PROVIDER, true",
            "ACCEPT_REVOCATION, REVOCATION_REQUESTED, PROVIDER, PROVIDER, false",
            "DENY_REVOCATION, REVOCATION_REQUESTED, PROVIDER, CONSUMER, true",
            "DENY_REVOCATION, REVOCATION_REQUESTED, CONSUMER, CONSUMER, false",
            "TERMINATE, ACTIVE, PROVIDER, , true",
            "TERMINATE, SUSPENDED, PROVIDER, , true",
            "TERMINATE, REACTIVATION_REQUESTED, PROVIDER, , true",
            "TERMINATE, REVOCATION_REQUESTED, PROVIDER, CONSUMER, true",
            "TERMINATE, ACTIVE, CONSUMER, , false",
            "TERMINATE, SIGNED, PROVIDER, , false",
            "TERMINATE, REVOKED, PROVIDER, , false",
            "TERMINATE, TERMINATED, PROVIDER, , false"
    })
    void lifecycleTable(LifecycleAction action, ContractStatus from, PartyRole actor, PartyRole requestedBy,
                        boolean allowed) {
        assertThat(action.allowed(from, actor, requestedBy)).isEqualTo(allowed);
    }

    @Test
    void targetsAndReasonRules() {
        assertThat(LifecycleAction.SUSPEND.to()).isEqualTo(SUSPENDED);
        assertThat(LifecycleAction.REQUEST_REACTIVATION.to()).isEqualTo(REACTIVATION_REQUESTED);
        assertThat(LifecycleAction.REACTIVATE.to()).isEqualTo(ACTIVE);
        assertThat(LifecycleAction.REQUEST_REVOCATION.to()).isEqualTo(REVOCATION_REQUESTED);
        assertThat(LifecycleAction.ACCEPT_REVOCATION.to()).isEqualTo(REVOKED);
        assertThat(LifecycleAction.DENY_REVOCATION.to()).isEqualTo(ACTIVE);
        assertThat(LifecycleAction.TERMINATE.to()).isEqualTo(TERMINATED);
        assertThat(LifecycleAction.REACTIVATE.reasonRequired()).isFalse();
        assertThat(LifecycleAction.ACCEPT_REVOCATION.reasonRequired()).isFalse();
        assertThat(LifecycleAction.DENY_REVOCATION.reasonRequired()).isTrue();
        assertThat(LifecycleAction.fromMessageType("REVOCATION_ACCEPTED")).contains(LifecycleAction.ACCEPT_REVOCATION);
    }

    @Test
    void terminalStatesAllowNothing() {
        for (ContractStatus s : ContractStatus.TERMINAL) {
            for (LifecycleAction a : LifecycleAction.values()) {
                for (PartyRole r : PartyRole.values()) {
                    assertThat(a.allowed(s, r, PartyRole.PROVIDER)).isFalse();
                }
            }
        }
    }

    @Test
    void technicalStatusMappingD38() {
        assertThat(REQUESTED.technical()).isEqualTo("CREATED");
        assertThat(ACCEPTED.technical()).isEqualTo("INITIATED");
        assertThat(SIGNED.technical()).isEqualTo("FINALIZING");
        for (ContractStatus s : ContractStatus.ESTABLISHED) {
            assertThat(s.technical()).isEqualTo("FINALIZED");
        }
        for (ContractStatus s : ContractStatus.TERMINAL) {
            assertThat(s.technical()).isEqualTo("TERMINATED");
        }
    }

    private static ContractRecord record(PartyRole role, ContractStatus status, ConsentStatus consent) {
        ContractRecord r = new ContractRecord();
        r.partyRole = role;
        r.status = status;
        r.consentStatus = consent;
        return r;
    }

    @Test
    void allowedActions() {
        assertThat(StateMachine.allowedActions(record(PartyRole.CONSUMER, REQUESTED, ConsentStatus.NOT_DEFINED)))
                .containsExactly("ACCEPT", "REJECT");
        assertThat(StateMachine.allowedActions(record(PartyRole.PROVIDER, REQUESTED, ConsentStatus.NOT_DEFINED)))
                .isEmpty();
        assertThat(StateMachine.allowedActions(record(PartyRole.PROVIDER, ACTIVE, ConsentStatus.PENDING)))
                .containsExactly("SUSPEND", "REQUEST_REVOCATION", "TERMINATE", "UPDATE_CONSENT");
        assertThat(StateMachine.allowedActions(record(PartyRole.CONSUMER, SUSPENDED, ConsentStatus.PENDING)))
                .containsExactly("REQUEST_REACTIVATION");
        assertThat(StateMachine.allowedActions(record(PartyRole.PROVIDER, FAILED, ConsentStatus.PENDING))).isEmpty();
    }
}
