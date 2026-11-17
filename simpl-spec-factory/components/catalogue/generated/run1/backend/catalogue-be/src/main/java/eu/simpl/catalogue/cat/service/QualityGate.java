package eu.simpl.catalogue.cat.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.simpl.catalogue.cat.quality.Qar;
import eu.simpl.catalogue.cat.quality.QualityClient;
import eu.simpl.catalogue.cat.quality.QualityEvaluator;
import eu.simpl.catalogue.cat.quality.QualityProfile;
import eu.simpl.catalogue.common.ApiException;
import eu.simpl.catalogue.common.ProblemTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** Quality gate at publication (SIMPL-27998, D50 to D52). */
@Component
public class QualityGate {
    private static final Logger log = LoggerFactory.getLogger(QualityGate.class);

    public record Outcome(JsonNode qar, ObjectNode summary, double overall, String classification) {}

    private final QualityClient client;

    public QualityGate(QualityClient client) {
        this.client = client;
    }

    public Outcome evaluate(JsonNode vc, String rdId, String schemaName) {
        JsonNode qarDoc;
        try {
            qarDoc = client.score(vc);
        } catch (QualityClient.ScoringException e) {
            log.warn("Quality gate failed for RD {}: scoring endpoint error: {}", rdId, e.getMessage());
            throw new ApiException(ProblemTypes.QUALITY_ASSESSMENT_FAILED,
                    "The metadata quality assessment could not be completed: the scoring service failed.");
        }
        Qar qar;
        try {
            qar = Qar.parse(qarDoc, rdId);
        } catch (Qar.InvalidQarException e) {
            log.warn("Quality gate failed for RD {}: invalid QAR: {}", rdId, e.getMessage());
            throw new ApiException(ProblemTypes.QUALITY_ASSESSMENT_FAILED,
                    "The metadata quality assessment returned an invalid Quality Assessment Record: " + e.getMessage());
        }
        Optional<QualityProfile> profile;
        try {
            profile = client.activeProfile(schemaName);
        } catch (RuntimeException e) {
            log.warn("Quality gate failed for RD {}: cannot read the quality profile: {}", rdId, e.getMessage());
            throw new ApiException(ProblemTypes.QUALITY_CONFIGURATION_ERROR,
                    "The active Metadata Quality Profile could not be read.");
        }
        if (profile.isEmpty()) {
            log.warn("Quality gate failed for RD {}: no active quality profile for schema {}", rdId, schemaName);
            throw new ApiException(ProblemTypes.QUALITY_CONFIGURATION_ERROR,
                    "No active Metadata Quality Profile is linked to schema '" + schemaName + "'.");
        }
        Optional<String> failure;
        try {
            failure = QualityEvaluator.firstFailure(qar, profile.get());
        } catch (QualityEvaluator.ConfigurationException e) {
            log.warn("Quality gate failed for RD {}: {}", rdId, e.getMessage());
            throw new ApiException(ProblemTypes.QUALITY_CONFIGURATION_ERROR, e.getMessage());
        } catch (Qar.InvalidQarException e) {
            log.warn("Quality gate failed for RD {}: invalid QAR: {}", rdId, e.getMessage());
            throw new ApiException(ProblemTypes.QUALITY_ASSESSMENT_FAILED, e.getMessage());
        }
        if (failure.isPresent()) {
            log.info("RD {} rejected by the quality gate: {}", rdId, failure.get());
            throw new ApiException(ProblemTypes.QUALITY_BELOW_THRESHOLD, failure.get());
        }
        ObjectNode summary = QualityEvaluator.summary(qar, profile.get());
        return new Outcome(qarDoc, summary, qar.overall, profile.get().classify(qar.overall));
    }

    /** QAR replacement through POST .../qualityAssessmentRecord: no gating (D55). */
    public Outcome summarise(JsonNode qarDoc, String rdId, String schemaName) {
        Qar qar = Qar.parse(qarDoc, rdId);
        QualityProfile profile;
        try {
            profile = client.activeProfile(schemaName).orElseGet(client::defaultProfile);
        } catch (RuntimeException e) {
            profile = client.defaultProfile();
        }
        ObjectNode summary = QualityEvaluator.summary(qar, profile);
        return new Outcome(qarDoc, summary, qar.overall, profile.classify(qar.overall));
    }
}
