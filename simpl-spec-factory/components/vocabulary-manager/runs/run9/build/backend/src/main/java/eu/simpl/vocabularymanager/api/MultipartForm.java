package eu.simpl.vocabularymanager.api;

import jakarta.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.tomcat.util.http.fileupload.MultipartStream;
import org.apache.tomcat.util.http.fileupload.ParameterParser;

/**
 * Reads a multipart/form-data request with the limits of D15 and D76 and the field rules of D52 and D70. Parsing
 * reuses Tomcat's multipart stream parser; the container's own part handling is not used because it silently drops
 * parts without a name, which D70 wants reported.
 */
public final class MultipartForm {

    private static final int MAX_PARTS = 64;

    private final Map<String, List<FormPart>> parts;

    private MultipartForm(Map<String, List<FormPart>> parts) {
        this.parts = parts;
    }

    /**
     * Reads all parts.
     *
     * @param request the request
     * @param fileField the name of the file part
     * @param maxFileBytes limit of the file part (413 above it, D15)
     * @param maxRequestBytes limit of the whole request (413 above it, D76)
     * @return the parts by name
     */
    public static MultipartForm read(
            HttpServletRequest request, String fileField, long maxFileBytes, long maxRequestBytes) {
        if (request.getContentLengthLong() > maxRequestBytes) {
            throw new ProblemException(Problems.payloadTooLarge());
        }
        byte[] boundary = boundary(request.getContentType());
        Map<String, List<FormPart>> parts = new LinkedHashMap<>();
        try (InputStream in = new BoundedInputStream(request.getInputStream(), maxRequestBytes)) {
            MultipartStream stream = new MultipartStream(in, boundary, 8192, null);
            stream.setHeaderEncoding(StandardCharsets.UTF_8.name());
            boolean next = stream.skipPreamble();
            int count = 0;
            while (next) {
                if (++count > MAX_PARTS) {
                    throw Problems.badRequest(
                            Issue.schemaViolation("body", "body", "The request has too many form fields.", null));
                }
                String name = partName(stream.readHeaders());
                long limit = fileField.equals(name) ? maxFileBytes : maxRequestBytes;
                BoundedOutputStream body = new BoundedOutputStream(limit);
                stream.readBodyData(body);
                parts.computeIfAbsent(name, n -> new ArrayList<>()).add(new FormPart(name, body.toByteArray()));
                next = stream.readBoundary();
            }
        } catch (ProblemException ex) {
            throw ex;
        } catch (IOException | RuntimeException ex) {
            if (hasTooLargeCause(ex)) {
                throw new ProblemException(Problems.payloadTooLarge());
            }
            throw malformed();
        }
        return new MultipartForm(parts);
    }

    private static boolean hasTooLargeCause(Throwable ex) {
        for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
            if (cause instanceof TooLargeException || cause instanceof PartTooLargeException) {
                return true;
            }
            if (cause.getCause() == cause) {
                break;
            }
        }
        return false;
    }

    private static byte[] boundary(String contentType) {
        ParameterParser parser = new ParameterParser();
        parser.setLowerCaseNames(true);
        Map<String, String> parameters = parser.parse(contentType == null ? "" : contentType, new char[] {';', ','});
        String boundary = parameters.get("boundary");
        if (boundary == null || boundary.isEmpty() || boundary.length() > 200) {
            throw malformed();
        }
        return boundary.getBytes(StandardCharsets.ISO_8859_1);
    }

    private static String partName(String headers) {
        for (String line : headers.split("\\r?\\n")) {
            int colon = line.indexOf(':');
            if (colon > 0 && line.substring(0, colon).trim().equalsIgnoreCase("Content-Disposition")) {
                ParameterParser parser = new ParameterParser();
                parser.setLowerCaseNames(true);
                String name = parser.parse(line.substring(colon + 1), ';').get("name");
                return name == null ? "" : name;
            }
        }
        return "";
    }

    private static ProblemException malformed() {
        return Problems.badRequest(Issue.schemaViolation("body", "body", "The multipart request is malformed.", null));
    }

    /** Names of parts that the operation does not declare (D52, D70), one issue each. */
    public List<Issue> unknownFields(Set<String> allowed) {
        List<Issue> issues = new ArrayList<>();
        for (String name : parts.keySet()) {
            if (!allowed.contains(name)) {
                issues.add(Issue.schemaViolation("body", name, "Form field '" + name + "' is not allowed.", null));
            }
        }
        return issues;
    }

    /** Whether the field is present. */
    public boolean has(String name) {
        return parts.containsKey(name);
    }

    /** All parts of a field, in request order. */
    public List<FormPart> all(String name) {
        return parts.getOrDefault(name, List.of());
    }

    /** The single part of a field, or null when absent; adds an issue when it is repeated. */
    public FormPart single(String name, List<Issue> issues) {
        List<FormPart> values = all(name);
        if (values.size() > 1) {
            issues.add(Issue.schemaViolation("body", name, "Form field '" + name + "' must appear once.", null));
            return null;
        }
        return values.isEmpty() ? null : values.get(0);
    }

    /** Thrown when the request limit is passed while reading. */
    private static final class TooLargeException extends IOException {
        private static final long serialVersionUID = 1L;

        TooLargeException() {
            super("request limit exceeded");
        }
    }

    /** Thrown when a part limit is passed while reading. */
    private static final class PartTooLargeException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        PartTooLargeException() {
            super("part limit exceeded");
        }
    }

    /** An input stream that fails once more than the limit is read. */
    private static final class BoundedInputStream extends FilterInputStream {
        private final long limit;
        private long count;

        BoundedInputStream(InputStream in, long limit) {
            super(in);
            this.limit = limit;
        }

        @Override
        public int read() throws IOException {
            int b = super.read();
            if (b >= 0 && ++count > limit) {
                throw new TooLargeException();
            }
            return b;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            int n = super.read(buffer, offset, length);
            if (n > 0) {
                count += n;
                if (count > limit) {
                    throw new TooLargeException();
                }
            }
            return n;
        }
    }

    /** A byte buffer that fails once more than the limit is written. */
    private static final class BoundedOutputStream extends ByteArrayOutputStream {
        private final long limit;

        BoundedOutputStream(long limit) {
            this.limit = limit;
        }

        @Override
        public synchronized void write(byte[] buffer, int offset, int length) {
            if (count + (long) length > limit) {
                throw new PartTooLargeException();
            }
            super.write(buffer, offset, length);
        }

        @Override
        public synchronized void write(int b) {
            if (count + 1L > limit) {
                throw new PartTooLargeException();
            }
            super.write(b);
        }
    }
}
