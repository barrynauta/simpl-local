package eu.simpl.cm.manager.service;

import eu.simpl.cm.common.CorrelationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Background work after an HTTP response (signing, validation, callbacks). */
@Component
public class Tasks {

    private static final Logger LOG = LoggerFactory.getLogger(Tasks.class);

    private final ExecutorService executor = Executors.newFixedThreadPool(8);

    public void submit(String name, Runnable task) {
        String cid = CorrelationContext.get();
        executor.submit(CorrelationContext.wrap(cid, () -> {
            try {
                task.run();
            } catch (Exception e) {
                LOG.error("Background task {} failed", name, e);
            }
        }));
    }
}
