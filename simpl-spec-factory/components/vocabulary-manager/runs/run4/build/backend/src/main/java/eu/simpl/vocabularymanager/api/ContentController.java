package eu.simpl.vocabularymanager.api;

import eu.simpl.vocabularymanager.service.VocabularyCommands.Content;
import eu.simpl.vocabularymanager.service.VocabularyService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Set;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** Downloads of vocabulary content: the stored Turtle bytes as uploaded (decision D29). */
@RestController
public class ContentController {

    static final String TURTLE = "text/turtle";

    private final VocabularyService service;

    public ContentController(VocabularyService service) {
        this.service = service;
    }

    /** GET /content/{vocabularyName}: the latest version, filename '<Name>.ttl'. */
    @GetMapping(value = "/content/{vocabularyName}", produces = TURTLE)
    public ResponseEntity<byte[]> latest(@PathVariable String vocabularyName, HttpServletRequest request) {
        String name = PathParameters.name("vocabularyName", vocabularyName);
        QueryParameters.read(request, Set.of());
        Content content = service.latestContent(name);
        return turtle(content.bytes(), name + ".ttl");
    }

    /** GET /content/{vocabularyName}/versions/{version}: filename '<Name>-v<n>.ttl'. */
    @GetMapping(value = "/content/{vocabularyName}/versions/{version}", produces = TURTLE)
    public ResponseEntity<byte[]> version(
            @PathVariable String vocabularyName, @PathVariable String version, HttpServletRequest request) {
        String name = PathParameters.name("vocabularyName", vocabularyName);
        int number = PathParameters.version(version);
        QueryParameters.read(request, Set.of());
        Content content = service.versionContent(name, number);
        return turtle(content.bytes(), name + "-v" + number + ".ttl");
    }

    static ResponseEntity<byte[]> turtle(byte[] bytes, String filename) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(TURTLE))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(filename)
                                .build()
                                .toString())
                .body(bytes);
    }
}
