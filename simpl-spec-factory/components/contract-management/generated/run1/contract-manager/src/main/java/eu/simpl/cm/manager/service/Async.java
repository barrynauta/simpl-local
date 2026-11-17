package eu.simpl.cm.manager.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Background work after an HTTP response (signing, validation, callbacks). Keeps the correlation id. */
@Component
public class Async {
    private static final Logger log = LoggerFactory.getLogger(Async.class);
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public void submit(String name, Runnable task) {
        String correlationId = MDC.get("correlationId");
        AfterCommit.run(() -> executor.submit(() -> {
            if (correlationId != null) {
                MDC.put("correlationId", correlationId);
            }
            try {
                task.run();
            } catch (Exception e) {
                log.error("Background task {} failed: {}", name, e.toString(), e);
            } finally {
                MDC.remove("correlationId");
            }
        }));
    }
}
