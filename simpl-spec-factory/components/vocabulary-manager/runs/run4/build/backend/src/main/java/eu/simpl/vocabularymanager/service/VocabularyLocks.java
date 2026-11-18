package eu.simpl.vocabularymanager.service;

import eu.simpl.vocabularymanager.error.Problems;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

/**
 * Serialises changes to one vocabulary (decision D43): one fair lock per lower-case name. Waiting for the lock counts
 * towards the request timeout; when it runs out the request fails with 503 and stores nothing.
 */
@Component
public class VocabularyLocks {

    private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();
    private final Duration maxWait;

    public VocabularyLocks() {
        this(Duration.ofSeconds(10));
    }

    VocabularyLocks(Duration maxWait) {
        this.maxWait = maxWait;
    }

    /** Runs the action while holding the lock of the key. */
    public <T> T withLock(String key, Supplier<T> action) {
        ReentrantLock lock = locks.computeIfAbsent(key.toLowerCase(Locale.ROOT), k -> new ReentrantLock(true));
        boolean acquired;
        try {
            acquired = lock.tryLock(maxWait.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw Problems.storeUnavailable(e);
        }
        if (!acquired) {
            throw Problems.storeUnavailable(null);
        }
        try {
            return action.get();
        } finally {
            lock.unlock();
        }
    }
}
