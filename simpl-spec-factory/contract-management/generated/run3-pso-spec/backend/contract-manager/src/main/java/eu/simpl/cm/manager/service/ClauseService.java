package eu.simpl.cm.manager.service;

import eu.simpl.cm.common.ApiException;
import eu.simpl.cm.manager.CmProperties;
import eu.simpl.cm.manager.domain.ClauseDocument;
import eu.simpl.cm.manager.domain.ClauseDocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Contract clause document upload (D63, A.7). */
@Service
public class ClauseService {

    static final Set<String> ALLOWED = Set.of("application/pdf", "text/plain", "text/markdown");

    private final ClauseDocumentRepository docs;
    private final CmProperties props;

    public ClauseService(ClauseDocumentRepository docs, CmProperties props) {
        this.docs = docs;
        this.props = props;
    }

    @Transactional
    public Map<String, Object> upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw ApiException.validation("A non-empty file part named 'file' is required");
        }
        String mediaType = baseType(file.getContentType());
        if (!ALLOWED.contains(mediaType)) {
            throw ApiException.invalidDocument(415, "Media type not allowed: " + mediaType
                    + "; allowed: application/pdf, text/plain, text/markdown");
        }
        if (file.getSize() > props.clauseMaxBytes()) {
            throw ApiException.invalidDocument(413, "The document is larger than " + props.clauseMaxBytes()
                    + " bytes");
        }
        byte[] bytes;
        String hash;
        try {
            bytes = file.getBytes();
            hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (Exception e) {
            throw ApiException.internal("The document could not be processed");
        }
        ClauseDocument d = new ClauseDocument();
        d.documentId = UUID.randomUUID().toString();
        String name = file.getOriginalFilename();
        d.documentName = name == null || name.isBlank() ? "document" : name;
        d.mediaType = mediaType;
        d.size = bytes.length;
        d.hash = hash;
        d.content = bytes;
        d.storedAt = Instant.now();
        docs.saveAndFlush(d);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("documentId", d.documentId);
        out.put("documentName", d.documentName);
        out.put("mediaType", d.mediaType);
        out.put("size", d.size);
        out.put("hash", d.hash);
        out.put("hashAlgorithm", "SHA-256");
        out.put("sourceReference", "urn:simpl:contract-clause:" + d.documentId);
        return out;
    }

    static String baseType(String contentType) {
        if (contentType == null) {
            return "";
        }
        int i = contentType.indexOf(';');
        return (i < 0 ? contentType : contentType.substring(0, i)).trim().toLowerCase();
    }
}
