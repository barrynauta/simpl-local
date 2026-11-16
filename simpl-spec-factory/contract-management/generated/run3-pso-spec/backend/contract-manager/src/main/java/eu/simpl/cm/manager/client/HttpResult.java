package eu.simpl.cm.manager.client;

import com.fasterxml.jackson.databind.JsonNode;

/** Result of an outbound call. Status -1 means the call did not reach the server or timed out. */
public record HttpResult(int status, JsonNode body) {
    public boolean ok() {
        return status >= 200 && status < 300;
    }

    public static HttpResult unreachable() {
        return new HttpResult(-1, null);
    }
}
