package eu.simpl.vocabularymanager.domain;

import eu.simpl.vocabularymanager.config.AppProperties;
import eu.simpl.vocabularymanager.error.ApiException;
import java.util.concurrent.Semaphore;
import org.springframework.stereotype.Component;

/** At most N create or new-version requests at the same time per instance (D92). */
@Component
public class UploadLimiter {

    private final Semaphore permits;

    public UploadLimiter(AppProperties props) {
        this.permits = new Semaphore(props.uploads().maxConcurrent());
    }

    /** A held permit; closing it releases the permit. */
    public interface Permit extends AutoCloseable {
        @Override
        void close();
    }

    /** Takes a permit or throws 503 'Too many uploads in progress. Retry later.'. */
    public Permit acquire() {
        if (!permits.tryAcquire()) {
            throw ApiException.unavailable(ApiException.TOO_MANY_UPLOADS);
        }
        return permits::release;
    }
}
