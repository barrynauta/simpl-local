package eu.simpl.vocabularymanager.api;

import eu.simpl.vocabularymanager.problem.Issue;
import eu.simpl.vocabularymanager.service.VocabularyService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Content downloads (decision D29): the stored Turtle bytes as uploaded. */
@RestController
@RequestMapping("/v1")
public class ContentController {

    /** Media type of vocabulary content. */
    public static final MediaType TURTLE = MediaType.parseMediaType("text/turtle");

    private final VocabularyService service;

    /**
     * Creates the controller.
     *
     * @param service the vocabulary service
     */
    public ContentController(VocabularyService service) {
        this.service = service;
    }

    /**
     * GET /content/{vocabularyName} (downloadVocabulary): the latest version.
     *
     * @param vocabularyName the name
     * @param request the request
     * @return the Turtle bytes
     */
    @GetMapping("/content/{vocabularyName}")
    public ResponseEntity<byte[]> latest(@PathVariable String vocabularyName, HttpServletRequest request) {
        RequestChecks.requireAcceptable(request, TURTLE);
        List<Issue> issues = new ArrayList<>();
        RequestChecks.checkQueryNames(RequestChecks.queryParameters(request, issues), Set.of(), issues);
        RequestChecks.checkPathName("vocabularyName", vocabularyName, issues);
        RequestChecks.throwIfAny(issues);
        return turtle(service.latestContent(vocabularyName), vocabularyName + ".ttl");
    }

    /**
     * GET /content/{vocabularyName}/versions/{version} (downloadVocabularyVersion).
     *
     * @param vocabularyName the name
     * @param version the version number
     * @param request the request
     * @return the Turtle bytes
     */
    @GetMapping("/content/{vocabularyName}/versions/{version}")
    public ResponseEntity<byte[]> version(
            @PathVariable String vocabularyName, @PathVariable String version, HttpServletRequest request) {
        RequestChecks.requireAcceptable(request, TURTLE);
        List<Issue> issues = new ArrayList<>();
        RequestChecks.checkQueryNames(RequestChecks.queryParameters(request, issues), Set.of(), issues);
        RequestChecks.checkPathName("vocabularyName", vocabularyName, issues);
        RequestChecks.checkPathVersion(version, issues);
        RequestChecks.throwIfAny(issues);
        byte[] content = service.versionContent(vocabularyName, version);
        return turtle(content, vocabularyName + "-v" + Long.parseLong(version) + ".ttl");
    }

    /**
     * Builds a Turtle download response.
     *
     * @param content the bytes
     * @param filename the attachment filename
     * @return the response
     */
    static ResponseEntity<byte[]> turtle(byte[] content, String filename) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(TURTLE);
        headers.setContentDisposition(
                ContentDisposition.attachment().filename(filename).build());
        return ResponseEntity.ok().headers(headers).body(content);
    }
}
