package eu.simpl.schemamanager.notify;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.simpl.schemamanager.domain.Webhook;
import eu.simpl.schemamanager.persistence.WebhookRepository;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Webhook delivery after commit: one POST per matching subscription, two retries (D42 to D44). */
@Component
public class WebhookNotifier {

    private static final Logger LOG = LoggerFactory.getLogger(WebhookNotifier.class);
    static final long[] RETRY_DELAYS_MS = {1000, 5000};
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private final WebhookRepository repository;
    private final HttpClient http;
    private final ObjectMapper mapper;
    private final ScheduledExecutorService scheduler;

    public WebhookNotifier(WebhookRepository repository, HttpClient http, ObjectMapper mapper,
                           ScheduledExecutorService webhookScheduler) {
        this.repository = repository;
        this.http = http;
        this.mapper = mapper;
        this.scheduler = webhookScheduler;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEvent(SchemaEvent event) {
        String type = event.webhookEventType();
        List<Webhook> targets = repository.findByEvent(type);
        if (targets.isEmpty()) {
            return;
        }
        byte[] body;
        try {
            body = mapper.writeValueAsBytes(payload(event));
        } catch (Exception e) {
            LOG.error("Could not serialise webhook payload", e);
            return;
        }
        for (Webhook w : targets) {
            scheduler.execute(() -> attempt(w, type, body, 0));
        }
    }

    public static Map<String, Object> payload(SchemaEvent event) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("eventId", UUID.randomUUID().toString());
        p.put("eventType", event.webhookEventType());
        p.put("occurredAt", DateTimeFormatter.ISO_INSTANT.format(event.occurredAt().truncatedTo(ChronoUnit.SECONDS)));
        p.put("schemaName", event.concept().name());
        p.put("schemaType", event.concept().schemaType().name());
        p.put("resourceType", event.concept().resourceType());
        p.put("version", String.valueOf(event.version()));
        p.put("status", event.concept().status().name());
        p.put("schemaUri", event.schemaUri());
        return p;
    }

    private void attempt(Webhook w, String type, byte[] body, int attemptNo) {
        boolean ok = false;
        String failure = null;
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(w.targetUrl()))
                    .timeout(TIMEOUT)
                    .header("Content-Type", "application/json")
                    .header("X-Simpl-Event", type)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .build();
            HttpResponse<Void> resp = http.send(req, HttpResponse.BodyHandlers.discarding());
            ok = resp.statusCode() >= 200 && resp.statusCode() < 300;
            if (!ok) {
                failure = "HTTP " + resp.statusCode();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            failure = e.toString();
        } catch (Exception e) {
            failure = e.toString();
        }
        if (ok) {
            return;
        }
        if (attemptNo < RETRY_DELAYS_MS.length) {
            LOG.warn("Webhook {} delivery to {} failed ({}); retry {} in {} ms", w.webhookId(), w.targetUrl(), failure,
                    attemptNo + 1, RETRY_DELAYS_MS[attemptNo]);
            scheduler.schedule(() -> attempt(w, type, body, attemptNo + 1), RETRY_DELAYS_MS[attemptNo],
                    TimeUnit.MILLISECONDS);
        } else {
            LOG.error("Webhook {} delivery to {} dropped after {} attempts ({})", w.webhookId(), w.targetUrl(),
                    attemptNo + 1, failure);
        }
    }
}
