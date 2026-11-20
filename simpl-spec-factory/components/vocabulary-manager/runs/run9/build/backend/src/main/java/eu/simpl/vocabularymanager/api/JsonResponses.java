package eu.simpl.vocabularymanager.api;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import tools.jackson.databind.json.JsonMapper;

/** JSON success responses written as bytes, so no content negotiation can change them. */
public final class JsonResponses {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private JsonResponses() {}

    /** A JSON response. */
    public static ResponseEntity<byte[]> json(int status, Object body) {
        return ResponseEntity.status(status)
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .body(MAPPER.writeValueAsBytes(body));
    }

    /** A 201 with Location (D26, D31). */
    public static ResponseEntity<byte[]> created(String location, Object body) {
        return ResponseEntity.status(201)
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .header(HttpHeaders.LOCATION, location)
                .body(MAPPER.writeValueAsBytes(body));
    }

    /** A Turtle download (D29, D70): Content-Type exactly text/turtle. */
    public static ResponseEntity<byte[]> turtle(String filename, byte[] content) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "text/turtle")
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(content);
    }
}
