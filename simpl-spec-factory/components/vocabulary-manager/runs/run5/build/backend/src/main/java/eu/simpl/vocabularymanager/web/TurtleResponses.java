package eu.simpl.vocabularymanager.web;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/** Download responses: stored bytes, text/turtle, attachment file name (D29). */
public final class TurtleResponses {

    /** Media type of vocabulary content. */
    public static final MediaType TURTLE = MediaType.parseMediaType("text/turtle");

    private TurtleResponses() {}

    public static ResponseEntity<byte[]> attachment(byte[] content, String fileName) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(TURTLE);
        headers.setContentDisposition(
                ContentDisposition.attachment().filename(fileName).build());
        headers.setContentLength(content.length);
        return ResponseEntity.ok().headers(headers).body(content);
    }
}
