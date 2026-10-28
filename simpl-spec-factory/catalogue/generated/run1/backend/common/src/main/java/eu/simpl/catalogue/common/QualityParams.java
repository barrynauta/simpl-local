package eu.simpl.catalogue.common;

import com.fasterxml.jackson.databind.JsonNode;

/** Optional quality filters (D54). Empty values mean not set. */
public record QualityParams(Double minimumScore, String minimumClassification) {

    public static final String SCORE_DETAIL = "minimumScore must be greater than or equal to 0.";
    public static final String CLASSIFICATION_DETAIL = "minimumClassification is not a supported quality classification value.";

    public static QualityParams parse(String score, String classification, Double max) {
        Double s = null;
        if (score != null && !score.isBlank()) {
            try {
                s = Double.parseDouble(score.trim());
            } catch (NumberFormatException e) {
                throw ApiException.invalidInput("minimumScore must be a number.");
            }
            if (s.isNaN() || s.isInfinite()) throw ApiException.invalidInput("minimumScore must be a number.");
            if (s < 0) throw ApiException.invalidInput(SCORE_DETAIL);
            if (max != null && s > max) throw ApiException.invalidInput("minimumScore must be less than or equal to " + max.intValue() + ".");
        }
        String c = classification == null || classification.isBlank() ? null : classification.trim();
        return new QualityParams(s, c);
    }

    /** Read the root fields of an advanced search body. */
    public static QualityParams fromBody(JsonNode body, Double max) {
        JsonNode s = body.get("minimumScore");
        JsonNode c = body.get("minimumClassification");
        String score = s == null || s.isNull() ? null : s.asText();
        if (s != null && !s.isNull() && !s.isNumber() && !s.isTextual()) {
            throw ApiException.invalidInput("minimumScore must be a number.");
        }
        String cls = c == null || c.isNull() ? null : c.asText();
        return parse(score, cls, max);
    }

    public boolean isSet() { return minimumScore != null || minimumClassification != null; }
}
