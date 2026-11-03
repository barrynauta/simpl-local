package eu.simpl.cm.manager.domain;

import eu.simpl.cm.manager.service.LifecycleService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.EnumSet;

import static eu.simpl.cm.manager.domain.ContractStatus.*;
import static eu.simpl.cm.manager.domain.PartyRole.CONSUMER;
import static eu.simpl.cm.manager.domain.PartyRole.PROVIDER;
import static org.assertj.core.api.Assertions.assertThat;

/** D37 tables and D38 mapping. */
class StateMachineTest {

    @Test
    void sets() {
        assertThat(ContractStatus.ESTABLISHED).containsExactlyInAnyOrder(ACTIVE, SUSPENDED, REACTIVATION_REQUESTED, REVOCATION_REQUESTED);
        assertThat(ContractStatus.TERMINAL).containsExactlyInAnyOrder(REJECTED, FAILED, REVOKED, TERMINATED);
        assertThat(ContractStatus.OPEN).containsExactlyInAnyOrder(REQUESTED, ACCEPTED, SIGNED, ACTIVE, SUSPENDED,
                REACTIVATION_REQUESTED, REVOCATION_REQUESTED);
    }

    @ParameterizedTest
    @CsvSource({"REQUESTED,CREATED", "ACCEPTED,INITIATED", "SIGNED,FINALIZING", "ACTIVE,FINALIZED", "SUSPENDED,FINALIZED",
            "REACTIVATION_REQUESTED,FINALIZED", "REVOCATION_REQUESTED,FINALIZED", "REJECTED,TERMINATED", "FAILED,TERMINATED",
            "REVOKED,TERMINATED", "TERMINATED,TERMINATED"})
    void technicalStatusD38(ContractStatus status, String technical) {
        assertThat(status.technical()).isEqualTo(technical);
    }

    /** Every (action, party, status) combination: exactly the L table is allowed. */
    @ParameterizedTest
    @CsvSource({
            "SUSPEND,PROVIDER,ACTIVE,SUSPENDED",
            "REQUEST_REACTIVATION,CONSUMER,SUSPENDED,REACTIVATION_REQUESTED",
            "REACTIVATE,PROVIDER,SUSPENDED,ACTIVE",
            "REACTIVATE,PROVIDER,REACTIVATION_REQUESTED,ACTIVE",
            "REQUEST_REVOCATION,PROVIDER,ACTIVE,REVOCATION_REQUESTED",
            "REQUEST_REVOCATION,CONSUMER,ACTIVE,REVOCATION_REQUESTED",
            "TERMINATE,PROVIDER,ACTIVE,TERMINATED",
            "TERMINATE,PROVIDER,SUSPENDED,TERMINATED",
            "TERMINATE,PROVIDER,REACTIVATION_REQUESTED,TERMINATED",
            "TERMINATE,PROVIDER,REVOCATION_REQUESTED,TERMINATED"})
    void allowedLifecycleTransitions(LifecycleAction action, PartyRole role, ContractStatus from, ContractStatus to) {
        assertThat(action.allowed(role, from, null)).isTrue();
        assertThat(action.to).isEqualTo(to);
    }

    @Test
    void everythingElseIsRefused() {
        int allowed = 0;
        for (LifecycleAction a : LifecycleAction.values()) {
            for (PartyRole role : PartyRole.values()) {
                for (ContractStatus s : ContractStatus.values()) {
                    for (PartyRole requestedBy : new PartyRole[]{null, PROVIDER, CONSUMER}) {
                        if (a.allowed(role, s, requestedBy)) {
                            allowed++;
                        }
                    }
                }
            }
        }
        // 10 rows above without revocation responders count 3 times (requestedBy null, PROVIDER, CONSUMER),
        // plus ACCEPT_REVOCATION and DENY_REVOCATION for the non-requesting party (2 actions x 2 parties).
        assertThat(allowed).isEqualTo(10 * 3 + 4);
    }

    @Test
    void onlyTheOtherPartyAnswersARevocationRequest() {
        assertThat(LifecycleAction.ACCEPT_REVOCATION.allowed(CONSUMER, REVOCATION_REQUESTED, PROVIDER)).isTrue();
        assertThat(LifecycleAction.ACCEPT_REVOCATION.allowed(PROVIDER, REVOCATION_REQUESTED, PROVIDER)).isFalse();
        assertThat(LifecycleAction.DENY_REVOCATION.allowed(PROVIDER, REVOCATION_REQUESTED, CONSUMER)).isTrue();
        assertThat(LifecycleAction.DENY_REVOCATION.allowed(CONSUMER, REVOCATION_REQUESTED, CONSUMER)).isFalse();
        assertThat(LifecycleAction.ACCEPT_REVOCATION.to).isEqualTo(REVOKED);
        assertThat(LifecycleAction.DENY_REVOCATION.to).isEqualTo(ACTIVE);
    }

    @Test
    void terminalStatesHaveNoExit() {
        for (ContractStatus s : ContractStatus.TERMINAL) {
            for (LifecycleAction a : LifecycleAction.values()) {
                for (PartyRole r : PartyRole.values()) {
                    assertThat(a.allowed(r, s, PROVIDER)).isFalse();
                    assertThat(a.allowed(r, s, CONSUMER)).isFalse();
                }
            }
        }
    }

    @Test
    void reasonRequirementsD39() {
        assertThat(EnumSet.allOf(LifecycleAction.class).stream().filter(a -> a.reasonRequired))
                .containsExactlyInAnyOrder(LifecycleAction.SUSPEND, LifecycleAction.REQUEST_REACTIVATION,
                        LifecycleAction.REQUEST_REVOCATION, LifecycleAction.DENY_REVOCATION, LifecycleAction.TERMINATE);
    }

    @Test
    void allowedActionsForDetails() {
        ContractRecord r = new ContractRecord();
        r.partyRole = CONSUMER;
        r.status = REQUESTED;
        r.consentStatus = "NOT_DEFINED";
        assertThat(LifecycleService.allowedActions(r)).containsExactly("ACCEPT", "REJECT");
        r.status = ACTIVE;
        assertThat(LifecycleService.allowedActions(r)).containsExactly("REQUEST_REVOCATION");
        r.partyRole = PROVIDER;
        r.consentStatus = "PENDING";
        assertThat(LifecycleService.allowedActions(r)).containsExactly("SUSPEND", "REQUEST_REVOCATION", "TERMINATE", "UPDATE_CONSENT");
        r.status = REQUESTED;
        assertThat(LifecycleService.allowedActions(r)).containsExactly("UPDATE_CONSENT");
        r.status = FAILED;
        assertThat(LifecycleService.allowedActions(r)).isEmpty();
    }
}
