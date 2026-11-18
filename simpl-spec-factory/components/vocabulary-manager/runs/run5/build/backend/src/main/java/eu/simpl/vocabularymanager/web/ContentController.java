package eu.simpl.vocabularymanager.web;

import eu.simpl.vocabularymanager.service.VocabularyService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Set;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Downloads of vocabulary content (SIMPL-18165, SIMPL-18452; D29). */
@RestController
@RequestMapping("/v1/content")
public class ContentController {

    private final VocabularyService service;
    private final RequestInspector inspector;

    public ContentController(VocabularyService service, RequestInspector inspector) {
        this.service = service;
        this.inspector = inspector;
    }

    /** GET /content/{vocabularyName}: the latest version. */
    @GetMapping("/{vocabularyName}")
    public ResponseEntity<byte[]> latest(@PathVariable String vocabularyName, HttpServletRequest request) {
        inspector.requireAcceptable(request, TurtleResponses.TURTLE);
        List<Issue> issues = inspector.queryIssues(inspector.query(request), Set.of());
        PathValues.checkName("vocabularyName", vocabularyName, issues);
        PathValues.failOn(issues);
        return TurtleResponses.attachment(service.latestContent(vocabularyName), vocabularyName + ".ttl");
    }

    /** GET /content/{vocabularyName}/versions/{version}. */
    @GetMapping("/{vocabularyName}/versions/{version}")
    public ResponseEntity<byte[]> version(
            @PathVariable String vocabularyName, @PathVariable String version, HttpServletRequest request) {
        inspector.requireAcceptable(request, TurtleResponses.TURTLE);
        List<Issue> issues = inspector.queryIssues(inspector.query(request), Set.of());
        PathValues.checkName("vocabularyName", vocabularyName, issues);
        int number = PathValues.checkVersion(version, issues);
        PathValues.failOn(issues);
        return TurtleResponses.attachment(
                service.content(vocabularyName, number), vocabularyName + "-v" + number + ".ttl");
    }
}
