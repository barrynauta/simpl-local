package eu.simpl.catalogue.common;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdvancedQueryTest {
    static final JsonNode VC = Json.parse("""
        {"credentialSubject": {"@id": "did:web:x:1", "rdf:type": {"@id": "simpl:DataOffering"},
          "simpl:generalServiceProperties": {"rdf:type": {"@id": "simpl:GeneralServiceProperties"},
             "simpl:name": "Climate Dataset", "simpl:offeringType": "data"},
          "simpl:offeringPrice": {"rdf:type": {"@id": "simpl:OfferingPrice"},
             "simpl:price": {"@type": "xsd:decimal", "@value": 100}}}}""");

    @Test
    void stringCriteriaMatchCaseInsensitiveSubstring() {
        assertThat(AdvancedQuery.parse("{\"simpl:GeneralServiceProperties\": {\"name\": \"climate\"}}").matches(VC)).isTrue();
        assertThat(AdvancedQuery.parse("{\"simpl:GeneralServiceProperties\": {\"name\": \"weather\"}}").matches(VC)).isFalse();
    }

    @Test
    void rangeCriteriaCompareNumerically() {
        assertThat(AdvancedQuery.parse("{\"simpl:OfferingPrice\": {\"price\": {\"op1\": \">=\", \"value1\": \"15\", \"op2\": \"<\", \"value2\": 150}}}")
                .matches(VC)).isTrue();
        assertThat(AdvancedQuery.parse("{\"simpl:OfferingPrice\": {\"price\": {\"op1\": \">\", \"value1\": 100}}}").matches(VC)).isFalse();
        assertThat(AdvancedQuery.parse("{\"simpl:OfferingPrice\": {\"price\": 100}}").matches(VC)).isTrue();
    }

    @Test
    void allCriteriaAreAndCombinedAndMissingNodesDoNotMatch() {
        assertThat(AdvancedQuery.parse("{\"simpl:GeneralServiceProperties\": {\"name\": \"climate\", \"offeringType\": \"app\"}}").matches(VC)).isFalse();
        assertThat(AdvancedQuery.parse("{\"simpl:ContractTemplate\": {\"contractTemplateDocument\": \"x\"}}").matches(VC)).isFalse();
    }

    @Test
    void reservedAndAtKeysAreIgnored() {
        AdvancedQuery q = AdvancedQuery.parse("{\"catalogueIds\": [\"a\"], \"minimumScore\": 5, "
                + "\"simpl:GeneralServiceProperties\": {\"@type\": \"x\", \"name\": \"Climate\"}}");
        assertThat(q.nodes()).hasSize(1);
        assertThat(q.matches(VC)).isTrue();
    }

    @Test
    void contractErrorsAre400WithContractDetails() {
        assertThatThrownBy(() -> AdvancedQuery.parse("{\"simpl:ContractTemplate\": {}}"))
                .isInstanceOf(ApiException.class)
                .hasMessage("No Properties defined in simpl:ContractTemplate node. Please provide at least one property with searched value.");
        assertThatThrownBy(() -> AdvancedQuery.parse("{\"simpl:OfferingPrice\": {\"price\": {\"customOperator\": \">=\", \"value1\": \"15\"}}}"))
                .hasMessage("Wrong property names in node simpl:OfferingPrice. Allowed property names: op1, op2, value1, value2");
        assertThatThrownBy(() -> AdvancedQuery.parse("{\"simpl:OfferingPrice\": {\"price\": {\"op1\": \"~\", \"value1\": 1}}}"))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> AdvancedQuery.parse("{\"simpl:OfferingPrice\": {\"price\": {\"op1\": \">\", \"value1\": \"abc\"}}}"))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> AdvancedQuery.parse("[1]")).isInstanceOf(ApiException.class);
    }
}
