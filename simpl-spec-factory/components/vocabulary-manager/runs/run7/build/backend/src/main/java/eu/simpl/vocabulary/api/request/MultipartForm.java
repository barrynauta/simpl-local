package eu.simpl.vocabulary.api.request;

import eu.simpl.vocabulary.api.problem.Issue;
import eu.simpl.vocabulary.api.problem.Problems;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.fileupload2.core.DiskFileItem;
import org.apache.commons.fileupload2.core.DiskFileItemFactory;
import org.apache.commons.fileupload2.core.FileItemHeaders;
import org.apache.commons.fileupload2.core.FileItemInput;
import org.apache.commons.fileupload2.core.FileItemInputIterator;
import org.apache.commons.fileupload2.core.FileUploadFileCountLimitException;
import org.apache.commons.fileupload2.core.FileUploadSizeException;
import org.apache.commons.fileupload2.jakarta.servlet6.JakartaServletFileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A multipart/form-data body read part by part. Apache Commons FileUpload parses the parts (REUSE-02); this
 * class applies the limits of D76 and keeps the parts in order for the field checks of D37, D52 and D70.
 */
public final class MultipartForm {

    /**
     * One part.
     *
     * @param name the part name, "" when missing
     * @param bytes the part content
     */
    public record FormPart(String name, byte[] bytes) {

        public String text() {
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }

    public static final String FILE_FIELD = "vocabularyFile";

    /** Upper bound of parts; 20 aliases plus the other fields fit well below it. */
    static final long MAX_PARTS = 200;

    private static final Logger LOG = LoggerFactory.getLogger(MultipartForm.class);

    private final List<FormPart> parts;

    MultipartForm(List<FormPart> parts) {
        this.parts = List.copyOf(parts);
    }

    /**
     * Reads every part of the request with Apache Commons FileUpload (streaming, nothing on disk).
     *
     * @param request the request
     * @param maxFileBytes limit of the vocabularyFile part (413 above it)
     * @param maxRequestBytes limit of the whole body (413 above it)
     * @return the form
     * @throws eu.simpl.vocabulary.api.problem.ApiException 413 when a limit is passed, 400 when the body is
     *     not a readable multipart body
     */
    public static MultipartForm read(HttpServletRequest request, long maxFileBytes, long maxRequestBytes) {
        JakartaServletFileUpload<DiskFileItem, DiskFileItemFactory> upload = new KeepUnnamedParts();
        upload.setMaxSize(maxRequestBytes);
        upload.setMaxFileCount(MAX_PARTS);
        upload.setHeaderCharset(StandardCharsets.UTF_8);
        List<FormPart> parts = new ArrayList<>();
        try {
            FileItemInputIterator items = upload.getItemIterator(request);
            while (items.hasNext()) {
                FileItemInput item = items.next();
                String name = item.getFieldName() == null ? "" : item.getFieldName();
                long limit = FILE_FIELD.equals(name) ? maxFileBytes : maxRequestBytes;
                try (InputStream in = item.getInputStream()) {
                    byte[] bytes = in.readNBytes((int) Math.min(Integer.MAX_VALUE - 8, limit + 1));
                    if (bytes.length > limit) {
                        throw Problems.payloadTooLarge();
                    }
                    parts.add(new FormPart(name, bytes));
                }
            }
        } catch (FileUploadFileCountLimitException e) {
            throw Problems.badRequest(
                    Issue.schemaViolation(Issue.IN_BODY, "body", "The multipart body has too many parts."));
        } catch (FileUploadSizeException e) {
            if (e.getPermitted() == maxRequestBytes) {
                throw Problems.payloadTooLarge();
            }
            LOG.info("Multipart part headers rejected: {}", e.getMessage());
            throw Problems.badRequest(
                    Issue.schemaViolation(Issue.IN_BODY, "body", "The multipart body could not be read."));
        } catch (IOException e) {
            if (causedBySize(e, maxRequestBytes)) {
                throw Problems.payloadTooLarge();
            }
            LOG.info("Multipart body not readable: {}", e.getMessage());
            throw Problems.badRequest(
                    Issue.schemaViolation(Issue.IN_BODY, "body", "The multipart body could not be read."));
        }
        return new MultipartForm(parts);
    }

    /**
     * FileUpload skips form-data parts without a usable name; D70 needs them reported as the unknown field ''.
     */
    private static final class KeepUnnamedParts extends JakartaServletFileUpload<DiskFileItem, DiskFileItemFactory> {

        @Override
        public String getFieldName(FileItemHeaders headers) {
            String name = super.getFieldName(headers);
            if (name != null) {
                return name;
            }
            String disposition = headers.getHeader("Content-Disposition");
            return disposition != null
                            && disposition.toLowerCase(java.util.Locale.ROOT).startsWith("form-data")
                    ? ""
                    : null;
        }
    }

    private static boolean causedBySize(Throwable e, long maxRequestBytes) {
        Throwable current = e;
        int depth = 0;
        while (current != null && depth++ < 10) {
            if (current instanceof FileUploadSizeException size && size.getPermitted() == maxRequestBytes) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    /**
     * Issues for parts the operation does not define, one per unknown name (D52, D70), and for single-value
     * fields given more than once.
     *
     * @param known the names the operation defines
     * @param repeatable the names that may repeat
     * @return the issues
     */
    public List<Issue> unknownOrRepeated(Set<String> known, Set<String> repeatable) {
        List<Issue> issues = new ArrayList<>();
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (FormPart part : parts) {
            counts.merge(part.name(), 1, Integer::sum);
        }
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            String name = entry.getKey();
            if (!known.contains(name)) {
                issues.add(Issue.schemaViolation(Issue.IN_BODY, name, "Field '" + name + "' is not supported."));
            } else if (entry.getValue() > 1 && !repeatable.contains(name)) {
                issues.add(Issue.schemaViolation(Issue.IN_BODY, name, "Field '" + name + "' is given more than once."));
            }
        }
        return issues;
    }

    /**
     * The first part with the name.
     *
     * @param name the name
     * @return the part
     */
    public Optional<FormPart> first(String name) {
        return parts.stream().filter(p -> p.name().equals(name)).findFirst();
    }

    /**
     * All parts with the name, in order.
     *
     * @param name the name
     * @return the parts
     */
    public List<FormPart> all(String name) {
        return parts.stream().filter(p -> p.name().equals(name)).toList();
    }

    /** True when a part with the name is present. */
    public boolean has(String name) {
        return first(name).isPresent();
    }

    /** Distinct part names in order. */
    public Set<String> names() {
        Set<String> names = new LinkedHashSet<>();
        parts.forEach(p -> names.add(p.name()));
        return names;
    }
}
