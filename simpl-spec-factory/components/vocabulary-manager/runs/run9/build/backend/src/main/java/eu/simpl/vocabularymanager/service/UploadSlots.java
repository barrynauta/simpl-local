package eu.simpl.vocabularymanager.service;

import eu.simpl.vocabularymanager.config.VocabularyManagerProperties;
import eu.simpl.vocabularymanager.domain.ServiceBusyException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

/** At most N creates or new versions at the same time per instance (D92). */
@Component
public class UploadSlots {

    private final Semaphore slots;
    private final long waitMillis;

    /** @param properties component configuration */
    public UploadSlots(VocabularyManagerProperties properties) {
        this.slots = new Semaphore(properties.limits().maxConcurrentUploads(), true);
        this.waitMillis = properties.limits().uploadPermitWait().toMillis();
    }

    /**
     * Takes a slot, waiting a short bounded time for one to free up.
     *
     * @return the slot; close it when the upload is done
     * @throws ServiceBusyException when no slot frees up in time (503, D92)
     */
    public Slot acquire() {
        boolean acquired;
        try {
            acquired = slots.tryAcquire(waitMillis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ServiceBusyException(ServiceBusyException.UPLOADS_DETAIL, ex);
        }
        if (!acquired) {
            throw new ServiceBusyException(ServiceBusyException.UPLOADS_DETAIL);
        }
        return new Slot();
    }

    /** One taken slot. */
    public final class Slot implements AutoCloseable {
        private boolean released;

        @Override
        public void close() {
            if (!released) {
                released = true;
                slots.release();
            }
        }
    }
}
