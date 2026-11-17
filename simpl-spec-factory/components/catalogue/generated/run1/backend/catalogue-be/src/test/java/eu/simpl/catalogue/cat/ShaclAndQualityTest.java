package eu.simpl.catalogue.cat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.simpl.catalogue.cat.quality.Qar;
import eu.simpl.catalogue.cat.quality.QualityEvaluator;
import eu.simpl.catalogue.cat.quality.QualityProfile;
import eu.simpl.catalogue.cat.rdf.JsonLdSupport;
import eu.simpl.catalogue.cat.rdf.ShapeSupport;
import eu.simpl.catalogue.common.Json;
import org.apache.jena.graph.Graph;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ShaclAndQualityTest {
    static String shape() throws Exception {
        return Files.readString(Path.of("../../env/seed/schemas/DataOfferingShape.ttl"));
    }

    @Test
    void seedShapeIsDetectedAndAValidRdConforms() throws Exception {
        Graph sg = ShapeSupport.parseTurtle(shape());
        assertThat(ShapeSupport.offeringShapeId(sg)).isEqualTo("DataOfferingShape");
        JsonNode vc = TestVc.signed("provider-a", "provider-a", "t1", "CONSUMER");
        JsonLdSupport.checkExpands(vc);
        List<String> v = ShapeSupport.validate(sg, "DataOfferingShape", JsonLdSupport.subjectGraph(vc), TestVc.id(vc));
        assertThat(v).isEmpty();
    }

    @Test
    void violationsNameTheNestedPath() throws Exception {
        ObjectNode vc = TestVc.unsigned("provider-a", "provider-a", "t1", "CONSUMER");
        ((ObjectNode) vc.get("credentialSubject").get("simpl:generalServiceProperties")).remove("simpl:name");
        ((ObjectNode) vc.get("credentialSubject").get("simpl:generalServiceProperties")).put("simpl:offeringType", "other");
        List<String> v = ShapeSupport.validate(ShapeSupport.parseTurtle(shape()), "DataOfferingShape",
                JsonLdSupport.subjectGraph(vc), TestVc.id(vc));
        assertThat(String.join(";", v)).contains("simpl:generalServiceProperties/simpl:name")
                .contains("simpl:generalServiceProperties/simpl:offeringType");
    }

    @Test
    void remoteContextsAreRefused() {
        ObjectNode vc = TestVc.unsigned("provider-a", "provider-a", "t1", "CONSUMER");
        vc.putArray("@context").add("https://www.w3.org/2018/credentials/v1").add("https://example.org/other");
        assertThatThrownBy(() -> JsonLdSupport.checkExpands(vc)).isInstanceOf(IllegalArgumentException.class);
    }

    static final String PROFILE = """
        {"@graph": [
          {"@id": "urn:simpl:mqp:default", "@type": "MetadataQualityProfile", "profileVersion": "1.0.0",
           "includesDimension": [{"@id": "urn:d:completeness"}, {"@id": "urn:d:accuracy"}],
           "includesThreshold": [{"@id": "urn:t:overall"}, {"@id": "urn:t:completeness"}]},
          {"@id": "urn:d:completeness", "@type": "QualityDimension", "name": "Completeness"},
          {"@id": "urn:r:red", "@type": "ScoreRange", "minScore": 0, "maxScore": 60, "rangeClassification": {"@id": "urn:c:Red"}},
          {"@id": "urn:r:green", "@type": "ScoreRange", "minScore": {"@value": "80", "@type": "xsd:decimal"}, "maxScore": 100, "rangeClassification": {"@id": "urn:c:Green"}},
          {"@id": "urn:r:yellow", "@type": "ScoreRange", "minScore": 60, "maxScore": 80, "rangeClassification": {"@id": "urn:c:Yellow"}},
          {"@id": "urn:t:overall", "@type": "Threshold", "thresholdValue": 70, "classificationScope": "overall"},
          {"@id": "urn:t:completeness", "@type": "Threshold", "thresholdValue": 90, "classificationScope": {"@id": "urn:d:completeness"}}
        ]}""";

    static JsonNode qar(double overall, double completeness) {
        return Json.parse("""
            {"@graph": [
              {"@id": "rd1", "@type": "simpl:DataOffering"},
              {"@id": "s1", "@type": "SelfDescriptionScore", "computedOn": "rd1", "score": %s, "maxScore": 100},
              {"@id": "d1", "@type": "DimensionScore", "dimension": "urn:d:completeness", "score": %s, "maxScore": 100},
              {"@id": "e1", "@type": "RuleEvaluation", "result": "passed"},
              {"@id": "q1", "@type": "QualityAssessmentRecord", "computedOn": "rd1"}]}""".formatted(overall, completeness));
    }

    @Test
    void classificationUsesRangesWithTheTopRangeInclusive() {
        QualityProfile p = QualityProfile.parse(Json.parse(PROFILE));
        assertThat(p.labels()).containsExactly("Red", "Yellow", "Green");
        assertThat(p.classify(59.99)).isEqualTo("Red");
        assertThat(p.classify(60.0)).isEqualTo("Yellow");
        assertThat(p.classify(100.0)).isEqualTo("Green");
        assertThat(p.rank("green")).isGreaterThan(p.rank("YELLOW"));
    }

    @Test
    void thresholdsOverallFirstThenDimensionsWithTheStoryMessage() {
        QualityProfile p = QualityProfile.parse(Json.parse(PROFILE));
        assertThat(QualityEvaluator.firstFailure(Qar.parse(qar(65, 50), "rd1"), p))
                .contains("Quality Score 65 is below treshold 70.");
        assertThat(QualityEvaluator.firstFailure(Qar.parse(qar(72.5, 89.456), "rd1"), p))
                .contains("Quality Score 89.46 is below treshold 90.");
        assertThat(QualityEvaluator.firstFailure(Qar.parse(qar(70, 90), "rd1"), p)).isEmpty();
        ObjectNode summary = QualityEvaluator.summary(Qar.parse(qar(87.5, 95), "rd1"), p);
        assertThat(summary.path("overall").path("classification").asText()).isEqualTo("Green");
        assertThat(summary.path("dimensions").get(0).path("name").asText()).isEqualTo("Completeness");
        assertThat(summary.path("dimensions").get(0).path("thresholdStatus").asText()).isEqualTo("PASSED");
        assertThat(summary.path("rules").get(0).path("status").asText()).isEqualTo("PASSED");
    }

    @Test
    void invalidQarStructures() {
        assertThatThrownBy(() -> Qar.parse(Json.parse("{\"@graph\": []}"), "rd1")).isInstanceOf(Qar.InvalidQarException.class);
        assertThatThrownBy(() -> Qar.parse(Json.parse("{\"@graph\": [{\"@id\": \"x\"}]}"), "rd1")).isInstanceOf(Qar.InvalidQarException.class);
        assertThatThrownBy(() -> Qar.parse(qar(1, 1), "other")).isInstanceOf(Qar.InvalidQarException.class);
    }
}
