package eu.simpl.cm.manager.web;

import eu.simpl.cm.common.proof.DataIntegrity;
import eu.simpl.cm.common.web.ApiException;
import eu.simpl.cm.common.web.Problems;
import eu.simpl.cm.common.web.Times;
import eu.simpl.cm.manager.CmSettings;
import eu.simpl.cm.manager.domain.ClauseDocument;
import eu.simpl.cm.manager.domain.ClauseRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** D63 / A.7: contract clause document upload. */
@RestController
public class ClauseController {
    static final List<String> ALLOWED = List.of("application/pdf", "text/plain", "text/markdown");
    private final ClauseRepository repo;
    private final CmSettings settings;

    public ClauseController(ClauseRepository repo, CmSettings settings) {
        this.repo = repo;
        this.settings = settings;
    }

    @PostMapping(value = "/contractClauseDocuments", consumes = "multipart/form-data", produces = "application/json")
    public ResponseEntity<Map<String, Object>> upload(@RequestPart(value = "file", required = false) MultipartFile file)
            throws IOException {
        if (file == null || file.isEmpty()) {
            throw ApiException.validation("A non-empty part 'file' is required");
        }
        String mediaType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        int semi = mediaType.indexOf(';');
        if (semi >= 0) {
            mediaType = mediaType.substring(0, semi).trim();
        }
        if (!ALLOWED.contains(mediaType)) {
            throw new ApiException(415, Problems.INVALID_DOCUMENT, "Media type not allowed: " + mediaType);
        }
        if (file.getSize() > settings.clauseMaxBytes) {
            throw new ApiException(413, Problems.INVALID_DOCUMENT, "Document is larger than " + settings.clauseMaxBytes + " bytes");
        }
        byte[] bytes = file.getBytes();
        ClauseDocument d = new ClauseDocument();
        d.documentId = UUID.randomUUID().toString();
        String name = file.getOriginalFilename();
        d.documentName = name == null || name.isBlank() ? "document" : name;
        d.mediaType = mediaType;
        d.size = bytes.length;
        d.hash = DataIntegrity.sha256Hex(bytes);
        d.content = bytes;
        d.uploadedAt = Times.now();
        repo.save(d);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("documentId", d.documentId);
        out.put("documentName", d.documentName);
        out.put("mediaType", d.mediaType);
        out.put("size", d.size);
        out.put("hash", d.hash);
        out.put("hashAlgorithm", "SHA-256");
        out.put("sourceReference", "urn:simpl:contract-clause:" + d.documentId);
        return ResponseEntity.status(201).body(out);
    }
}
