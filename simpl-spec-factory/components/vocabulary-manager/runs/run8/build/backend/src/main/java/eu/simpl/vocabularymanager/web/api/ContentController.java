package eu.simpl.vocabularymanager.web.api;

import eu.simpl.vocabularymanager.service.VocabularyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** Content downloads (D29, D70): the stored Turtle bytes as uploaded, text/turtle without charset. */
@RestController
public class ContentController {

    private final VocabularyService service;
    private final Responses responses;

    /**
     * @param service the vocabulary service
     * @param responses response builder
     */
    public ContentController(VocabularyService service, Responses responses) {
        this.service = service;
        this.responses = responses;
    }

    /**
     * downloadVocabulary: the latest version (D5).
     *
     * @param vocabularyName the name
     * @return the Turtle bytes, filename '&lt;Name&gt;.ttl'
     */
    @GetMapping("/content/{vocabularyName}")
    public ResponseEntity<byte[]> downloadVocabulary(@PathVariable String vocabularyName) {
        return responses.turtle(service.content(vocabularyName, null), vocabularyName + ".ttl");
    }

    /**
     * downloadVocabularyVersion.
     *
     * @param vocabularyName the name
     * @param version the version number
     * @return the Turtle bytes, filename '&lt;Name&gt;-v&lt;n&gt;.ttl'
     */
    @GetMapping("/content/{vocabularyName}/versions/{version}")
    public ResponseEntity<byte[]> downloadVocabularyVersion(
            @PathVariable String vocabularyName, @PathVariable String version) {
        long number = Long.parseLong(version);
        return responses.turtle(service.content(vocabularyName, number), vocabularyName + "-v" + number + ".ttl");
    }
}
