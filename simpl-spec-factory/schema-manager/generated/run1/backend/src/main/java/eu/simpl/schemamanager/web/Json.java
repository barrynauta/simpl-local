package eu.simpl.schemamanager.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/**
 * Writes JSON bodies as bytes with an explicit content type. The Accept header is checked
 * by the controllers (D8), so Spring's own negotiation must not interfere.
 */
@Component
public class Json {

    private final ObjectMapper mapper;

    public Json(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public ObjectMapper mapper() {
        return mapper;
    }

    public ResponseEntity<byte[]> response(HttpStatus status, Object body, HttpHeaders extra) {
        HttpHeaders headers = new HttpHeaders();
        if (extra != null) {
            headers.putAll(extra);
        }
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(HttpHeaders.VARY, HttpHeaders.ACCEPT);
        try {
            return new ResponseEntity<>(mapper.writeValueAsBytes(body), headers, status);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static String iso(Instant instant) {
        return instant == null ? null : DateTimeFormatter.ISO_INSTANT.format(instant);
    }
}
