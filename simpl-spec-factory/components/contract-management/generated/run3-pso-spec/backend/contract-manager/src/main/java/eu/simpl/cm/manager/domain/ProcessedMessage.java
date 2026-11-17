package eu.simpl.cm.manager.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** Lifecycle message ids already handled (D47 idempotency). */
@Entity
@Table(name = "processed_message")
public class ProcessedMessage {
    @Id
    @Column(length = 100)
    public String messageId;
    public Instant processedAt;
}
