package eu.simpl.vocabularymanager.service;

import eu.simpl.vocabularymanager.config.AppProperties;
import eu.simpl.vocabularymanager.problem.Problems;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

/**
 * Serialises changes to one vocabulary (decision D43) with a fixed set of lock stripes, so memory stays bounded
 * (NFR-05). Names are compared in lower case, so two creations of 'Health' and 'HEALTH' are serialised too. A
 * request that waits longer than the lock timeout gets 503 and stores nothing (decision D43).
 */
@Component
public class VocabularyLocks {

    private static final int STRIPES = 256;

    private final ReentrantLock[] stripes = new ReentrantLock[STRIPES];
    private final ReentrantLock externalLock = new ReentrantLock(true);
    private final Duration timeout;

    /**
     * Creates the locks.
     *
     * @param properties the configuration (lock timeout)
     */
    public VocabularyLocks(AppProperties properties) {
        this.timeout = properties.store().lockTimeout();
        for (int i = 0; i < STRIPES; i++) {
            stripes[i] = new ReentrantLock(true);
        }
    }

    /**
     * Runs an action while holding the lock of one vocabulary name.
     *
     * @param name the vocabulary name
     * @param action the action
     * @param <T> the result type
     * @return the action result
     * @throws eu.simpl.vocabularymanager.problem.ProblemException 503 when the lock is not free in time
     */
    public <T> T withVocabulary(String name, Supplier<T> action) {
        int index = Math.floorMod(name.toLowerCase(Locale.ROOT).hashCode(), STRIPES);
        return withLock(stripes[index], action);
    }

    /**
     * Runs an action while holding the lock of the external vocabulary registry.
     *
     * @param action the action
     * @param <T> the result type
     * @return the action result
     * @throws eu.simpl.vocabularymanager.problem.ProblemException 503 when the lock is not free in time
     */
    public <T> T withExternalRegistry(Supplier<T> action) {
        return withLock(externalLock, action);
    }

    private <T> T withLock(ReentrantLock lock, Supplier<T> action) {
        boolean acquired;
        try {
            acquired = lock.tryLock(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw Problems.serviceUnavailable(Problems.STORE_DOWN_DETAIL);
        }
        if (!acquired) {
            throw Problems.serviceUnavailable(Problems.STORE_DOWN_DETAIL);
        }
        try {
            return action.get();
        } finally {
            lock.unlock();
        }
    }
}
