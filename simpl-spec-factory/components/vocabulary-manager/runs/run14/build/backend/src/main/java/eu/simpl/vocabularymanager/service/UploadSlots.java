package eu.simpl.vocabularymanager.service;

import eu.simpl.vocabularymanager.config.AppProperties;
import java.util.concurrent.Semaphore;
import org.springframework.stereotype.Component;

/** At most four create or new-version requests per instance at the same time (D92, D170). */
@Component
public class UploadSlots {

    private final Semaphore semaphore;

    public UploadSlots(AppProperties properties) {
        this.semaphore = new Semaphore(Math.max(1, properties.uploadSlots()));
    }

    /** Takes a slot without waiting. */
    public boolean tryAcquire() {
        return semaphore.tryAcquire();
    }

    /** Returns a slot. */
    public void release() {
        semaphore.release();
    }

    /** Free slots, for tests and metrics. */
    public int available() {
        return semaphore.availablePermits();
    }
}
