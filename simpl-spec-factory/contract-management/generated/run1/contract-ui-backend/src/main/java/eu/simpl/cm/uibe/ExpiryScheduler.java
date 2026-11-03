package eu.simpl.cm.uibe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

/** D44: scheduled offer expiry, OFFER_EXPIRY_CRON (default daily 01:00 UTC). */
@Component
public class ExpiryScheduler {
    private static final Logger log = LoggerFactory.getLogger(ExpiryScheduler.class);
    private final OfferService offers;

    public ExpiryScheduler(OfferService offers) {
        this.offers = offers;
    }

    @Scheduled(cron = "${OFFER_EXPIRY_CRON:0 0 1 * * *}", zone = "UTC")
    public void run() {
        log.info("Scheduled offer expiry run: {}", offers.expire(Instant.now(), null));
    }
}
