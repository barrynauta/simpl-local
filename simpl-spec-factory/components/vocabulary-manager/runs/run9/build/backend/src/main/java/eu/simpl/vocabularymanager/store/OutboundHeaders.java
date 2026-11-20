package eu.simpl.vocabularymanager.store;

import java.util.Map;

/** Headers forwarded on calls to the store and the identity provider: traceparent and X-Correlation-Id (D86). */
public interface OutboundHeaders {

    /** @return header names and values for the current request */
    Map<String, String> current();
}
