package eu.simpl.vocabularymanager.security;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Fetches the realm keys once at start (decision D44), without delaying or failing the start. */
@Component
public class RealmKeyWarmup {

    private final RealmKeySource keySource;

    /**
     * Creates the warm-up.
     *
     * @param keySource the realm key source
     */
    public RealmKeyWarmup(RealmKeySource keySource) {
        this.keySource = keySource;
    }

    /** Starts the first key fetch in the background. */
    @EventListener(ApplicationReadyEvent.class)
    public void fetchKeys() {
        Thread.ofVirtual().name("realm-key-warmup").start(keySource::refresh);
    }
}
