package eu.simpl.vocabularymanager.api;

import eu.simpl.vocabularymanager.service.VocabularyService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** Downloads of vocabulary content (D29, D70): the stored bytes, text/turtle without charset, as an attachment. */
@RestController
public class ContentController {

    static final MediaType TURTLE = MediaType.parseMediaType("text/turtle");

    private final VocabularyService vocabularies;

    public ContentController(VocabularyService vocabularies) {
        this.vocabularies = vocabularies;
    }

    @GetMapping("/v1/content/{vocabularyName}")
    public ResponseEntity<byte[]> downloadVocabulary(@PathVariable String vocabularyName, HttpServletRequest request) {
        Checks checks = new Checks();
        checks.noQuery(request);
        String name = checks.pathName(vocabularyName, "vocabularyName");
        checks.throwIfAny();
        return turtle(name + ".ttl", vocabularies.latestContent(name));
    }

    @GetMapping("/v1/content/{vocabularyName}/versions/{version}")
    public ResponseEntity<byte[]> downloadVocabularyVersion(
            @PathVariable String vocabularyName, @PathVariable String version, HttpServletRequest request) {
        Checks checks = new Checks();
        checks.noQuery(request);
        String name = checks.pathName(vocabularyName, "vocabularyName");
        Integer number = checks.pathVersion(version);
        checks.throwIfAny();
        return turtle(name + "-v" + number + ".ttl", vocabularies.content(name, number));
    }

    static ResponseEntity<byte[]> turtle(String fileName, byte[] content) {
        return ResponseEntity.ok()
                .contentType(TURTLE)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(fileName)
                                .build()
                                .toString())
                .body(content);
    }
}
