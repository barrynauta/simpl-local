package eu.simpl.catalogue.common;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AccessPolicyTest {
    static JsonNode vc(String policy) {
        var n = Json.object();
        var p = n.putObject("credentialSubject").putObject("simpl:servicePolicy");
        if (policy != null) p.put("simpl:access-policy", policy);
        return n;
    }

    static final String SEARCH_CONSUMER = "{\"permission\":[{\"action\":[\"http://simpl.eu/odrl/actions/search\"],"
            + "\"assignee\":{\"uid\":\"CONSUMER\"},\"constraint\":[{\"leftOperand\":\"x\"}]}]}";

    @Test
    void searchPermissionForAnAttributeOfTheCaller() {
        assertThat(AccessPolicy.discoverable(vc(SEARCH_CONSUMER), List.of("RESEARCHER", "CONSUMER"))).isTrue();
        assertThat(AccessPolicy.discoverable(vc(SEARCH_CONSUMER), List.of("PROVIDER"))).isFalse();
        assertThat(AccessPolicy.discoverable(vc(SEARCH_CONSUMER), List.of())).isFalse();
    }

    @Test
    void otherActionsMissingOrBrokenPoliciesAreNotDiscoverable() {
        String use = SEARCH_CONSUMER.replace("http://simpl.eu/odrl/actions/search", "http://www.w3.org/ns/odrl/2/use");
        assertThat(AccessPolicy.discoverable(vc(use), List.of("CONSUMER"))).isFalse();
        assertThat(AccessPolicy.discoverable(vc(null), List.of("CONSUMER"))).isFalse();
        assertThat(AccessPolicy.discoverable(vc("not json"), List.of("CONSUMER"))).isFalse();
    }
}
