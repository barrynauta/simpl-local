package eu.simpl.vocabularymanager.web;

import eu.simpl.vocabularymanager.service.VocabularyService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Set;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Downloads of vocabulary content (SIMPL-18165, SIMPL-18166, SIMPL-18452, D29, D70). */
@RestController
@RequestMapping("/v1")
public class ContentController {

    static final MediaType TURTLE = MediaType.parseMediaType("text/turtle");
    static final String TURTLE_TYPE = "text/turtle";
    static final String ANY_APPLICATION = "application/*";

    private final VocabularyService service;

    public ContentController(VocabularyService service) {
        this.service = service;
    }

    /** GET /content/{vocabularyName}: content of the latest version. */
    @GetMapping(
            path = "/content/{vocabularyName}",
            produces = {TURTLE_TYPE, ANY_APPLICATION})
    public ResponseEntity<byte[]> downloadVocabulary(
            @PathVariable("vocabularyName") String rawName, HttpServletRequest request) {
        String name = RequestRules.vocabularyName(rawName);
        QueryParameters.parse(request.getQueryString()).requireOnly(Set.of());
        VocabularyService.Content content = service.content(name, null);
        return turtle(content.bytes(), name + ".ttl");
    }

    /** GET /content/{vocabularyName}/versions/{version}: content of one version. */
    @GetMapping(
            path = "/content/{vocabularyName}/versions/{version}",
            produces = {TURTLE_TYPE, ANY_APPLICATION})
    public ResponseEntity<byte[]> downloadVocabularyVersion(
            @PathVariable("vocabularyName") String rawName,
            @PathVariable("version") String rawVersion,
            HttpServletRequest request) {
        String name = RequestRules.vocabularyName(rawName);
        long version = RequestRules.version(rawVersion);
        QueryParameters.parse(request.getQueryString()).requireOnly(Set.of());
        VocabularyService.Content content = service.content(name, version);
        return turtle(content.bytes(), name + "-v" + version + ".ttl");
    }

    /** A Turtle download with Content-Type exactly text/turtle (D70). */
    static ResponseEntity<byte[]> turtle(byte[] bytes, String filename) {
        return ResponseEntity.ok()
                .contentType(TURTLE)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(filename)
                                .build()
                                .toString())
                .body(bytes);
    }
}
