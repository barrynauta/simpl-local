package eu.simpl.vocabularymanager.web.body;

import eu.simpl.vocabularymanager.web.problem.ProblemIssue;
import eu.simpl.vocabularymanager.web.problem.Problems;
import jakarta.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.apache.tomcat.util.http.fileupload.MultipartStream;
import org.apache.tomcat.util.http.fileupload.ParameterParser;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;

/**
 * The parts of a multipart/form-data request, read with the multipart stream parser bundled with the servlet container
 * (Tomcat's copy of Apache Commons FileUpload, REUSE-02). The container's own part handling is not used because it
 * silently drops a part with an empty name, which D70 requires to be reported. Limits (D15, D76): a vocabularyFile
 * part over 5 MB or a request over 6 MB gives 413. Unknown, empty-named and repeated parts give 400 (D52, D70).
 */
public final class MultipartForm {

    /** Largest vocabularyFile part (D15). */
    public static final long MAX_FILE_BYTES = 5L * 1024 * 1024;
    /** Largest multipart request (D76). */
    public static final long MAX_REQUEST_BYTES = 6L * 1024 * 1024;
    /** Most parts in one request: 20 aliases plus the other fields, with room to report unknown ones. */
    static final int MAX_PARTS = 50;
    private static final int MAX_PART_HEADER_BYTES = 2048;

    private final Map<String, List<byte[]>> parts;

    private MultipartForm(Map<String, List<byte[]>> parts) {
        this.parts = parts;
    }

    /**
     * @param request the request
     * @param allowed the part names the operation defines
     * @param repeatable the part names that may appear more than once
     * @return the form, with every part read
     * @throws eu.simpl.vocabularymanager.web.problem.ApiException 413 or 400
     */
    public static MultipartForm read(HttpServletRequest request, Set<String> allowed, Set<String> repeatable) {
        byte[] boundary = boundary(request.getContentType());
        Map<String, List<byte[]>> parts = new LinkedHashMap<>();
        LimitedInputStream input;
        try {
            input = new LimitedInputStream(request.getInputStream(), MAX_REQUEST_BYTES);
        } catch (IOException e) {
            throw unreadable();
        }
        try {
            MultipartStream stream = new MultipartStream(input, boundary, 8192, null);
            stream.setHeaderEncoding(StandardCharsets.UTF_8.name());
            stream.setPartHeaderSizeMax(MAX_PART_HEADER_BYTES);
            boolean next = stream.skipPreamble();
            int count = 0;
            while (next) {
                if (++count > MAX_PARTS) {
                    throw Problems.schemaViolation("body", "body", "The request has too many form fields.");
                }
                String name = fieldName(stream.readHeaders());
                ByteArrayOutputStream body = new ByteArrayOutputStream();
                stream.readBodyData(body);
                if ("vocabularyFile".equals(name) && body.size() > MAX_FILE_BYTES) {
                    throw Problems.payloadTooLarge("vocabularyFile");
                }
                parts.computeIfAbsent(name, key -> new ArrayList<>()).add(body.toByteArray());
                next = stream.readBoundary();
            }
            if (count == 0) {
                throw unreadable();
            }
        } catch (IOException | RuntimeException e) {
            if (input.exceeded()) {
                throw Problems.payloadTooLarge("vocabularyFile");
            }
            if (e instanceof eu.simpl.vocabularymanager.web.problem.ApiException api) {
                throw api;
            }
            throw unreadable();
        }
        List<ProblemIssue> issues = new ArrayList<>();
        parts.forEach((name, values) -> {
            if (!allowed.contains(name)) {
                issues.add(ProblemIssue.schemaViolation("body", name, "Form field '" + shorten(name)
                        + "' is not defined for this operation."));
            } else if (values.size() > 1 && !repeatable.contains(name)) {
                issues.add(ProblemIssue.schemaViolation("body", name, "Form field '" + name
                        + "' may be given only once."));
            }
        });
        if (!issues.isEmpty()) {
            throw Problems.badRequest(issues);
        }
        return new MultipartForm(parts);
    }

    private static byte[] boundary(String contentType) {
        try {
            String boundary = contentType == null ? null : MediaType.parseMediaType(contentType).getParameter("boundary");
            if (boundary != null && boundary.length() > 1 && boundary.startsWith("\"") && boundary.endsWith("\"")) {
                boundary = boundary.substring(1, boundary.length() - 1);
            }
            if (boundary == null || boundary.isEmpty() || boundary.length() > 70) {
                throw unreadable();
            }
            return boundary.getBytes(StandardCharsets.ISO_8859_1);
        } catch (InvalidMediaTypeException e) {
            throw unreadable();
        }
    }

    /**
     * @param headers the raw header block of one part
     * @return the name parameter of its Content-Disposition, or an empty string when it has none (D70)
     */
    static String fieldName(String headers) {
        for (String line : headers.split("\r\n")) {
            int colon = line.indexOf(':');
            if (colon > 0 && line.substring(0, colon).trim().toLowerCase(Locale.ROOT).equals("content-disposition")) {
                ParameterParser parser = new ParameterParser();
                parser.setLowerCaseNames(true);
                String name = parser.parse(line.substring(colon + 1), ';').get("name");
                return name == null ? "" : name;
            }
        }
        return "";
    }

    private static RuntimeException unreadable() {
        return Problems.schemaViolation("body", "body", "The multipart request body cannot be read.");
    }

    /** Counts the bytes read and stops after the limit. */
    static final class LimitedInputStream extends FilterInputStream {

        private final long limit;
        private long count;
        private boolean exceeded;

        LimitedInputStream(InputStream in, long limit) {
            super(in);
            this.limit = limit;
        }

        boolean exceeded() {
            return exceeded;
        }

        @Override
        public int read() throws IOException {
            int value = super.read();
            if (value >= 0) {
                count(1);
            }
            return value;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            int read = super.read(buffer, offset, length);
            if (read > 0) {
                count(read);
            }
            return read;
        }

        private void count(long read) throws IOException {
            count += read;
            if (count > limit) {
                exceeded = true;
                throw new IOException("Request body larger than " + limit + " bytes");
            }
        }
    }

    private static String shorten(String value) {
        return value.length() > 64 ? value.substring(0, 64) + "..." : value;
    }

    /**
     * @param name a part name
     * @return true when the part is present
     */
    public boolean has(String name) {
        return parts.containsKey(name);
    }

    /**
     * @param name a part name
     * @return the bytes of the first part with this name, or null
     */
    public byte[] bytes(String name) {
        List<byte[]> values = parts.get(name);
        return values == null ? null : values.get(0);
    }

    /**
     * @param name a part name
     * @return the first part as UTF-8 text, or null
     */
    public String text(String name) {
        byte[] value = bytes(name);
        return value == null ? null : new String(value, StandardCharsets.UTF_8);
    }

    /**
     * @param name a part name
     * @return every part with this name as UTF-8 text, in request order
     */
    public List<String> texts(String name) {
        return parts.getOrDefault(name, List.of()).stream()
                .map(value -> new String(value, StandardCharsets.UTF_8))
                .toList();
    }
}
