package eu.simpl.vocabularymanager.service;

import eu.simpl.vocabularymanager.config.AppProperties;
import eu.simpl.vocabularymanager.error.ApiException;
import java.util.concurrent.Semaphore;
import org.springframework.stereotype.Component;

/** At most 4 create or new-version requests at the same time per instance (D92, bulkhead). */
@Component
public class UploadLimiter {

    public static final String BUSY = "Too many uploads in progress. Retry later.";

    private final Semaphore permits;

    public UploadLimiter(AppProperties properties) {
        this.permits = new Semaphore(properties.limits().maxParallelUploads());
    }

    /** A permit, released with close(). */
    public interface Permit extends AutoCloseable {
        @Override
        void close();
    }

    /**
     * Takes a permit without waiting.
     *
     * @throws ApiException 503 when all permits are taken
     */
    public Permit acquire() {
        if (!permits.tryAcquire()) {
            throw ApiException.unavailable(BUSY);
        }
        return permits::release;
    }
}
