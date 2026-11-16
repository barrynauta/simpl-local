package eu.simpl.cm.uibe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

/** D44: daily expiry run, OFFER_EXPIRY_CRON in UTC. */
@Component
public class ExpiryScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(ExpiryScheduler.class);

    private final OfferService offers;

    public ExpiryScheduler(OfferService offers) {
        this.offers = offers;
    }

    @Scheduled(cron = "${uibe.offer-expiry-cron}", zone = "UTC")
    public void run() {
        LOG.info("Offer expiry run: {}", offers.expire(Instant.now(), null));
    }
}
