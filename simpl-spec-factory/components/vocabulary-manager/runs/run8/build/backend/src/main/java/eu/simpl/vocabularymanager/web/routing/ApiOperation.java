package eu.simpl.vocabularymanager.web.routing;

import java.util.Set;
import org.springframework.http.HttpMethod;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * One operation of the published contract, with its role rule and request shape. The catalogue of these is the single
 * source for routing (405), authorisation (D16, D84) and request-shape checks (D21, D52, D63).
 *
 * @param operationId the OpenAPI operationId
 * @param method the HTTP method
 * @param path the full path template, including the /v1 base
 * @param access who may call it
 * @param requestBody the request body kind
 * @param response the success response kind
 * @param queryParameters the query parameters the operation declares
 */
public record ApiOperation(
        String operationId,
        HttpMethod method,
        String path,
        Access access,
        RequestBody requestBody,
        ResponseKind response,
        Set<String> queryParameters) {

    /** Who may call an operation. */
    public enum Access {
        /** No token needed (GET /health only). */
        PUBLIC,
        /** GA_VOCABULARY_VIEWER or GA_VOCABULARY_ADMIN. */
        READ,
        /** GA_VOCABULARY_ADMIN. */
        WRITE
    }

    /** Request body kinds. */
    public enum RequestBody {
        NONE,
        JSON,
        MULTIPART
    }

    /** Success response kinds, used for the Accept check (D63). */
    public enum ResponseKind {
        JSON,
        TURTLE
    }

    /**
     * @return the parsed path pattern
     */
    public PathPattern pattern() {
        return PathPatternParser.defaultInstance.parse(path);
    }

    /**
     * @param parameter a query parameter name
     * @return true when the operation declares it
     */
    public boolean declaresQueryParameter(String parameter) {
        return queryParameters.contains(parameter);
    }
}
