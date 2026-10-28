package eu.simpl.catalogue.cbe.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.simpl.catalogue.cbe.Downstream;
import eu.simpl.catalogue.common.ApiException;
import eu.simpl.catalogue.common.Caller;
import eu.simpl.catalogue.common.Http;
import eu.simpl.catalogue.common.Json;
import eu.simpl.catalogue.common.ProblemTypes;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.net.URI;
import java.util.Set;

/** Detail through qma, flattened to the VC plus qualityAssessment for own RDs (D43, D57). */
final class Detail {
    private static final Set<Integer> PASS = Set.of(400, 403, 404, 502, 504);

    private Detail() {}

    static ResponseEntity<String> fetch(Downstream down, String qma, Caller caller, String id, String catalogueId) {
        URI uri = Http.query().add("catalogueId", catalogueId)
                .uri(qma + "/governanceAuthority/resourceDescriptions/" + Http.seg(id));
        Downstream.Res r = down.call(HttpMethod.GET, uri, caller.bearer(), null, "application/json");
        if (!r.ok()) {
            if (PASS.contains(r.status())) throw new ApiException(r.status(), Http.problemBody(r.status(), r.body()));
            throw new ApiException(ProblemTypes.INTERNAL, "Unexpected internal error");
        }
        JsonNode body = Json.parseOrNull(r.body());
        JsonNode rd = body == null ? null : body.get("resourceDescription");
        if (rd == null || !rd.isObject()) throw new ApiException(ProblemTypes.INTERNAL, "Unexpected internal error");
        ObjectNode out = ((ObjectNode) rd).deepCopy();
        JsonNode qa = body.get("qualityAssessment");
        if (qa != null && !qa.isNull()) out.set("qualityAssessment", qa);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, "application/json").body(Json.write(out));
    }
}
