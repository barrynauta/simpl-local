package eu.simpl.vocabulary.service;

import eu.simpl.vocabulary.api.problem.Problems;
import java.time.Duration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * One fair lock per vocabulary name, compared in lower case (D43). Waiting is bounded (D71): after the
 * timeout the request fails with 503 and stores nothing. Valid for one backend instance only (D81).
 * Entries are removed when no request uses them, so the map stays bounded by the number of requests in
 * flight.
 */
public class NameLocks {

    private static final class Entry {
        private final ReentrantLock lock = new ReentrantLock(true);
        private int users;
    }

    private final Map<String, Entry> entries = new HashMap<>();
    private final Duration timeout;

    public NameLocks(Duration timeout) {
        this.timeout = timeout;
    }

    /**
     * Runs the action while holding the lock of the key.
     *
     * @param key the vocabulary name or a fixed key
     * @param action the action
     * @param <T> result type
     * @return the action result
     * @throws eu.simpl.vocabulary.api.problem.ApiException 503 when the lock is not obtained in time
     */
    public <T> T withLock(String key, Supplier<T> action) {
        String normalised = key.toLowerCase(Locale.ROOT);
        Entry entry = acquireEntry(normalised);
        boolean locked = false;
        try {
            locked = entry.lock.tryLock(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!locked) {
                throw Problems.storeUnavailable();
            }
            return action.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw Problems.storeUnavailable();
        } finally {
            if (locked) {
                entry.lock.unlock();
            }
            releaseEntry(normalised, entry);
        }
    }

    private synchronized Entry acquireEntry(String key) {
        Entry entry = entries.computeIfAbsent(key, k -> new Entry());
        entry.users++;
        return entry;
    }

    private synchronized void releaseEntry(String key, Entry entry) {
        entry.users--;
        if (entry.users == 0) {
            entries.remove(key);
        }
    }

    /** Number of keys in use, for tests. */
    synchronized int size() {
        return entries.size();
    }
}
