package eu.simpl.vocabularymanager.api;

import eu.simpl.vocabularymanager.error.ApiException;
import eu.simpl.vocabularymanager.error.ProblemIssue;
import eu.simpl.vocabularymanager.error.Problems;
import eu.simpl.vocabularymanager.security.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Set;
import org.slf4j.MDC;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/** Small helpers shared by the controllers. */
final class ApiSupport {

    static final String JSON = MediaType.APPLICATION_JSON_VALUE;
    static final String PROBLEM_JSON = MediaType.APPLICATION_PROBLEM_JSON_VALUE;
    static final String TURTLE = "text/turtle";
    static final String MULTIPART = MediaType.MULTIPART_FORM_DATA_VALUE;
    static final Set<String> PAGING = Set.of("page", "pageSize");

    private ApiSupport() {}

    static AuthenticatedUser user(HttpServletRequest request) {
        return (AuthenticatedUser) request.getAttribute(AuthenticatedUser.REQUEST_ATTRIBUTE);
    }

    static void failOnIssues(List<ProblemIssue> issues) {
        if (!issues.isEmpty()) {
            throw Problems.badRequest(issues);
        }
    }

    /** Rejects every query parameter (POST, PATCH, DELETE and operations without parameters, D52). */
    static void noQuery(HttpServletRequest request, List<ProblemIssue> issues) {
        QueryParameters.read(request, Set.of(), issues);
    }

    /** Checks a path name; the issue is named after the path parameter (D49). */
    static String pathName(String value, String parameter, List<ProblemIssue> issues) {
        String name = RequestRules.name(value, "path", parameter, issues);
        if (name != null) {
            MDC.put("vocabularyName", name);
        }
        return name;
    }

    static Integer pathVersion(String value, List<ProblemIssue> issues) {
        Integer version = RequestRules.positiveInt(value, "path", "version", RequestRules.INT32_MAX, issues);
        if (version != null) {
            MDC.put("version", version.toString());
        }
        return version;
    }

    /** Paging parameters with their defaults (D22). Returns {page, pageSize}. */
    static int[] paging(QueryParameters query, List<ProblemIssue> issues) {
        int page = 1;
        int pageSize = RequestRules.PAGE_SIZE_DEFAULT;
        if (query.get("page") != null) {
            Integer value =
                    RequestRules.positiveInt(query.get("page"), "query", "page", RequestRules.INT32_MAX, issues);
            page = value == null ? page : value;
        }
        if (query.get("pageSize") != null) {
            Integer value = RequestRules.positiveInt(
                    query.get("pageSize"), "query", "pageSize", RequestRules.PAGE_SIZE_MAX, issues);
            pageSize = value == null ? pageSize : value;
        }
        return new int[] {page, pageSize};
    }

    static <T> ResponseEntity<T> json(T body) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(body);
    }

    /** A Turtle download: bytes as stored, Content-Type exactly text/turtle (D29, D70). */
    static ResponseEntity<byte[]> turtle(byte[] content, String fileName) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_TYPE, TURTLE);
        headers.setContentDisposition(
                ContentDisposition.attachment().filename(fileName).build());
        return ResponseEntity.ok().headers(headers).body(content);
    }

    static ApiException missing(String field) {
        return Problems.badRequest(ProblemIssue.schemaViolation("body", field, "'" + field + "' is required.", null));
    }
}
