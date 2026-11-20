package eu.simpl.vocabularymanager.api;

import eu.simpl.vocabularymanager.service.VocabularyService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** Content downloads (D29, D70; SIMPL-18165, SIMPL-18166, SIMPL-18452). */
@RestController
public class ContentController {

    private final VocabularyService service;

    /** @param service vocabulary service */
    public ContentController(VocabularyService service) {
        this.service = service;
    }

    /** GET /content/{vocabularyName}: the latest version, filename Name.ttl. */
    @GetMapping("/v1/content/{vocabularyName}")
    public ResponseEntity<byte[]> latest(HttpServletRequest request, @PathVariable String vocabularyName) {
        RequestChecks.acceptTurtle(request);
        List<Issue> issues = new ArrayList<>();
        String name = VocabularyController.pathName(vocabularyName, issues);
        RequestChecks.query(request, Set.of(), issues);
        if (!issues.isEmpty()) {
            throw new ProblemException(Problems.badRequest(issues));
        }
        return JsonResponses.turtle(name + ".ttl", service.content(name, null));
    }

    /** GET /content/{vocabularyName}/versions/{version}: filename Name-vN.ttl. */
    @GetMapping("/v1/content/{vocabularyName}/versions/{version}")
    public ResponseEntity<byte[]> version(
            HttpServletRequest request, @PathVariable String vocabularyName, @PathVariable String version) {
        RequestChecks.acceptTurtle(request);
        List<Issue> issues = new ArrayList<>();
        String name = VocabularyController.pathName(vocabularyName, issues);
        Integer number = VocabularyController.pathVersion(version, issues);
        RequestChecks.query(request, Set.of(), issues);
        if (!issues.isEmpty()) {
            throw new ProblemException(Problems.badRequest(issues));
        }
        return JsonResponses.turtle(name + "-v" + number + ".ttl", service.content(name, number));
    }
}
