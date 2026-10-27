package eu.simpl.vocabulary.web;

import eu.simpl.vocabulary.service.VocabularyService;
import java.nio.charset.StandardCharsets;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Turtle downloads (SIMPL-18165, 18452, D24). */
@RestController
@RequestMapping(path = "/v1/content", produces = "text/turtle")
public class ContentController {

    public static final MediaType TURTLE = new MediaType("text", "turtle", StandardCharsets.UTF_8);

    private final VocabularyService service;

    public ContentController(VocabularyService service) {
        this.service = service;
    }

    @GetMapping("/{vocabularyName}")
    public ResponseEntity<byte[]> latest(@PathVariable String vocabularyName) {
        RequestChecks.requireName(vocabularyName, "vocabularyName");
        return turtle(service.content(vocabularyName, null), vocabularyName + ".ttl");
    }

    @GetMapping("/{vocabularyName}/versions/{version}")
    public ResponseEntity<byte[]> version(@PathVariable String vocabularyName, @PathVariable String version) {
        RequestChecks.requireName(vocabularyName, "vocabularyName");
        int number = RequestChecks.requireVersion(version);
        return turtle(service.content(vocabularyName, number), vocabularyName + "-v" + number + ".ttl");
    }

    static ResponseEntity<byte[]> turtle(String text, String filename) {
        return ResponseEntity.ok()
                .contentType(TURTLE)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename).build().toString())
                .body(text.getBytes(StandardCharsets.UTF_8));
    }
}
