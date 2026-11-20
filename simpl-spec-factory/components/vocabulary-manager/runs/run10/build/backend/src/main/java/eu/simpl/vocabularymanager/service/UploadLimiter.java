package eu.simpl.vocabularymanager.service;

import eu.simpl.vocabularymanager.error.Problems;
import java.util.concurrent.Semaphore;
import java.util.function.Supplier;

/** At most a fixed number of create or new-version requests per instance at the same time (D92). */
public class UploadLimiter {

    private final Semaphore permits;

    public UploadLimiter(int maxConcurrent) {
        this.permits = new Semaphore(maxConcurrent);
    }

    /**
     * Runs the upload when a permit is free.
     *
     * @throws eu.simpl.vocabularymanager.error.ProblemException 503 when all permits are taken
     */
    public <T> T run(Supplier<T> upload) {
        if (!permits.tryAcquire()) {
            throw Problems.serviceUnavailable(Problems.TOO_MANY_UPLOADS);
        }
        try {
            return upload.get();
        } finally {
            permits.release();
        }
    }
}
