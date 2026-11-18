package eu.simpl.vocabularymanager.service;

import eu.simpl.vocabularymanager.config.VocabularyProperties;
import eu.simpl.vocabularymanager.web.ApiException;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

/**
 * One lock per vocabulary name in lower case (D43): changes to one vocabulary run one after another; changes to
 * different vocabularies do not wait for each other. Waiting counts towards the request timeout; when it runs out
 * the request gets 503 and stores nothing (D44).
 */
@Component
public class NameLocks {

    private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();
    private final Duration timeout;

    public NameLocks(VocabularyProperties properties) {
        this.timeout = properties.lockTimeout();
    }

    /**
     * Runs the action while holding the lock of the key.
     *
     * @param key lock key, compared case-insensitively
     * @param action the change
     * @param <T> result type
     * @return the result of the action
     * @throws ApiException 503 when the lock is not obtained within the timeout
     */
    public <T> T withLock(String key, Supplier<T> action) {
        ReentrantLock lock = locks.computeIfAbsent(key.toLowerCase(Locale.ROOT), k -> new ReentrantLock(true));
        boolean locked;
        try {
            locked = lock.tryLock(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw ApiException.serviceUnavailable(VocabularyService.STORE_UNAVAILABLE);
        }
        if (!locked) {
            throw ApiException.serviceUnavailable(VocabularyService.STORE_UNAVAILABLE);
        }
        try {
            return action.get();
        } finally {
            lock.unlock();
        }
    }
}
