package eu.simpl.vocabularymanager.web;

import eu.simpl.vocabularymanager.service.VocabularyService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** Content downloads (D29, D70): stored bytes as uploaded, text/turtle without charset. */
@RestController
public class ContentController {

    static final String TURTLE = "text/turtle";

    private final VocabularyService service;

    public ContentController(VocabularyService service) {
        this.service = service;
    }

    @GetMapping(value = "/v1/content/{vocabularyName}", produces = TURTLE)
    public ResponseEntity<byte[]> downloadVocabulary(
            @PathVariable("vocabularyName") String vocabularyName, HttpServletRequest request) {
        RequestChecks.start()
                .pathName("vocabularyName", vocabularyName)
                .noQuery(request.getQueryString())
                .throwIfAny();
        return turtle(service.content(vocabularyName, null), vocabularyName + ".ttl");
    }

    @GetMapping(value = "/v1/content/{vocabularyName}/versions/{version}", produces = TURTLE)
    public ResponseEntity<byte[]> downloadVocabularyVersion(
            @PathVariable("vocabularyName") String vocabularyName,
            @PathVariable("version") String version,
            HttpServletRequest request) {
        RequestChecks checks = RequestChecks.start().pathName("vocabularyName", vocabularyName);
        int number = checks.pathVersion(version);
        checks.noQuery(request.getQueryString()).throwIfAny();
        return turtle(service.content(vocabularyName, number), vocabularyName + "-v" + number + ".ttl");
    }

    /** A Turtle download with Content-Disposition attachment. */
    static ResponseEntity<byte[]> turtle(byte[] body, String filename) {
        return ResponseEntity.ok()
                .header("Content-Type", TURTLE)
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .body(body);
    }
}
