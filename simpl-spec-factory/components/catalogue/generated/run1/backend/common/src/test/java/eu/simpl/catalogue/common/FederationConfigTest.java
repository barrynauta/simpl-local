package eu.simpl.catalogue.common;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FederationConfigTest {
    @Test
    void mappingReplacesMappedCodesDropsOthersAndAddsAdditionalOnes() {
        FederationConfig.TrustedAuthority ta = new FederationConfig.TrustedAuthority();
        FederationConfig.AttributeMapping m = new FederationConfig.AttributeMapping();
        m.remoteAuthorityAttribute = "CONSUMER";
        m.localAuthorityAttribute = "PARTNER_CONSUMER";
        ta.attributeMapping = List.of(m);
        ta.additionalIdentityAttributes = List.of("FEDERATED");
        assertThat(FederationConfig.mapAttributes(ta, List.of("CONSUMER", "RESEARCHER")))
                .containsExactly("PARTNER_CONSUMER", "FEDERATED");
        assertThat(FederationConfig.mapAttributes(ta, FederationConfig.splitAttributes(""))).containsExactly("FEDERATED");
    }

    @Test
    void localIsAnAliasOfTheOwnCatalogue() {
        FederationConfig cfg = new FederationConfig();
        cfg.ownCatalogue = new FederationConfig.Catalogue();
        cfg.ownCatalogue.id = "catalogue-a";
        assertThat(cfg.isOwn("local")).isTrue();
        assertThat(cfg.isOwn("catalogue-a")).isTrue();
        assertThat(cfg.isOwn("catalogue-b")).isFalse();
    }
}
